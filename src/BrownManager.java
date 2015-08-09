import java.io.*;
import java.util.*;

public class BrownManager {

    HashMap<String, String> brown;
    public boolean ttic;

    public BrownManager(String brownFileName, boolean ttic) throws IOException
    {
        this.ttic = ttic;
        brown = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(brownFileName)));

        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\t");
            if(parts.length >= 2) brown.put(parts[1], parts[0]);
        }

        System.err.println("BrownManager init " + brownFileName + " ttic " + (ttic ? "on" : "off"));
    }

    public String getCluster(String form, int bits) {
      if(brown.containsKey(form)) {
          String b = brown.get(form);
          if(bits == 0) return b;
          if(bits >= b.length()) return null;
          return b.substring(0, bits);
      } else if(brown.containsKey("*UNKNOWN*")) {
          String b = brown.get("*UNKNOWN*");
          if(bits == 0) return b;
          if(bits >= b.length()) return null;
          return b.substring(0, bits);
      }

      return null;
    } 
}
