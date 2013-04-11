package inpro.synthesis.hts;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * 
 * concurrency: this class allows concurrent access by one reading and one appending thread
 * 
 */
public class ListBackedFullPStream extends FullPStream {
	// TODO: think about whether it's wise to really store this in an ArrayList, 
	// instead of e.g. a linked list of arraylists, one per append operation 
	// OR simply: a queue (which could also block if nothing is available
	List<FullPFeatureFrame> frames = Collections.synchronizedList(new ArrayList<FullPFeatureFrame>());
	
	@Override
	public FullPFeatureFrame getFullFrame(int t) {
		return frames.get(t);
	}

	@Override
	public int getMaxT() {
		return frames.size();
	}

	public void appendFeatures(List<FullPFeatureFrame> fullFrames) {
		frames.addAll(fullFrames);
	}
	
	public void appendFeatures(FullPStream fullStream) {
		fullStream.setNextFrame(0);
		while (fullStream.hasNextFrame()) {
			frames.add(fullStream.getNextFrame());
		}
	}

}
