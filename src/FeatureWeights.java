import java.util.HashSet;

public class FeatureWeights {
    double []FeaturesWeights;
    double []FeaturesWeightsAvg;

    int examples;

    public FeatureWeights(int nFeatures)
    {
        FeaturesWeights = new double[nFeatures + 1];
        FeaturesWeightsAvg = new double[nFeatures + 1];
        examples = 0;
    }

    public FeatureWeights(double []weights)
    {
        FeaturesWeights = weights;
        FeaturesWeightsAvg = new double[weights.length];
        examples = 0;
    }

    public double[][] PredictArcs(int[][][] features)
    {
        return PredictArcs(features, FeaturesWeights);
    }

    public double[][] PredictArcs(int[][][] features, double []weights)
    {
        double[][] arcsWeight = new double[features.length][features.length];

        for (int iHead = 0; iHead < features.length; iHead++) {
            arcsWeight[0][iHead] = Double.NEGATIVE_INFINITY;
            for (int iModifier = 1; iModifier < features.length; iModifier++) {
                for (int iFeature = 0; iFeature < features[iModifier][iHead].length; iFeature++) {
                    arcsWeight[iModifier][iHead] += weights[(features[iModifier][iHead][iFeature]) % weights.length];
                }
            }
        }

        return arcsWeight;
    }

    public void Update(int[][][] features, int []test, int[]gold)
    {
        double diff = 0.0, loss = 0.0;
        double[] dots = new double[features.length];
        examples ++;

        for (int iModifier = 0; iModifier < features.length; iModifier++)
        {
          if(test[iModifier] == gold[iModifier]) continue;

          loss += 1.0;

          int []testFeatures = features[iModifier][test[iModifier]];
          int []goldFeatures = features[iModifier][gold[iModifier]];

          int i = 0, j = 0;

          while (i < testFeatures.length && j < goldFeatures.length)
          {
              if (testFeatures[i] < goldFeatures[j]) { i++; diff++; }
              else if (testFeatures[i] > goldFeatures[j]) { j++; diff++; }
              else { j++; i++; }
          }
 
          diff += (testFeatures.length - i);
          diff += (goldFeatures.length - j);
 
          for (int feature : goldFeatures)
              loss -= 0.3 * FeaturesWeights[(feature) % FeaturesWeights.length];
          for (int feature : testFeatures)
              loss += 0.3 * FeaturesWeights[(feature) % FeaturesWeights.length];

       }
          
        for (int iModifier = 0; iModifier < features.length; iModifier++)
        {
          if(test[iModifier] == gold[iModifier]) continue;

          int []goldFeatures = features[iModifier][gold[iModifier]];
          int []testFeatures = features[iModifier][test[iModifier]];

          double tau = 0.3 * Math.min( 0.01, loss / ( (double)diff * 0.3 * 0.3) );
          double tauAvg = (((double)examples) * tau);

            for (int goldFeature : goldFeatures)
            {
                FeaturesWeights[(goldFeature) % FeaturesWeights.length] += tau;
                FeaturesWeightsAvg[(goldFeature) % FeaturesWeights.length] += tauAvg;
            }

            for (int testFeature : testFeatures)
            {
                FeaturesWeights[(testFeature) % FeaturesWeights.length] -= tau;
                FeaturesWeightsAvg[(testFeature) % FeaturesWeights.length] -= tauAvg;
            }
        }
    }

    public double[] GetFinalWeights()
    {
        double []weights = new double[FeaturesWeights.length];

        for (int i = 0; i < FeaturesWeights.length; i++) {
            weights[i] = FeaturesWeights[i] - (examples > 0 ? (FeaturesWeightsAvg[i] / ((double)examples)) : 0.0);
        }

        return weights;
    }
}
