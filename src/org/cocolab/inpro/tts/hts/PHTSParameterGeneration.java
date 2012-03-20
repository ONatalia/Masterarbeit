package org.cocolab.inpro.tts.hts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSPStream;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.htsengine.HMMData.FeatureType;



public class PHTSParameterGeneration extends HTSParameterGeneration {

    FullPStream outputFeatureStream = new ListBackedFullPStream();

    private boolean[] voiced;
    
    private final HMMData htsData; 
    
    public PHTSParameterGeneration(HMMData htsData) {
    	this.htsData = htsData;
	}
    
    /** 
     * incremental formulation for parameter generation, following pHTS
     * however, this interface is totaly non-incremental and will have to be replaced soon 
     * */
    public void phtsIncrementalParameterGeneration(HTSUttModel um) throws Exception {
        // foreach phone triplet
        // we start at 1 and end at max-1, because we're talking triplets
        int numModels = um.getNumUttModel() - 2;
        for (int i = 1; i <= numModels; i++) { 
            // build and optimize sequence
        	HTSModel prev = um.getUttModel(i - 1);
        	HTSModel curr = um.getUttModel(i);
        	HTSModel next = um.getUttModel(i + 1);
        	FullPStream pstream = buildFullPStreamFor(Arrays.asList(prev, curr, next));
            // copy center phone's data to output
        	// handle first HMM: also copy data for i-1 to output
        	int startFrame = (i == 1) ? 0 : prev.getTotalDur();
        	// handle last HMM differently: also copy data for i+1 to output
        	int length = curr.getTotalDur() + ((i == 1) ? prev.getTotalDur() : 0) + ((i == numModels) ? next.getTotalDur() : 0);
        	((ListBackedFullPStream) outputFeatureStream).appendFeatures(pstream.getFullFrames(startFrame, length));
        } /**/
        /* non-incremental version * /
        List<HTSModel> hmms = new java.util.ArrayList<HTSModel>(um.getNumUttModel()); 
        for (int i = 0; i < um.getNumUttModel(); i++) {
        	hmms.add(um.getUttModel(i));
        }
        outputFeatureStream = buildFullPStreamFor(hmms, features, htsData); /**/
    }
    
    public FullPStream buildFullPStreamFor(List<HTSModel> hmms) {
        // find out what types of streams we're dealing with:
        Set<FeatureType> features = htsData.getFeatureSet();
        // original code does not deal with durations, so I explicitly remove DUR to make sure that it's never there; this might in fact not be necessary
        features.remove(FeatureType.DUR);
    	return buildFullPStreamFor(hmms, features);
    }

    /** 
     * build a parameter stream for some given HMMs
     * there can only ever be one call to buildFullPStreamFor() per object because 
     */
	private synchronized FullPStream buildFullPStreamFor(List<HTSModel> hmms, Set<FeatureType> features) {
		HashMap<FeatureType, HTSPStream> pStreamMap = new HashMap<FeatureType, HTSPStream>();
		for (FeatureType type : features) { // these could be submitted concurrently to an ExecutorService
			if (type == FeatureType.LF0) {
				if (!htsData.getUseAcousticModels()) 
					// FIXME: this is actually never called in our code, should be removed
					pStreamMap.put(type, calculateLF0Stream(hmms));
				else 
					buildVoicingArray(hmms);
			} else
				pStreamMap.put(type, calculateNormalStream(hmms, type));
		}
		return new HTSFullPStream(pStreamMap.get(FeatureType.MCP), 
								  pStreamMap.get(FeatureType.STR),
								  pStreamMap.get(FeatureType.MAG),
								  pStreamMap.get(FeatureType.LF0),
								  voiced);
	}
	
	/** fill in data into PStream and run optimization */
	private HTSPStream calculateNormalStream(List<HTSModel> hmms, FeatureType type) {
		assert type != FeatureType.LF0;
		CartTreeSet ms = htsData.getCartTreeSet();
		// initialize pStream
		int maxIterationsGV = (type == FeatureType.MCP) ? htsData.getMaxMgcGvIter() : htsData.getMaxLf0GvIter();
		int length = lengthOfEmissions(hmms, type);
		HTSPStream pStream = new HTSPStream(ms.getVsize(type), length, type, maxIterationsGV);
		// fill in data into pStream
		int uttFrame = 0; // count all frames
		for (HTSModel hmm : hmms) {
			for (int state = 0; state < ms.getNumStates(); state++) { // number of states is uniform for all HMMs
				for (int frame = 0; frame < hmm.getDur(state); frame++) {
					/* copy pdfs for types */
					pStream.setMseq(uttFrame, hmm.getMean(type, state));
		            pStream.setVseq(uttFrame, hmm.getVariance(type, state));
					uttFrame++;
		}}}
		boolean useGV = useGVperType(type);
		pStream.mlpg(htsData, useGV);
		return pStream;
	}
	
