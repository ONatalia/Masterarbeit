package inpro.irmrsc.simplepcfg;

/**
 * A symbol used in the {@link Production}s of a {@link Grammar}
 * @author Andreas Peldszus
 */
public class Symbol implements Comparable<Symbol> {

	private String mSymbol;
	
	public Symbol(String s) {
		mSymbol = s;
	}
	
	public Symbol(Symbol s) {
		mSymbol = s.getSymbol();
	}
	
	public String getSymbol() {
		return mSymbol;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Symbol) {
		    return mSymbol.equals(((Symbol)obj).getSymbol());
		}
		return false;
	}
	
	public int compareTo(Symbol sym) {
		return mSymbol.compareTo(sym.getSymbol());
	}
	
	public int hashCode() {
		return mSymbol.hashCode();
	}
	
	@Override
	public String toString() {
		return mSymbol;
	}	
	
}
