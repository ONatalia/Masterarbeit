/* 
 * Copyright 2008, 2009, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package org.cocolab.inpro.incremental.listener;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

public class TEDviewNotifier extends HypothesisChangeListener {

    @S4Integer(defaultValue = 2000)
    public final static String PROP_TEDVIEW_PORT = "tedPort";

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
			Logger.getLogger(this.getClass()).warn("Cannot connect to TEDview. I will not retry.");
			tedOutput = false;
		}
	}

	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (tedOutput && (edits.size() > 0)) {
	    	StringBuffer sb = new StringBuffer();
	    	sb.append("<event time='");
	    	sb.append(currentFrame * 10);
	    	sb.append("' originator='asr_words'>");
	    	for (IU iu : ius) {
	    		sb.append(iu.toTEDviewXML());
	    	}
	    	sb.append("</event>\n\n");
			writer.print(sb.toString());
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