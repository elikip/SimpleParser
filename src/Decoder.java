
public class Decoder {

    int[] Decode(double[][] arcScores)
    {
        return Decode(arcScores, null);
    }

    int[] Decode(double[][] arcScores, int []gold)
    {
        int N = arcScores.length;
        ParseForest pf = new ParseForest(N);
        for (int i = 0; i < N; ++i) {
            pf.addItem(i, i, 1, i, -1, 0.0, null, null);
        }

        for (int l = 1; l < N; ++l)
            for (int s = 0; s + l < N; ++s) {
                int t = s + l;

                double arcST = arcScores[t][s]; //arcScores[s][t];
                double arcTS = arcScores[s][t]; //arcScores[t][s];
                int typeST = 0, typeTS = 0;

                if (gold != null) {
                    arcST += gold[t] == s ? 0.0 : 1.0;
                    arcTS += gold[s] == t ? 0.0 : 1.0;
                }

                for (int r = s; r < t; ++r) {
                    ParseForestItem x = pf.getItem(s, r, 1);
                    ParseForestItem y = pf.getItem(t,r+1, 1);
                    if (x == null || y == null) continue;

                    pf.addItem(s, t, 0, r, typeST,
                            arcST + x.score + y.score, x, y);
                    pf.addItem(t, s, 0, r, typeTS,
                            arcTS + x.score + y.score, x, y);
                }

                for (int r = s; r <= t; ++r) {

                    if (r != s) {
                        ParseForestItem x = pf.getItem(s, r, 0);
                        ParseForestItem y = pf.getItem(r, t, 1);
                        if (x == null || y == null) continue;

                        pf.addItem(s, t, 1, r, -1,
                                x.score + y.score, x, y);
                    }

                    if (r != t) {
                        ParseForestItem x = pf.getItem(r, s, 1);
                        ParseForestItem y = pf.getItem(t, r, 0);
                        if (x == null || y == null) continue;

                        pf.addItem(t, s, 1, r, -1,
                                x.score + y.score, x, y);
                    }
                }
            }


        int[] predInst = new int[arcScores.length];
        pf.getBestParse(predInst);

        return predInst;
    }
}

class ParseForest {

    public ParseForestItem[][][][] chart;
    public int K = 1;
    public int N;

    public ParseForest(int N) {
        chart = new ParseForestItem[N][N][2][K];
        this.N = N;
    }

    public void addItem(int s, int t, int comp, int r, int type,
                        double value, ParseForestItem left, ParseForestItem right) {

        if (chart[s][t][comp][K-1] == null || value > chart[s][t][comp][K-1].score) {
            ParseForestItem item = new ParseForestItem(s, t, comp, r, type, value, left, right);

            int i = K-1;
            while (i > 0 && (chart[s][t][comp][i-1] == null || value > chart[s][t][comp][i-1].score)) {
                chart[s][t][comp][i] = chart[s][t][comp][i-1];
                --i;
            }
            chart[s][t][comp][i] = item;
        }
    }

    public ParseForestItem getItem(int s, int t, int comp) {
        return chart[s][t][comp][0];
    }

    public void getBestParse(int []predInst) {
        getDeps(predInst, chart[0][N-1][1][0]);
    }

    private void getDeps(int []predInst, ParseForestItem item) {

        if (item == null || item.s == item.t) return;

        getDeps(predInst, item.left);
        getDeps(predInst, item.right);

        if (item.comp != 1) {
            predInst[item.t] = item.s;
        }
    }
}

class ParseForestItem {

    int s, t, comp, r, type;
    double score;
    ParseForestItem left, right;

    public ParseForestItem(int s, int t, int comp, int r, int type,
                           double value, ParseForestItem left, ParseForestItem right) {
        this.s = s;
        this.t = t;
        this.comp = comp;
        this.r = r;
        this.type = type;
        this.score = value;
        this.left = left;
        this.right = right;
    }
}
