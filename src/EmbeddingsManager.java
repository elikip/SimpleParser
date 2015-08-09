import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class EmbeddingsManager implements DataManager { 
    HashMap<String, double[]> words;
    HashMap<String, double[]> contexts;

    int dims;

    public static EmbeddingsManager instance = null;

    public static EmbeddingsManager Instance(String wordsFileName, String cxtsFileName) throws IOException
    {
        if(instance == null) instance = new EmbeddingsManager(wordsFileName, cxtsFileName);
        return instance;
    }

    public EmbeddingsManager(String wordsFileName, String cxtsFileName) throws IOException {
        String line;
        this.dims = 0;

        System.err.println("EmbeddingsManager init " + wordsFileName + " cxt " + cxtsFileName);
        words = new HashMap<String, double[]>();
        contexts = new HashMap<String, double[]>();

        BufferedReader reader = new BufferedReader(new FileReader(wordsFileName));
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            this.dims = parts.length - 1;
            double[] vec = new double[dims];

            for (int i = 0; i < dims; i++)
                vec[i] = Double.parseDouble(parts[i + 1]);

            words.put(parts[0].toLowerCase(), vec);
        }

        reader = new BufferedReader(new FileReader(cxtsFileName));
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            this.dims = parts.length - 1;
            double[] vec = new double[dims];

            for (int i = 0; i < dims; i++)
                vec[i] = Double.parseDouble(parts[i + 1]);

            contexts.put(parts[0].toLowerCase(), vec);
        }
    }

    public double Predict(String hPrev, String hCurr, String hNext, String mPrev, String mCurr, String mNext, int offset)
    {
        double []headp = words.get(hPrev + "_-1");
        double []heado = words.get(hCurr + "_0");
        double []headn = words.get(hNext + "_1");

        double []modifierp = contexts.get("-1_" + mPrev);
        double []modifiero = contexts.get("0_"  + mCurr);
        double []modifiern = contexts.get("1_"  + mNext);

        if((headp == null && heado == null && headn == null) || (modifierp == null && modifiero == null && modifiern == null))
            return 1.1;

        double sum = 0.0;

        for(int i = 0 ; i < dims ; i++)
            sum += 
                ( ( (headp == null? 0.0 :headp[i]) + (heado == null? 0.0 :heado[i]) + (headn == null? 0.0 :headn[i]) ) *
                  ( (modifierp == null? 0.0 :modifierp[i]) + (modifiero == null? 0.0 :modifiero[i]) + (modifiern == null? 0.0 :modifiern[i]) ) );

        double result = (1.0 / (1.0 + Math.exp(-sum)));
        return result;
    }

}
