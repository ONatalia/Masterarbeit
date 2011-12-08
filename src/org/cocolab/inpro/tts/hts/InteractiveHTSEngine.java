package org.cocolab.inpro.tts.hts;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.w3c.dom.Element;

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

    final List<HTSParameterDataListener> parameterDataListeners = new ArrayList<HTSParameterDataListener>();
    
    public void registerParameterDataListener(HTSParameterDataListener pdl) {
        parameterDataListeners.add(pdl);
    }
    
    public void setParameterDataListener(HTSParameterDataListener pdl) {
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
        HMMVoice hmmv = (HMMVoice)v;

        /** The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for current label file. */
        HTSUttModel um = new HTSUttModel();
        /* Process label file of Mary context features and creates UttModel um */
        processTargetList(targetFeaturesList, segmentsAndBoundaries, um, hmmv.getHMMData());

        /* Process UttModel */
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */  
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, hmmv.getHMMData(),"", false);
    
        for (HTSParameterDataListener pdl : parameterDataListeners) {
            pdl.newParameterData(d, pdf2par);
        }

        AudioInputStream ais = null;
        if (synthesizeAudio) {
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
        }	       
        MaryData output = new MaryData(outputType(), d.getLocale());
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

        // set the actualDurations in tokensAndBoundaries
        if(tokensAndBoundaries != null)
            setRealisedProsody(tokensAndBoundaries, um);
        return output;
    }
    
    public interface HTSParameterDataListener {
        public void newParameterData(MaryData d, HTSParameterGeneration pdf2par);
    }
}