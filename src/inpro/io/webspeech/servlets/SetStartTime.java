package inpro.io.webspeech.servlets;

import inpro.util.TimeUtil;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class SetStartTime extends HttpServlet {

	static Logger log = Logger.getLogger(SetStartTime.class.getName());
	/**
	 * Simple class to set the start time which can be called from JavaScript. 
	 */
	private static final long serialVersionUID = 1L;
	private boolean alignFirstOnly;
	private boolean firstIsAligned;
	
	
	public SetStartTime(boolean alignFirst) {
		this.setAlignFirstOnly(alignFirst);
		this.setFirstIsAligned(false);
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		long time = System.currentTimeMillis();
		if (this.getAlignFirstOnly() && !this.isFirstIsAligned()) {
			log.info("Setting start time " + time);
			TimeUtil.startupTime = time;
			DialogAsrResult.previousTimestamp = time;
		}
		else if (!this.getAlignFirstOnly()) {
			log.info("Setting start time " + time);
			TimeUtil.startupTime = time;
			DialogAsrResult.previousTimestamp = time;
		}		
		this.setFirstIsAligned(true);
	}


	public Boolean getAlignFirstOnly() {
		return alignFirstOnly;
	}


	public void setAlignFirstOnly(Boolean alignFirstOnly) {
		this.alignFirstOnly = alignFirstOnly;
	}


	public boolean isFirstIsAligned() {
		return firstIsAligned;
	}


	public void setFirstIsAligned(boolean firstIsAligned) {
		this.firstIsAligned = firstIsAligned;
	}
	
}