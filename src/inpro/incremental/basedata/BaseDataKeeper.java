package inpro.incremental.basedata;

import weka.core.Instance;

/**
 * stores information about the real world(tm)
 * 
 * this interface allows access to feature streams
 * (in principle acoustic or other), which describe
 * the real world on which our IUs are based. An 
 * interested IU (for example, SegmentIU) will have
 * a link to BaseData and can access it for further
 * considerations
 * 
 * @author timo
 */
public interface BaseDataKeeper {
	
	double getVoicing(double time);
	
	double getPitchInCent(double time);
	
	double getLoudness(double time);

	double getSpectralTilt(double time);

	public double getSpectralTiltQual(double time);
	
	public Instance getEOTFeatures(double time);
}
