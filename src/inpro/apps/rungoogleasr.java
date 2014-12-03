package inpro.apps;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;
import inpro.apps.util.RecoCommandLineParser;
import inpro.incremental.sink.CurrentHypothesisViewer;
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
        gasr.setLanguageCode("de-de");
        LabelWriter label = new LabelWriter();
        label.writeToFile();
        label.setFileName(args[args.length - 1]);
        //gasr.addListener(new CurrentHypothesisViewer().show());
        gasr.addListener(label);
        gasr.recognize(); 
    }
}