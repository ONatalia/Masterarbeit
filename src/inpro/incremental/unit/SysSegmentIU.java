package inpro.incremental.unit;

import inpro.annotation.Label;
import inpro.synthesis.MaryAdapter4internal;
import inpro.synthesis.PitchMark;
import inpro.synthesis.hts.FullPFeatureFrame;
import inpro.synthesis.hts.FullPStream;
import inpro.synthesis.hts.PHTSParameterGeneration;
import inpro.util.TimeUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import marytts.htsengine.HTSModel;

import org.apache.log4j.Logger;

public class SysSegmentIU extends SegmentIU {
	
	private static Logger logger = Logger.getLogger(SysSegmentIU.class);
	
	Label plannedLabel; // -> alternatively store "realizedLabel"
	/** the label that was originally planned by TTS, before any stretching has been done */ 
	Label originalLabel;
	List<PitchMark> pitchMarks;
	HTSModel htsModel = null;
	List<FullPFeatureFrame> hmmSynthesisFeatures;
	public double pitchShiftInCent = 0.0;
	/** the state of delivery that this unit is in */
	Progress progress = Progress.UPCOMING;
	/** the number of frames that this segment has already been going on */
	int realizedDurationInSynFrames = 0;
	
	boolean awaitContinuation; // used to mark that a continuation will follow, even though no fSLL exists yet.
	
	public SysSegmentIU(Label l, List<PitchMark> pitchMarks, HTSModel htsModel, List<FullPFeatureFrame> featureFrames) {
		super(l);
		plannedLabel = new Label(l);
		this.pitchMarks = pitchMarks;
		this.htsModel = htsModel;
		this.hmmSynthesisFeatures = featureFrames;
	}
	
	public SysSegmentIU(Label l, List<PitchMark> pitchMarks) {
		this(l, pitchMarks, null, null);
	}
	
	@Override
	public StringBuilder toMbrolaLine() {
		StringBuilder sb = l.toMbrola();
		for (PitchMark pm : pitchMarks) {
			sb.append(" ");
			sb.append(pm.toString());
		}
		sb.append("\n");
		return sb;
	}
	
	@Override
	public void appendMaryXML(StringBuilder sb) {
		sb.append("<ph d='");
		sb.append((int) (l.getDuration() * TimeUtil.SECOND_TO_MILLISECOND_FACTOR));
		sb.append("' end='");
		sb.append(l.getEnd());
		sb.append("' f0='");
		for (PitchMark pm : pitchMarks) {
			sb.append(pm.toString());
		}
		sb.append("' p='");
		sb.append(l.getLabel());
		sb.append("'/>\n");
	}
	
	public void setHmmSynthesisFrames(List<FullPFeatureFrame> hmmSynthesisFeatures) {
		this.hmmSynthesisFeatures = hmmSynthesisFeatures;
		assert Math.abs(hmmSynthesisFeatures.size() - duration() * 200) < 0.001 : "" + hmmSynthesisFeatures.size() + " != " + (duration() * 200) + " in " + toString();
	}
	
	/**	the duration of this segment in multiples of 5 ms */
	public int durationInSynFrames() {
		return (int) Math.round(duration() * FullPStream.FRAMES_PER_SECOND / Double.valueOf(System.getProperty("inpro.tts.tempoScaling", "1.0")));
	}
	
	public double originalDuration() {
		if (originalLabel != null)
			return originalLabel.getDuration();
		else
			return duration();
	}
	
	/** adds fSLL, and allows continuation of synthesis */
	@Override
	public synchronized void addNextSameLevelLink(IU iu) {
		super.addNextSameLevelLink(iu);
		awaitContinuation = false;
		notifyAll();
	}
	
	/**
	 * tell this segment that it will be followed by more input later on
	 * @return whether a continuation is possible (i.e., the segment hasn't been completed yet
	 */
	public synchronized boolean setAwaitContinuation(boolean b) {
		awaitContinuation = b;
		return !isCompleted();
	}
	
	// awaits the awaitContinuation field to be cleared
	private synchronized void awaitContinuation() {
		while (awaitContinuation) {
			try { wait(); } catch (InterruptedException e) {}
		}		
	}
	
	/** helper, append HMM if possible, return emission length */
	private static int appendSllHtsModel(List<HTSModel> hmms, IU sll) {
		int length = 0;
		if (sll != null && sll instanceof SysSegmentIU) {
			HTSModel hmm = ((SysSegmentIU) sll).htsModel;
			hmms.add(hmm);
			length = hmm.getTotalDur();
		}
		return length;
	}
	
