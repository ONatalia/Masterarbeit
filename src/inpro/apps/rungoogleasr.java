package inpro.apps;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;
import inpro.apps.util.RecoCommandLineParser;
import inpro.incremental.sink.LabelWriter;
import inpro.incremental.source.GoogleASR;
import inpro.sphinx.frontend.DataThrottle;

public class rungoogleasr {

    public static void main(String[] args) throws PropertyException, IOException, UnsupportedAudioFileException {
        // TODO Auto-generated method stub
        
        SimpleReco sr = new SimpleReco(new RecoCommandLineParser(args));
        BaseDataProcessor realtime = new DataThrottle();
        realtime.setPredecessor(sr.setupFileInput());
       
        GoogleASR gasr = new GoogleASR(realtime);
        //Use your own Google API key! 
        // Spyros: AIzaSyBJbDtcdABOzZ4xExvMbNeyRtx-ZpU3NeM
        // second key: AIzaSyBR8gjIWNoEd1PltsZDH_7OhvftJ_eDPhM
        gasr.setAPIKey("AIzaSyBR8gjIWNoEd1PltsZDH_7OhvftJ_eDPhM");
        //uncomment line to change the language
        gasr.setLanguageCode("de-de");
        // set the sampling rate
        gasr.setSamplingRate("16000");
        LabelWriter label = new LabelWriter();
        label.setWriteToFile(true);
        label.setFileName(args[args.length - 1]);
        //gasr.addListener(new CurrentHypothesisViewer().show());
        gasr.addListener(label);
        gasr.recognize(); 
    }
}