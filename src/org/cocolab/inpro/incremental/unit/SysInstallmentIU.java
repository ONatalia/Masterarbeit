package org.cocolab.inpro.incremental.unit;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.cocolab.inpro.tts.IUUtil;
import org.cocolab.inpro.tts.MaryAdapter;

/**
 *
 * TODO: add support for canned audio (i.e. read from WAV and TextGrid, maybe even transparently)
 * TODO: cache previously synthesized audio
 * @author timo
 */
public class SysInstallmentIU extends InstallmentIU {
	
	AudioInputStream synthesizedAudio;
	
	@SuppressWarnings("unchecked") // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(String tts) {
		super(null, tts);
		InputStream is = MaryAdapter.getInstance().text2maryxml(tts);
		List<WordIU> words = Collections.<WordIU>emptyList();
		try {
			words = IUUtil.wordIUsFromMaryXML(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
		groundedIn = (List) words;
	}
	
	@SuppressWarnings("unchecked") // allow cast from List<WordIU> to List<IU>
	public SysInstallmentIU(String tts, List<WordIU> words) {
		this(tts);
		groundedIn = (List) words;
	}
	
	public void synthesize() {
		String mbrola = toMbrola();
		synthesizedAudio = MaryAdapter.getInstance().mbrola2audio(mbrola);
	}
	
	@SuppressWarnings("unchecked") // allow cast of groundedIn to List<WordIU> 
	private String toMbrola() {
		List<WordIU> words = (List<WordIU>) groundedIn();
		StringBuilder sb = new StringBuilder();
		for (WordIU word : words) {
			sb.append(word.toMbrolaLines());
		}
		sb.append("#\n");
		return sb.toString();
	}
	
	/** 
	 * returns true if this SysInstallmentIU starts with the given words
	 * silence words are ignored in the comparison
	 */
	public boolean matchesPrefix(List<WordIU> prefix) {
		
		return false;
	}
	
	public static void main(String[] args) {
		SysInstallmentIU installment = new SysInstallmentIU("hallo welt");
		installment.synthesize();
		sun.audio.AudioPlayer.player.start(installment.synthesizedAudio);
	}
}
