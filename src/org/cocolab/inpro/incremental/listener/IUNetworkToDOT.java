package org.cocolab.inpro.incremental.listener;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

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
	/** Property for ftring for dot executable */
	public final static String PROP_DOT = "dot";
	/** The dot executable */
	private File dot;
	
	/** Boolean for determining whether to turn dot output into images */
	@S4Boolean(defaultValue = false)
	/** Property for boolean for determining whether to turn dot output into images */
	public final static String PROP_RUN_DOT = "runDot";
	/** Whether to turn dot output into images */
	private boolean runDot;
	
	/** Boolean for whether to show dot output images */
	@S4Boolean(defaultValue = false)
	/** Property for boolean for whether to show dot output images */
	public final static String PROP_DISPLAY_OUTPUT = "display";
	/** Whether to show dot output images. This is not thread-safe and makes system calls. Use for debug or inspection only!*/
	private boolean display;
	
	/** String for image extension, defaults to .png */
	@S4String(defaultValue = "png")
	/** Property for string for image extension */
	public final static String PROP_OUTPUT_FORMAT = "outputFormat";
	/** String for image extension, defaults to .png */
	private String outputFormat;

	/** Output file ID counter. Increases with each edit. */
	private static int File_idCounter = 0;
	/** PrintStream to write dot output to. */
	private PrintStream outStream;
	/** The current .dot output file. */
	private File out;
	/** List of lists of nodes to cluster */
	private List<List<IU>> nodeClusters = new ArrayList<List<IU>>();
	/** Frame for display of output */
	JFrame f = new JFrame("IU Network");;
	/** Label for display of output */
	JLabel l;

	/**
	 * Sets up the dot listener
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		dot = new File(ps.getString(PROP_DOT));
		runDot = ps.getBoolean(PROP_RUN_DOT);
		outputFormat = ps.getString(PROP_OUTPUT_FORMAT);
		display = ps.getBoolean(PROP_DISPLAY_OUTPUT);
		if (display) {
			System.err.println("Warning: turn off display of dot output if you're using real audio input.");
			if (!outputFormat.equals("png")) {
				System.err.println("Forcing output format to 'png' to display IU network");				
				this.outputFormat = "png";
			}
		}
		if (this.display) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					f.setLocation(0, 100);
					l = new JLabel();
					// Add a dummy image of dims 800x600.
					l.setIcon(new ImageIcon(new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB)));
					f.add(l);
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					f.pack();
					f.setVisible(true);
				}
			});

		}
	}

	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (edits != null && !edits.isEmpty()) {
			this.nodeClusters.clear();
			try {
				out = new File("/tmp/out." + this.getNewFileID() + ".dot");
				outStream = new PrintStream(out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Set<IU> processedIUs = new HashSet<IU>();
			Queue<IU> iuProcessingQueue = new ArrayDeque<IU>();
			List<IU> iuList = new ArrayList<IU>(ius);
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
		this.addToIUCluster(iu);
	}
	
	private void printSLL(IU iu, IU sll) {
		int id1 = iu.getID();
		int id2 = sll.getID();
		outStream.println("\"" + id1 + "\" -> \"" + id2 + "\" [style=dotted];");
	}

	private void printGrin(IU iu, IU gr) {
		int id1 = iu.getID();
		int id2 = gr.getID();
		boolean constraint = iu.getClass() == gr.getClass() ? true : false;
		outStream.println("\"" + id1 + "\" -> \"" + id2 + "\" [constraint=" + constraint + "];");
	}
	
	private void addToIUCluster(IU iu) {
		// Build clusters for IU types for subgraphs.
		boolean added = false;
		for (List<IU> cluster : nodeClusters) {
			if (cluster.get(0).getClass() == iu.getClass()) {
				if (!cluster.contains(iu)) {
					cluster.add(iu);
					added = true;
					break;
				}
			}
		}
		if (!added) {
			List<IU> newCluster = new ArrayList<IU>();
			newCluster.add(iu);
			nodeClusters.add(newCluster);
		}
	}
	
	private void printTail() {
		if (!nodeClusters.isEmpty()) {
			for (List<IU> cluster : this.nodeClusters) {
				if (!cluster.isEmpty()) {
					outStream.println("subgraph cluster" + cluster.get(0).getClass().getSimpleName() + " {");
					outStream.println("label=" + cluster.get(0).getClass().getSimpleName());
					for (IU iu : cluster) {
						outStream.println("\"" + iu.getID() + "\"");
					}
					outStream.println("}");
				}
			}			
		}
		outStream.println("}");
	}

	/**
	 * Runs dot for the output .dot file with the configured output format
	 * and displays in window (if so configured).<br/>
	 * Stops running dot or displaying if something goes awry (IO exceptions etc.).
	 */
	private void runDot() {
		if (this.dot.exists()) {
			if (this.dot.canExecute()) {
				try {
					String cmd = this.dot.toString() + " -O -T" + this.outputFormat + " " + this.out;
					Process p = Runtime.getRuntime().exec(cmd);
					if (this.display) {
						p.waitFor();  // Not thread-safe!
						File imageFile = new File(this.out + "." + this.outputFormat);
						if (imageFile.exists()) {
							ImageIcon icon = new ImageIcon(imageFile.toString());
							if (icon.getIconHeight() > l.getHeight() || icon.getIconWidth() > l.getWidth()) {
								float wFactor = (float) l.getWidth() / (float) icon.getIconWidth();
								float hFactor = (float) l.getHeight() / (float) icon.getIconHeight();
								float factor = wFactor > hFactor ? hFactor : wFactor;
								Image img = icon.getImage();
								BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
								Graphics g = bi.createGraphics();
								int w = (int) (icon.getIconWidth() * factor);
								int h = (int) (icon.getIconHeight() * factor);
								g.drawImage(img, 0, 0, w, h, null);
								icon = new ImageIcon(bi);								
							}
							l.setIcon(icon);
						} else {
							System.err.println("Cannot find output: " + dot.toString());
							System.err.println("Skipping display from now on.");
							this.display = false;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Cannot execute dot executable at:" + dot.toString());
				System.err.println("Skipping from now on.");
				this.runDot = false;
				this.display = false;
			}
		} else {
			System.err.println("Cannot find dot executable at:" + dot.toString());
			System.err.println("Skipping from now on.");
			this.runDot = false;
			this.display = false;
		}
	}

}
