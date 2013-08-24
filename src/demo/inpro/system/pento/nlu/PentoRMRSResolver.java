package demo.inpro.system.pento.nlu;

import inpro.incremental.processor.RMRSComposer.Resolver;
import inpro.irmrsc.rmrs.Formula;
import inpro.irmrsc.rmrs.SimpleAssertion;
import inpro.irmrsc.rmrs.SimpleAssertion.Type;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.xml.bind.JAXBException;

import work.inpro.gui.pentomino.january.GameCanvas;
import work.inpro.system.pentomino.Column;
import work.inpro.system.pentomino.Field;
import work.inpro.system.pentomino.PentoObject;
import work.inpro.system.pentomino.Piece;
import work.inpro.system.pentomino.Row;
import work.inpro.system.pentomino.Setting;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4String;

public class PentoRMRSResolver implements Resolver {
	
	/** the pento setting to work with */
	private Setting setting;
	/** config for creating setting from XML */
 	@S4String(defaultValue = "")
	public final static String PROP_SETTING_XML = "settingXML";
	/** a list of lemmata to ignore when resolving */
	private List<String> skipList = new ArrayList<String>();
	/** a mapping of rmrs lemmata to pento attributes */
	Map<String,String> mappedAttributeLemmata = new HashMap<String,String>();
 	/** a canvas to display current setting */
	private GameCanvas gameCanvas;
	/** a frame for display the setting's canvas */
	private static JFrame gameFrame = new JFrame("Pento Setting");

	/** boolean for determining whether to show the setting */
	@S4Boolean(defaultValue = true)
	public final static String PROP_SHOW_SETTING = "showSetting";
	private boolean showSetting;

	private boolean updateSetting = true; // Change to suppress changes to the GUI

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		showSetting = ps.getBoolean(PROP_SHOW_SETTING);
		try {
			setting = Setting.createFromXML(new URL(ps.getString(PROP_SETTING_XML)).openStream());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// FIXME: load this from somewhere else...
		skipList.add("def");
		skipList.add("nehmen");
		skipList.add("legen");
		skipList.add("löschen");
		skipList.add("drehen");
		skipList.add("spiegeln");
		skipList.add("addressee");
		mappedAttributeLemmata.put("rot","red");
		mappedAttributeLemmata.put("gelb","yellow");
		mappedAttributeLemmata.put("grün","green");
		mappedAttributeLemmata.put("blau","blue");
		mappedAttributeLemmata.put("grau","gray");
		mappedAttributeLemmata.put("kreuz","X");
		mappedAttributeLemmata.put("gewehr","N");
		mappedAttributeLemmata.put("schlange","Z");
		mappedAttributeLemmata.put("treppe","W");
		mappedAttributeLemmata.put("brücke","U");
		mappedAttributeLemmata.put("handy","P");
		mappedAttributeLemmata.put("krücke","Y");
		mappedAttributeLemmata.put("balken","I");
		mappedAttributeLemmata.put("winkel","V");
		//mappedAttributeLemmata.put("ecke","V"); Grrrrr, piece and location have the same lemma
		mappedAttributeLemmata.put("mast","T");
		mappedAttributeLemmata.put("pistole","F");
		mappedAttributeLemmata.put("sieben","L");
		// Build the gui
		if (showSetting) {
			this.updateCanvas();
			this.drawScene();			
		}
	}
	
	public Setting getSetting() {
		return setting;
	}	

