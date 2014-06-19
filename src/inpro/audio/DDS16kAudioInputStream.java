package inpro.audio;

import javax.sound.sampled.AudioFormat;

import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;

public class DDS16kAudioInputStream extends DDSAudioInputStream {

	public DDS16kAudioInputStream(DoubleDataSource source) {
	/*	float sampleRate = 16000.0F;  //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16;  //8,16
        int channels = 1;     //1,2
        boolean signed = true;    //true,false
        boolean bigEndian = false;  //true,false */
		super(source, new AudioFormat(16000.0F, 16, 1, true, false));
	}
	
	@Override
	public String toString() {
		return "DDS16k: " + source.toString();
	}

}
