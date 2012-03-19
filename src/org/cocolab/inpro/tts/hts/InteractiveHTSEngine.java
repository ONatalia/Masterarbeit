package org.cocolab.inpro.tts.hts;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.w3c.dom.Element;

import marytts.datatypes.MaryData;
import marytts.htsengine.HMMVoice;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.modules.HTSEngine;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.select.Target;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.data.audio.DDSAudioInputStream;

public class InteractiveHTSEngine extends HTSEngine {

    public boolean synthesizeAudio = true; 
    
    public List<HTSModel> uttHMMs = null;
    
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

        uttHMMs = new ArrayList<HTSModel>(um.getNumModel());
        for (int i = 0; i < um.getNumModel(); i++) {
        	uttHMMs.add(um.getUttModel(i));
        }
        
        // this can then later be done
        /* Process UttModel * /
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

        return output;
    }
    
}