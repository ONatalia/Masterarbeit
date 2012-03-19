package org.cocolab.inpro.tts.hts;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.cocolab.inpro.incremental.unit.SysSegmentIU;
import org.cocolab.inpro.tts.hts.util.SynthesisHeatMap;
import org.cocolab.inpro.tts.hts.util.SynthesisHeatMapComparator;
import org.w3c.dom.Element;

import test.org.cocolab.inpro.synthesis.ITTSExperimenter;

import marytts.datatypes.MaryData;
import marytts.htsengine.HMMVoice;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.modules.HTSEngine;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.select.Target;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.data.audio.DDSAudioInputStream;

public class InteractiveHTSEngine extends HTSEngine {

    final List<FullPStreamListener> parameterDataListeners = new ArrayList<FullPStreamListener>();
    
    public void registerParameterDataListener(FullPStreamListener pdl) {
        parameterDataListeners.add(pdl);
    }
    
    public void setParameterDataListener(FullPStreamListener pdl) {
    	parameterDataListeners.clear();
    	registerParameterDataListener(pdl);
    }
    
    public boolean synthesizeAudio = true; 
    
    @Override
    public MaryData process(MaryData d, List<Target> targetFeaturesList, List<Element> segmentsAndBoundaries, List<Element> tokensAndBoundaries)
    throws Exception
    {
        Voice v = d.getDefaultVoice(); /* This is the way of getting a Voice through a MaryData type */
        assert v instanceof HMMVoice;
        HMMVoice hmmv = (HMMVoice) v;

        /** The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for current label file. */
        /* Process label file of Mary context features and creates UttModel um */
        HTSUttModel um = processTargetList(targetFeaturesList, segmentsAndBoundaries, hmmv.getHMMData());

        if (segments != null) {
        	assert segments.size() <= um.getNumUttModel() : segments.size() + ">" + um.getNumUttModel();
        	for (int i = 0; i < segments.size() && i < um.getNumUttModel(); i++) {
        		double dur = um.getUttModel(i).getTotalDurMillisec() * 0.001;
        		assert um.getUttModel(i).getPhoneName().equals(segments.get(i).toPayLoad()) : um.getUttModel(i).getPhoneName() + " ne " + segments; 
        		SysSegmentIU seg = segments.get(i);
        		if (seg.duration() != dur)
        			logger.info("changing duration of segment from " + seg.duration() + " to " + dur);
        		seg.setNewDuration(dur);
        		// append Mary Target Data to syssegment:
        		seg.setHTSModel(um.getUttModel(i));
        	}
        }
        
        // this can then later be done
        /* Process UttModel */
        PHTSParameterGeneration ipdf2par = new PHTSParameterGeneration(hmmv.getHMMData());
        // Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's  
        ipdf2par.phtsIncrementalParameterGeneration(um); /**/

        //ITTSExperimenter.visualize("Incremental", new SynthesisHeatMap(new HTSFullPStream(pdf2par)));
        //ITTSExperimenter.visualize("Incremental", new SynthesisHeatMap(ipdf2par.getFullPStream()));

        
        
        MaryData output = new MaryData(outputType(), d.getLocale());
        if (synthesizeAudio) {
            AudioInputStream ais = null;
            /* Process UttModel */
            HTSParameterGeneration pdf2par = new HTSParameterGeneration();
            // Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's  
            pdf2par.htsMaximumLikelihoodParameterGeneration(um, hmmv.getHMMData()); /**/

//            ITTSExperimenter.visualize("Non-Incremental", new SynthesisHeatMap(new HTSFullPStream(pdf2par)));
//            ITTSExperimenter.visualize("Difference", new SynthesisHeatMapComparator(ipdf2par.getFullPStream(), new HTSFullPStream(pdf2par)));
	        /* Vocode speech waveform out of sequence of parameters */
	        DoubleDataSource dds = new VocodingAudioStream(pdf2par, hmmv.getHMMData(), false);
	        float sampleRate = 16000.0F;  //8000,11025,16000,22050,44100
	        int sampleSizeInBits = 16;  //8,16
	        int channels = 1;     //1,2
	        boolean signed = true;    //true,false
	        boolean bigEndian = false;  //true,false
	        AudioFormat af = new AudioFormat(
	              sampleRate,
	              sampleSizeInBits,
	              channels,
	              signed,
	              bigEndian);
	        ais = new DDSAudioInputStream(dds, af);
	        if (d.getAudioFileFormat() != null) {
	            output.setAudioFileFormat(d.getAudioFileFormat());
	            if (d.getAudio() != null) {
	               // This (empty) AppendableSequenceAudioInputStream object allows a 
	               // thread reading the audio data on the other "end" to get to our data as we are producing it.
	                assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
	                output.setAudio(d.getAudio());
	            }
	        }     
	        output.appendAudio(ais);
        }	       

        // set the actualDurations in tokensAndBoundaries
        if(tokensAndBoundaries != null)
            setRealisedProsody(tokensAndBoundaries, um);

        for (FullPStreamListener pdl : parameterDataListeners) {
        	if (ipdf2par instanceof PHTSParameterGeneration)
        		pdl.newParameterData(d, ((PHTSParameterGeneration)ipdf2par).getFullPStream());
        	else 
        		pdl.newParameterData(d, new HTSFullPStream(ipdf2par));
        }
        return output;
    }
    
    List<SysSegmentIU> segments;
    
	public void setSegmentIUs(List<SysSegmentIU> segments) {
		this.segments = segments;
	}

	public interface FullPStreamListener {
        public void newParameterData(MaryData d, FullPStream pstream);
    }
}