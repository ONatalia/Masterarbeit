package org.cocolab.inpro.pitch.util;

public class PitchUtils {

	public static final double CENT_CONST = 1731.2340490667560888319096172; // 1200 / ln(2)
	
	public static double hzToCent(double hz) {
		return CENT_CONST * Math.log(hz / 110); 
	}
	
	public static double centToHz(double cent) {
		return Math.exp(cent / CENT_CONST) * 110;
	}
	
}
