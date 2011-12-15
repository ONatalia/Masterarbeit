package org.cocolab.inpro.tts.hts;

import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.SysSegmentIU;

public class IUBasedFullPStream extends FullPStream {

    SysSegmentIU firstIU;
    SysSegmentIU currIU;
    int currIUFrameOffset;
    
    public IUBasedFullPStream(IU firstIU) {
    	while (firstIU != null && !(firstIU instanceof SysSegmentIU)) {
    		firstIU = firstIU.groundedIn().get(0);
    	}
    	if (firstIU == null) {
    		throw new IllegalArgumentException("the IU you gave me does not ground in a SysSegmentIU!");
    	}
    	this.firstIU = (SysSegmentIU) firstIU;
    	this.currIU = this.firstIU;
    	currIUFrameOffset = 0;
    }

    @Override
	public int getMaxT() {
        return 20;
    	//return getTrueLength();
    }
    
    public int getTrueLength() {
    	int t = 0;
    	while (hasFrame(t)) {
    		t++;
    	}
    	return t;
    }
    
    @Override
    public boolean hasNextFrame() {
    	return currIU != null && ((currPosition < currIUFrameOffset + currIU.durationInSynFrames()) 
    						     || currIU.getNextSameLevelLink() != null);
    }
    
    @Override
    public FullPFeatureFrame getNextFrame() {
    	while (currPosition >= currIUFrameOffset + currIU.durationInSynFrames()) {
    		currIUFrameOffset += currIU.durationInSynFrames();
    		currIU = (SysSegmentIU) currIU.getNextSameLevelLink();
    	}
		int dur = currIU.durationInSynFrames(); // the duration in frames (= the number of frames that should be there)
		int fra = currIU.getHMMSynthesisFrames().size(); // the number of frames available
		int req = currPosition - currIUFrameOffset; // the frame requested
		// just repeat/drop frames as necessary if the amount of frames available is not right
		currPosition++;
		return currIU.getHMMSynthesisFrames().get((int) (req * (fra / (double) dur)));
    }
    
    @Override
	public FullPFeatureFrame getFullFrame(int t) {
//    	throw new RuntimeException("not implemented");
    	setNextFrame(t);
    	return getNextFrame();
    }
    
}
