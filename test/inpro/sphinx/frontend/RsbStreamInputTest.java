package inpro.sphinx.frontend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import inpro.audio.AudioUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import org.junit.Test;

import rsb.Factory;
import rsb.Informer;
import rsb.RSBException;
import rst.audition.SoundChunkType.SoundChunk;

import com.google.protobuf.ByteString;

import edu.cmu.sphinx.frontend.Data;
import static org.junit.Assert.*;

/**
 * JUnit test for {@link RsbStreamInputSource}. It sends an audio chunk 
 * via RSB. The RsbStreamInputSource should receive it and the post processor
 * {@link Monitor} plays back the audio received onto the speakers.
 * 
 * @author bcarlmey
 * 
 */
public class RsbStreamInputTest {

	// RSB Scope for sending/receiving audio chunks
	private static final String SCOPE = "/test/audio";

	@Test
	public void test() throws UnsupportedAudioFileException, IOException,
			RSBException, InterruptedException {

		// initiate RsbStreamInputSource instance
		RsbStreamInputSource instance = new RsbStreamInputSource(false, true, 10, SCOPE);
		instance.initialize();

		// initiate Monitor instance and start RsbStreamInputSource
		Monitor monitor = new Monitor();
		monitor.initialize();
		monitor.setPredecessor(instance);
		Boolean isStarted = instance.startRecording();
		System.out.println("System is started: " + isStarted);
		assertTrue(isStarted);

		// send RSB SoundChunk - in this case one SoundChunk with audio data from DE_1234.wav 
		URL audioURL = RsbStreamInputTest.class.getResource("DE_1234.wav");
		AudioInputStream audioStream = AudioUtils
				.getAudioStreamForURL(audioURL);
		SoundChunk chunk = createSoundChunk(audioStream.getFormat(),
				getAudioData(audioStream));
		// simple RSB SoundChunk informer
		Informer<SoundChunk> informer = Factory.getInstance().createInformer(
				SCOPE);
		informer.activate();
		System.out.println("Send SoudChunk");
		informer.send(chunk);

		// send data end signal
		System.out.println("Send audio end signal");
		chunk = createSoundChunk(audioStream.getFormat(), new byte[0]);
		informer.send(chunk);
		informer.deactivate();

		// play back the audio received onto the speakers
		Data d = null;
		do {
			d = monitor.getData();
		} while (d != null);
		System.out.println("Stop recording");
		instance.stopRecording();

	}

	/**
	 * Create an audio soundChunk.
	 * 
	 * @param audio
	 *            format
	 * @param data
	 * @return soundChunk
	 */
	private SoundChunk createSoundChunk(AudioFormat audio, byte[] data) {
		SoundChunk.Builder sc = SoundChunk.newBuilder();
		// set audio data
		ByteString bs = ByteString.copyFrom(data);
		sc.setData(bs);

		// set audio format
		boolean isbig = audio.isBigEndian();
		sc.setChannels(audio.getChannels());
		sc.setRate((int) audio.getSampleRate());

		if (isbig) {
			sc.setEndianness(SoundChunk.EndianNess.ENDIAN_BIG);
		} else {
			sc.setEndianness(SoundChunk.EndianNess.ENDIAN_LITTLE);
		}

		sc.setSampleCount(data.length);
		Encoding enc = audio.getEncoding();

		if (enc.equals(Encoding.PCM_SIGNED)) {
			int size = audio.getSampleSizeInBits();
			if (size == 8) {
				sc.setSampleType(SoundChunk.SampleType.SAMPLE_S8);
			} else if (size == 16) {
				sc.setSampleType(SoundChunk.SampleType.SAMPLE_S16);
			} else if (size == 24) {
				sc.setSampleType(SoundChunk.SampleType.SAMPLE_S24);
			} else {
				System.err.println("Audio Sample Size Not Valid");
			}
		} else if (enc.equals(Encoding.PCM_UNSIGNED)) {
			int size = audio.getSampleSizeInBits();
			if (size == 8) {
				sc.setSampleType(SoundChunk.SampleType.SAMPLE_U8);
			} else if (size == 16) {
				sc.setSampleType(SoundChunk.SampleType.SAMPLE_U16);
			} else if (size == 24) {
				sc.setSampleType(SoundChunk.SampleType.SAMPLE_U24);
			} else {
				System.err.println("Audio Sample Size not valid");
				System.exit(-1);
			}
		} else {
			System.err.println("Audio Encoding is invalid");
			System.exit(-1);
		}
		return sc.build();
	}

	/**
	 * Extract byte[] from audio stream.
	 * 
	 * @param audioStream
	 * @return audio data
	 * @throws IOException
	 */
	private byte[] getAudioData(AudioInputStream audioStream)
			throws IOException {
		int nBufferSize = audioStream.getFormat().getFrameSize();
		ByteArrayOutputStream baout = new ByteArrayOutputStream();

		byte[] data = new byte[nBufferSize];
		while (true) {
			int nBytesRead = audioStream.read(data);
			if (nBytesRead == -1) {
				break;
			}
			baout.write(data, 0, nBytesRead);
		}
		audioStream.close();
		AudioFormat f = audioStream.getFormat();
		System.out.println("audio format:");
		System.out.println("type " + f.getEncoding());
		System.out.println("samplerate " + f.getSampleRate());
		System.out.println("numbits " + f.getSampleSizeInBits());
		System.out.println("channel " + f.getChannels());
		System.out.println("framesize " + f.getFrameSize());
		System.out.println("framerate " + f.getFrameRate());
		System.out.println("bigendia " + f.isBigEndian());

		data = baout.toByteArray();
		baout.close();

		return data;
	}
}
