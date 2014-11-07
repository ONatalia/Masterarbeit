package inpro.audio;

import inpro.apps.SimpleMonitor;

import org.junit.Test;

public class DispatchStreamTest {
	
	@Test(timeout = 20000)
	public void testPlayTTS() {
		DispatchStream dispatcher = SimpleMonitor.setupDispatcher();
		dispatcher.playTTS("eins, zwei, drei, vier", true);
		dispatcher.waitUntilDone();
	}
}
