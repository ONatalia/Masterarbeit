package inpro.sphinx.frontend;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.frontend.util.Utterance;
import edu.cmu.sphinx.util.props.*;

import javax.sound.sampled.*;

import rsb.AbstractEventHandler;
import rsb.Event;
import rsb.Factory;
import rsb.Listener;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.audition.SoundChunkType.SoundChunk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * <p/>
 * RsbStremInputSource captures audio data from RSB listener and converts these
 * audio data into Data objects. When the method <code>startRecording()</code>
 * is called, a new thread will be created and used to capture audio, and will
 * stop when <code>stopRecording()</code> is called. Calling
 * <code>getData()</code> returns the captured audio data as Data objects.
 * </p>
 */
public class RsbStreamInputSource extends BaseDataProcessor {

	

	/**
	 * The property that specifies the number of milliseconds of audio data to
	 * read each time from the underlying Java Sound audio device.
	 */
	@S4Integer(defaultValue = 10)
	public final static String PROP_MSEC_PER_READ = "msecPerRead";

	
	/**
	 * The property specify the endianness of the data.
	 */
	@S4Boolean(defaultValue = false)
	public static final String PROP_BIG_ENDIAN = "bigEndian";

	/**
	 * The property specify whether the data is signed.
	 */
	@S4Boolean(defaultValue = true)
	public static final String PROP_SIGNED = "signed";


	/**
	 * The RSB scope for the SoundChunk listener.
	 */
	@S4String(defaultValue = "/mysystem/audio")
	public final static String PROP_SCOPE = "scope";

	private AudioFormat audioFormat;
	private BlockingQueue<Data> audioList;
	private Utterance currentUtterance;
	private volatile boolean recording;
	private volatile boolean utteranceEndReached = true;
	private RecordingThread recorder;

	private boolean signed;
	private boolean bigEndian;


	private int frameSizeInBytes;
	private int msecPerRead;

	private BlockingQueue<SoundChunk> soundChunks;
	private AudioInputStream audioInputStream = null;

	private String scope;

	/**
	 * @param bigEndian
	 *            the endianness of the data
	 * @param signed
	 *            whether the data is signed.
	 * @param msecPerRead
	 *            the number of milliseconds of audio data to read each time
	 *            from the underlying Java Sound audio device.
	 * @param scope
	 *            RSB scope
	 */
	public RsbStreamInputSource(Boolean bigEndian, Boolean signed, int msecPerRead, String scope) {
		initLogger();

		this.bigEndian = bigEndian;
		this.signed = signed;
		this.msecPerRead = msecPerRead;
		this.scope = scope;
		this.audioFormat = getConfAudioFormat();

	}

