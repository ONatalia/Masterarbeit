package org.cocolab.inpro.sphinx.frontend;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

public class ConversionUtil {

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