	@SuppressWarnings("unchecked")
	@Override
	public Map<Integer, List<?>> resolvesObjects(Formula f) {
		Map<Integer, List<?>> matches = new HashMap<Integer, List<?>>();
		List<SimpleAssertion> transitives = new ArrayList<SimpleAssertion>();
		List<SimpleAssertion> ditransitives = new ArrayList<SimpleAssertion>();
		// Sort assertions, since we want to do transitives before ditransitives
		// Also initialize a map for each predicate argument to all world objects (these lists will be reduced below)
		for (SimpleAssertion sa : f.getNominalAssertions()) {
			if (skipList.contains(sa.getPredicateName())) {
				continue;
			} else if (sa.getType() == Type.TRANSITIVE) {
				transitives.add(sa);
				matches.put(sa.getArguments().get(0), this.setting.getPentoObjects());
			} else if (sa.getType() == Type.DITRANSITIVE) {
				ditransitives.add(sa);
				matches.put(sa.getArguments().get(0), this.setting.getPentoObjects());
				matches.put(sa.getArguments().get(1), this.setting.getPentoObjects());
			}
		}
		//logger.warn("New Formula - Trans: " + transitives.toString() + " Ditrans: " + ditransitives.toString());
		// Reduce the number of objects that resolve for each transitive argument
		for (SimpleAssertion sa : transitives) {
			List<PentoObject> newMatches = new ArrayList<PentoObject>();
			for (PentoObject po : (List<PentoObject>) matches.get(sa.getArguments().get(0))) {
				if (transitivePredicateResolvesObject(po, sa.getPredicateName())) {
					newMatches.add(po);
				}
			}
			matches.put(sa.getArguments().get(0), newMatches);
		}
		// Reduce the number of objects that resolve for each ditransitive argument
		for (SimpleAssertion dsa : ditransitives) {
			List<PentoObject> firstObjectMatches = new ArrayList<PentoObject>();
			List<PentoObject> secondObjectMatches = new ArrayList<PentoObject>();
			for (PentoObject firstObject : (List<PentoObject>) matches.get(dsa.getArguments().get(0)) ) {
				for (PentoObject secondObject : (List<PentoObject>) matches.get(dsa.getArguments().get(1)) ) {
					if (this.ditransitivePredicateResolvesObjects(firstObject, secondObject, dsa.getPredicateName())) {
						if (!firstObjectMatches.contains(firstObject))
							firstObjectMatches.add(firstObject);
						if (!secondObjectMatches.contains(secondObject))
							secondObjectMatches.add(secondObject);
					}
				}
			}
			
			matches.put(dsa.getArguments().get(0), firstObjectMatches);
			matches.put(dsa.getArguments().get(1), secondObjectMatches);
		}
		if (this.showSetting) {
			for (Integer id : matches.keySet()) {
				for (Piece piece : this.setting.getPieces()) {
					piece.setSelect(false);
					for (PentoObject po : (List<PentoObject>) matches.get(id)) {
						if (po instanceof Piece) {
							System.out.println(po);
							((Piece) po).setSelect(true);
						}
					}					
				}
			}
			if (this.updateSetting)
				this.updateCanvas();
		}
		return matches;
	}
	
