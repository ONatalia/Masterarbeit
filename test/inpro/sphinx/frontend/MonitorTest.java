package inpro.sphinx.frontend;

import inpro.audio.AudioUtils;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

public class MonitorTest {

	@Test
	public void test() throws UnsupportedAudioFileException, IOException {
		URL audioURL = new URL("file:/home/timo/uni/projekte/inpro/res/DE_1234.wav");
		AudioInputStream ais = AudioUtils.getAudioStreamForURL(audioURL);
		StreamDataSource sds = new StreamDataSource(16000, 320, 16, false, true);
		sds.initialize();
		sds.setInputStream(ais, audioURL.getFile());
		Monitor monitor = new Monitor();
		monitor.initialize();
		monitor.setPredecessor(sds);
		Data d = null;
		do {
			d = monitor.getData();
		} while (d != null);
	}

}
