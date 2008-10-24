package org.cocolab.inpro.sphinx.instrumentation;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;


import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

public class TEDviewNotifier extends LabelWriter {
    public final static String PROP_ZEITGEIST_PORT = "zeitgeistPort";

    private int zeitgeistPort = 2000;
    private boolean zeitgeistOutput = false;

    public void register(String name, Registry registry) throws PropertyException {
    	super.register(name, registry);
    	registry.register(PROP_ZEITGEIST_PORT, PropertyType.INT);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        zeitgeistPort = ps.getInt(PROP_ZEITGEIST_PORT, 2000);
        zeitgeistOutput = true;
    }

    private void messageZeitGeist(List<Token> list, String origin) {
    	StringBuffer sb = new StringBuffer();
    	if (!list.isEmpty()) {
	    	sb.append("<event time='");
	    	sb.append(step * 10);
	    	sb.append("' originator='");
	    	sb.append(origin);
	    	sb.append("'>");
	    	
			// iterate over the list and print the associated times
			Token lastToken = list.get(0);
	        for (int i = 1; i < list.size() - 1; i++) {
	            Token token = list.get(i);
	            sb.append("<event time='");
	            sb.append(lastToken.getFrameNumber() * 10);
	            sb.append("' duration='");
	            sb.append((token.getFrameNumber() - lastToken.getFrameNumber()) * 10);
	            sb.append("'>");
	            // depending on whether word, filler or other, dump the string-representation
	            SearchState state = token.getSearchState();
	            String event = stringForSearchState(state);
	            sb.append(event.replace("<", " ").replace(">", " "));
	            sb.append("</event>");
	            lastToken = token;
	    	}    	
	    	sb.append("</event>");
			try {
				Socket sock = new Socket("localhost", zeitgeistPort);
				PrintWriter writer = new PrintWriter(sock.getOutputStream());
		    	writer.print(sb.toString());
		    	writer.close();
		    	sock.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Could not open connection to zeitgeist (no further attempts will be made)!");
				zeitgeistOutput = false;
			}
    	}
    }    

    public void newResult(Result result) {
    	if (zeitgeistOutput && ((intermediateResults == !result.isFinal()) 
    	|| (finalResult && result.isFinal()))) {
    		if (wordAlignment) {
    			List<Token> list = getBestWordTokens(result);
    			messageZeitGeist(list, "asr_words");
    		}
    		if (phoneAlignment) {
    			List<Token> list = getBestPhoneTokens(result);
    			messageZeitGeist(list, "asr_phones");
    		}
    		
    	}
    	step += stepWidth;
    }

}
