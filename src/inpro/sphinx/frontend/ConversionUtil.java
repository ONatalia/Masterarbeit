package inpro.sphinx.frontend;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

import edu.cmu.sphinx.frontend.DoubleData;

/**
 * encapsulates conversion of DoubleData objects to a stream of bytes and back
 * @author timo
 */
public class ConversionUtil {
	
	public static int SPHINX_RTP_HEADER_LENGTH = 20;
	
	public static int SAMPLING_RATE = 16000;
	
	public static byte[] doubleDataToBytes(DoubleData dd) {
		long collectTime = dd.getCollectTime();
		long firstSampleNumber = dd.getFirstSampleNumber();
		int sampleRate = dd.getSampleRate();
		double[] da = dd.getValues();
		byte[] ba = new byte[SPHINX_RTP_HEADER_LENGTH
		                          // 2 * ByteUtil.NUM_BYTES_IN_LONG + 
		                          // 1 * ByteUtil.NUM_BYTES_IN_INT +
		                     + da.length * 2]; // ByteUtil.NUM_BYTES_IN_SHORT];
		ByteBuffer bb = ByteBuffer.wrap(ba);
		bb.putLong(collectTime);
		bb.putLong(firstSampleNumber);
		bb.putInt(sampleRate);
		for (double d : da) {
			short s = (short) d;
			bb.putShort(s);
		}
		return ba;
	}
	
	public static DoubleData bytesToDoubleData(byte[] ba) {
		ByteBuffer bb = ByteBuffer.wrap(ba);
		long firstSampleNumber = bb.getLong();
		int sampleRate = bb.getInt();
		double[] da = new double[(ba.length - 20) / 2];
		for (int i = 0; i < da.length; i++) {
			da[i] = bb.getShort();
		}
		return new DoubleData(da, sampleRate, firstSampleNumber);
	}

	public static byte[] doublesToBytes(double[] da) {
		ByteBuffer bb = ByteBuffer.allocate(da.length * 8);
		DoubleBuffer db = bb.asDoubleBuffer();
		db.put(da);
		byte[] ba = bb.array();
		return ba;
	}
	
	public static double[] bytesToDoubles(byte[] ba) {
		ByteBuffer bb = ByteBuffer.wrap(ba);
		DoubleBuffer db = bb.asDoubleBuffer();
		double[] da = new double[ba.length / 8];
		db.get(da);
		return da;
	}

	public static void main(String[] args) {
	}
	
}
