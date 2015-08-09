import java.io.*;
import java.util.*;

public class Parser {

    static FeatureExtractor featureExtractor;
    static FeatureWeights featureWeights;
    static Decoder decoder = new Decoder();
    static DataManager dm;
    static BrownManager bm;

    static DataManager getDataManager(HashMap<String, String> params) throws IOException
    {
        dm = null;

        if(params.containsKey("vec") && params.containsKey("cxt")) {
            dm = EmbeddingsManager.Instance(params.get("vec"), params.get("cxt"));
        } else if(params.containsKey("counts")) {
            dm = CountsManager.Instance(params.get("counts"));
        } else if(params.containsKey("pmi")) {
            dm = PmiManager.Instance(params.get("pmi"));
        }
        
        return dm;
    }

    static void trainIter(String trainFileName)
    {
        for( ArrayList<String[]> sentence : new CONLLReader(trainFileName) )
        {
            int[][][] sentenceFeatures = featureExtractor.Extract(sentence, bm, dm, false);
            int[] gold = sentence.stream().mapToInt(s -> Integer.parseInt(s[6])).toArray();

            //synchronized (FeatureWeights.class) {
                double[][] arcScores = featureWeights.PredictArcs(sentenceFeatures);
                int[] deps = decoder.Decode(arcScores, gold);
                featureWeights.Update(sentenceFeatures, deps, gold);
            //}
        }
    }

    static void loadModel(String modelFileName) throws IOException
    {
        BufferedReader modelFP = new BufferedReader(new FileReader(modelFileName));

        ArrayList<Double> featuresWeights = new ArrayList<Double>();
        ArrayList<String> featuresName = new ArrayList<String>();

        String line = null;
        while((line = modelFP.readLine()) != null)
        {
            String[] parts = line.split(" ", 2);
            featuresWeights.add(Double.parseDouble(parts[0]));
            featuresName.add(parts[1]);
        }

        modelFP.close();

        double[] weights = new double[featuresWeights.size()];

        int iFeature = 0;
        for(Double weight : featuresWeights)
            weights[iFeature++] = weight;

        featureExtractor = new FeatureExtractor(featuresName);
        featureWeights = new FeatureWeights(weights);

    }

    public static void main(String[] args) throws IOException {

        HashMap<String,String> params = new HashMap<String, String>();
        for(String arg : args)
        {
            String[] argParts = arg.split(":", 2);
            params.put(argParts[0], argParts[1]);
        }

        long sTime = System.currentTimeMillis();

        dm = getDataManager(params);
        bm = params.containsKey("brown") ? new BrownManager(params.get("brown"), false) : null;
        bm = params.containsKey("ttic") ? new BrownManager(params.get("ttic"), true) : bm;
        double[] weights = null;

        if(params.containsKey("train-file")) {
            featureExtractor = new FeatureExtractor();

            for( ArrayList<String[]> sentence : new CONLLReader(params.get("train-file")) ) 
                featureExtractor.Extract(sentence, bm, dm, true);

            System.out.println(featureExtractor.NumberOfFeatures() + " features");
            System.out.println();
            featureWeights = new FeatureWeights(featureExtractor.NumberOfFeatures());

            int iters = params.containsKey("iters") ? Integer.parseInt(params.get("iters")) : 10;
            for (int iter = 0; iter < iters; iter++) {
                System.err.println("Time: " + ((System.currentTimeMillis()-sTime)/1000) + ". Starting iteration " + iter);
                trainIter(params.get("train-file"));
           }

           weights = featureWeights.GetFinalWeights();

           PrintStream model = new PrintStream(params.get("model"));

           for(Map.Entry<String, Integer> featureEntry : featureExtractor.getFeatureIndex().entrySet())
           {
               model.print(weights[featureEntry.getValue()]);
               model.print(" ");
               model.println(featureEntry.getKey());
           }

           model.close();

        } else {
            loadModel(params.get("model"));
 
            System.out.println(featureExtractor.NumberOfFeatures() + " features");
            System.out.println();
           
            weights = featureWeights.GetFinalWeights();
       }

        System.err.println("Time: " + ((System.currentTimeMillis()-sTime)/1000) + ". Evaluating...");

        PrintStream out = params.containsKey("out-file")? new PrintStream(params.get("out-file")) : System.out;

        for(ArrayList<String []> sentence : new CONLLReader(params.get("test-file")))
        {
            int[][][] testSentenceFeatures = featureExtractor.Extract(sentence, bm, dm, false);
            int[] gold = sentence.stream().mapToInt(s -> Integer.parseInt(s[6])).toArray();
            int[] deps = decoder.Decode(featureWeights.PredictArcs(testSentenceFeatures, weights));

            for (int i = 1; i < gold.length; i++) {
                String []conllEntry = sentence.get(i);
                conllEntry[6] = String.valueOf(deps[i]);
                out.println(String.join("\t", conllEntry));
            }

            out.println();
        }

        out.flush();
        System.err.println("Time: " + ((System.currentTimeMillis()-sTime)/1000) + ".");
    }
}
