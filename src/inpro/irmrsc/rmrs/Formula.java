package inpro.irmrsc.rmrs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

//TODO: make xml output for every rmrs object
//TODO: add sorts to variables

public class Formula extends VariableEnvironment
					 implements VariableIDsInterpretable {
		
	// Hook
	private Hook mHook;

	// Slots
	private Deque<Hook> mSlots;

	// Relations
	private List<Relation> mRels;
	
	// Scope Constraints
	private List<VariableIDPair> mScons;
	
	// Variable Equalities
	private List<VariableIDPair> mEqs;
	
	// cp constr.
	public Formula (Formula f) {
		// deep copy vars, not only copying collections
		super(f);
		mHook = new Hook(f.mHook);		
		mSlots = new ArrayDeque<Hook>();
		for (Hook h : f.mSlots) mSlots.add(new Hook(h));
		mRels = new ArrayList<Relation>();
		for (Relation r : f.mRels) mRels.add(new Relation(r));
		mScons = new ArrayList<VariableIDPair>();
		for (VariableIDPair p : f.mScons) mScons.add(new VariableIDPair(p));
		mEqs = new ArrayList<VariableIDPair>();
		for (VariableIDPair p : f.mEqs) mEqs.add(new VariableIDPair(p));
	}
	
	public Formula () {
		super();
		mSlots = new ArrayDeque<Hook>();
		mRels = new ArrayList<Relation>();
		mScons = new ArrayList<VariableIDPair>();
		mEqs = new ArrayList<VariableIDPair>();		
	}
	
	
	// make a simple new formula for a lexical object
	public Formula (String lexname, Variable.Type semtype) {
		this();
		// make variable ids
		int l=0, a=1, i=2;
		// make and add variables
		mVariables.put(l, new Variable(l, Variable.Type.LABEL));
		mVariables.put(a, new Variable(a, Variable.Type.ANCHOR));
		if (semtype != null) {
			mVariables.put(i, new Variable(i, semtype));
		} else {
			mVariables.put(i, new Variable(i, Variable.Type.UNDERSPEC));
		}
		// make hook
		mHook = new Hook(l,a,i);
		// make and add lexical relation
		if (semtype != null) {
			mRels.add(new Relation(l,a,i,lexname,Relation.Type.LEXICAL));
		} else {
			mRels.add(new Relation(l,a,lexname,Relation.Type.LEXICAL));
		}
	}
	
	// applies all variable equalities
	public void reduce () {
		// iterate all variable equalities
		int m = mEqs.size();
		for (int i = 0; i < m ; i++) {
			VariableIDPair p = mEqs.remove(0);
			Variable reduceTo = this.mVariables.get(p.getLeft());
			Variable reduce = this.mVariables.get(p.getRight());		
			if (reduceTo.sameTypeAs(reduce)) { //TODO: later more to do with feature unification
				// types match, all good: remove reduce and replace all ids.
				int oldID = reduce.getID();
				int newID = reduceTo.getID();
				this.mVariables.remove(oldID);
				this.replaceVariableID(oldID, newID);
			} else {
				// check type hierarchy: indexes can be specified to x or e.
				int oldID = reduce.getID();
				int newID = reduceTo.getID();
				if (reduceTo.getType() == Variable.Type.INDEX) {
					switch(reduce.getType()) {
					case EVENT:
						reduceTo.setType(Variable.Type.EVENT);
						this.mVariables.remove(oldID);
						this.replaceVariableID(oldID, newID);
						break;
					case INDIVIDUAL:
						reduceTo.setType(Variable.Type.INDIVIDUAL);
						this.mVariables.remove(oldID);
						this.replaceVariableID(oldID, newID);
						break;
					}
				}
				if (reduce.getType() == Variable.Type.INDEX){
					// no need to replace reduceTo type, but replace ids.
					switch(reduceTo.getType()) {
					case EVENT:
					case INDIVIDUAL:
						this.mVariables.remove(oldID);
						this.replaceVariableID(oldID, newID);
						break;
					}
				}
				
				// cnt mismatches, reduce gracefully
				// TODO:
			}
		}
	}
	
	public boolean isReduced () {
		return mEqs.isEmpty();
	}
	
	public boolean isComplete () {
		return mSlots.isEmpty();
	}
	
	// implemented as mutation. if fc fails use another method to build
	// gracefully degraded results.
	public boolean forwardCombine(Formula fo) {
		// copy argument formula so that it is not changed by renumbering
		Formula f = new Formula(fo);
		// renumber f
		f.renumber(this.getMaxID()+1);
//		System.out.println("N "+f.toRMRSString());
		
		// join variable assignments
		for (Map.Entry<Integer, Variable> e : f.mVariables.entrySet()) {
			if (this.mVariables.containsKey(e.getKey())) {
				// this should not happen after a successful renumbering of f
				System.out.println("Warning: Cannot merge variable assignments due to inconsistent mapping.");
			} else {
				this.mVariables.put(e.getKey(), e.getValue());
			}
		}
		
		// copy relations, scons, eqs
		for (Relation r : f.mRels) this.mRels.add(r);
		for (VariableIDPair p : f.mScons) this.mScons.add(p);
		for (VariableIDPair p : f.mEqs) this.mEqs.add(p);
		
		// pop slot
		Hook slotToFill = this.mSlots.pop();
		
		// push new slots (forward!)
		Iterator<Hook> i = f.mSlots.descendingIterator();
		while(i.hasNext())
			this.mSlots.push(i.next());
		
		// add new equalities
		this.mEqs.add(new VariableIDPair(slotToFill.getLabel(),  f.mHook.getLabel()));
		this.mEqs.add(new VariableIDPair(slotToFill.getAnchor(), f.mHook.getAnchor()));
		this.mEqs.add(new VariableIDPair(slotToFill.getIndex(),  f.mHook.getIndex()));

		return true;
	}
	
	// simply adding another formula without consuming a slot and equating variables
	public void simpleAdd(Formula fo) {
		Formula f = new Formula(fo);
		f.renumber(this.getMaxID()+1);
		for (Map.Entry<Integer, Variable> e : f.mVariables.entrySet()) {
			if (this.mVariables.containsKey(e.getKey())) {
				// this should not happen after a successful renumbering of f
				System.out.println("Warning: Cannot merge variable assignments due to inconsistent mapping.");
			} else {
				this.mVariables.put(e.getKey(), e.getValue());
			}
		}
		for (Relation r : f.mRels) this.mRels.add(r);
		for (VariableIDPair p : f.mScons) this.mScons.add(p);
		for (VariableIDPair p : f.mEqs) this.mEqs.add(p);
		// TODO: what about the added formulas slots if it has any?
		//       -> we will only add lexical formulas without slots
	}
	
	public Set<Integer> getVariableIDs() {
		Set<Integer> s = new TreeSet<Integer>();
		// hook
		s.addAll(mHook.getVariableIDs());
		// slots
		for (Hook h : mSlots) s.addAll(h.getVariableIDs());
		// rels
		for (Relation r : mRels) s.addAll(r.getVariableIDs());
		// scons
		for (VariableIDPair p : mScons) s.addAll(p.getVariableIDs());
		// eqs
		for (VariableIDPair p : mEqs) s.addAll(p.getVariableIDs());
		return s;
	}
	
	public void update() {
		Set<Integer> s = this.getVariableIDs();
		// check for undefined variables
		for (Integer i : s) {
			if (! mVariables.containsKey(i)) {
				System.out.println("Warning: Undefined variable: id="+i);
				// TODO: do some useful repair
			}
		}
		// ... is there more to do here?
	}

	public void replaceVariableID(int oldID, int newID) {
		//hook
		mHook.replaceVariableID(oldID, newID);
		//slots
		for (Hook h : mSlots) h.replaceVariableID(oldID, newID);
		//rels
		for (Relation r : mRels) r.replaceVariableID(oldID, newID);
		//scons
		for (VariableIDPair p : mScons) p.replaceVariableID(oldID, newID);
		//eqs
		for (VariableIDPair p : mEqs) p.replaceVariableID(oldID, newID);
	}
	
	// reassigns new indices for all variables in this expression 
	// starting from 0
	public void renumber() {
		this.renumber(0);
	}
	
	// reassigns new indices for all variables in this expression 
	// starting from StartIndex
	public void renumber(int StartIndex) {
		int myMaxID = this.getMaxID();
		if (StartIndex <= myMaxID) {
			this.rerenumber(myMaxID+1);
		}
		this.rerenumber(StartIndex);
	}
	
	// if startindex is smaller than the max index in this formula, bad things will happen.
	private void rerenumber(int StartIndex) {
		int cntNewID = StartIndex;
		Map<Integer,Integer> oldToNewIDs = new HashMap<Integer,Integer>();
		HashMap<Integer,Variable> newVariables = new HashMap<Integer,Variable>(this.mVariables);
		// need deep copy?
		for (Map.Entry<Integer,Variable> e : mVariables.entrySet()) {
			int key = e.getKey();
			Variable v = e.getValue();
			int currentID = v.getID();
			if (key != currentID) {
				System.out.println("Warning: inconsistent variable assignment!");
			}
			if (oldToNewIDs.containsKey(currentID)) {
				// rename it with the new one
				newVariables.remove(currentID);
				int newID = oldToNewIDs.get(currentID);
				v.setID(newID); // mutation
				newVariables.put(newID, v);
				this.replaceVariableID(currentID, newID);
			} else {
				// use counter as new ID and add to the mapping
				oldToNewIDs.put(currentID, cntNewID);
				newVariables.remove(currentID);
				v.setID(cntNewID);
				newVariables.put(cntNewID, v);
				this.replaceVariableID(currentID, cntNewID);
				cntNewID++;
			}	
		}
		mVariables = newVariables;
	}

	@Override
	public String toString() {
		return this.toRMRSString();
//		List<SimpleAssertion> l = this.getUnscopedPredicateLogic();
//		StringBuilder sb = new StringBuilder();
//		for (SimpleAssertion sa : l) {
//			sb.append(sa.toString()+"\\n");
//		}
//		return sb.toString();
	}

	public String toStringOneLine() {
		return this.toString().replace("\\n", " ");
	}
	
	public String toStringMultiLine() {
		return this.toString().replace("\\n", "\n");
	}
	
	public String toRMRSString() {
		// i'm using \\n line feeds here for the dot notifier
		StringBuilder s = new StringBuilder();
		s.append("[ "+mHook+"\\n{ ");
		for (Hook h : mSlots)
			s.append(h+" ");
		s.append("}");
		for (Relation r : mRels)
			s.append("\\n"+r+",");
		for (VariableIDPair p : mScons)
			s.append("\\nqeq("+getVariableString(p.getLeft())+","+getVariableString(p.getRight())+"),");
		for (VariableIDPair p : mEqs)
			s.append("\\n"+getVariableString(p.getLeft())+"="+getVariableString(p.getRight())+",");
		s.deleteCharAt(s.length()-1);
		s.append("]");
		// replace all variable id string representations (of the form #v1) by their
		// correct string according to the variable assignment and the types stored
		// there.
		Pattern pat = Pattern.compile("#v(\\d+)");
		StringBuffer sb = new StringBuffer();
		Matcher matcher = pat.matcher(s.toString());
		while (matcher.find()) {
			int varID = Integer.parseInt(matcher.group(1));
			if (mVariables.containsKey(varID)) {
				matcher.appendReplacement(sb, this.getVariableString(varID));
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
	

	public List<SimpleAssertion> getUnscopedPredicateLogic() {
		List<SimpleAssertion> l = new ArrayList<SimpleAssertion>();
		// find all anchors ids
		for (Variable v : mVariables.values()) {
			if (v.getType() == Variable.Type.ANCHOR) {
				int aid = v.getID();
				String name = "_";
				Integer arg0 = null;
				Integer arg1 = null;
				Integer arg2 = null;
				// get all relations of that anchor
				for (Relation r : mRels) {
					if (r.isAnchoredAs(aid)) {
						// arrange predicate argument structure
						switch (r.getType()) {
						case NONLEXICAL:
						case LEXICAL:
							name = r.getName();
							if (r.hasArgument()) {
								arg0 = r.getArgument();
							}
							break;
						case ARGREL:
							String argrelname = r.getName();
							if (argrelname.equals("BV")) {
								arg0 = r.getArgument();
								break;
							}
							if (argrelname.equals("ARG1") || argrelname.equals("RSTR") || argrelname.equals("ARGN")) {
								arg1 = r.getArgument();
								break;
							}
							if (argrelname.equals("ARG2") || argrelname.equals("BODY")) {
								arg2 = r.getArgument();
								break;
							}
						}
					}
				}
				// build a SimpleAssertion				
				if (arg0 == null && arg1 == null && arg2 == null && name.equals("_")) {
					// do nothing
					continue;
				} 
				if (arg0 != null && arg1 != null && arg2 != null) {
					List<Integer> ids = new ArrayList<Integer>(3);
					ids.add(arg0); ids.add(arg1); ids.add(arg2);
					l.add(new SimpleAssertion(name,ids));
					continue;
				}
				if (arg0 != null && arg1 != null && arg2 == null) {
					List<Integer> ids = new ArrayList<Integer>(2);
					ids.add(arg0); ids.add(arg1);
					l.add(new SimpleAssertion(name,ids));
					continue;
				} 
				if (arg0 != null && arg1 == null && arg2 == null) {
					List<Integer> ids = new ArrayList<Integer>(1);
					ids.add(arg0);
					l.add(new SimpleAssertion(name,ids));
					continue;
				}
			}
		}
		return l;
	}
	
	
	// not yet fully working
	public List<SimpleAssertion> getNominalAssertions() {
		List<SimpleAssertion> l = new ArrayList<SimpleAssertion>();
		// find all individual variables and look where they occur
		Set<Integer> anchorsIDsOfRelevantRelations = new TreeSet<Integer>();
		for (Variable v : mVariables.values()) {
			if (v.getType() == Variable.Type.INDIVIDUAL) {
				int id = v.getID();
				// find all relations concerning it
				for (Relation r : mRels) {
					if (r.isAbout(id)) {
						anchorsIDsOfRelevantRelations.add(r.getAnchor());
					}
				}
			}
		}
		// find predicate and all arguments for the relevant anchor
		for (Integer i : anchorsIDsOfRelevantRelations) {
			Relation lex  = null;
			Relation arg1 = null;
			Relation arg2 = null;
			for (Relation r : mRels) {
				if(r.isAnchoredAs(i)) {
					if (r.getType() == Relation.Type.LEXICAL) {
						lex = r;
						continue;
					}
					if (r.getType() == Relation.Type.ARGREL) {
						if (r.getName().equals("BV")) arg1 = r;
						if (r.getName().equals("ARG1")) arg1 = r;
						if (r.getName().equals("ARG2")) arg2 = r;
						if (r.getName().equals("ARGN")) arg1 = r;
						continue;
					}
				}
			}
			// now build SimpleAssert
			if(lex != null) {
				List<Integer> ids = new ArrayList<Integer>(2);
				String name = lex.getName();
				// here take index if nominal:
				if (lex.hasArgument()) {
					if (this.mVariables.get(lex.getArgument()).getType() == Variable.Type.INDIVIDUAL) {
						ids.add(lex.getArgument());
					}
				}				
				if (arg1 != null) {
					ids.add(arg1.getArgument());
					if (arg2 != null) {
						ids.add(arg2.getArgument());
					}
				}
				l.add(new SimpleAssertion(name,ids));
			}
		}
		return l;
	}
	
	// initializes this object from XML element: <rmrsincrement>
	@SuppressWarnings("unchecked")
	public void parseXML(Element e) {
		List<Element> children = e.getChildren();
		if (children != null && children.size() == 5) {
			Element next = null;
			// variable definitions
			next = children.remove(0);
			if (next.getName().equals("vars")) {
				for (Object i : next.getChildren()) {
					Element x = (Element) i;
					if (x.getName().equals("vdef")) {
						Variable v = new Variable();
						v.parseXML(x);
						mVariables.put(v.getID(), v);
					}
				}
			}
			// hook
			next = children.remove(0);
			if (next.getName().equals("hook")) {
				mHook = new Hook();
				mHook.parseXML(next);
			}
			// slots
			next = children.remove(0);
			if (next.getName().equals("slots")) {
				for (Object i : next.getChildren("hook")) {
					Element x = (Element) i;
					Hook h = new Hook();
					h.parseXML(x);
					mSlots.addLast(h);
				}
			}
			// relations
			next = children.remove(0);
			if (next.getName().equals("rels")) {
				for (Object i : next.getChildren()) {
					Element x = (Element) i;
					if (x.getName().equals("rel")) {
						Relation r = new Relation();
						r.parseXML(x);
						mRels.add(r);
					}
				}
			}
			// scope constraints
			next = children.remove(0);
			if (next.getName().equals("scons")) {
				for (Object i : next.getChildren()) {
					Element x = (Element) i;
					if (x.getName().equals("qeq")) {
						Integer h = Integer.parseInt(x.getAttributeValue("h"));
						Integer l = Integer.parseInt(x.getAttributeValue("l"));
						VariableIDPair p = new VariableIDPair(h,l);
						mScons.add(p);
					}
				}
			}
		}
		update();
	}
	
//	public static void main (String[]args) {
//		SAXBuilder builder = new SAXBuilder();	
//		try {
//			Document doc = builder.build(new File("/home/andreas/workspace/ISem/data/rmrs.xml"));
//			Element root = doc.getRootElement();
//			if (root.getName() == "rmrsincrement") {
//				Formula f = new Formula();
//				f.parseXML(root);
//				System.out.println(f+""+f.mVariables+"\n");
//				Formula g = new Formula(f);
//				System.out.println(g+""+g.mVariables+"\n");
//				f.forwardCombine(g);
//				System.out.println(f+""+f.mVariables+"\n");
//				//System.out.println(g+""+g.mVariables+"\n");
//				f.reduce();
//				System.out.println(f+""+f.mVariables+"\n");
//				System.out.println(f.isReduced());
//			}
//		} catch (IOException e) {
//			System.out.println(e);
//		} catch (JDOMException e) {
//			System.out.println(e);
//		}
//	}
	
}