import java.io.*;
import java.util.*;

public class CONLLReader implements Iterable<ArrayList<String []>> {
    String conllPath;

    public CONLLReader(String conllPath)
    {
        this.conllPath = conllPath;
    }

    public Iterator<ArrayList<String []>> iterator(){
        try {
            return new Iterator<ArrayList<String []>>() {
                BufferedReader conllFP;
                String line;
                ArrayList<String []> current;

                public Iterator<ArrayList<String []>> Init(String conllPath) throws IOException {
                    conllFP = new BufferedReader(new FileReader(conllPath));
                    current = null;
                    return this;
                }

                @Override
                public boolean hasNext() {
                    if(current != null) return true;

                    current = new ArrayList<String []>();

                    try {
                        while((line = conllFP.readLine()) != null && !line.trim().isEmpty())
                        {
                            String []parts = line.split("\t");
                            current.add(parts);
                        }
                    } catch (IOException e) {
                        line = null;
                    }

                    if(current.size() == 0) current = null;
                    else current.add(0, new String[]{"0", "*ROOT*", "", "ROOT-CPOS", "ROOT-POS", "", "0", "", "", ""});
                    return current != null;
                }

                @Override
                public ArrayList<String []> next() {
                    ArrayList<String []> result = current;
                    current = null;
                    return result;
                }

            }.Init(conllPath);
        } catch (IOException e) {
            return null;
        }
    }

    public static String normalize (String s) {
        if(s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+"))
            return "<num>";

        return s;
    }
}
