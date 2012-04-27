package inpro.synthesis.hts;

import java.util.ArrayList;
import java.util.List;

import marytts.htsengine.HTSPStream;

/**
 * A stream of all Parameter Features ordered by time necessary for the vocoder.
 * i.e., the data that Mary/HTS stores in paramtypes->time ordering re-organized for time->paramtypes access,
 * (with paramtypes conveniently wrapped in  FullPFeatureFrames)
 * which, surprise!, allows for incremental production of parameter features.
 * @author timo
 */
public abstract class FullPStream {
	
	public static int FRAMES_PER_SECOND = 200;

	/** the current position in the feature stream */
    int currPosition = 0;
    
    public abstract FullPFeatureFrame getFullFrame(int t);
    
    public FullPFeatureFrame getNextFrame() {
    	return getFullFrame(currPosition++);
    }
    
	public boolean hasNextFrame() {
		return hasFrame(currPosition);
	}
	
	public void setNextFrame(int newPosition) {
	//	assert hasFrame(newPosition);
		currPosition = newPosition;
	}
	
    /** get a section of the FullPFeatureFrames in the list */
    public List<FullPFeatureFrame> getFullFrames(int start, int length) {
		List<FullPFeatureFrame> subList = new ArrayList<FullPFeatureFrame>(length);
		for (int t = start; t < start + length; t++) {
			subList.add(getFullFrame(t));
		}
		return subList;
    }
    
	public int getMcepParSize() { return getNextFrame().getMcepParSize(); }
	public int getMcepVSize() { return getMcepParSize() * HTSPStream.NUM; }
	public int getStrParSize() { return getNextFrame().getStrParSize(); }
    
	public boolean hasFrame(int t) {
		return t < getMaxT();
	}
	
    public abstract int getMaxT();
}
