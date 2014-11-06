package inpro.incremental.source;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.sound.sampled.UnsupportedAudioFileException;

import inpro.apps.SimpleReco;
import inpro.apps.util.RecoCommandLineParser;
import inpro.incremental.IUModule;
import inpro.incremental.sink.CurrentHypothesisViewer;
import inpro.incremental.sink.LabelWriter;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.sphinx.frontend.DataThrottle;

import org.junit.Test;
import static org.junit.Assert.*;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;

public class GoogleASRTest {

	@Test
	public void testWithSomeFile() throws PropertyException, IOException, UnsupportedAudioFileException {
		SimpleReco sr = new SimpleReco(new RecoCommandLineParser("-F", "file:res/once16.wav"));
		BaseDataProcessor realtime = new DataThrottle();
		realtime.setPredecessor(sr.setupFileInput());
		GoogleASR gasr = new GoogleASR(realtime);
		LabelWriter label = new LabelWriter();
		//label.writeToFile();
		//label.setFileName("/home/casey/Desktop/test");
		gasr.addListener(label);
		MyIUModule mymod = new MyIUModule();
		gasr.addListener(mymod);
		gasr.addListener(new CurrentHypothesisViewer().show());
		gasr.recognize();
		assertTrue(mymod.wordsToBeRecognized.isEmpty());
	}
	
	private class MyIUModule extends IUModule {
		Queue<String> wordsToBeRecognized = new LinkedList(Arrays.asList("once", "upon", "a", "time", "in", "ancient", "greece")); 
		@Override
		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			for (EditMessage edit : edits) {
				if (edit.getType().isCommit()) {
					System.err.println("now checking commit of" + edit);
					assertTrue(edit.getIU().toPayLoad().equals(wordsToBeRecognized.poll()));
				}
			}
		}
	}
	
//	@Test 
//	public void testJSONParser() {
//		
//		try {
//			
//
//			LinkedList<String> json = new LinkedList<String>();
//			json.add("{\"result\":[]}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"und\"}],\"stability\":0.0099999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"eine Unze\"}],\"stability\":0.0099999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine untere\"}],\"stability\":0.0099999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren\"}],\"stability\":0.0099999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren H\u00E4lfte\"}],\"stability\":0.0099999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine\"}],\"stability\":0.89999998},{\"alternative\":[{\"transcript\":\" unteren H\u00E4lfte\"}],\"stability\":0.0099999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren\"}],\"stability\":0.89999998},{\"alternative\":[{\"transcript\":\" H\u00E4lfte\"}],\"stability\":0.0099999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren H\u00E4lfte\"}],\"stability\":0.89999998}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren H\u00E4lfte\",\"confidence\":0.86194396},{\"transcript\":\"unteren H\u00E4lfte\"},{\"transcript\":\"M\u00E4nner unteren H\u00E4lfte\"},{\"transcript\":\"nein meine unteren H\u00E4lfte\"},{\"transcript\":\"meine untere H\u00E4lfte\"},{\"transcript\":\"untere H\u00E4lfte\"},{\"transcript\":\"eine untere H\u00E4lfte\"},{\"transcript\":\"in eine untere H\u00E4lfte\"}],\"final\":true}],\"result_index\":0}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" das\"}],\"stability\":0.0099999998}],\"result_index\":1}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" SC\"}],\"stability\":0.0099999998}],\"result_index\":1}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" SC\",\"confidence\":0.53307968},{\"transcript\":\" sc\"},{\"transcript\":\" das C\"},{\"transcript\":\" das Ziel\"},{\"transcript\":\" das c\"},{\"transcript\":\" Das c\"},{\"transcript\":\" dass c\"},{\"transcript\":\" does c\"},{\"transcript\":\" da\u00DF c\"}],\"final\":true}],\"result_index\":1}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" richtig\"}],\"stability\":0.0099999998}],\"result_index\":2}");
//			json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" richtig\",\"confidence\":0.68098658},{\"transcript\":\" wichtig\"},{\"transcript\":\" f*** dich\"},{\"transcript\":\" richtig u\"}],\"final\":true}],\"result_index\":2}");
//			
//			GoogleASR gasr = new GoogleASR(new DataThrottle());
//			GoogleJSONListener gjson = gasr.new GoogleJSONListener("");
//			
//			for (String j : json) {
//				try {
//					Thread.sleep(250);
//				} 
//				catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				gjson.parseJSON(j);
//			}
//			
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
}