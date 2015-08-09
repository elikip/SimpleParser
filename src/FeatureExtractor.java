import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class FeatureExtractor {
    
    HashMap<String, Integer> FeaturesIndex = new HashMap<>();

    public int NumberOfFeatures()
    {
        return FeaturesIndex.size();
    }

    public AbstractMap<String, Integer> getFeatureIndex()
    {
        return FeaturesIndex;
    }

    public FeatureExtractor() { }

    public FeatureExtractor(List<String> features) 
    { 
        int iFeature = 0;

        for(String feature : features) {
            FeaturesIndex.put(feature, iFeature++);
        }
    }
    
    /**
     * "H"	: head
     * "M"	: modifier
     * "B"	: in-between tokens
     *
     * "P"	: pos tag
     * "W"	: word form or lemma
     * "EMB": word embedding (word vector)
     *
     * "p": previous token
     * "n": next token
     */

    public int[][][] Extract(ArrayList<String []> sentenceNotNormalized, BrownManager bm, DataManager dm, boolean grow)
    {
        int[][][] result = new int[sentenceNotNormalized.size()][sentenceNotNormalized.size()][];

        HashMap<String, int []> posAccumulative  = new HashMap<String, int []>();
        HashMap<String, int []> cposAccumulative = new HashMap<String, int []>();

        final ArrayList<String []> sentence = new ArrayList<String[]>();

        for(String [] conllEntry : sentenceNotNormalized)
        {
            String []conllEntryClone = conllEntry.clone();
//            System.out.println(Arrays.toString(conllEntryClone));
            conllEntryClone[1] = CONLLReader.normalize(conllEntryClone[1]);
            sentence.add(conllEntryClone);
        }


        for (int i = 0; i < sentence.size(); i++) {
            String []entry = sentence.get(i);
            cposAccumulative.computeIfAbsent(entry[3], p -> (new int[sentence.size()]));
            posAccumulative.computeIfAbsent(entry[4], p -> (new int[sentence.size()]));
        }


        for(Map.Entry<String, int[]> entry: posAccumulative.entrySet()) {
            int recurrences = 0;
            String pos = entry.getKey();
            int []accumulative = entry.getValue();

            for (int i = 0; i < accumulative.length; i++) {
                recurrences += (sentence.get(i)[4].equals(pos) ? 1 : 0);
                accumulative[i] = recurrences;
            }
        }

        for(Map.Entry<String, int[]> entry: cposAccumulative.entrySet()) {
            int recurrences = 0;
            String cpos = entry.getKey();
            int []accumulative = entry.getValue();

            for (int i = 0; i < accumulative.length; i++) {
                recurrences += (sentence.get(i)[3].equals(cpos) ? 1 : 0);
                accumulative[i] = recurrences;
            }
        }
        String[] root = new String[]{"0", "*ROOT*", "", "ROOT-CPOS", "ROOT-POS", "", "0", "", "", ""};
        String[] end  = new String[]{"0", "*END*", "", "END-CPOS", "END-POS", "", "", "", "", ""};

        for (int iModifier = 0; iModifier < sentence.size(); iModifier++) {
            int head = Integer.parseInt(sentence.get(iModifier)[6]);
            for (int iHead = 0; iHead < sentence.size(); iHead++) {
                if(iHead == iModifier || iModifier == 0 || (grow && iHead != head))
                {
                    result[iModifier][iHead] = new int[0];
                    continue;
                }

                String direction = iModifier > iHead ? "R" : "L";
                int distance = Math.abs(iModifier - iHead);
                int binnedDistance = distance >= 10 ? 10 : (distance >= 5 ? 5 : distance);


                String[] modEntryPrev = iModifier > 0 ? sentence.get(iModifier - 1) : root;
                String[] modEntry = sentence.get(iModifier);
                String[] modEntryNext = iModifier == sentence.size() - 1 ? end : sentence.get(iModifier + 1);

                String[] headEntryPrev = iHead > 0 ? sentence.get(iHead - 1) : root;
                String[] headEntry = sentence.get(iHead);
                String[] headEntryNext = iHead == sentence.size() - 1 ? end : sentence.get(iHead + 1);

                Vector<Integer> fv = new Vector<Integer>();

                if(Math.abs(iModifier - iHead) > 0) {
                    for (Map.Entry<String, int[]> entry : posAccumulative.entrySet()) {
                        int[] accumulative = entry.getValue();
                        String pos = entry.getKey();

                        int recurrences = accumulative[Math.max(iModifier, iHead) - 1] - accumulative[Math.min(iModifier, iHead)];
                        for (int i = 0; i < recurrences; i++) {
                        //if (recurrences >0 ){
                            AddFeature(fv, grow, "HP_BP_MP", headEntry[4], pos, modEntry[4]);//, String.valueOf(recurrences >= 7 ? 7 : (recurrences >= 3 ? 3 : recurrences)));
                            AddFeature(fv, grow, "HP_BP_MP", ""+binnedDistance, direction, headEntry[4], pos, modEntry[4]);//, String.valueOf(recurrences >= 7 ? 7 : (recurrences >= 3 ? 3 : recurrences)));
                        }
                    }

                    for (Map.Entry<String, int[]> entry : cposAccumulative.entrySet()) {
                        int[] accumulative = entry.getValue();
                        String pos = entry.getKey();

                        int recurrences = accumulative[Math.max(iModifier, iHead) - 1] - accumulative[Math.min(iModifier, iHead)];
                        for (int i = 0; i < recurrences; i++) {
                        //if(recurrences>0){
                            AddFeature(fv, grow, "HP_BP_MP", headEntry[3], pos, modEntry[3]);//, String.valueOf(recurrences >= 7 ? 7 : (recurrences >= 3 ? 3 : recurrences)));
                            AddFeature(fv, grow, "HP_BP_MP", ""+binnedDistance, direction, headEntry[3], pos, modEntry[3]);//, String.valueOf(recurrences >= 7 ? 7 : (recurrences >= 3 ? 3 : recurrences)));
                        }
                    }
                }

                for(String distStr : new String[]{"", "" + binnedDistance + direction}) {
                    for(int posInd : new int[]{3, 4})
                    {
                        AddFeature(fv, grow, "HPp_HP_MP_MPn", distStr, headEntryPrev[posInd], headEntry[posInd], modEntry[posInd], headEntryNext[posInd]);
                        AddFeature(fv, grow, "HP_MP_MPn", distStr, headEntry[posInd], modEntry[posInd], modEntryNext[posInd]);
                        AddFeature(fv, grow, "HPp_HP_MP", distStr, headEntryPrev[posInd], headEntry[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "HPp_MP_MPn", distStr, headEntryPrev[posInd], modEntry[posInd], modEntryNext[posInd]);
                        AddFeature(fv, grow, "HPp_HP_MPn", distStr, headEntryPrev[posInd], headEntry[posInd], modEntryNext[posInd]);

                        AddFeature(fv, grow, "HP_HPn_MPp_MP", distStr, headEntry[posInd], headEntryNext[posInd], modEntryPrev[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "HP_MPp_MP", distStr, headEntry[posInd], modEntryPrev[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "HP_HPn_MP", distStr, headEntry[posInd], headEntryNext[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "HPn_MPp_MP", distStr, headEntryNext[posInd], modEntryPrev[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "HP_HPn_MPp", distStr, headEntry[posInd], headEntryNext[posInd], modEntryPrev[posInd]);

                        AddFeature(fv, grow, "HPp_HP_MPp_MP", distStr, headEntryPrev[posInd], headEntry[posInd], modEntryPrev[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "HP_HPn_MP_MPn", distStr, headEntry[posInd], headEntryNext[posInd], modEntry[posInd], modEntryNext[posInd]);

                        AddFeature(fv, grow, "HW_MW_HP_MP", distStr, headEntry[1], modEntry[1], headEntry[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "MW_HP_MP", distStr, modEntry[1], headEntry[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "HW_HP_MP", distStr, headEntry[1], headEntry[posInd], modEntry[posInd]);
                        AddFeature(fv, grow, "MW_HP", distStr, modEntry[1], headEntry[posInd]);
                        AddFeature(fv, grow, "HW_MP", distStr, headEntry[1], modEntry[posInd]);
                        AddFeature(fv, grow, "HW_HP", distStr, headEntry[1], headEntry[posInd]);
                        AddFeature(fv, grow, "MW_MP", distStr, modEntry[1], modEntry[posInd]);
                        AddFeature(fv, grow, "HP_MP", distStr, headEntry[posInd], modEntry[posInd]);
                    
                        AddFeature(fv, grow, "CORE_MOD_POS", distStr, modEntry[posInd]);
                        AddFeature(fv, grow, "CORE_HEAD_POS", distStr, headEntry[posInd]);

                        if(bm != null) {
                            AddFeature(fv, grow, "HBROWN4_MPOS", distStr, bm.getCluster(headEntry[1], 4), modEntry[posInd]);
                            AddFeature(fv, grow, "HBROWN6_MPOS", distStr, bm.getCluster(headEntry[1], 8), modEntry[posInd]);
                            AddFeature(fv, grow, "HBROWN8_MPOS", distStr, bm.getCluster(headEntry[1], 8), modEntry[posInd]);
                            if(!bm.ttic) AddFeature(fv, grow, "HBROWN10_MPOS", distStr, bm.getCluster(headEntry[1], 10), modEntry[posInd]);
                            if(bm.ttic)
                            {
                                AddFeature(fv, grow, "HBROWN12_MPOS", distStr, bm.getCluster(headEntry[1], 12), modEntry[posInd]);
                                AddFeature(fv, grow, "HBROWN16_MPOS", distStr, bm.getCluster(headEntry[1], 16), modEntry[posInd]);
                                AddFeature(fv, grow, "HBROWN20_MPOS", distStr, bm.getCluster(headEntry[1], 20), modEntry[posInd]);
                            }
                            AddFeature(fv, grow, "HBROWN*_MPOS", distStr, bm.getCluster(headEntry[1], 0), modEntry[posInd]);
 
                            AddFeature(fv, grow, "HPOS_MBROWN4", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 4));
                            AddFeature(fv, grow, "HPOS_MBROWN6", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 8));
                            AddFeature(fv, grow, "HPOS_MBROWN8", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 8));
                            if(!bm.ttic) AddFeature(fv, grow, "HPOS_MBROWN10", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 10));
                            if(bm.ttic)
                            {
                                AddFeature(fv, grow, "HPOS_MBROWN12", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 12));
                                AddFeature(fv, grow, "HPOS_MBROWN16", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 16));
                                AddFeature(fv, grow, "HPOS_MBROWN20", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 20));
                            }
                            AddFeature(fv, grow, "HPOS_MBROWN*", distStr, headEntry[posInd], bm.getCluster(modEntry[1], 0));
                        }
                    }

                    if(bm != null) {
                        AddFeature(fv, grow, "HBROWN4_MBROWN4", distStr, bm.getCluster(headEntry[1],4), bm.getCluster(modEntry[1], 4));
                        AddFeature(fv, grow, "HBROWN6_MBROWN6", distStr, bm.getCluster(headEntry[1],6), bm.getCluster(modEntry[1], 6));
                        AddFeature(fv, grow, "HBROWN8_MBROWN8", distStr, bm.getCluster(headEntry[1],8), bm.getCluster(modEntry[1], 8));
                        if(!bm.ttic) AddFeature(fv, grow, "HBROWN10_MBROWN10", distStr, bm.getCluster(headEntry[1],10), bm.getCluster(modEntry[1], 10));
                        if(bm.ttic)
                        {
                            AddFeature(fv, grow, "HBROWN12_MBROWN12", distStr, bm.getCluster(headEntry[1],12), bm.getCluster(modEntry[1], 12));
                            AddFeature(fv, grow, "HBROWN16_MBROWN16", distStr, bm.getCluster(headEntry[1],16), bm.getCluster(modEntry[1], 16));
                            AddFeature(fv, grow, "HBROWN20_MBROWN20", distStr, bm.getCluster(headEntry[1],20), bm.getCluster(modEntry[1], 20));
                        }
                        AddFeature(fv, grow, "HBROWN*_MBROWN*", distStr, bm.getCluster(headEntry[1],0), bm.getCluster(modEntry[1], 0));

                        AddFeature(fv, grow, "HBROWN4_MW", distStr, bm.getCluster(headEntry[1],4), modEntry[1]);
                        AddFeature(fv, grow, "HBROWN6_MW", distStr, bm.getCluster(headEntry[1],6), modEntry[1]);
                        AddFeature(fv, grow, "HBROWN8_MW", distStr, bm.getCluster(headEntry[1],8), modEntry[1]);
                        if(!bm.ttic) AddFeature(fv, grow, "HBROWN10_MW", distStr, bm.getCluster(headEntry[1],10), modEntry[1]);
                        if(bm.ttic)
                        {
                            AddFeature(fv, grow, "HBROWN12_MW", distStr, bm.getCluster(headEntry[1],12), modEntry[1]);
                            AddFeature(fv, grow, "HBROWN16_MW", distStr, bm.getCluster(headEntry[1],16), modEntry[1]);
                            AddFeature(fv, grow, "HBROWN20_MW", distStr, bm.getCluster(headEntry[1],20), modEntry[1]);
                        }
                        AddFeature(fv, grow, "HBROWN*_MW", distStr, bm.getCluster(headEntry[1],0), modEntry[1]);

                        AddFeature(fv, grow, "HW_MBROWN4", distStr, headEntry[1], bm.getCluster(modEntry[1], 4));
                        AddFeature(fv, grow, "HW_MBROWN6", distStr, headEntry[1], bm.getCluster(modEntry[1], 6));
                        AddFeature(fv, grow, "HW_MBROWN8", distStr, headEntry[1], bm.getCluster(modEntry[1], 8));
                        if(!bm.ttic) AddFeature(fv, grow, "HW_MBROWN10", distStr, headEntry[1], bm.getCluster(modEntry[1], 10));
                        if(bm.ttic)
                        {
                            AddFeature(fv, grow, "HW_MBROWN12", distStr, headEntry[1], bm.getCluster(modEntry[1], 12));
                            AddFeature(fv, grow, "HW_MBROWN16", distStr, headEntry[1], bm.getCluster(modEntry[1], 16));
                            AddFeature(fv, grow, "HW_MBROWN20", distStr, headEntry[1], bm.getCluster(modEntry[1], 20));
                        }
                        AddFeature(fv, grow, "HW_MBROWN*", distStr, headEntry[1], bm.getCluster(modEntry[1], 0));
                    }

                    AddFeature(fv, grow, "CORE_HEAD_WORD", distStr, headEntry[1]);
                    AddFeature(fv, grow, "CORE_MOD_WORD", distStr, modEntry[1]);
                    AddFeature(fv, grow, "CORE_HEAD_pWORD", distStr, headEntryPrev[1]);
                    AddFeature(fv, grow, "CORE_HEAD_nWORD", distStr, headEntryNext[1]);
                    AddFeature(fv, grow, "CORE_MOD_pWORD", distStr, modEntryPrev[1]);
                    AddFeature(fv, grow, "CORE_MOD_nWORD", distStr, modEntryNext[1]);

                    AddFeature(fv, grow, "HW_MW", distStr, headEntry[1], modEntry[1]);

                    if(dm != null) {
                        double embd = dm.Predict("", headEntry[1], "", modEntryPrev[1], "", "", binnedDistance);
                        if(embd != 1.1)
                        {
                            AddFeature(fv, grow, "ATP_0_-1", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_0_-1", distStr, headEntry[4], modEntryPrev[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict("", headEntry[1], "", "", modEntry[1], "", binnedDistance);
                        if(embd != 1.1)
                        {
                            AddFeature(fv, grow, "ATP_0_0", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_0_0", distStr, headEntry[4], modEntry[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict("", headEntry[1], "", "", "", modEntryNext[1], binnedDistance);
                        if(embd != 1.1)
                        {
                            AddFeature(fv, grow, "ATP_0_1", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_0_1", distStr, headEntry[4], modEntryNext[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict(headEntryPrev[1], "", "", modEntryPrev[1], "", "", binnedDistance);
                        if(embd != 1.1)     
                        {
                            AddFeature(fv, grow, "ATP_-1_-1", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_-1_-1", distStr, headEntryPrev[4], modEntryPrev[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict(headEntryPrev[1], "", "", "", modEntry[1], "", binnedDistance);
                        if(embd != 1.1)     
                        {
                            AddFeature(fv, grow, "ATP_-1_0", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_-1_0", distStr, headEntryPrev[4], modEntry[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict(headEntryPrev[1], "", "", "", "", modEntryNext[1], binnedDistance);
                        if(embd != 1.1)     
                        {
                            AddFeature(fv, grow, "ATP_-1_1", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_-1_1", distStr, headEntryPrev[4], modEntryNext[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict("", "", headEntryNext[1], modEntryPrev[1], "", "", binnedDistance);
                        if(embd != 1.1)
                        {
                            AddFeature(fv, grow, "ATP_1_-1", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_1_-1", distStr, headEntryNext[4], modEntryPrev[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict("", "", headEntryNext[1], "", modEntry[1], "", binnedDistance);
                        if(embd != 1.1)
                        {
                            AddFeature(fv, grow, "ATP_1_0", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_1_0", distStr, headEntryNext[4], modEntry[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }

                        embd = dm.Predict("", "", headEntryNext[1], "", "", modEntryNext[1], binnedDistance);
                        if(embd != 1.1)
                        {
                            AddFeature(fv, grow, "ATP_1_1", distStr, String.valueOf(Math.round(embd * 10.0) / 10.0) );
                            AddFeature(fv, grow, "ATP_1_1", distStr, headEntryNext[4], modEntryNext[4], String.valueOf(Math.round(embd * 10.0) / 10.0) );
                        }
                    }
                }

                result[iModifier][iHead] = new int[fv.size()];
                int iFeature = 0;

                for(int feature : fv)
                    result[iModifier][iHead][iFeature++] = feature;

                Arrays.sort(result[iModifier][iHead]);
            }
        }

        return result;
    }

    public void AddFeature(Vector<Integer> fv, boolean grow, String... parts)
    {
        String featureStr = String.join(" ", parts);
        if(grow && !FeaturesIndex.containsKey(featureStr))
            FeaturesIndex.put(featureStr, FeaturesIndex.size());
        if(FeaturesIndex.containsKey(featureStr))
            fv.add(FeaturesIndex.get(featureStr));
    }
}

