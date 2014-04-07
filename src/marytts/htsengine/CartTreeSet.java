/**   
*           The HMM-Based Speech Synthesis System (HTS)             
*                       HTS Working Group                           
*                                                                   
*                  Department of Computer Science                   
*                  Nagoya Institute of Technology                   
*                               and                                 
*   Interdisciplinary Graduate School of Science and Engineering    
*                  Tokyo Institute of Technology                    
*                                                                   
*                Portions Copyright (c) 2001-2006                       
*                       All Rights Reserved.
*                         
*              Portions Copyright 2000-2007 DFKI GmbH.
*                      All Rights Reserved.                  
*                                                                   
*  Permission is hereby granted, free of charge, to use and         
*  distribute this software and its documentation without           
*  restriction, including without limitation the rights to use,     
*  copy, modify, merge, publish, distribute, sublicense, and/or     
*  sell copies of this work, and to permit persons to whom this     
*  work is furnished to do so, subject to the following conditions: 
*                                                                   
*    1. The source code must retain the above copyright notice,     
*       this list of conditions and the following disclaimer.       
*                                                                   
*    2. Any modifications to the source code must be clearly        
*       marked as such.                                             
*                                                                   
*    3. Redistributions in binary form must reproduce the above     
*       copyright notice, this list of conditions and the           
*       following disclaimer in the documentation and/or other      
*       materials provided with the distribution.  Otherwise, one   
*       must contact the HTS working group.                         
*                                                                   
*  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
*  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
*  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
*  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
*  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
*  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
*  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
*  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
*  PERFORMANCE OF THIS SOFTWARE.                                    
*                                                                   
*/

package marytts.htsengine;

import marytts.cart.CART;
import marytts.cart.Node;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.cart.io.HTSCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * Set of CART trees used in HMM synthesis.
 * 
 * some additional documentation regarding the incrementality stuff:
 * CartTreeSet contains all CARTs (dur, lf0, mcp, str, mag) that are used by a voice;
 * MAKE SURE THAT only one HMM voice is loaded during startup by disabling all but one config file 
 * 
 *  - a CartTreeSet is owned by an HMMData object owned by a HMMWaveformSynthesizer which encapsulates all data that is used during parametric synthesis 
 *    (in InproTK via inpro.synthesis.hts.PHTSParameterGeneration)
 *  - another CartTreeSet, also owned by another HMMData owned by the Voice which is in charge of duration/f0 
 *    modelling which happens already in a processing step that precedes synthesis
 * 
 * 
 * 
 * @author Marcela Charfuelan
 *
 */
public class CartTreeSet {
    
    private CART[] durTree;   // CART trees for duration 
    private CART[] lf0Tree;   // CART trees for log F0 
    private CART[] mcpTree;   // CART trees for spectrum 
    private CART[] strTree;   // CART trees for strengths 
    private CART[] magTree;   // CART trees for Fourier magnitudes

    
    private int numStates;            /* # of HMM states for individual HMM */
    
    private int lf0Stream;            /* # of stream for log f0 modeling */
    private int mcepVsize;            /* vector size for mcep modeling */
    private int strVsize;             /* vector size for strengths modeling */
    private int magVsize;             /* vector size for Fourier magnitudes modeling */
   
    final HTSCARTReader htsReader; 
    
    /** TODO: monitoredFeatures needs a comment */
    // private List<List<String>> monitoredFeatures;
   // private int[] staticFeatureCounts; // index is featureIndex in featureDef
    //private double[] weightedStaticFeatureCounts;
    //private static int[] dynamicFeatureCounts;
    //private static int[][] dynamicFeatureValueCounts; // counts, for every feature, the occurrence of the corresponding values
    //private static boolean[][] dynamicFeatureValueUsage;
    //private static FeatureVector previousFeature;
    /** CartTreeSet is used in two distinct places in Mary Code (first in overal duration/f0 targetting, second in HMM state selection) */
    private boolean isHMMCartTreeSet = false;
    /** the feature definitions used during HMM state selection */
    //private static FeatureDefinition featDef;
/*    
*/    
    public CartTreeSet() {
        htsReader = new HTSCARTReader();
//        monitoredFeatures = new ArrayList<String>();
//        monitoredFeatures.add("phone");
    }
    
