package inpro.incremental.source;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import inpro.apps.SimpleReco;
import inpro.apps.util.RecoCommandLineParser;
import inpro.sphinx.frontend.DataThrottle;

import org.junit.Test;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;

public class GoogleASRTest {

	@Test
	public void testWithSomeFile() throws PropertyException, IOException, UnsupportedAudioFileException {
		SimpleReco sr = new SimpleReco(new RecoCommandLineParser("-F", "file:/home/timo/uni/lehre/abschlussarbeiten/05_johannestwiefel/developGit/data/front_fs_1387379085134_m1.wav"));
		BaseDataProcessor realtime = new DataThrottle();
		realtime.setPredecessor(sr.setupFileInput());
		GoogleASR gasr = new GoogleASR(realtime);
		gasr.recognize();
	}
}