package inpro.incremental.unit;

import java.util.Collections;
import java.util.List;

public class HesitationIU extends PhraseIU {

	public static final SysInstallmentIU protoHesitation;
	
	static { // setup and lengthen protoHesitation and add HMM features
		protoHesitation = new SysInstallmentIU("ähm?");
		//List<SysSegmentIU> segs = protoHesitation.getSegments();
		//for (IU seg : segs) {
			//((SysSegmentIU) seg).stretch(2); // unfortunately, stretching hurts synthesis quality -- why?
		//}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" }) // the cast for GRINs
	public HesitationIU() {
		super("<hes>", PhraseIU.PhraseType.NONFINAL);
		this.groundedIn = (List) protoHesitation.getSegments();
		protoHesitation.scaleDeepCopyAndStartAtZero(1f); // create new IU substructure for the next protohesitation
	}
	
	@Override
	public void addNextSameLevelLink(IU iu) {
		super.addNextSameLevelLink(iu);
		setToZeroDuration();
	}

	private void setToZeroDuration() {
		for (IU iu : getSegments()) {
			((SysSegmentIU) iu).setNewDuration(0f);
		}
	}
	
	/** a hesitation PhraseIU is it's own word */
	@Override
	public List<WordIU> getWords() {
		return Collections.<WordIU>singletonList(this);
	}

}
