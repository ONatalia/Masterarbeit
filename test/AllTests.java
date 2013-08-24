import gov.nist.sphere.SphereExceptionUnitTest;
import gov.nist.sphere.SphereHeaderItemUnitTest;
import inpro.annotation.AnnotationUtilUnitTest;
import inpro.annotation.LabelUnitTest;
import inpro.annotation.TextGridUnitTest;
import inpro.incremental.processor.AdaptableSynthesisModuleUnitTest;
import inpro.incremental.processor.SynthesisModuleUnitTest;
import inpro.incremental.source.CurrentASRHypothesisTest;
import inpro.synthesis.MaryAdapterIUUnitTest;
import inpro.synthesis.MaryAdapterMbrolaUnitTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import work.inpro.alchemy.spatial.util.SpatialRandomUnitTest;

@RunWith(Suite.class)
@SuiteClasses({ AnnotationUtilUnitTest.class, LabelUnitTest.class, 
	TextGridUnitTest.class, 
	SphereExceptionUnitTest.class, SphereHeaderItemUnitTest.class,
	MaryAdapterMbrolaUnitTest.class, MaryAdapterIUUnitTest.class, 
	SynthesisModuleUnitTest.class, AdaptableSynthesisModuleUnitTest.class, 
	SpatialRandomUnitTest.class, CurrentASRHypothesisTest.class })
public class AllTests {

}
