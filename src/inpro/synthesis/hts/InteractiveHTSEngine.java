package inpro.synthesis.hts;

import inpro.synthesis.MaryAdapter4internal;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.w3c.dom.Element;

import marytts.datatypes.MaryData;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.modules.HTSEngine;
import marytts.unitselection.select.Target;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.data.audio.DDSAudioInputStream;

public class InteractiveHTSEngine extends HTSEngine {

    public boolean synthesizeAudio = true; 
    
    public static boolean returnIncrementalAudioStream = false;
    
    public final List<HTSModel> uttHMMs = new ArrayList<HTSModel>();
    
    @Override
    public MaryData process(MaryData d, List<Target> targetFeaturesList, List<Element> segmentsAndBoundaries, List<Element> tokensAndBoundaries)
    throws Exception
    {

        /** The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for current label file. */
        /* Process label file of Mary context features and creates UttModel um */
        HTSUttModel um = processTargetList(targetFeaturesList, segmentsAndBoundaries, MaryAdapter4internal.getDefaultHMMData());

        for (int i = 0; i < um.getNumModel(); i++) {
        	uttHMMs.add(um.getUttModel(i));
        }
        
        MaryData output = new MaryData(outputType(), d.getLocale());
        if (synthesizeAudio) {
            AudioInputStream ais = null;
            /* Process UttModel */
            // Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's  
            // # non-incremental MaryTTS version:
            /**/ HTSParameterGeneration npdf2par = new HTSParameterGeneration();
            npdf2par.htsMaximumLikelihoodParameterGeneration(um, MaryAdapter4internal.getDefaultHMMData());
            FullPStream pstream = new HTSFullPStream(npdf2par); /**/
            // # incremental pHTS version:
            /* PHTSParameterGeneration pdf2par = MaryAdapter4internal.getNewParamGen(); // new PHTSParameterGeneration(hmmv.getHMMData());
            FullPStream pstream = pdf2par.buildFullPStreamFor(uttHMMs); /**/
	        /* Vocode speech waveform out of sequence of parameters */
	        DoubleDataSource dds = new VocodingAudioStream(pstream, MaryAdapter4internal.getDefaultHMMData(), returnIncrementalAudioStream);
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

	public List<HTSModel> getUttHMMs() {
		assert uttHMMs != null : "You are calling getUttHMMs without having called my process() method before (Hint: you may think that it was called but the buildpath order might be in your way.)";
		return new ArrayList<HTSModel>(uttHMMs);
	}
	
	public void resetUttHMMstore() {
		uttHMMs.clear();
	}
    
}