    public int getNumStates(){ return numStates; }
    public int getLf0Stream(){ return lf0Stream; }
    public int getMcepVsize(){ return mcepVsize; }
    public int getStrVsize(){ return strVsize; }
    public int getMagVsize(){ return magVsize; }
    
    public int getVsize(HMMData.FeatureType type) {
        switch (type) {
        case MCP: return mcepVsize;
        case STR: return strVsize;
        case MAG: return magVsize;
        default: return 1; // DUR and LF0
        }
    }
    
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            public void run() {
////                printFeatureStatistics(dynamicFeatureCounts);
////                printFeatureValueStatistics(dynamicFeatureValueCounts);
//                System.err.println("f0 (Cent) mean:\t" + f0Stats.getMean());
//                System.err.println("f0 (Cent) stddev:\t" + f0Stats.getStandardDeviation());
//                System.err.println("f0 (Cent) error:\t" + f0Error.getMean());
//                System.err.println("f0 z-normalized error:\t" + f0Error.getMean() / f0Stats.getStandardDeviation());
//
//                System.err.println("dur (ms) mean:\t" + durStats.getMean());
//                System.err.println("dur (ms) stddev:\t" + durStats.getStandardDeviation());
//                System.err.println("dur (ms) error:\t" + durError.getMean());
//                System.err.println("dur z-normalized error:\t" + durError.getMean() / durStats.getStandardDeviation());
//
//                System.err.println("MCP Euklidian z-normalized error:\t" + aggregatedError(mcpStats, mcpError));
//                System.err.println("STR Euklidian z-normalized error:\t" + aggregatedError(strStats, strError));
//
//                System.err.println("minimum decision tree depth: " + minDepth);
//                System.err.println("maximum decision tree depth: " + maxDepth);
//            }
//        }); /**/
//    }
    
/*    private void generateStaticFeatureStatistics() {
        List<CART> allCarts = new ArrayList<CART>(Arrays.asList(durTree));
        allCarts.addAll(Arrays.asList(lf0Tree));
        allCarts.addAll(Arrays.asList(mcpTree));
        allCarts.addAll(Arrays.asList(strTree));
        assert featDef != null;
        staticFeatureCounts = new int[featDef.getNumberOfFeatures()];
        weightedStaticFeatureCounts = new double[featDef.getNumberOfFeatures()];
        dynamicFeatureCounts = new int[featDef.getNumberOfFeatures()];
        for (int i = 0; i < featDef.getNumberOfFeatures(); i++) {
            for (CART cart : allCarts) {
                assert featDef.equals(cart.getFeatureDefinition());
                staticFeatureCounts[i] += cart.countAppearanceOfFeatureAtMaxDepth(featDef.getFeatureName(i), 1);
                weightedStaticFeatureCounts[i] += cart.weightedCountAppearanceOfFeatureAtMaxDepth(featDef.getFeatureName(i), 1);
            }
        }
//        printFeatureStatistics(staticFeatureCounts);
//        printFeatureStatistics(weightedStaticFeatureCounts);
    }
    */
//    private static void printFeatureStatistics(int[] featureCounts) {
//        // get total counts (to be able to output percentages below) 
//        int total = 0; for (int val : featureCounts) total += val;
//        if (total > 0) { // ignore empty arrays
//            for (int i = 0; i < featDef.getNumberOfFeatures(); i++) {
//                System.err.println(featureCounts[i] + "\t: " + featDef.getFeatureName(i));
//            }
//            for (FeatureClasses fc : FeatureClasses.values()) {
//                int sum = 0;
//                for (String featName : featureClasses.get(fc)) {
//                    if (featDef.hasFeature(featName))
//                        sum += featureCounts[featDef.getFeatureIndex(featName)];
//                }
//                System.err.println(sum + "\t" + (sum * 100 / total) + "%\tin feature class " + fc.toString());
//            }
//        }
//    }
    
/*    private static class FeatureCount implements Comparable<FeatureCount> {
        String valueName; 
        int count;
        FeatureCount(String featureName, int count) {
            this.valueName = featureName; 
            this.count = count;
        }
        public int compareTo(FeatureCount o) {
            return Integer.compare(o.count, count);
        }
    }
    
    private static void printFeatureValueStatistics(int[][] featureValueCounts) {
        for (int i = 0; i < featDef.getNumberOfFeatures(); i++) {
            System.err.print(featDef.getFeatureName(i));
            String[] valueNames = featDef.getPossibleValues(i);
            if ((valueNames.length == 10 || valueNames.length == 20) && valueNames[8].equals("8")) {
                // numeric: calculate mean value
                int total = 0; 
                int weightedSum = 0;
                for (int j = 0; j < valueNames.length; j++) {
                    total += featureValueCounts[i][j];
                    weightedSum += featureValueCounts[i][j] * j;
                }
                System.err.println("\twas used " + total + " times.");
                System.err.println("\tmean value: " + (((float)weightedSum) / total));
//                System.err.print(Math.round(((float)weightedSum) / total) + ", ");
            } else { // non-numeric: show most frequent values
                List<FeatureCount> namedValues = new ArrayList<FeatureCount>(valueNames.length);
                int total = 0; 
                for (int j = 0; j < valueNames.length; j++) {
                    namedValues.add(new FeatureCount(valueNames[j], featureValueCounts[i][j]));
                    total += featureValueCounts[i][j];
                }
                System.err.println("\twas used " + total + " times.");
                Collections.sort(namedValues);
                if (total > 0) {
                    for (int j = 0; j < 10 && j < namedValues.size(); j++) {
                        System.err.print("\t" + namedValues.get(j).valueName + " : " + (namedValues.get(j).count * 100 / total) + "%,");
                    }
                    System.err.println();
                } else {
                    System.err.println("\tnever used at all in any encountered path along the decision trees");
                }
// use the following line for outputting the feature default list:
//                System.err.print((namedValues.size() > 0 ? featDef.getFeatureValueAsByte(i, namedValues.get(0).valueName) : 0) + ", ");
            }
        }
    }
    */
//    private static void printFeatureStatistics(double[] featureCounts) {
//        for (FeatureClasses fc : FeatureClasses.values()) {
//            double sum = 0;
//            for (String featName : featureClasses.get(fc)) {
//                if (featDef.hasFeature(featName))
//                    sum += featureCounts[featDef.getFeatureIndex(featName)];
//            }
//            System.err.println(String.format(Locale.US, "%.2f", sum) + "\t");//mass in feature class " + fc.toString());
//        }
//    }
    
    /** print statistics regarding feature use in a tree at given maximum depths */
/*    private void printMonitoredFeatureStatistics(String name, CART[] carts) {
        try {
        if (carts != null)
            for (int i = 0; i < carts.length; i++) {
                CART cart = carts[i];
                System.err.println(name + i + ": " + cart.countAppearanceOfFeatureAtMaxDepth("phone", 2));
                PrintWriter pw = new PrintWriter("/tmp/" + name + i + ".CART");
                (new MaryCARTWriter()).toTextOut(cart, pw);
                pw.close();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    private static int minDepth = Integer.MAX_VALUE;
    private static int maxDepth = Integer.MIN_VALUE;

    private static void recordFeatureUsage(Node node) {
        if (dynamicFeatureCounts == null)
            dynamicFeatureCounts = new int[featDef.getNumberOfFeatures()];
        Node it = node;
        if (it instanceof LeafNode) it = it.getMother();
        int depth = 0;
        while (it != null) {
            depth++;
            DecisionNode dn = (DecisionNode) it;
            dynamicFeatureCounts[dn.getFeatureIndex()]++;
            it = it.getMother();
        }
        if (depth < minDepth) minDepth = depth;
        if (depth > maxDepth) maxDepth = depth; 
    }
    
    private static void recordFeatureValues(FeatureVector fv) {
        if (dynamicFeatureValueCounts == null)
            dynamicFeatureValueCounts = new int[featDef.getNumberOfFeatures()][256];
        for (int i = 0; i < featDef.getNumberOfFeatures(); i++) {
            assert fv.isByteFeature(i);
            dynamicFeatureValueCounts[i][fv.getFeatureAsInt(i)]++;
        }
    }
  */
    /** this is actually annoyingly complex: record usage of the same feature in a boolean array, 
     * carry over to int array when the next feature starts to be used */
/*    private static void recordFeatureValueUsage(FeatureVector fv, Node node) {
        if (dynamicFeatureValueCounts == null)
            dynamicFeatureValueCounts = new int[featDef.getNumberOfFeatures()][256];
        if (fv != previousFeature && dynamicFeatureValueUsage != null) {
            for (int i = 0; i < dynamicFeatureValueUsage.length; i++) {
                for (int j = 0; j < dynamicFeatureValueUsage[0].length; j++) {
                    dynamicFeatureValueCounts[i][j] += dynamicFeatureValueUsage[i][j] ? 1 : 0;
                }
            }
            dynamicFeatureValueUsage = null;
        }
        if (dynamicFeatureValueUsage == null)
            dynamicFeatureValueUsage = new boolean[featDef.getNumberOfFeatures()][256];
        Node it = node;
        if (it instanceof LeafNode) it = it.getMother();
        while (it != null) {
            DecisionNode dn = (DecisionNode) it;
            int i = dn.getFeatureIndex();
            dynamicFeatureValueUsage[i][fv.getFeatureAsInt(i)] = true;
            it = it.getMother();
        }
        previousFeature = fv;
    }
    */
    /** Loads all the CART trees */
    public void loadTreeSet(HMMData htsData, FeatureDefinition featureDef, String trickyPhones) throws Exception {
      try {
        // Check if there are tricky phones, and create a PhoneTranslator object
        PhoneTranslator phTranslator = new PhoneTranslator(trickyPhones);
        /* DUR, LF0 and MCP are required as minimum for generating voice. 
        * The duration tree has only one state.
        * The size of the vector in duration is the number of states. */
        if(htsData.getTreeDurFile() != null){
          durTree = htsReader.load(1, htsData.getTreeDurFile(), htsData.getPdfDurFile(), featureDef, phTranslator);  
          numStates = htsReader.getVectorSize();
        }
        if(htsData.getTreeLf0File() != null){
          lf0Tree = htsReader.load(numStates, htsData.getTreeLf0File(), htsData.getPdfLf0File(), featureDef, phTranslator);
          lf0Stream = htsReader.getVectorSize();
        }
        if( htsData.getTreeMcpFile() != null){
          mcpTree = htsReader.load(numStates, htsData.getTreeMcpFile(), htsData.getPdfMcpFile(), featureDef, phTranslator);
          mcepVsize = htsReader.getVectorSize();
          isHMMCartTreeSet = true;
        }
        /* STR and MAG are optional for generating mixed excitation */ 
        if( htsData.getTreeStrFile() != null){
           strTree = htsReader.load(numStates, htsData.getTreeStrFile(), htsData.getPdfStrFile(), featureDef, phTranslator);
           strVsize = htsReader.getVectorSize();
           isHMMCartTreeSet = true;
        }
        if( htsData.getTreeMagFile() != null){
          magTree = htsReader.load(numStates, htsData.getTreeMagFile(), htsData.getPdfMagFile(), featureDef, phTranslator);
          magVsize = htsReader.getVectorSize();
        }
        if (isHMMCartTreeSet) {
    //        featDef = featureDef;
//            printMonitoredFeatureStatistics("mcp", mcpTree);
//            printMonitoredFeatureStatistics("str", strTree);
//            printMonitoredFeatureStatistics("lf0", lf0Tree);
//            printMonitoredFeatureStatistics("dur", durTree);
        }
      } catch (Exception e) {
        throw new Exception("LoadTreeSet failed: ", e);
      }
    }

    /** Loads duration CART */
    public void loadDurationTree(String treeDurFile, String pdfDurFile, FeatureDefinition featureDef, String trickyPhones) throws Exception {
      try {
        // Check if there are tricky phones, and create a PhoneTranslator object
        PhoneTranslator phTranslator = new PhoneTranslator(trickyPhones);
        /* The duration tree has only one state.
        * The size of the vector in duration is the number of states. */  
        durTree = htsReader.load(1, treeDurFile, pdfDurFile, featureDef, phTranslator);  
        numStates = htsReader.getVectorSize();
      } catch (Exception e) {
        throw new Exception("LoadTreeSet failed: ", e);
      }
    }

    /***
     * Searches fv in durTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param htsData HMMData with configuration settings
     * @return duration
     * @throws Exception
     */
    public double searchDurInCartTree(HTSModel m, FeatureVector fv, HMMData htsData, double diffdur) throws Exception {
        return searchDurInCartTree(m, fv, htsData, false, false, diffdur);
    }
    public double searchDurInCartTree(HTSModel m, FeatureVector fv, HMMData htsData,
            boolean firstPh, boolean lastPh, double diffdur) 
      throws Exception {     
      double data, dd;
      double rho = htsData.getRho();
      double durscale = htsData.getDurationScale();
      double meanVector[], varVector[];
      PdfLeafNode node = (PdfLeafNode) durTree[0].interpretToNode(fv, 1);
      // the duration tree has only one state
/*      if (isHMMCartTreeSet) {
          //System.err.println(node.getDecisionPath());
          recordFeatureUsage(node);
          recordFeatureValueUsage(fv, node);
      }
*/      //System.out.println("  PDF INDEX = " + ((PdfLeafNode)node).getUniqueLeafId() );  
        meanVector = node.getMean();
        varVector = node.getVariance();
          
      dd = diffdur;
      // in duration the length of the vector is the number of states.
      for(int s=0; s<numStates; s++){
        data = (meanVector[s] + rho * varVector[s] )* durscale;
        /* check if the model is initial/final pause, if so reduce the length of the pause 
         * to 10% of the calculated value. */       
//        if(m.getPhoneName().contentEquals("_") && (firstPh || lastPh ))
//          data = data * 0.1;
        m.setDur(s, (int)(data+dd+0.5));
        if(m.getDur(s) < 1 )
          m.setDur(s, 1);
        //System.out.println("   state: " + s + " dur=" + m.getDur(s) + "  dd=" + dd);               
        m.incrTotalDur(m.getDur(s));      
        dd += data - m.getDur(s);
      }
      m.setDurError(dd);
      return dd; 
    }
    
    /***
     * Searches fv in Lf0Tree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchLf0InCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef, double uvthresh) 
      throws Exception {
      for(int s=0; s<numStates; s++) {          
        PdfLeafNode node = (PdfLeafNode) lf0Tree[s].interpretToNode(fv, 1);
/*        double trueValue = lf0ToCent(node.getMean()[0]);
        if (node.getVoicedWeight() > uvthresh) {
            f0Stats.addValue(trueValue);
            f0Error.addValue(Math.abs(trueValue - lf0ToCent(node.getMean()[0])));
        }
*/        if (isHMMCartTreeSet) {
        //    recordFeatureValueUsage(fv, node);
        }
          m.setLf0Mean(s, node.getMean());
          m.setLf0Variance(s, node.getVariance());
        // set voiced or unvoiced
        if(node.getVoicedWeight() > uvthresh)
            m.setVoiced(s, true);
        else
            m.setVoiced(s,false);       
      }
    }
    
    /***
     * Searches fv in mcpTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchMcpInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef) 
      throws Exception {     
      for(int s=0; s<numStates; s++) {
        PdfLeafNode node = (PdfLeafNode) mcpTree[s].interpretToNode(fv, 1);
/*        for (int i = 0; i < 25; i++) {
            double trueValue = fullNode.getMean()[i];
            mcpStats[i].addValue(trueValue);
            mcpError[i].addValue(Math.abs(trueValue - fullNode.getMean()[i]));
        } */
      //  recordFeatureValueUsage(fv, node);
