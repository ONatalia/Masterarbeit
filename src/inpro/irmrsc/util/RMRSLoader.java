package inpro.irmrsc.util;

import inpro.irmrsc.rmrs.Formula;
import inpro.irmrsc.rmrs.SemanticMacro;
import inpro.irmrsc.rmrs.Variable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class RMRSLoader {
	
	@SuppressWarnings("unchecked")
	public static List<SemanticMacro> loadMacros (URL fileURL) {
		List<SemanticMacro> l = new ArrayList<SemanticMacro>();
		SAXBuilder builder = new SAXBuilder();	
		try {
			Document doc = builder.build(fileURL.openStream());
			Element root = doc.getRootElement();
			if (root.getName() == "semmacros") {
				List<Element> macros = root.getChildren("defmacro");
				for (Element m : macros) {
					String longname = m.getAttributeValue("longname");
					String shortname = m.getAttributeValue("shortname");		
					Formula f = new Formula();
					f.parseXML(m.getChild("rmrsincrement"));
					SemanticMacro newMacro = new SemanticMacro(shortname, longname, f);
					l.add(newMacro);
				}
			}
		} catch (IOException e) {
			System.out.println(e);
		} catch (JDOMException e) {
			System.out.println(e);
		}
		return l;
	}
	
	// read the mapping syntactic rule id <-> semantic macro longname
	@SuppressWarnings("unchecked")
	public static Map<String,String> loadRules (URL fileURL) {
		Map<String,String> map = new HashMap<String,String>();
		SAXBuilder builder = new SAXBuilder();	
		try {
			Document doc = builder.build(fileURL.openStream());
			Element root = doc.getRootElement();
			if (root.getName() == "semrules") {
				List<Element> rules = root.getChildren("defrule");
				for (Element rule : rules) {
					String id = rule.getAttributeValue("id");
					String longname = rule.getChild("refmacro").getAttributeValue("longname");
					map.put(id, longname);
				}
			}
		} catch (IOException e) {
			System.out.println(e);
		} catch (JDOMException e) {
			System.out.println(e);
		}
		return map;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,Variable.Type> loadTagLexicon (URL fileURL) {
		Map<String,Variable.Type> map = new HashMap<String,Variable.Type>();
		SAXBuilder builder = new SAXBuilder();	
		try {
			Document doc = builder.build(fileURL.openStream());
			Element root = doc.getRootElement();
			if (root.getName() == "taglexicon") {
				List<Element> entries = root.getChildren("map");
				for (Element entry : entries) {
					String tag = entry.getAttributeValue("tag");
					String type = entry.getAttributeValue("type");
					Variable.Type mType = Variable.Type.UNDERSPEC;
					if (type != null && type.length() == 1) {
						switch(type.charAt(0)) {
						case 'i' : mType = Variable.Type.INDEX; break;
						case 'x' : mType = Variable.Type.INDIVIDUAL; break;
						case 'e' : mType = Variable.Type.EVENT; break;
						case '_' : mType = null; break;
						}
					} else {
						System.out.println("Warning: No specific semantic type given for tag"+tag+".");
					}
					map.put(tag, mType);
				}
			}
		} catch (IOException e) {
			System.out.println(e);
		} catch (JDOMException e) {
			System.out.println(e);
		}
		return map;
	}
	
//	public static void main (String [] args) {
//		try {
//			List<SemanticMacro> l = loadMacros(new URL("file:/home/andreas/workspace/Inpro/src/org/cocolab/inpro/domains/pentomino/resources/irmrsc-semmacros.xml"));
//			Map<String,Formula> macrosLongname = new HashMap<String,Formula>();
//			Map<String,Formula> macrosShortname = new HashMap<String,Formula>();
//			for (Map.Entry<String, Formula> e : m.entrySet()) {
//				String[] name = e.getKey().split("##");
//				if (! name[0].equals("null")) macrosShortname.put(name[0], e.getValue());
//				if (! name[1].equals("null")) macrosLongname.put(name[0], e.getValue());
//			}
//
//			
//			Map<String,String> rules = loadRules(new URL("file:/home/andreas/workspace/Inpro/src/org/cocolab/inpro/domains/pentomino/resources/irmrsc-semrules.xml"));
//			System.out.println(rules);
//			for (Map.Entry<String, String> e : rules.entrySet()) {
//				String[] name = e.getValue().split("##");
//				System.out.println(Arrays.asList(name));
//				if (name[0].equals("null")) {
//					if (! macrosShortname.containsKey(name[1])) {
//						System.out.println("BAD");
//					} else {
//						System.out.println("good");
//					}
//				
//				} else {
//					if (! macrosLongname.containsKey(name[0])) {
//						System.out.println("BAD");
//					} else {
//						System.out.println("Good");
//					}
//				}
//			}
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//	}
//	
}
