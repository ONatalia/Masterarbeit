package org.cocolab.inpro.irmrsc.simplepcfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.io.*;
import java.net.URL;

import org.jdom.*;
import org.jdom.input.*;

import org.cocolab.inpro.irmrsc.simplepcfg.Production;

// TODO:
// - it is possible to load a grammar into an already existing grammar.
//   i am not sure whether this is actually useful or rather harmful.
// - rule probabilities can still be arbitrary. at some time this should be
//   consistently and mathematically grounded? .. but why should the grammar check?
// - allow adding a bunch of productions and updating only once in the end.
// - compute leftcorner set to reduce search space

public class Grammar {

	private Set<Symbol> mTerminals;
	private Set<Symbol> mNonTerminals;
	private Symbol mStart;
	private Symbol mEnd;
	private Map<String, Production> mProductions;
	private Map<Symbol, ArrayList<String>> mExpandsRelation;
	private Set<Symbol> mEliminable;
	
	public Grammar() {
		mProductions = new HashMap<String, Production>();
		this.update();
	}

	public void addProduction(String id, Symbol LHS, Symbol[] RHS, double Prob) {
		ArrayList<Symbol> rhslist = new ArrayList<Symbol>();
		for (Symbol sym : RHS) rhslist.add(sym);
		Production p = new Production(id, LHS, rhslist, Prob);
		this.addProduction(id, p);
	}
	
	public void addProduction(String id, Symbol LHS, ArrayList<Symbol> RHS, double Prob) {
		Production p = new Production(id, LHS, RHS, Prob);
		this.addProduction(id, p);
	}
	
	public void addProduction(String id, Production p) {
		mProductions.put(id, p);
		//TODO: warn if id != p.id 
		this.update();
	}

	public Symbol getStart() {
		return mStart;
	}
	
	public void setStart(Symbol sym) {
		if (sym != null) {
			mStart = sym;
			this.update();
		}
	}
	
	public Symbol getEnd() {
		return mEnd;
	}
	
	public boolean isTerminalSymbol(Symbol sym) {
//		if (sym.equals(mEnd)) {
//			return true;
//		}
		return mTerminals.contains(sym);
	}
	
	public boolean isNonTerminalSymbol(Symbol sym) {
		return mNonTerminals.contains(sym);
	}
	
	public boolean isEliminable(Symbol sym) {
		if (sym.equals(mEnd)) {
			return true;
		}
		return mEliminable.contains(sym);
	}
	
	public List<String> getProductionsExpandingSymbol(Symbol sym) {
		return mExpandsRelation.get(sym);
	}
	
	public Production getProduction(String id) {
		return mProductions.get(id);
	}
	
	public void update() {
		TreeSet<Symbol> symbols = new TreeSet<Symbol>(); 
		mTerminals = new TreeSet<Symbol>();
		mNonTerminals = new TreeSet<Symbol>();
		mEliminable = new TreeSet<Symbol>();
		mExpandsRelation = new HashMap<Symbol, ArrayList<String>>();
		// add start symbol
		if (mStart != null) {
			mNonTerminals.add(mStart);
		}
		// add end symbol
		if (mEnd != null) {
			mTerminals.add(mEnd);
			mEliminable.add(mEnd);
		}
		// for each production, add LHSs to NonTerminals and RHSs to symbol
		for (Map.Entry<String,Production> e : mProductions.entrySet()) {
			String id = (String) e.getKey();
			Production p = (Production) e.getValue();
			Symbol LHS = p.getLHS();
			if (mExpandsRelation.keySet().contains(LHS)) {
				mExpandsRelation.get(LHS).add(id);
			} else {
				ArrayList<String> l = new ArrayList<String>();
				l.add(id);
				mExpandsRelation.put(LHS, l);
			}
			mNonTerminals.add(LHS);
			if (p.getRHS().isEmpty()) {
				mEliminable.add(LHS);
			} else {
				for (Symbol sym : p.getRHS())
					symbols.add(sym);
			}
		}
		// compute terminals
		for (Symbol sym : symbols) {
			if (! (mNonTerminals.contains(sym))) {
				mTerminals.add(sym);
			}
		}
	}
	
	public void info() {
		System.out.println("Start: "+mStart);
		System.out.println("Terminals: "+mTerminals);
		System.out.println("NonTerminals: "+mNonTerminals);
		System.out.println("ExpandRel: "+mExpandsRelation);
		System.out.println("Eliminable: "+mEliminable);
		// print productions
		for (Production p : mProductions.values()) {
			System.out.println(p);
		}	
	}
	
	private boolean hasProductionWithID(String id) {
		return mProductions.keySet().contains(id);
	}
	
	@SuppressWarnings("unchecked")
	public void loadXML(URL url) {
		String filename = url.toString();
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(url);
			Element root = doc.getRootElement();
			if (root.getName() == "simplecfggrammar") {
				mStart = new Symbol(root.getAttributeValue("start"));
				mEnd = new Symbol(root.getAttributeValue("end"));
				// read rules
				for (Object child : root.getChildren()) {
					String id   = ((Element)child).getAttributeValue("id");
					if (this.hasProductionWithID(id)) {
						System.out.println("Grammar already has a production with id '"+id+"'. Skipping.");
						continue;
					}
					double prob = Double.parseDouble(((Element)child).getAttributeValue("prob"));
					List<Element> rulesyms = new ArrayList<Element>(((Element) child).getChild("syntax").getChildren());
					
					boolean firstsym = true;
					Symbol lhs = null;
					ArrayList<Symbol> rhs = new ArrayList<Symbol>();
					for (Element sym : rulesyms) {
						// the first symbol is the lhs, remainings are on the rhs
						if (firstsym) {
							lhs = new Symbol(sym.getTextTrim());
							firstsym = false;
						} else {
							rhs.add(new Symbol(sym.getTextTrim()));
						}
					}
					this.addProduction(id, lhs, rhs, prob);
				}
			} else {
				System.out.println("Grammar file '"+filename+"' does not specify a simplecfggrammar.");
			}			
		} catch (IOException e) {
			System.out.println("Grammar file '"+filename+"' was not found.");
		} catch (JDOMException e) {
			System.out.println("Could not prase grammar file '"+filename+"':\n"+e);
		}
		this.update();
	}
	
}
