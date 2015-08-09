import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PmiManager implements DataManager 
{ 
    HashMap<String, Double> data;
    HashMap<String, Double> contexts;
    HashMap<String, Double> words;

    double totalCount = 0.0;

    public static PmiManager instance = null;

    public static PmiManager Instance(String countsFile) throws IOException
    {
        if(instance == null) instance = new PmiManager(countsFile);
        return instance;
    }

    public PmiManager(String countsFile) throws IOException {

        System.err.println("PmiManager init " + countsFile);

        String line;

        data = new HashMap<String, Double>();
        words = new HashMap<String, Double>();
        contexts = new HashMap<String, Double >();

        BufferedReader reader = new BufferedReader(new FileReader(countsFile));

        reader.readLine();

        while ((line = reader.readLine()) != null) {
            String[] parts = line.toLowerCase().trim().split(" ");
            
            double count = (double) Long.parseLong(parts[0]);

            totalCount += count;

            data.put(parts[1] + " " + parts[2], count);
            words.put(parts[1], count + (words.containsKey(parts[1]) ? words.get(parts[1]) : 0.0));
            contexts.put(parts[2], count + (contexts.containsKey(parts[2]) ? contexts.get(parts[2]) : 0.0));

        }

        reader.close();
    }

    public double Predict(String hPrev, String hCurr, String hNext, String mPrev, String mCurr, String mNext, int offset)
    {
        String word = (hPrev.isEmpty() ? "" : hPrev + "_-1") + (hCurr.isEmpty() ? "" : hCurr + "_0") + (hNext.isEmpty() ? "" : hNext + "_1");
        String context =  (mPrev.isEmpty() ? "" : "-1_" + mPrev) + (mCurr.isEmpty() ? "" : "0_" + mCurr) + (mNext.isEmpty() ? "" : "1_" + mNext); 


        word = word.toLowerCase();
        context = context.toLowerCase();
        String key = word + " " + context;
        if(!data.containsKey(key)) return 1.1;
        double result = data.get(key);

        result /= words.get(word);
        result /= contexts.get(context);

        result *= totalCount;
        result = 1.0 / ( 1.0 + (1.0/result));

        //if(result > 0.1) 
        //    System.err.println("PMI " + result + " " + key + " " + data.get(key) + " " + words.get(word) + " " + contexts.get(context));


        return result;
    }

}
