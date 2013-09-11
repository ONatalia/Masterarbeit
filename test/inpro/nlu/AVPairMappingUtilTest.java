package inpro.nlu;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class AVPairMappingUtilTest {

	@Test
	public void test() throws IOException {
		Map<String, List<AVPair>> avPairs;
		avPairs = AVPairMappingUtil.readAVPairs(AVPairMappingUtilTest.class.getResource("AVMapping"));
		System.out.println(avPairs.toString());
	}

}
