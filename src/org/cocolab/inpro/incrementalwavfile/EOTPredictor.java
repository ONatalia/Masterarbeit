package org.cocolab.inpro.incrementalwavfile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;

import weka.classifiers.Classifier;
import weka.core.Instance;

public class EOTPredictor extends EOTFeatureAggregator {

	private Classifier classifier;
	
	/**
	 * load classifier from file
	 * @param filename name of the file the serialized classifier is stored in
	 * @return classifier from file
	 */
	public void loadClassifier(String filename) {
		try {
			InputStream is = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(is);
			classifier = (Classifier) ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public double getCurrentPrediction() {
		Instance inst = getNewestFeatures();
		double index = classifyInstance(inst);
// currently we do regression and do not need to lookup time predictions from class indices		
		return index;
	}
	
	/**
	 * 
	 * @param instance
	 * @return class index of the instance
	 */
	private double classifyInstance(Instance instance) {
		assert classifier != null;
		double classIndex = -1;
		try {
			classIndex = classifier.classifyInstance(instance);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return classIndex;
	}
	
}
