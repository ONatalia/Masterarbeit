package inpro.config;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

public class SynthesisConfig implements Configurable {

	private static final int maxHmmContextValue = 999;

	@S4Integer(defaultValue = maxHmmContextValue, range = {0, maxHmmContextValue})
	public final static String PROP_HMM_OPT_PAST = "hmmOptPastContext";
	/** the (maximum) number of phonemes to consider preceding the current phone during HMM optimization */
	private int hmmOptimizationFutureContext = maxHmmContextValue;
	
	@S4Integer(defaultValue = maxHmmContextValue, range = {0, maxHmmContextValue})
	public final static String PROP_HMM_OPT_FUTURE = "hmmOptFutureContext";
	/** the (maximum) number of phonemes to consider following the current phone during HMM optimization */
	private int hmmOptimizationPastContext = maxHmmContextValue;

	
    private static byte[] dTreeFeatureDefaultsGerman = { // on my corpus with BITS-1 voice
        43, 0, 2, 1, 0, 0, 0, 6, 2, 0, 1, 0, 0, 1, 1, 43, 1, 0, 0, 0, 0, 43, 1, 1, 1, 0, 0, 2, 2, 0, 1, 0, 0, 0, 0, 0, 2, 2, 0, 0, 2, 2, 0, 0, 0, 9, 12, 9, 0, 0, 1, 1, 1, 6, 2, 0, 0, 0, 43, 0, 2, 0, 0, 43, 2, 0, 0, 0, 0, 0, 1, 1, 2, 0, 2, 0, 0, 1, 1, 2, 3, 0, 1, 1, 9, 1, 1, 3, 4, 0, 1, 3, 6, 7, 2, 1, 1, 1, 2, 1, 0, 0, 0, 6, 2, 3, 4, 4, 4, 4, 4
    };
    @SuppressWarnings("unused")
	private static byte[] dTreeFeatureDefaultsEnglish = { // on my corpus with CMU-SLT voice
        1, 0, 2, 2, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 17, 2, 0, 0, 0, 0, 35, 0, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 2, 1, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 6, 1, 0, 0, 0, 1, 0, 1, 0, 0, 1, 2, 0, 0, 0, 0, 0, 1, 1, 1, 0, 2, 0, 0, 1, 0, 0, 2, 0, 0, 12, 0, 1, 4, 4, 0, 1, 0, 0, 7, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0
    };
    
    public enum DTreeContext { 
    	PHONE, NO_LOOKAHEAD, MINIMAL_LOOKAHEAD, CURRWORDN1, CURRWORDN2, CURRWORDIU, CURRPHRASEIU, IUINFORMED, PHRASE, FULL
    }
	
	private static SynthesisConfig defaultInstance;
	
	/** for now, this component ignores it's Sphinx-loading capability */
	@Override
	public void newProperties(PropertySheet arg0) throws PropertyException {
	}
	
	public void loadJavaProperties() {
		hmmOptimizationPastContext = Integer.parseInt(System.getProperty("inpro.synthesis.hmmOptPastContext", "999"));
		hmmOptimizationFutureContext = Integer.parseInt(System.getProperty("inpro.synthesis.hmmOptFutureContext", "999"));
	}
	
	private static void createDefaultInstance() {
		defaultInstance = new SynthesisConfig();
		defaultInstance.loadJavaProperties();
		// TODO: think about how to load data from Sphinx configuration files
	}
	
	public static SynthesisConfig getDefaultInstance() {
		if (defaultInstance == null)
			createDefaultInstance();
		return defaultInstance;
	}

	public int getHmmOptimizationPastContext() {
		return hmmOptimizationPastContext;
	}

	public int getHmmOptimizationFutureContext() {
		return 2;//hmmOptimizationFutureContext;
	}
	
	public byte[] getDTreeFeatureDefaults() {
		return dTreeFeatureDefaultsGerman; // FIXME!!
	}

	public DTreeContext getDTreeContext() {
		return DTreeContext.FULL;
	}

}
