package org.cocolab.inpro.incremental.listener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.IU;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import edu.cmu.sphinx.util.props.S4Boolean;

public class IUNetworkToDOT extends PushBuffer {

	/** String for dot executable */
	@S4String(defaultValue = "/usr/local/bin/dot")
	public final static String PROP_DOT = "dot";
	private File dot;
	
	/** Boolean for determining whether to turn dot output into images */
	@S4Boolean(defaultValue = false)
	public final static String PROP_RUN_DOT = "runDot";
	private boolean runDot;
	
	/** String for image extension */
	@S4String(defaultValue = "svg")
	public final static String PROP_OUTPUT_FORMAT = "outputFormat";
	private String outputFormat;

	/** File ID counter. Increases with each edit. */
	private static int File_idCounter = 0;
	/** PrintStream to write dot plain text to */
	private PrintStream outStream;
	/** The .dot output file  */
	private File out;

	/**
	 * Sets up the dot listener
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		dot = new File(ps.getString(PROP_DOT));
		runDot = ps.getBoolean(PROP_RUN_DOT);
		outputFormat = ps.getString(PROP_OUTPUT_FORMAT);
	}

	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (edits != null && !edits.isEmpty()) { // && edits.get(0).getType().equals(EditType.COMMIT)) {
			try {
				out = new File("/tmp/out." + this.getNewFileID() + ".dot");
				outStream = new PrintStream(out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Set<IU> processedIUs = new HashSet<IU>();
			Queue<IU> iuProcessingQueue = new ArrayDeque<IU>();
			List<IU> iuList = new ArrayList<IU>(ius);
			Collections.reverse(iuList);
			iuProcessingQueue.addAll(iuList);
			printHead();
			while (!iuProcessingQueue.isEmpty()) {
				IU iu = iuProcessingQueue.remove();
				if (!processedIUs.contains(iu)) {
					printNode(iu);
					IU sll = iu.getSameLevelLink();
					if (sll != null) {
						printSLL(iu, sll);
						iuProcessingQueue.add(sll);
					}
					List<? extends IU> grin = iu.groundedIn();
					if (grin != null) {
						for (IU gr : grin) {
							printGrin(iu, gr);
						}
						iuProcessingQueue.addAll(grin);
					}
				}
				processedIUs.add(iu);
			}
			printTail();
			this.outStream.close();
			if (runDot)
				runDot();			
		}
	}
	
	private int getNewFileID() {
		return File_idCounter++;
	}
	
	private void printHead() {
		outStream.println("digraph iunetwork {");
		outStream.println("rankdir=RL;");
	}

	private void printNode(IU iu) {
		int id = iu.getID();
		String label = iu.getClass().getSimpleName() + "\\n" + iu.toPayLoad();
		outStream.println("\"" + id + "\" [label=\"" + label + "\" shape=box, style=rounded];");
	}
	
	private void printSLL(IU iu, IU sll) {
		int id1 = iu.getID();
		int id2 = sll.getID();
		outStream.println("\"" + id1 + "\" -> \"" + id2 + "\";");
	}

	private void printGrin(IU iu, IU gr) {
		int id1 = iu.getID();
		int id2 = gr.getID();
		outStream.println("\"" + id1 + "\" -> \"" + id2 + "\" [constraint=false];");
	}
	
	private void printTail() {
		outStream.println("}");
	}

	@SuppressWarnings("unused")
	private void runDot() {
		if (this.dot.exists()) {
			if (this.dot.canExecute()) {
				try {
					String cmd = this.dot.toString() + " -O -T" + this.outputFormat + " " + this.out;
					Process p = Runtime.getRuntime().exec(cmd);
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {
				System.err.println("Cannot execute dot executable at:" + dot.toString());
				System.err.println("Skipping from now on.");
				this.runDot = false;
			}
		} else {
			System.err.println("Cannot find dot executable at:" + dot.toString());
			System.err.println("Skipping from now on.");
			this.runDot = false;
		}
	}

}
