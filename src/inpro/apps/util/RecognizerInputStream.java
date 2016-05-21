package inpro.apps.util;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class RecognizerInputStream {

	
	private byte [] soundData=null;
	private AudioInputStream ais=null;
	
	public RecognizerInputStream(AudioInputStream ais) throws IOException {
		// TODO Auto-generated constructor stub
		this.ais=ais;
		soundData=new byte [(int) (ais.getFrameLength()*ais.getFormat().getFrameSize())*ais.getFormat().getChannels()];
		ais.read(soundData, 0, soundData.length);
	}
	
	public byte[] getSoundData() {
		return soundData;
	}

	public void setSoundData(byte[] soundData) {
		this.soundData = soundData;
	}

	public AudioInputStream getAis() {
		return ais;
	}

	public void setAis(AudioInputStream ais) {
		this.ais = ais;
	}
	
	public AudioFormat getFormat () {
		return ais.getFormat();
	}
	
	public int getChannels (){
		return ais.getFormat().getChannels();
	}
		
	public float getSampleRate (){
		return ais.getFormat().getSampleRate();
	}
	
	public int getSiteInBits (){
		return ais.getFormat().getSampleSizeInBits();
	}
	

	
	
	

}