	public RsbStreamInputSource() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util
	 * .props.PropertySheet)
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		logger = ps.getLogger();

		bigEndian = ps.getBoolean(PROP_BIG_ENDIAN);
		signed = ps.getBoolean(PROP_SIGNED);
		msecPerRead = ps.getInt(PROP_MSEC_PER_READ);
		scope = ps.getString(PROP_SCOPE);
		this.audioFormat = getConfAudioFormat();
	}

	/**
	 * Constructs the frameSize and add RSB converter .
	 */
	@Override
	public void initialize() {
		super.initialize();
		final ProtocolBufferConverter<SoundChunk> converter = new ProtocolBufferConverter<SoundChunk>(
				SoundChunk.getDefaultInstance());
		DefaultConverterRepository.getDefaultConverterRepository()
				.addConverter(converter);
		audioList = new LinkedBlockingQueue<Data>();
		soundChunks = new LinkedBlockingQueue<SoundChunk>();

		float sec = msecPerRead / 1000.f;
		frameSizeInBytes = (audioFormat.getSampleSizeInBits() / 8)
				* (int) (sec * audioFormat.getSampleRate())
				* audioFormat.getChannels();

		logger.info("AudioFormat: " + audioFormat);
		logger.info("FrameSize: " + frameSizeInBytes);
	}

	/**
	 * Returns the format of the audio recorded by the RSB SoundChunks
	 * 
	 * @return the current AudioFormat
	 */
	public AudioFormat getAudioFormat() {
		return audioFormat;
	}

	/**
	 * Returns the current Utterance.
	 * 
	 * @return the current Utterance
	 */
	public Utterance getUtterance() {
		return currentUtterance;
	}

	/**
	 * Returns true if this StreamInput is recording.
	 * 
	 * @return true if this StreamInput is recording, false otherwise
	 */
	public boolean isRecording() {
		return recording;
	}

	/**
	 * Starts recording audio.
	 * 
	 * @return true if the recording started successfully; false otherwise
	 */
	public synchronized boolean startRecording() {
		if (recording) {
			return false;
		}

		utteranceEndReached = false;

		assert (recorder == null);
		recorder = new RecordingThread("Microphone");
		recorder.start();
		recording = true;
		return true;
	}

	/**
	 * Stops recording audio. This method does not return until recording has
	 * been stopped and all data has been read from the RSB listener.
	 */
	public synchronized void stopRecording() {

		if (recorder != null) {
			recorder.stopRecording();
			recorder = null;
		}
		recording = false;

	}

	/**
	 * This Thread records audio, and caches them in an audio buffer.
	 */
	class RecordingThread extends Thread {

		private boolean done;
		private volatile boolean started;
		private long totalSamplesRead;
		private final Object lock = new Object();

		/**
		 * Creates the thread with the given name
		 * 
		 * @param name
		 *            the name of the thread
		 */
		public RecordingThread(String name) {
			super(name);
			logger.info("init recording thread for RSB SoundChunks on scope "
							+ scope);
			try {
				Listener listener = Factory.getInstance().createListener(scope);
				listener.activate();
				AbstractEventHandler handler = new AbstractEventHandler() {

					@Override
					public void handleEvent(Event e) {
						SoundChunk soundChunk = (SoundChunk) e.getData();
						soundChunks.add(soundChunk);
					}
				};

				listener.addHandler(handler, true);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Error setting up RSB Listener");
			}
		}

		/**
		 * Starts the thread, and waits for recorder to be ready
		 */
		@Override
		public void start() {
			started = false;
			super.start();
		}

		/**
		 * Stops the thread. This method does not return until recording has
		 * actually stopped, and all the data has been read from the RSB
		 * listener.
		 */
		public void stopRecording() {
			try {
				synchronized (lock) {
					while (!done) {
						lock.wait();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Implements the run() method of the Thread class. Records audio, and
		 * cache them in the audio buffer.
		 */
		@Override
		public void run() {
			totalSamplesRead = 0;
			logger.info("started recording");

			audioList
					.add(new DataStartSignal((int) audioFormat.getSampleRate()));
			logger.info("DataStartSignal added");
			try {
				while (!done) {
					Data data = readData(currentUtterance);
					if (data == null) {
						done = true;
						break;
					}
					audioList.add(data);
				}

			} catch (IOException ioe) {
				logger.warning("IO Exception " + ioe.getMessage());
				ioe.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long duration = (long) (((double) totalSamplesRead / (double) audioFormat
					.getSampleRate()) * 1000.0);

			audioList.add(new DataEndSignal(duration));
			logger.info("DataEndSignal ended");
			logger.info("stopped recording");

			synchronized (lock) {
				lock.notify();
			}
		}

		/**
		 * Reads one frame of audio data, and adds it to the given Utterance.
		 * 
		 * @param utterance
		 * @return an Data object containing the audio data
		 * @throws java.io.IOException
		 * @throws InterruptedException
		 */
		private Data readData(Utterance utterance) throws IOException,
				InterruptedException {

			// Read the next chunk of data from the input stream.
			byte[] data = new byte[frameSizeInBytes];
			if (audioInputStream == null || audioInputStream.available() <= 0) {
				byte[] audioSource = soundChunks.take().getData().toByteArray();
				InputStream byteArrayInputStream = new ByteArrayInputStream(
						audioSource);
				audioInputStream = new AudioInputStream(byteArrayInputStream,
						audioFormat, audioSource.length);
			}

			int channels = audioFormat.getChannels();
			long firstSampleNumber = totalSamplesRead / channels;

			int numBytesRead = audioInputStream.read(data, 0, data.length);
			if (numBytesRead <= 0) {
				return null;
			}
			// notify the waiters upon start
			if (!started) {
				synchronized (this) {
					started = true;
					notifyAll();
				}
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.info("Read " + numBytesRead
						+ " bytes from audio stream.");
			}
			if (numBytesRead <= 0) {
				return null;
			}
			int sampleSizeInBytes = audioFormat.getSampleSizeInBits() / 8;
			totalSamplesRead += (numBytesRead / sampleSizeInBytes);

			if (numBytesRead != frameSizeInBytes) {
				if (numBytesRead % sampleSizeInBytes != 0) {
					throw new Error("Incomplete sample read.");
				}
				data = Arrays.copyOf(data, numBytesRead);
			}
			double[] samples;

			if (bigEndian) {
				samples = DataUtil.bytesToValues(data, 0, data.length,
						sampleSizeInBytes, signed);
			} else {
				samples = DataUtil.littleEndianBytesToValues(data, 0,
						data.length, sampleSizeInBytes, signed);
			}

			return (new DoubleData(samples, (int) audioFormat.getSampleRate(),
					firstSampleNumber));
		}
	}

	/**
	 * Clears all cached audio data.
	 */
	public void clear() {
		audioList.clear();
	}

	/**
	 * Reads and returns the next Data object from RSB listener, return null if
	 * there is no more audio data. All audio data captured in-between
	 * <code>startRecording()</code> and <code>stopRecording()</code> is cached
	 * in an Utterance object. Calling this method basically returns the next
	 * chunk of audio data cached in this Utterance.
	 * 
	 * @return the next Data or <code>null</code> if none is available
	 * @throws DataProcessingException
	 *             if there is a data processing error
	 */
	@Override
	public Data getData() throws DataProcessingException {
		getTimer().start();

		Data output = null;

		if (!utteranceEndReached) {
			try {
				output = audioList.take();
			} catch (InterruptedException ie) {
				throw new DataProcessingException(
						"cannot take Data from audioList", ie);
			}
			if (output instanceof DataEndSignal) {
				System.out.println("UTTERANCE END!");
				utteranceEndReached = true;
			}
		}

		getTimer().stop();
		return output;
	}

	/**
	 * Returns true if there is more data in the RSB listener. This happens
	 * either if the a DataEndSignal data was not taken from the buffer, or if
	 * the buffer is not yet empty.
	 * 
	 * @return true if there is more data
	 */
	public boolean hasMoreData() {
		return !(utteranceEndReached && audioList.isEmpty());
	}

	private static AudioFormat getConfAudioFormat() {
		float sampleRate = 16000.0F;
		// 8000,11025,16000,22050,44100
		int sampleSizeInBits = 16;
		// 8,16
		int channels = 1;
		// 1,2
		boolean signed = true;
		// true,false
		boolean bigEndian = false;
		// true,false
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}
}