	private static PHTSParameterGeneration paramGen = null;

	// synthesizes 
	private void generateParameterFrames() {
		assert this.htsModel != null;
		List<HTSModel> localHMMs = new ArrayList<HTSModel>(3);
		int start = appendSllHtsModel(localHMMs, getSameLevelLink() != null ? getSameLevelLink().getSameLevelLink() : null); 
		start += appendSllHtsModel(localHMMs, getSameLevelLink()); 
		localHMMs.add(htsModel);
		int length = htsModel.getTotalDur();
		awaitContinuation();
		appendSllHtsModel(localHMMs, getNextSameLevelLink());
		appendSllHtsModel(localHMMs, getNextSameLevelLink() != null ? getNextSameLevelLink().getNextSameLevelLink() : null);
		// make sure we have a paramGenerator
		if (paramGen == null) { paramGen = MaryAdapter4internal.getNewParamGen(); }
		FullPStream pstream = paramGen.buildFullPStreamFor(localHMMs);
		hmmSynthesisFeatures = new ArrayList<FullPFeatureFrame>(length);
		for (int i = start; i < start + length; i++)
			hmmSynthesisFeatures.add(pstream.getFullFrame(i));
		//assert htsModel.getNumVoiced() == pitchMarks.size();
	}
	
	public FullPFeatureFrame getHMMSynthesisFrame(int req) {
		if (hmmSynthesisFeatures == null) {
			generateParameterFrames();
		}
		assert req >= 0;
		setProgress(Progress.ONGOING);
		assert req < durationInSynFrames();
	//	req = Math.max(req, durationInSynFrames() - 1);
		int dur = durationInSynFrames(); // the duration in frames (= the number of frames that should be there)
		int fra = hmmSynthesisFeatures.size(); // the number of frames available
		// just repeat/drop frames as necessary if the amount of frames available is not right
		int frameNumber = (int) (req * (fra / (double) dur));
		FullPFeatureFrame frame =  hmmSynthesisFeatures.get(frameNumber);
		try { // übler hack!
		if (frame != null && frame.isVoiced()) {
			//FIXME: investigate why this is necessary
			frameNumber = Math.max(0, Math.min(frameNumber, pitchMarks.size() - 1)); 
			frame.setf0Par(pitchMarks.get(frameNumber).getPitch());
			frame.shiftlf0Par(pitchShiftInCent);
		} 
		} catch (Exception e) {
			// dann halt nicht!!
			frame = new FullPFeatureFrame(frame.getMcepParVec(), frame.getMagParVec(), frame.getStrParVec(), false, 0);
		}
		if (req == dur - 1) { // last frame 
			setProgress(Progress.COMPLETED);
//			logger.debug("completed " + deepToString());
			logger.debug("completed " + toMbrolaLine());
		}
		realizedDurationInSynFrames++;
		// check whether we've been requested to wait for our continuation
		if (realizedDurationInSynFrames == durationInSynFrames())
			awaitContinuation();
		return frame;
	}
	
	private void setProgress(Progress p) {
		if (p != this.progress) { 
			this.progress = p;
			notifyListeners();
			if (p == Progress.COMPLETED && getNextSameLevelLink() != null) {
				getNextSameLevelLink().notifyListeners();
			}
		}
	}
	
	/** 
	 * stretch the current segment by a given factor.
	 * NOTE: as time is discrete (in 5ms steps), the stretching factor that is actually applied
	 * may differ from the requested factor. 
	 * @param factor larger than 1 to lengthen, smaller than 1 to shorten; must be larger than 0
	 * @return the actual duration that has been applied 
	 */
	public double stretch(double factor) {
		assert factor > 0f;
		double newDuration = duration() * factor;
		return setNewDuration(newDuration);
	}
	
	public double stretchFromOriginal(double factor) {
		assert factor > 0f;
		double newDuration = originalDuration() * factor;
		return setNewDuration(newDuration);
	}
	
	/** TODO: must be reworked to use PitchMarks
	 * change the synthesis frames' pitch curve to attain the given pitch in the final pitch
	 * in a smooth manner
	 * @param finalPitch the log pitch to be reached in the final synthesis frame 
	 */
	public void attainPitch(double finalPitch) {
		ListIterator<FullPFeatureFrame> revFrames = hmmSynthesisFeatures.listIterator(hmmSynthesisFeatures.size());
	//	finalPitch = Math.log(finalPitch);
		double difference = finalPitch - revFrames.previous().getlf0Par();
		double adjustmentPerFrame = difference / hmmSynthesisFeatures.size();
		System.err.println("attaining pitch in " + toString() + " by " + difference);
		revFrames.next();
		// adjust frames in reverse order (in case this segment is already being synthesized)
		for (; revFrames.hasPrevious(); ) {
			FullPFeatureFrame frame = revFrames.previous(); 
			frame.setlf0Par(frame.getlf0Par() - difference);
			difference -= adjustmentPerFrame;
		}
	}
	
