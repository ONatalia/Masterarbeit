package test.unit;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.unit.inpro.annotation.AnnotationUtilUnitTest;
import test.unit.inpro.annotation.LabelUnitTest;
import test.unit.inpro.annotation.TextGridUnitTest;
import test.unit.inpro.incremental.processor.AdaptableSynthesisModuleUnitTest;
import test.unit.inpro.incremental.processor.SynthesisModuleUnitTest;
import test.unit.inpro.synthesis.MaryAdapterIUUnitTest;
import test.unit.inpro.synthesis.MaryAdapterMbrolaUnitTest;
import test.unit.work.inpro.alchemy.spatial.util.SpatialRandomUnitTest;


@RunWith(Suite.class)
@SuiteClasses({ AnnotationUtilUnitTest.class, LabelUnitTest.class, 
	TextGridUnitTest.class, 
	//SphereExceptionUnitTest.class, SphereHeaderItemUnitTest.class,
	MaryAdapterMbrolaUnitTest.class, MaryAdapterIUUnitTest.class, 
	SynthesisModuleUnitTest.class, AdaptableSynthesisModuleUnitTest.class, 
	SpatialRandomUnitTest.class })
public class AllTests {

}
