package test.unit;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.unit.inpro.annotation.AnnotationUtilUnitTest;
import test.unit.inpro.annotation.LabelUnitTest;
import test.unit.gov.nist.sphere.SphereHeaderItemUnitTest;
import test.unit.gov.nist.sphere.SphereExceptionUnitTest;
import test.unit.work.inpro.alchemy.spatial.util.SpatialRandomUnitTest;


@RunWith(Suite.class)
@SuiteClasses({ AnnotationUtilUnitTest.class, LabelUnitTest.class, 
	SphereExceptionUnitTest.class, SphereHeaderItemUnitTest.class,
	SpatialRandomUnitTest.class })
public class AllTests {

}
