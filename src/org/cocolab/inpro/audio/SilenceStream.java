package org.cocolab.inpro.audio;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * 
 * @author timo
 *
 */
public class SilenceStream extends AudioInputStream {
	
	/** 
	 * create an AudioInputStream that contains that many milliseconds of silence
	 * in 16000Hz, 1 channel, 16 bits per sample 
	 * (never mind signedness and endiannness: zero is zero) 
	 * @param milliseconds
	 */
	public SilenceStream(int milliseconds) {
		this(new AudioFormat(16000, 8 * 2, 1, true, true), milliseconds * 16 * 2);
	}

	public SilenceStream(AudioFormat format, long length) {
		super(new ZeroInputStream(format.getFrameSize() * length), format, length);
	}

	private static class ZeroInputStream extends InputStream {
		
		long remainingSize;
		
		private ZeroInputStream(long sizeInBytes) {
			remainingSize = sizeInBytes;
		}
		
		@Override
		public int read() throws IOException {
			if (remainingSize > 0) {
				remainingSize--;
				return 0;
			} else {
				return -1;
			}
		}
		
		@Override
		public int available() throws IOException {
			return (int) remainingSize;
		}
		
	}
	
}
