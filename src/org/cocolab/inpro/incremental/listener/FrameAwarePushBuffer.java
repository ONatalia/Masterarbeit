package org.cocolab.inpro.incremental.listener;

import org.cocolab.inpro.incremental.PushBuffer;

public abstract class FrameAwarePushBuffer extends PushBuffer {

	int currentFrame = 0;

	public void setCurrentFrame(int frame) {
		currentFrame = frame;
	}
	
}
