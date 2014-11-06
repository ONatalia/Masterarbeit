import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	done.inpro.system.carchase.CarChaseViewerTest.class,
	done.inpro.system.carchase.HesitatingSynthesisIUTest.class,
	done.inpro.system.carchase.MyYamlTest.class,
	gov.nist.sphere.SphereExceptionUnitTest.class, 
	gov.nist.sphere.SphereHeaderItemUnitTest.class,
	inpro.annotation.AnnotationUtilUnitTest.class, 
	inpro.annotation.LabelUnitTest.class, 
	inpro.annotation.TextGridUnitTest.class, 
	inpro.apps.util.RecoCommandLineParserUnitTest.class, 
	inpro.incremental.util.TTSUtilTest.class,
	inpro.incremental.processor.SynthesisModuleUnitTest.class,
	inpro.incremental.processor.SynthesisModulePauseStopUnitTest.class,
	inpro.incremental.processor.SynthesisModuleAdaptationUnitTest.class,
	inpro.incremental.processor.ThreadingModuleTest.class,
	inpro.incremental.source.CurrentASRHypothesisTest.class,
	inpro.incremental.source.GoogleASRTest.class, 
	inpro.incremental.unit.HesitationIUTest.class,
	//inpro.incremental.unit.IncrementalCARTTest.class, // there aren't any tests in this anymore
	inpro.incremental.unit.IUPathTest.class,
	inpro.incremental.unit.IUUpdateListenerTest.class, 
	inpro.incremental.util.TTSUtilTest.class, 
	inpro.irmrsc.parser.SITDBSParserTest.class,
	inpro.nlu.AVMComposerTest.class,
	inpro.nlu.AVMWorldUtilTest.class,
	inpro.nlu.AVPairMappingUtilTest.class,
	inpro.pitch.util.ShortestPathTest.class,
	inpro.sphinx.frontend.ConversionUtilTest.class,
	inpro.sphinx.frontend.MonitorTest.class,
	inpro.synthesis.MaryAdapterIUUnitTest.class,
	inpro.synthesis.MaryAdapterMbrolaUnitTest.class,  
	inpro.synthesis.RevokingTest.class, 
	inpro.synthesis.SimpleSynthesis.class, 
//	work.inpro.alchemy.spatial.util.SpatialRandomUnitTest.class
	})
public class AllTests {

}
