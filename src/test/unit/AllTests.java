package test.unit;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.unit.inpro.annotation.AnnotationUtilUnitTest;
import test.unit.inpro.annotation.LabelUnitTest;

@RunWith(Suite.class)
@SuiteClasses({ AnnotationUtilUnitTest.class, LabelUnitTest.class })
public class AllTests {

}
