package inpro.irmrsc.util;

import inpro.irmrsc.rmrs.Formula;
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

/**
 * A helper class to load the semantic macros, the mapping from syntactic rules
 * to semantic macros and the POS-tag lexicon.
 * @author Andreas Peldszus
 */
public class RMRSLoader {
	
	/**
	 * Loads a xml specification of semantic macros from a given url.
	 * @param url the specified url of the xml to load
	 * @return the list of semantic macros
	 */
	@SuppressWarnings("unchecked")
	public static List<SemanticMacro> loadMacros (URL url) {
		List<SemanticMacro> l = new ArrayList<SemanticMacro>();
		SAXBuilder builder = new SAXBuilder();	
		try {
			Document doc = builder.build(url.openStream());
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

	/**
	 * Loads a xml specification of the mapping between syntactic rules IDs
	 * and semantic macro longnames. 
	 * @param url the specified url of the xml to load
	 * @return the mapping
	 */
	@SuppressWarnings("unchecked")
	public static Map<String,String> loadRules (URL url) {
		Map<String,String> map = new HashMap<String,String>();
		SAXBuilder builder = new SAXBuilder();	
		try {
			Document doc = builder.build(url.openStream());
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
	
	/**
	 * Loads a xml specification of the mapping between POS-tags and the
	 * basic semantic types that lexical predicate formulas of words with
	 * this tags receive.
	 * @param url the specified url of the xml to load
	 * @return the mapping from POS-tags as Strings to {@link Variable.Type}s
	 */
	@SuppressWarnings("unchecked")
	public static Map<String,Variable.Type> loadTagLexicon (URL url) {
		Map<String,Variable.Type> map = new HashMap<String,Variable.Type>();
		SAXBuilder builder = new SAXBuilder();	
		try {
			Document doc = builder.build(url.openStream());
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
						System.out.println("Warning: No specific semantic type given for tag "+tag+".");
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
	
}
