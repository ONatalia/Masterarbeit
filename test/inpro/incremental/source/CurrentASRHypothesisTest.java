package inpro.incremental.source;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import inpro.apps.SimpleReco;
import inpro.apps.util.RecoCommandLineParser;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;

import org.junit.Test;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

/**
 * moderately important test: make sure that there are not revoke/add pairs within one call to leftBufferUpdate (i.e., do not send revoke/add if nothing changes
 * @author timo
 */
public class CurrentASRHypothesisTest {

	@Test
	public void testRecognition() throws PropertyException, IOException, UnsupportedAudioFileException {
		testConfiguration("-F", "file:res/DE_1234.wav"); // test with built-in SLM
		testConfiguration("-F", "file:res/DE_1234.wav", "-gr", "file:src/demo/inpro/system/echodm/digits.gram"); // test a grammar
		testConfiguration("-F", "file:res/DE_1234.wav", "-Is", "7"); // test smoothing with a common value  
		testConfiguration("-F", "file:res/DE_1234.wav", "-If", "7"); // test right-context with a common value  
		testConfiguration("-F", "file:res/DE_1234.wav", "-N"); // test right-context with a common value  
	}
	
	private void testConfiguration(String... recoArgs) throws PropertyException, IOException, UnsupportedAudioFileException {
		RecoCommandLineParser clp = new RecoCommandLineParser(recoArgs);
		ConfigurationManager cm = new ConfigurationManager(clp.getConfigURL());
		SimpleReco simpleReco = new SimpleReco(cm, clp);
		CurrentASRHypothesis casrh = (CurrentASRHypothesis) cm.lookup("currentASRHypothesis");
		TestModule tm = new TestModule();
		casrh.addListener(tm);
		simpleReco.recognizeOnce();
		assertTrue("for some reason, the testing IU module has not been called!", tm.hasBeenCalled > 0);
		if (recoArgs[recoArgs.length - 1].equals("-N"))
			assertTrue(tm.hasBeenCalled == 1);
    	simpleReco.getRecognizer().deallocate();
	}

	private class TestModule extends IUModule {
		int hasBeenCalled = 0;
		@Override
		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			hasBeenCalled++;
			IU previouslyRevokedWord = null;
			for (EditMessage<?> edit : edits) {
				logger.info(edit.getType() + " " + edit.getIU().toPayLoad());
				assertTrue("every IU output by CurrentASRHypothesis should be a WordIU", edit.getIU() instanceof WordIU);
				WordIU inputWord = (WordIU) edit.getIU();
				switch (edit.getType()) {
				case REVOKE:
					previouslyRevokedWord = inputWord;
					break;
				case ADD:
					assertTrue(previouslyRevokedWord == null || !previouslyRevokedWord.payloadEquals(inputWord));
					previouslyRevokedWord = null;
					break;
				default: 
				}
			}
		}
	}
}
