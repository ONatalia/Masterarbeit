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

import java.util.Arrays;
import marytts.htsengine.HMMData.FeatureType;
/**
 * HMM model for a particular phone (or line in context feature file)
 * This model is the unit when building a utterance model sequence.
 * For every phone (or line)in the context feature file, one of these
 * models is created.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSModel {
  
  //private String name;              /* the name of this HMM, it includes ph(-2)^ph(-1)-ph(0)+ph(1)=ph(2) + context features */
  private String phoneName;         /* the name of the phone corresponding to this model, ph(0) in name */
//  private int durPdf;               /* duration pdf index for this HMM */
//  private int lf0Pdf[];             /* mel-cepstrum pdf indexes for each state of this HMM */  
//  private int mcepPdf[];            /* log f0 pdf indexes for each state of this HMM */
//  private int strPdf[];             /* str pdf indexes for each state of this HMM  */
//  private int magPdf[];             /* str pdf indexes for each state of this HMM  */

  /** the difference between the actual duration (which is discretized to full frame) and the duration proposed by CART duration modelling */ 
  private double durError;
  private int dur[];                /* duration for each state of this HMM */
  private int totalDur;             /* total duration of this HMM in frames */
  private int totalDurMillisec;     /* total duration of this model in milliseconds */
  private double lf0Mean[][];       /* mean vector of log f0 pdfs for each state of this HMM */
  private double lf0Variance[][];   /* variance (diag) elements of log f0 for each state of this HMM */
  private double mcepMean[][];      /* mean vector of mel-cepstrum pdfs for each state of this HMM */
  private double mcepVariance[][];  /* variance (diag) elements of mel-cepstrum for each state of this HMM */

  private double strMean[][];       /* mean vector of strengths pdfs for each state of this HMM */
  private double strVariance[][];   /* variance (diag) elements of strengths for each state of this HMM */
  private double magMean[][];       /* mean vector of fourier magnitude pdfs for each state of this HMM */
  private double magVariance[][];   /* variance (diag) elements of fourier magnitudes for each state of this HMM */

  private boolean voiced[];         /* voiced/unvoiced decision for each state of this HMM */
  
  private String maryXmlDur;        /* duration in maryXML input acoustparams, format d="val" in millisec. */
  private String maryXmlF0;         /* F0 values in maryXML input acoustparams, format f0="(1,val1)...(100,val2)" (%pos in total duration, f0 Hz)*/
    
  public void setPhoneName(String var){ phoneName = var; }
  public String getPhoneName(){return phoneName;}
  
  public void setDur(int i, int val){ dur[i] = val; }
  public int getDur(int i){ return dur[i]; }
  
  public void setDurError(double e) { durError = e; }
  public double getDurError() { return durError; }
  
  public void setTotalDur(int val){ totalDur = val; }
  public int getTotalDur(){return totalDur;}
  public void incrTotalDur(int val) { totalDur += val; }
  
  public void setTotalDurMillisec(int val){ totalDurMillisec = val; }
  public int getTotalDurMillisec(){return totalDurMillisec;}
  
  public void setLf0Mean(int i, int j, double val){ lf0Mean[i][j] = val; }
  public double getLf0Mean(int i, int j){ return lf0Mean[i][j]; } 
  public double[] getLf0Mean(int i) { return Arrays.copyOf(lf0Mean[i], lf0Mean[i].length); }
  public void setLf0Variance(int i, int j, double val){ lf0Variance[i][j] = val; }
  public double getLf0Variance(int i, int j){ return lf0Variance[i][j]; } 
  public double[] getLf0Variance(int i) { return Arrays.copyOf(lf0Variance[i], lf0Variance[i].length); }
  // set the vector per state
  public void setLf0Mean(int i, double val[]){ lf0Mean[i] = val; }
  public void setLf0Variance(int i, double val[]){ lf0Variance[i] = val; }
  
  
  public void setMcepMean(int i, int j, double val){ mcepMean[i][j] = val; }
  public double getMcepMean(int i, int j){ return mcepMean[i][j]; } 
  public double[] getMcepMean(int i) { return Arrays.copyOf(mcepMean[i], mcepMean[i].length); }
  public void setMcepVariance(int i, int j, double val){ mcepVariance[i][j] = val; }
  public double getMcepVariance(int i, int j){ return mcepVariance[i][j]; }
  public double[] getMcepVariance(int i) { return Arrays.copyOf(mcepVariance[i], mcepVariance[i].length); }
  // set the vector per state
  public void setMcepMean(int i, double val[]){ mcepMean[i] = val; }
  public void setMcepVariance(int i, double val[]){ mcepVariance[i] = val; }
  
  public double[] getMean(FeatureType type, int i) {
      switch (type) {
      case MCP: return Arrays.copyOf(mcepMean[i], mcepMean[i].length);
      case STR: return Arrays.copyOf(strMean[i], strMean[i].length);
      case MAG: return Arrays.copyOf(magMean[i], magMean[i].length);
      default: throw new RuntimeException("You must not ask me about DUR or LF0");
      }
  }
  
  public double[] getVariance(FeatureType type, int i) {
      switch (type) {
      case MCP: return Arrays.copyOf(mcepVariance[i], mcepVariance[i].length);
      case STR: return Arrays.copyOf(strVariance[i], strVariance[i].length);
      case MAG: return Arrays.copyOf(magVariance[i], magVariance[i].length);
      default: throw new RuntimeException("You must not ask me about DUR or LF0");
      }
  }
  
  public void printMcepMean(){  
	for(int i=0; i<mcepMean.length; i++) {
	  System.out.print("mcepMean[" + i + "]: ");
	  for(int j=0; j<mcepMean[i].length; j++)
		  System.out.print(mcepMean[i][j] + "  ");
	  System.out.println();
	}
  }
  
  public void printDuration(int numStates){
    System.out.print("phoneName: " + phoneName + "\t");
    for(int i=0; i<numStates; i++)
       System.out.print("dur[" + i + "]=" + dur[i] + " ");
    System.out.println("  totalDur=" + totalDur + "  totalDurMillisec=" + totalDurMillisec );      
  }
  
  public void setStrMean(int i, int j, double val){ strMean[i][j] = val; }
  public double getStrMean(int i, int j){ return strMean[i][j]; }
  public double[] getStrMean(int i) { return Arrays.copyOf(strMean[i], strMean[i].length); }
  public void setStrVariance(int i, int j, double val){ strVariance[i][j] = val; }
  public double getStrVariance(int i, int j){ return strVariance[i][j]; }
  public double[] getStrVariance(int i){ return strVariance[i]; }
  // set the vector per state
  public void setStrMean(int i, double val[]){ strMean[i] = val; }
  public void setStrVariance(int i, double val[]){ strVariance[i] = val; }
  
  public void setMagMean(int i, int j, double val){ magMean[i][j] = val; }
  public double getMagMean(int i, int j){ return magMean[i][j]; } 
  public double[] getMagMean(int i) { return Arrays.copyOf(magMean[i], magMean[i].length); }
  public void setMagVariance(int i, int j, double val){ magVariance[i][j] = val; }
  public double getMagVariance(int i, int j){ return magVariance[i][j]; }
  public double[] getMagVariance(int i){ return magVariance[i]; }
  // set the vector per state
  public void setMagMean(int i, double val[]){ magMean[i] = val; }
  public void setMagVariance(int i, double val[]){ magVariance[i] = val; }
  
  public void setVoiced(int i, boolean val){ voiced[i] = val; }
  /** whether state i is voiced or not */
  public boolean getVoiced(int i){ return voiced[i]; }
  public int getNumVoiced(){
      int numVoiced = 0;
      for (int i = 0; i < voiced.length; i++) {
          if (getVoiced(i))
              numVoiced += getDur(i);
      }
      return numVoiced; 
  }
  /** whether frame of the model is voiced */
  public boolean getFrameVoicing(int frame) {
      assert frame >= 0;
      assert frame < getTotalDur();
      int countFrames = 0;
      for (int i = 0; i < voiced.length; i++) {
          countFrames += getDur(i);
          if (frame < countFrames)
              return getVoiced(i);
      }
      throw new RuntimeException("bug in getFrameVoicing.");
  }
  
  public void setMaryXmlDur(String str){ maryXmlDur = str;}
  public String getMaryXmlDur(){ return maryXmlDur;}
  
  public void setMaryXmlF0(String str){ maryXmlF0 = str;}
  public String getMaryXmlF0(){ return maryXmlF0;}
  
  /* Constructor */
  /* Every Model is initialised with the information in ModelSet*/
  public HTSModel(int nstate){
	totalDur = 0;
	dur = new int[nstate];
	lf0Mean = new double[nstate][];
    lf0Variance = new double[nstate][];
    voiced = new boolean[nstate];

	mcepMean = new double[nstate][];
    mcepVariance = new double[nstate][];
	 
	strMean = new double[nstate][];
    strVariance = new double[nstate][];
    
	magMean = new double[nstate][];
    magVariance = new double[nstate][];
    
    maryXmlDur = null;
    maryXmlF0 = null;
    
  } /* method Model, initialise a Model object */
  @Override
  public String toString() {
    return getPhoneName();
  }
  
  
} /* class Model */
