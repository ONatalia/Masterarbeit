package inpro.sphinx.frontend;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConversionUtilTest {

	@Test
	public void test() {
		double[] da = { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 };
		System.out.println("original double array (length " + da.length + "):");
		for (double d : da) {
			System.out.print(d + " ");
		}
		byte[] ba = ConversionUtil.doublesToBytes(da);
		System.out.println("\nas byte sequence (length " + ba.length + "):");
		for (byte b : ba) {
			System.out.print(b + " ");
		}
		double[] da2 = ConversionUtil.bytesToDoubles(ba);
		System.out.println("\nand converted back to double (length " + da2.length + "):");
		for (double d : da2) {
			System.out.print(d + " ");
		}
		assertArrayEquals(da, da, 0.001);
		System.out.println("\nzum Abschluss ein paar komische doubles:");
		byte[] batest = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
						  64, 63, 62, 61, 60, 59, 58, 57 };
		for (double d : ConversionUtil.bytesToDoubles(batest)) {
			System.out.print(d + " ");
		}
		System.out.println("\nthat's all folks");
	}

}
