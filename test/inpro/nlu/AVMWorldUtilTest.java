package inpro.nlu;

import org.junit.Test;

public class AVMWorldUtilTest {

	@Test
	public void test() {
		AVMWorldUtil.setAVMsFromFile(AVMWorldUtilTest.class.getResource("AVMWorldList").toString(), 
				 AVMStructureUtil.parseStructureFile(AVMWorldUtilTest.class.getResource("AVMStructure")));
	}

}
