package org.cocolab.inpro.domains.calendar;

import org.cocolab.inpro.incremental.unit.IU.IUUpdateListener;

import scalendar.adaptionmanager.AdaptionManager;

public class NoiseThread extends Thread {
	private SynthesisModule sm;
	private IUUpdateListener updateListener;
	private AdaptionManager am;
	
	public NoiseThread(AdaptionManager am, SynthesisModule sm, IUUpdateListener updateListener) {
		this.am = am;
		this.sm = sm;
		this.updateListener = updateListener;
	}
	
	public void run() {
		try {
			Thread.sleep(1300);
			System.out.println("BRRRRRRRRRRRRRRRUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUMMMMMMMMMMMMMM!");
			sm.playNoise("file:///Users/hendrik/Desktop/iNLG_iSS/pinknoise.1000ms.wav");
			Thread.sleep(850);
		} catch (InterruptedException e) {e.printStackTrace();}
		am.noGrounding();
		updateListener.update(null);
	}
	
}