	private void buildVoicingArray(List<HTSModel> hmms) {
		CartTreeSet ms = htsData.getCartTreeSet();
		int totalLength = 0;
		for (HTSModel hmm : hmms) {
			totalLength += hmm.getTotalDur();
		}
		voiced = new boolean[totalLength]; // automatically initialized to false
		int uttFrame = 0; // count all frames
		for (HTSModel hmm : hmms) {
			for (int state = 0; state < ms.getNumStates(); state++) {
				if (hmm.getVoiced(state))
					Arrays.fill(voiced, uttFrame, uttFrame + hmm.getDur(state), true);
				uttFrame += hmm.getDur(state);
			}

		}
	}
	
	/** 
	 * like calculateNormalStream for FeatureType.LF0 which requires some additional voiced/voiceless handling
	 * !! also sets this.voiced[] to appropriate values
	 */ 
	private HTSPStream calculateLF0Stream(List<HTSModel> hmms) {
		CartTreeSet ms = htsData.getCartTreeSet();
		// initialize pStream
		int maxIterationsGV = htsData.getMaxLf0GvIter();
		int totalLength = 0;
		int voicedLength = 0;
		for (HTSModel hmm : hmms) {
			voicedLength += hmm.getNumVoiced();
			totalLength += hmm.getTotalDur();
		}
		HTSPStream pStream = new HTSPStream(ms.getLf0Stream(), voicedLength, FeatureType.LF0, maxIterationsGV);
		// figure out voicing
		int uttFrame = 0; // count all frames
		int lf0Frame = 0; // count all voiced frames
		voiced = new boolean[totalLength]; // automatically initialized to false
		boolean prevVoicing = false; // records whether the last state was voiced 
		for (HTSModel hmm : hmms) {
			for (int state = 0; state < ms.getNumStates(); state++) {
				// fill in data into pStream
				if (hmm.getVoiced(state)) {
					Arrays.fill(voiced, uttFrame, uttFrame + hmm.getDur(state), true);
					// handle boundaries between voiced/voiceless states
					boolean boundary = !prevVoicing; // if the last state was voiceless and this one is voiced, we have a boundary  
					for (int frame = 0; frame < hmm.getDur(state); frame++) {
						// copy pdfs for types 
						pStream.setMseq(lf0Frame, hmm.getLf0Mean(state));
						if (boundary) {// the variances for dynamic features are set to inf on v/uv boundary
							pStream.setIvseq(state, 0, finv(hmm.getLf0Variance(state, 0)));
							for (int k = 1; k < ms.getLf0Stream(); k++)
								pStream.setIvseq(lf0Frame, k, 0.0);
						} else {
							pStream.setVseq(lf0Frame, hmm.getLf0Variance(state));
						}
						lf0Frame++;
					}
				}
				uttFrame += hmm.getDur(state);
				prevVoicing = hmm.getVoiced(state);
			}
		}
		// fill in data into pStream
		return pStream;
	}

	private boolean useGVperType(FeatureType type) {
		switch (type) {
		case STR: return htsData.getUseGV() && htsData.getPdfStrGVFile() != null;
		case MAG: return htsData.getUseGV() && htsData.getPdfMagGVFile() != null;
		default: return htsData.getUseGV();
		}
	}

	private static int lengthOfEmissions(List<HTSModel> hmms, FeatureType type) {
		int length = 0;
		for (HTSModel hmm : hmms) {
			if (type == FeatureType.LF0) {
				// TODO: make sure that numVoiced is set already (by whom?), otherwise set it myself
				length += hmm.getNumVoiced();
			} else {
				length += hmm.getTotalDur();
			}
		}
		return length;
	}  

    public FullPStream getFullPStream() {
    	return outputFeatureStream;
    }

}
