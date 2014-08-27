package inpro.audio;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;

public class FrontEndBackedAudioInputStream extends AudioInputStream {

	private final BaseDataProcessor frontend;
	private boolean speechEnd = false;
	private boolean dataEnd = false;
	
	public FrontEndBackedAudioInputStream(BaseDataProcessor fe) {
		super(null,new AudioFormat(16000, 16, 1, true, false), 0);
		frontend = fe;
	}
	
	@Override
	public int read(byte[] buf, int off, int len) {
		Data d=null; 
		
		//if still in speech get data from frontend
		if(!speechEnd)
		{
			d= frontend.getData();
		} else speechEnd=false;
		
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		int framesize = -1;
		
		//do this while data from frontend is not null
		while (d != null) {
			//check if data is DoubleData which means audio data
			if (d instanceof DoubleData) {
				
				//convert frame data back to raw data
				DoubleData dd = (DoubleData) d;
				double[] values = dd.getValues();
				if (framesize == -1)
					framesize = values.length * 2;
		
				for (double value : values) {
					try {
						short be = new Short((short) value);
						dos.writeByte(be & 0xFF);
						dos.writeByte((be >> 8) & 0xFF);


					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				//read new data from frontend if frame size is not exceeded
				if (baos.size() + framesize <= len) {
					d = frontend.getData();
				} else
					d = null;
			} else if(d instanceof SpeechEndSignal)
			{
				//stopp pulling if end of speech is reached
				speechEnd = true;
				break;
			} else if (d instanceof DataEndSignal) {
				dataEnd = true;
				break;
			}
			else	//get data from frontend if data is not yet containing audio data or an end signal
				d = frontend.getData();

		}
		assert buf.length >= baos.size() : "Please use a larger buffer for reading (or smaller DoubleData objects), or fix this.";
		//write the converted data to the output buffer
		System.arraycopy(baos.toByteArray(), 0, buf, 0, baos.size());

		// TODO Auto-generated method stub
		return baos.size();
	}

	public boolean hasMoreAudio() {
		return !dataEnd;
	}



}
