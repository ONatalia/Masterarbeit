package inpro.util;

public class TimeUtil {
	/** a frame lasts 0.01 seconds (= 10 milliseconds) */
	public static double FRAME_TO_SECOND_FACTOR = 0.01;
	/** a second lasts 1000 milliseconds */
	public static double SECOND_TO_MILLISECOND_FACTOR = 1000.0;

	public static long startupTime;
	static { startupTime = System.currentTimeMillis(); }
	
	public static long timeSinceStartupInMilliseconds() {
		return System.currentTimeMillis() - startupTime;
	}
	
	public static double timeSinceStartup() {
		return timeSinceStartupInMilliseconds() / SECOND_TO_MILLISECOND_FACTOR;
	}

}
