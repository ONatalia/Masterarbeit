package inpro.incremental.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import inpro.incremental.source.GoogleASR.GoogleJSONListener;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.sphinx.frontend.DataThrottle;
import inpro.util.PathUtil;

import org.junit.Test;

import static org.junit.Assert.*;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;

public class GoogleASRTest {

	@Test
	public void testRepeatableBehaviour() throws IOException, PropertyException, UnsupportedAudioFileException {
		SimpleReco sr = new SimpleReco(new RecoCommandLineParser("-F", "file:res/once16.wav"));
		BaseDataProcessor realtime = new DataThrottle();
		realtime.setPredecessor(sr.setupFileInput());
		GoogleASR gasr = new GoogleASR(realtime);
		gasr.setAPIKey("AIzaSyCXHs3mzb1IyfGx2tYxDC1ClgYUv0x8Kw8");
		File liveTempFile = File.createTempFile("google", ".timedjson");
		gasr.setExportFile(liveTempFile);
		gasr.recognize();
		// recognize again, this time from previously generated dump
		File replayedTempFile = File.createTempFile("googleAgain", ".timedjson");
		gasr.setExportFile(replayedTempFile);
		gasr.setImportFile(PathUtil.anyToURL(liveTempFile));
		realtime.setPredecessor(sr.setupFileInput());
		gasr.recognize();
		// now compare the content from google-connected run1 with replayed run2:
		BufferedReader orig = new BufferedReader(new InputStreamReader(new FileInputStream(liveTempFile)));
		BufferedReader repl = new BufferedReader(new InputStreamReader(new FileInputStream(replayedTempFile)));
		String origLine, replLine;
		while ((origLine = orig.readLine()) != null && (replLine = repl.readLine()) != null) {
			String[] origSplit = origLine.split("\t", 2);
			String[] replSplit = replLine.split("\t", 2);
			assertTrue("content of live and replayed JSON differs", origSplit[1].equals(replSplit[1]));
			int origTime = Integer.parseInt(origSplit[0]);
			int replTime = Integer.parseInt(replSplit[0]);
			assertTrue("deviation of more than 2ms between live and replayed JSON timings", Math.abs(origTime - replTime) < 2);
		}
		assertTrue("one of the files is longer than the other", (orig.readLine() == null && repl.readLine() == null));
		orig.close(); repl.close();
	}

	@Test
	public void testWithSomeFile() throws PropertyException, IOException, UnsupportedAudioFileException {
		SimpleReco sr = new SimpleReco(new RecoCommandLineParser("-F", "file:res/once16.wav"));
		BaseDataProcessor realtime = new DataThrottle();
		realtime.setPredecessor(sr.setupFileInput());
		GoogleASR gasr = new GoogleASR(realtime);
		gasr.setAPIKey("AIzaSyCXHs3mzb1IyfGx2tYxDC1ClgYUv0x8Kw8");
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
		Queue<String> wordsToBeRecognized = new LinkedList<String>(Arrays.asList("once", "upon", "a", "time", "in", "ancient", "greece")); 
		@Override
		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			for (EditMessage<? extends IU> edit : edits) {
				if (edit.getType().isCommit()) {
					System.err.println("now checking commit of" + edit);
					assertTrue(edit.getIU().toPayLoad().equals(wordsToBeRecognized.poll()));
				}
			}
		}
	}

	@Test 
	public void testJSONParser() throws IOException {
		LinkedList<String> json = new LinkedList<String>();
		json.add("{\"result\":[]}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"und\"}],\"stability\":0.0099999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"eine Unze\"}],\"stability\":0.0099999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine untere\"}],\"stability\":0.0099999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren\"}],\"stability\":0.0099999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren H\u00E4lfte\"}],\"stability\":0.0099999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine\"}],\"stability\":0.89999998},{\"alternative\":[{\"transcript\":\" unteren H\u00E4lfte\"}],\"stability\":0.0099999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren\"}],\"stability\":0.89999998},{\"alternative\":[{\"transcript\":\" H\u00E4lfte\"}],\"stability\":0.0099999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren H\u00E4lfte\"}],\"stability\":0.89999998}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\"meine unteren H\u00E4lfte\",\"confidence\":0.86194396},{\"transcript\":\"unteren H\u00E4lfte\"},{\"transcript\":\"M\u00E4nner unteren H\u00E4lfte\"},{\"transcript\":\"nein meine unteren H\u00E4lfte\"},{\"transcript\":\"meine untere H\u00E4lfte\"},{\"transcript\":\"untere H\u00E4lfte\"},{\"transcript\":\"eine untere H\u00E4lfte\"},{\"transcript\":\"in eine untere H\u00E4lfte\"}],\"final\":true}],\"result_index\":0}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" das\"}],\"stability\":0.0099999998}],\"result_index\":1}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" SC\"}],\"stability\":0.0099999998}],\"result_index\":1}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" SC\",\"confidence\":0.53307968},{\"transcript\":\" sc\"},{\"transcript\":\" das C\"},{\"transcript\":\" das Ziel\"},{\"transcript\":\" das c\"},{\"transcript\":\" Das c\"},{\"transcript\":\" dass c\"},{\"transcript\":\" does c\"},{\"transcript\":\" da\u00DF c\"}],\"final\":true}],\"result_index\":1}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" richtig\"}],\"stability\":0.0099999998}],\"result_index\":2}");
		json.add("{\"result\":[{\"alternative\":[{\"transcript\":\" richtig\",\"confidence\":0.68098658},{\"transcript\":\" wichtig\"},{\"transcript\":\" f dich\"},{\"transcript\":\" richtig u\"}],\"final\":true}],\"result_index\":2}");
		
		GoogleASR gasr = new GoogleASR(new DataThrottle());
		GoogleJSONListener gjson = gasr.new GoogleJSONListener() {
			public void run() {} 
			void shutdown() {}
		};
		LabelWriter label = new LabelWriter();
		gasr.addListener(label);
		
		for (String j : json) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			gjson.processJSON(j);
		}
	}
	
}
