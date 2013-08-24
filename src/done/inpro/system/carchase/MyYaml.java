package done.inpro.system.carchase;


import java.awt.Point;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import done.inpro.system.carchase.CarChaseExperimenter.ShutdownAction;
import done.inpro.system.carchase.CarChaseExperimenter.TTSAction;
import done.inpro.system.carchase.CarChaseExperimenter.WorldAction;
import done.inpro.system.carchase.CarChaseExperimenter.WorldStartAction;

public class MyYaml extends Yaml {

	public MyYaml() {
		super();
		representer = new Representer() { // fix for java.awt.Point representation
	        @Override
	        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
	                Object propertyValue, Tag customTag) {
	            if (javaBean instanceof java.awt.Point && "location".equals(property.getName())) {
	                return null;
	            } else {
	                return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
	            }
	        }
		};
		representer.addClassTag(TTSAction.class, new Tag("!TTS"));
		representer.addClassTag(ShutdownAction.class, new Tag("!shutdown"));
		representer.addClassTag(WorldAction.class, new Tag("!world"));
		representer.addClassTag(WorldStartAction.class, new Tag("!worldstart"));
		representer.addClassTag(Point.class, new Tag("!p"));
		Constructor constructor = new Constructor();
		constructor.addTypeDescription(new TypeDescription(TTSAction.class, "!TTS"));
		constructor.addTypeDescription(new TypeDescription(ShutdownAction.class, "!shutdown"));
		constructor.addTypeDescription(new TypeDescription(WorldAction.class, "!world"));
		constructor.addTypeDescription(new TypeDescription(WorldStartAction.class, "!worldstart"));
		constructor.addTypeDescription(new TypeDescription(Point.class, "!p"));
		this.constructor = constructor;

	}
}
