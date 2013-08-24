package done.inpro.system.carchase;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class MyYamlTest {

	/** 
	 * test loading/printing of one config file
	 */
	@Test
	public void test() {
		Yaml yaml = new MyYaml();
		System.out.println(yaml.dumpAll(yaml.loadAll(MyYaml.class.getResourceAsStream("configs/config1")).iterator()));
	}

}
