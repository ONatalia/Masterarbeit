package org.cocolab.inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

import org.cocolab.inpro.annotation.Label;
import org.cocolab.inpro.audio.DDS16kAudioInputStream;
import org.cocolab.inpro.audio.DispatchStream;
import org.cocolab.inpro.tts.MaryAdapter;
import org.cocolab.inpro.tts.MaryAdapter4internal;
import org.cocolab.inpro.tts.PitchMark;
import org.cocolab.inpro.tts.hts.FullPFeatureFrame;
import org.cocolab.inpro.tts.hts.FullPStream;
import org.cocolab.inpro.tts.hts.IUBasedFullPStream;
import org.cocolab.inpro.tts.hts.VocodingAudioStream;

import test.org.cocolab.inpro.synthesis.PatternDemonstrator;

public class HesitationIU extends SysSegmentIU {
	
	private static final int DEFAULT_DURATION = 300; // ms

	public HesitationIU(SegmentIU sll) {
		super(new Label(0.0, DEFAULT_DURATION * 0.001 + 0.1, "<hes>"), null);
		setSameLevelLink(sll);
		System.err.println(toMaryXML());
		FullPStream pstream = ((MaryAdapter4internal) MaryAdapter.getInstance()).maryxml2hmmFeatures(toMaryXML());
		List<FullPFeatureFrame> hmmSynthesisFeatures = pstream.getFullFrames(20, 80);
		for (FullPFeatureFrame frame : hmmSynthesisFeatures) {
			frame.setlf0Par(5f);
		}
		setHmmSynthesisFrames(hmmSynthesisFeatures);
		//stretch(1.5);
	}
	
	@Override
	public void appendMaryXML(StringBuilder sb) {
		List<PitchMark> pms = ((SysSegmentIU) getSameLevelLink()).pitchMarks;
		sb.append("<ph d='");
		sb.append(DEFAULT_DURATION);
		sb.append("' f0='");
		sb.append(pms.get(pms.size() - 1).toString());
		sb.append("' p='E' end='0.4'/>");
	}
	
	public String toMaryXML() {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"de\">\n<p>\n<s>\n<phrase>\n<t>\n<syllable>\n");
		((SegmentIU) getSameLevelLink()).appendMaryXML(sb);
		appendMaryXML(sb);
		sb.append("</syllable>\n</t>\n<boundary duration='500' />\n</phrase>\n</s>\n</p>\n</maryxml>");
		return sb.toString();
	}

	public static void main(String[] args) {
		MaryAdapter.getInstance();
		DispatchStream dispatcher = PatternDemonstrator.setupDispatcher();
		HesitationIU hes = new HesitationIU(new SysSegmentIU(new Label(0.0, 0.1, "n"), Collections.<PitchMark>singletonList(new PitchMark("(0,100)"))));
		dispatcher.playStream(new DDS16kAudioInputStream(new VocodingAudioStream(new IUBasedFullPStream(hes), true)), true);
		dispatcher.shutdown();
	}
	
}
