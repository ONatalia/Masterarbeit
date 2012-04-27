package inpro.features;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

import weka.classifiers.Classifier;
import weka.core.Instance;

public class EOTPredictor extends EOTFeatureAggregator {

	@S4String(defaultValue = "")
	private final static String PROP_CLASSIFIER = "classifier";

	private Classifier classifier;
	
	/**
	 * load classifier from file
	 * @param filename name of the file the serialized classifier is stored in
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
	
	public String getCurrentPrediction() {
		Instance inst = getNewestFeatures();
		double index = classifyInstance(inst);
// currently we do regression and do not need to lookup time predictions from class indices		
		int c = Double.valueOf(index).intValue();
		if (CLUSTERED_TIME) {
			return turnBin.value(c);
		}
		else {
			return "" + c;
		}
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
	
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		loadClassifier(ps.getString(PROP_CLASSIFIER));
	}
	
}
