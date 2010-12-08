package org.cocolab.inpro.incremental.listener;

import org.cocolab.inpro.incremental.FrameAware;
import org.cocolab.inpro.incremental.PushBuffer;

public abstract class FrameAwarePushBuffer extends PushBuffer implements FrameAware {

	int currentFrame = 0;

	@Override
	public void setCurrentFrame(int frame) {
		currentFrame = frame;
	}
	
}