	@Override
	public int resolves(Formula f) {
		Map<Integer, List<?>> matches = this.resolvesObjects(f);
		if (matches.keySet().isEmpty()) {
			return 0;
		}
		boolean allUnique = true;
		boolean piecesUnique = true;
		for (Integer id : matches.keySet()) {
			if (matches.get(id).isEmpty()) {
				//logger.warn("Nothing resolved for predicate argument " + id);
				// else any empties? -> -1
				return -1;
			} else if (matches.get(id).size() > 1) {
				allUnique = false;
				if (matches.get(id).get(0) instanceof Piece) {
					piecesUnique = false;
				}
			}
		}
		if (allUnique || piecesUnique) {
			// all uniques or only pieces are uniques -> 1
			return 1;
		} else {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public int resolvesObject(Formula f, String tileId) {
		int ret = -1; // nothing matched yet
		Map<Integer, List<?>> matches = this.resolvesObjects(f);
		for (Integer id : matches.keySet()) {
			for (PentoObject po : (List<PentoObject>) matches.get(id)) {
				if (po instanceof Piece && po.getID().equals(tileId)) {
					if (matches.get(id).size() == 1) {
						return 1; // one object was a match and it was in a singleton list
					} else if (matches.get(id).size() == 2) {
						// if two, then they have to be in two different grids.
						if (!((PentoObject) matches.get(id).get(0)).getField().getGrid().equals(((PentoObject) matches.get(id).get(1)).getField().getGrid())) {
							return 1;
						} else {
							ret = 0;
						}
					} else {
						ret = 0; // continue, but at least we had a match in a longer list
					}
				}
			}
		}
		return ret;
	}
	
	public void updateSetting(Setting newsetting) {
		setting = newsetting;
		drawScene();
	}	

	public void setPerformDomainAction(boolean performActions) {
		updateSetting = performActions;
	}

	/**
	 * Method for checking if a PentoObject matches attributes.
	 * @param po the PentoObject to check
	 * @param predicate the attribute (lemmatized)
	 * @return true if po matched attribute
	 */
	private boolean transitivePredicateResolvesObject(PentoObject po, String predicate) {
		boolean resolved = false;
		if (mappedAttributeLemmata.containsKey(predicate)) {
			if (po instanceof Piece) {
				resolved = (Character.toString(((Piece) po).getType()).equals(mappedAttributeLemmata.get(predicate)) ||
						po.hasColor(mappedAttributeLemmata.get(predicate)) ||
						po.hasLabel(predicate));				
			} else if (po instanceof Field) {
				resolved = (po.hasColor(mappedAttributeLemmata.get(predicate)) ||
						po.hasLabel(predicate));
			}
		} else if (predicate.equals("oben")) {
			resolved = po.isTop();
		} else if (predicate.equals("unten")) {
			resolved = po.isBottom();
		} else if (predicate.equals("links")) {
			resolved = po.isLeft();
		} else if (predicate.equals("rechts")) {
			resolved = po.isRight();
		} else if (predicate.equals("ecke")) {
			resolved = po.isCorner();
		} else if (predicate.equals("mitte")) {
			resolved = po.isCentre();
		} else if (predicate.equals("teil") || predicate.toLowerCase().equals("pper")) { // lower casing because robust ppers are PPERs
			resolved = po instanceof Piece;
		} else if (predicate.equals("feld")) {
			resolved = po instanceof Field;
		} else if (predicate.equals("erste")) {
			resolved = po.isFirst();
		} else if (predicate.equals("zweite")) {
			resolved = po.isSecond();
		} else if (predicate.equals("dritte")) {
			resolved = po.isThird(); 
		} else if (predicate.equals("spalte")) {
			resolved = po instanceof Column;
		} else if (predicate.equals("zeile")) {
			resolved = po instanceof Row;
		} else if (predicate.equals("horizontal")) {
			resolved = false;
		} else if (predicate.equals("vertikal")) {
			resolved = false;
		}
		return resolved;
	}
	
	/**
	 * Method for checking if two PentoObject resolve with a given predicate.
	 * For instance, in(x,y) is true if x is in y and x is a piece and y a tile...
	 * @param firstObject the first object
	 * @param secondObject the second object
	 * @param predicate the predicate to check the two objects with
	 */
	private boolean ditransitivePredicateResolvesObjects(PentoObject firstObject, PentoObject secondObject, String predicate) {
		boolean resolves = false;
		if (predicate.equals("in") ||
				predicate.equals("aus")) {
			resolves = firstObject.isIn(secondObject);
			// Use this instead if you only want pieces to be in things, not fields
//			resolves = firstObject.isIn(secondObject) &&
//					firstObject instanceof Piece &&
//					!(secondObject instanceof Piece);
		} else if (predicate.equals("auf") ||
				predicate.equals("über")) {
			resolves = firstObject.isAbove(secondObject) &&
					firstObject instanceof Piece &&
					secondObject instanceof Piece;
		} else if (predicate.equals("neben")) {
			resolves = firstObject.isNextTo(secondObject) &&
					firstObject instanceof Piece &&
					secondObject instanceof Piece;
		} else if (predicate.equals("unter")) {
			resolves = firstObject.isBelow(secondObject) &&
					firstObject instanceof Piece &&
					secondObject instanceof Piece;
		}
		return resolves;
	}

	/**	must be called to update the gameCanvas to reflect the current setting */
	private synchronized void updateCanvas() {	
		gameCanvas = new GameCanvas(setting);
		gameCanvas.hideCursor();
		gameCanvas.setVisible(true);
		gameFrame.setContentPane(gameCanvas);
		gameFrame.validate();
		gameFrame.repaint();
	}

	/**
	 * Sets up the scene from local variable for setting XML.
	 */
	private void drawScene() {
		try {
			gameCanvas = new GameCanvas(setting);
			gameCanvas.hideCursor();
			resetGUI(this.setting, "Pento Canvas", this.gameCanvas);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** 
	 * Resets GUI 
	 */
	protected static void resetGUI(Setting setting, String frameName, GameCanvas c) {
		gameFrame.setName(frameName);
		gameFrame.setContentPane(c);
        gameFrame.pack();
		gameFrame.setSize(setting.getGrids().get(0).getNumColumns()*115, setting.getGrids().get(0).getNumRows()*120);
		gameFrame.setVisible(true);
	}
	
}