	/** 
	 * set a new duration of this segment; if this segment is already ongoing, the 
	 * resulting duration cannot be shortend to less than what has already been going on (obviously, we can't revert the past)  
	 * @return the actual new duration (multiple of 5ms) 
	 */
	public synchronized double setNewDuration(double d) {
		assert d >= 0;
		d = Math.max(d, realizedDurationInSynFrames / 200.f); 
		if (originalLabel == null) 
			originalLabel = this.l;
		Label oldLabel = this.l;
		// change duration to conform to 5ms limit
		double newDuration = Math.round(d * 200.f) / 200.f;
		shiftBy(newDuration - duration(), true);
		// correct this' label to start where it used to start (instead of the shifted version)
		this.l = new Label(oldLabel.getStart(), this.l.getEnd(), this.l.getLabel());
		return newDuration;
	}
	
	/** shift the start and end times of this (and possibly all following SysSegmentIUs */
	public void shiftBy(double offset, boolean recurse) {
		super.shiftBy(offset, recurse);
		this.plannedLabel = this.l;
	}

	@Override
	public Progress getProgress() {
		return progress;
	}
	
	/** SysSegmentIUs may have a granularity finer than 10 ms (5ms) */
	@Override
	public String toLabelLine() {
		return String.format(Locale.US,	"%.3f\t%.3f\t%s", startTime(), endTime(), toPayLoad());
	}

	public boolean isVoiced() {
		return htsModel != null && htsModel.getVoiced(3); // just assume that a segment is voiced if its center state is voiced 
	}
	
	/* the bug with a pitchmark landing in the wrong frame is triggered by
	 * 		SysInstallmentIU preheat = new SysInstallmentIU("der nächste termin am Montag den 14. Mai 10 bis 12 uhr betreff Einkaufen auf dem Wochenmarkt überschneidet sich mit dem termin:");
	 * in "überschneidet" y: will get one voiced frame to few, b wil get one too many
	 */
	public void setHTSModel(HTSModel hmm) {
		// FIXME: dirty hack, we should also check voicing for silence
		// FIXME: hmm.getNumVoiced() == pitchMarks.size() should always be the case!
		if (pitchMarks != null && !hmm.getPhoneName().equals("_") && hmm.getNumVoiced() == pitchMarks.size()) { 
			// check consistency of PMs and voicing information in the model
			// FIXME: this is weird, very seldomly there are more pitch marks than hmm states
			// assert hmm.getNumVoiced() == pitchMarks.size();
			assert hmm.getNumVoiced() <= pitchMarks.size() : hmm.getNumVoiced() + " != " + pitchMarks.size(); // check that there are enough pms, for now
			// re-position PMs to reflect positions of voiced frames in the model
			Iterator<PitchMark> pitchIt = pitchMarks.iterator();
			int frameTotal = hmm.getTotalDur();
			for (int frame = 0; frame < frameTotal; frame++) {
				if (hmm.getFrameVoicing(frame)) {
					assert pitchIt.hasNext() : "there should still be a PM left, something is wrong with getFrameVoicing, getTotalDur or the like";
					PitchMark pm = pitchIt.next();
					pm.setRelativePosition((0.0 + frame) / frameTotal);
				}
			}
			// FIXME: should work if the above (hmm.getNumVoiced() == pitchMarks.size()) would work
			// assert !pitchIt.hasNext() : "there shouldn't be any Pitchmarks left over, the foor loop above is broken";
			// let's just delete spurious PMs to get rid of them
			while (pitchIt.hasNext()) {
				pitchIt.next();
				pitchIt.remove();
				logger.debug("removing spurious PM in segment " + toPayLoad());
			}
		}
		htsModel = hmm;
	}

	/** copy all data necessary for synthesis -- i.e. the htsModel and pitchmarks */
	public void copySynData(SysSegmentIU newSeg) {
		assert payloadEquals(newSeg);
		this.l = newSeg.l;
		this.pitchMarks = newSeg.pitchMarks;
		setHTSModel(newSeg.htsModel);
	}

}
