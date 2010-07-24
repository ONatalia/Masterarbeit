package org.cocolab.inpro.incremental.listener;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.cocolab.inpro.domains.greifarm.ActionIU;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;
import org.cocolab.inpro.incremental.unit.SegmentIU;
import org.cocolab.inpro.incremental.unit.SemIU;
import org.cocolab.inpro.incremental.unit.WordIU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

public class TEDviewNotifier extends FrameAwarePushBuffer {

    @S4Integer(defaultValue = 2000)
    public final static String PROP_TEDVIEW_PORT = "tedPort";

    Logger logger = Logger.getLogger(TEDviewNotifier.class);
    
    private int tedPort;

    @S4String(defaultValue = "localhost")
    public final static String PROP_TEDVIEW_ADDRESS = "tedAddress";
    
    private String tedAddress;
    
    private boolean tedOutput = true;

    private Socket sock;
    private PrintWriter writer;
    
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		try {
			tedPort = ps.getInt(PROP_TEDVIEW_PORT);
			tedAddress = ps.getString(PROP_TEDVIEW_ADDRESS);
			sock = new Socket(tedAddress, tedPort);
			writer = new PrintWriter(sock.getOutputStream());
		} catch (IOException e) {
			logger.warn("Cannot connect to TEDview. I will not retry.");
			tedOutput = false;
		}
	}

	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (tedOutput && (edits.size() > 0) && (ius.size() > 0)) {
	    	StringBuilder sbIUs = new StringBuilder();
	    	sbIUs.append("<event time='");
	    	sbIUs.append(currentFrame * 10);
	    	IU iuType = ius.iterator().next();
	    	if (iuType instanceof WordIU) {
	    		sbIUs.append("' originator='asr_words'>");
	    	} else if (iuType instanceof SegmentIU) {
	    		sbIUs.append("' originator='asr_phones'>");
	    	} else if (iuType instanceof SemIU) {
	    		sbIUs.append("' originator='semantics'>");
	    	} else  if (iuType instanceof ActionIU) {
	    		sbIUs.append("' originator='action'>");
	    	} else {
	    		logger.warn("Dunno in what track to log IUs of type " + iuType.getClass().toString());
	    	}
	    	for (IU iu : ius) {
	    		sbIUs.append(iu.toTEDviewXML());
	    	}
	    	sbIUs.append("</event>\n\n");
			writer.print(sbIUs.toString());
			writer.flush();
		}
	}

	protected void finalize() {
		writer.flush();
    	writer.close();
    	try {
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
