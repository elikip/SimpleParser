import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public  interface DataManager {

    public double Predict(String hPrev, String hCurr, String hNext, String mPrev, String mCurr, String mNext, int offset);

}
