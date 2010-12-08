package org.cocolab.inpro.incremental.unit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.cocolab.inpro.incremental.util.TTSUtil;
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
			words = TTSUtil.wordIUsFromMaryXML(is);
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
	
	private String toMbrola() {
		List<WordIU> words = myWords();
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
		Iterator<WordIU> myIter= myWords().iterator();
		boolean isPrefix = true;
		for (WordIU prefWord : prefix) {
			if (!prefWord.isSilence()) {
				WordIU myWord = myIter.hasNext() ? myIter.next() : null;
				while (myWord != null && myIter.hasNext() && myWord.isSilence()) {
					myWord = myIter.next();
				}
				if (!prefWord.spellingEquals(myWord)) {
					isPrefix = false;
				}
			}
		}
		return isPrefix;
	}
	
	public List<WordIU> getPrefix(List<WordIU> prefix) {
		assert matchesPrefix(prefix);
		List<WordIU> myPrefix = new ArrayList<WordIU>();
		Iterator<WordIU> myIter= myWords().iterator();
		for (WordIU prefWord : prefix) {
			if (!prefWord.isSilence()) { // skip silences in user input
				WordIU myWord = myIter.next();
				while (myWord.isSilence()) { // skip silences in tts input
					myWord = myIter.next();
				}
				if (prefWord.spellingEquals(myWord)) {
					myPrefix.add(myWord);
				} else {
					break;
				}
			}
		}
		return myPrefix;
	}
	
	/** get all the words (including silences) that follow the prefix */
	public List<WordIU> getRemainder(List<WordIU> prefix) {
		assert matchesPrefix(prefix);
		Iterator<WordIU> myIter= myWords().iterator();
		List<WordIU> remainder = new ArrayList<WordIU>();
		WordIU myWord = null;
		for (WordIU prefWord : prefix) {
			if (!prefWord.isSilence()) {
				myWord = myIter.next();
				if (!myWord.isSilence() && !prefWord.spellingEquals(myWord)) {
					break;
				}
			}
		}
		if (myWord != null) {
			remainder.add(myWord);
		}
		while (myIter.hasNext()) {
			remainder.add(myIter.next());
		}
		return Collections.<WordIU>unmodifiableList(remainder);
	}
	
	@SuppressWarnings("unchecked") // allow cast of groundedIn to List<WordIU> 
	private List<WordIU> myWords() {
		return (List<WordIU>) groundedIn();
	}
	
	public static void main(String[] args) {
		SysInstallmentIU installment = new SysInstallmentIU("hallo welt");
		installment.synthesize();
		sun.audio.AudioPlayer.player.start(installment.synthesizedAudio);
	}
}
