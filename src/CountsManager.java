import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CountsManager implements DataManager 
{ 
    HashMap<String, Double> data;

    public static CountsManager instance = null;

    public static CountsManager Instance(String countsFile) throws IOException
    {
        if(instance == null) instance = new CountsManager(countsFile);
        return instance;
    }


    public CountsManager(String countsFile) throws IOException {

        System.err.println("CountsManager init " + countsFile);

        BufferedReader reader = new BufferedReader(new FileReader(countsFile));
        String line;

    	double nLines = 0;
        
        while ((line = reader.readLine()) != null) 
		    nLines += 1.0;

        reader.close();

        data = new HashMap<String, Double>();

        reader = new BufferedReader(new FileReader(countsFile));

    	double count = 0;
        reader.readLine();

        while ((line = reader.readLine()) != null) {
            line = line.toLowerCase();
            String[] parts = line.trim().split(" ");
            data.put((parts[1] + " " + parts[2]).toLowerCase(), 1.0 - (count/nLines));
//            System.err.println("CountsManager Init " + (parts[1] + " " + parts[2]).toLowerCase());
            count += 1.0;
        }

        reader.close();
    }

    public double Predict(String hPrev, String hCurr, String hNext, String mPrev, String mCurr, String mNext, int offset)
    {
        String word = (hPrev.isEmpty() ? "" : hPrev + "_-1") + (hCurr.isEmpty() ? "" : hCurr + "_0") + (hNext.isEmpty() ? "" : hNext + "_1");
        String cxt =  (mPrev.isEmpty() ? "" : "-1_" + mPrev) + (mCurr.isEmpty() ? "" : "0_" + mCurr) + (mNext.isEmpty() ? "" : "1_" + mNext); 

        String key = word + " " + cxt;
        key = key.toLowerCase();
	    double result = data.containsKey(key) ? data.get(key) : 1.1;

 //       if(result < 1.0)
 //       System.err.println("CountsManager " + key + " " + result);

        return result;
    }

}
