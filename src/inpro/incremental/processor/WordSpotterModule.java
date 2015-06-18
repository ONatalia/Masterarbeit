package inpro.incremental.processor;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.unit.WordSpotterIU;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

public class WordSpotterModule extends IUModule {
	
 	@S4String(defaultValue = "resource/scopes.xml")
	public final static String PATH_PROP = "path";	
	
	HashMap<String,TreeSet<String>> scopes;
	IU prevIU = null;
	
//	private String prev = "<s>";
//	private String prevprev = prev;
 	
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		scopes = parseXMLFile(ps.getString(PATH_PROP));
	}
	
	public HashMap<String, TreeSet<String>> parseXMLFile(String string) {
		HashMap<String,TreeSet<String>> newScopes = new HashMap<String,TreeSet<String>>();
		try {
			File xmlFile = new File(string);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("scope");
			
			for (int temp = 0; temp < nList.getLength(); temp++){
				
				Node nNode = nList.item(temp);
				Element eElement = (Element) nNode;
				String scopeName = eElement.getAttribute("ID");
				
				if (!newScopes.containsKey(scopeName))
					newScopes.put(scopeName, new TreeSet<String>());
				
				Element newElement = (Element) nNode;
				NodeList childList = newElement.getElementsByTagName("item");
					
				for (int t = 0; t < childList.getLength(); t++){
					Element childElement = (Element) childList.item(t);
					newScopes.get(scopeName).add(childElement.getTextContent().trim().toLowerCase());
				}
			
			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return newScopes;
	}
	
	public String getScope(String word) {
		
		for (String scope: scopes.keySet()) {
			if (scopes.get(scope).contains(word))
				return scope;
		}
		return null;
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {

		ArrayList<EditMessage<WordSpotterIU>> newIUs = new ArrayList<EditMessage<WordSpotterIU>>();
		
		for (EditMessage<? extends IU> edit : edits) {
			IU iu = edit.getIU();
			if (iu instanceof WordIU) {
				if (iu != null || iu.getSameLevelLink() != null || iu.getSameLevelLink().toPayLoad().equals(WordIU.FIRST_WORD_IU.toPayLoad())) {
					prevIU = null;
				}
		

				String word = iu.toPayLoad().toLowerCase();
				String scope = getScope(word);


				if (scope == null) continue; // ignore this word if it's not in any scope
//				see if word is in one of the scopes lists
//				if it is, then make a new WordSpotterIU, add it to newIUs
				WordSpotterIU newIU = new WordSpotterIU(scope);
				newIU.setSameLevelLink(prevIU);
				newIU.groundIn(iu);
				newIUs.add(new EditMessage<WordSpotterIU>(EditType.ADD, newIU));
				prevIU = newIU;
			}
		}
		
		this.rightBuffer.setBuffer(newIUs);
	}

}
