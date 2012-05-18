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
		long collectTime = bb.getLong();
		long firstSampleNumber = bb.getLong();
		int sampleRate = bb.getInt();
		double[] da = new double[(ba.length - 20) / 2];
		for (int i = 0; i < da.length; i++) {
			da[i] = bb.getShort();
		}
		return new DoubleData(da, sampleRate, collectTime, firstSampleNumber);
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
		double[] da = { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 };
		System.out.println("original double array (length " + da.length + "):");
		for (double d : da) {
			System.out.print(d + " ");
		}
		byte[] ba = doublesToBytes(da);
		System.out.println("\nas byte sequence (length " + ba.length + "):");
		for (byte b : ba) {
			System.out.print(b + " ");
		}
		double[] da2 = bytesToDoubles(ba);
		System.out.println("\nand converted back to double (length " + da2.length + "):");
		for (double d : da2) {
			System.out.print(d + " ");
		}
		System.out.println("\nzum Abschluss ein paar komische doubles:");
		byte[] batest = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
						  64, 63, 62, 61, 60, 59, 58, 57 };
		for (double d : bytesToDoubles(batest)) {
			System.out.print(d + " ");
		}
		System.out.println("\nthat's all folks");
	}
	
}
