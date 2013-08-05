package test.unit.inpro.synthesis;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;

import inpro.synthesis.MaryAdapter;

import org.junit.Before;
import org.junit.Test;

import extra.inpro.synthesis.visual.SegmentModel;

public class MaryAdapterMbrolaUnitTest {

	String testKurz = "Hallo";
	String testLang = "Nimm bitte das Kreuz und lege es in den Kopf des Elefanten.";
	
	@Before
	public void setupMinimalSynthesisEnvironment() {
        System.setProperty("inpro.tts.language", "de");
		System.setProperty("inpro.tts.voice", "de6");
	}

	/** make sure that text2mbrola succeeds and results in something that can be argued as being in mbrola format */
	@Test
	public void testText2mbrola() {
		SegmentModel.readFromStream(MaryAdapter.getInstance().text2mbrola(testKurz));
		SegmentModel.readFromStream(MaryAdapter.getInstance().text2mbrola(testLang));
	}

	@Test
	public void testMbrola2audio() {
		// basic functionality
		MaryAdapter.getInstance().mbrola2audio(SegmentModel.createTestModel().toString());
	}
	
	@Test (expected = Exception.class)
	public void testMbrola2audioFailsWithNonsense() {
		// make sure that something which is not mbrola fails the test:
		MaryAdapter.getInstance().mbrola2audio(testLang);
		// --> unfortunately, this results in an empty wave file instead of an error 
	}

	@Test
	public void testText2audio() throws IOException {
		MaryAdapter ma = MaryAdapter.getInstance();
		ma.text2audio(testKurz);
		ma.text2audio(testLang);
		// make sure that the concatentation of text2mbrola with mbrola2audio is identical with text2audio
		AudioInputStream concatResult = 
				ma.mbrola2audio(SegmentModel.readFromStream(ma.text2mbrola(testKurz)).toString());
		AudioInputStream atomicResult = 
				ma.text2audio(testKurz);
		assertEquals(concatResult.getFrameLength(), atomicResult.getFrameLength());
		int frameSize = concatResult.getFormat().getFrameSize();
		assertEquals(frameSize, atomicResult.getFormat().getFrameSize());
		byte[] sampleConcat = new byte[frameSize];
		byte[] sampleAtomic = new byte[frameSize];
		while (concatResult.read(sampleConcat, 0, frameSize) != -1 
			&& atomicResult.read(sampleAtomic, 0, frameSize) != -1) {
			assertArrayEquals(sampleConcat, sampleAtomic);
		}
	}
	
}