//        if (isMainHMMCartTreeSet) {
//            //System.err.println(node.getDecisionPath());
//            recordFeatureUsage(node);
//        }
        
          m.setMcepMean(s, node.getMean());
          m.setMcepVariance(s, node.getVariance());
      }
    }
    
    /***
     * Searches fv in StrTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchStrInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef) 
      throws Exception {     
      for(int s=0; s<numStates; s++) {      
          PdfLeafNode node = (PdfLeafNode) strTree[s].interpretToNode(fv, 1);
/*          for (int i = 0; i < 5; i++) {
              double trueValue = fullNode.getMean()[i];
              strStats[i].addValue(trueValue);
              strError[i].addValue(Math.abs(trueValue - croppedNode.getMean()[i]));
          }
*/      m.setStrMean(s, node.getMean());
        m.setStrVariance(s, node.getVariance());
      }
    }
    
    /***
     * Searches fv in MagTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchMagInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef) 
      throws Exception {     
      for(int s=0; s<numStates; s++) {    
        Node node = magTree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          m.setMagMean(s, ((PdfLeafNode)node).getMean());
          m.setMagVariance(s, ((PdfLeafNode)node).getVariance());
        } else
            throw new Exception("searchMagInCartTree: The node must be a PdfLeafNode");   
      }
    }

    /** 
     * creates a HTSModel (pre-HMM optimization vector data for all parameter streams of a given phoneme) given a feature vector
     * compare with original code in the main loop of marytts.modules.HTSEngine#processTargetList()   
     * @param oldErr 
     * @throws Exception 
     */  
    public HTSModel generateHTSModel(HMMData htsData, FeatureDefinition feaDef, FeatureVector fv, double oldErr) {
        HTSModel m = new HTSModel(getNumStates());
        String phoneFeature = fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef);
        m.setPhoneName(phoneFeature);
        try {
        	
            double diffDur = searchDurInCartTree(m, fv, htsData, oldErr);
            m.setDurError(diffDur);
            // m.setTotalDurMillisec((int)(fperiodmillisec * m.getTotalDur())); nobody ever uses totaldurmillisec and it's really redundant to gettotaldur

            /* Find pdf for LF0, this function sets the pdf for each state. 
             * here it is also set whether the model is voiced or not */ 
            // if ( ! htsData.getUseUnitDurationContinuousFeature() )
            // Here according to the HMM models it is decided whether the states of this model are voiced or unvoiced
            // even if f0 is taken from maryXml here we need to set the voived/unvoiced values per model and state
            searchLf0InCartTree(m, fv, feaDef, htsData.getUV());

            /* Find pdf for MCP, this function sets the pdf for each state.  */
            searchMcpInCartTree(m, fv, feaDef);

            /* Find pdf for strengths, this function sets the pdf for each state.  */
            if(htsData.getTreeStrFile() != null)
              searchStrInCartTree(m, fv, feaDef);
            
            /* Find pdf for Fourier magnitudes, this function sets the pdf for each state.  */
            if(htsData.getTreeMagFile() != null)
              searchMagInCartTree(m, fv, feaDef);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return m;
    }
    
}
