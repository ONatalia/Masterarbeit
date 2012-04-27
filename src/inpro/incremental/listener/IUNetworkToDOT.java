package inpro.incremental.listener;

import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

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


import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import edu.cmu.sphinx.util.props.S4Boolean;

public class IUNetworkToDOT extends PushBuffer {

	/** String for dot executable */
	@S4String(defaultValue = "/usr/bin/dot")
	/** Property for string for dot executable */
	public final static String PROP_DOT = "dot";
	/** The dot executable */
	private File dot;
	
	/** String for the temporary directory */
	@S4String(defaultValue = "/tmp")
	/** Property for string for the temporary directory */
	public final static String PROP_TMP_DIR = "tmpDir";
	/** The dot executable */
	private String tmpDir;
	
	/** String, comma-separated, for what iu types to display */
	@S4String(defaultValue = "")
	/** Property for string for iu types to display <br/> Beware that the iumodule that you hook this into must have links to the types you specify here (sll or grin). */
	public final static String PROP_IU_TYPES = "iuTypes";
	/** The string of comma separated iu types */
	private List<String> iuTypes = new ArrayList<String>();

	/** Boolean for determining whether to turn dot output into images */
	@S4Boolean(defaultValue = false)
	/** Property for boolean for determining whether to turn dot output into images */
	public final static String PROP_RUN_DOT = "runDot";
	/** Whether to turn dot output into images */
	private boolean runDot;

	/** Boolean for determining whether to scale output images quickly rather than smoothly*/
	@S4Boolean(defaultValue = false)
	/** Property for boolean for determining whether to scale output images quickly rather than smoothly */
	public final static String PROP_FAST_SCALE = "useFastScaling";
	/** Setting for scaling images smooth or fast */
	private int scale = Image.SCALE_SMOOTH;

	
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
	
	/** Offset of time for when the first dot file was written. */
	private long offset;
	/** Time for when the last dot file was written. */
	private long last;

	/**
	 * Sets up the dot listener
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		dot = new File(ps.getString(PROP_DOT));
		tmpDir = ps.getString(PROP_TMP_DIR);
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
		if (!ps.getString(PROP_IU_TYPES).isEmpty()) {
			String[] strings = ps.getString(PROP_IU_TYPES).split(",");
			for (String string : strings) {
				this.iuTypes.add(string);
				System.err.println("Displaying " + string);
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
			if (ps.getBoolean(PROP_FAST_SCALE)) {
				this.scale = Image.SCALE_FAST; 
			}
		}
		this.offset = System.currentTimeMillis();
	}

	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (edits != null && !edits.isEmpty()) {
			this.nodeClusters.clear();
			try {
				out = new File(tmpDir + "/out." + this.getNewFileID() + ".dot");
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
			long now = System.currentTimeMillis() - this.offset;
//			double r = 1.0 / (now-last)*1000;
			last = now;
			if (runDot)
				runDot();
//			System.err.println("BATCH: /Users/okko/Desktop/Resources/ffmpeg -r " + r + " -f image2 -i '" + out.toString() + ".png' " + out.toString() + ".mov");
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
		if (this.iuTypes.isEmpty() || this.iuTypes.contains(iu.getClass().getSimpleName())) {
			int id = iu.getID();
			String label = iu.getClass().getSimpleName() + "\\n" + iu.toPayLoad();
			outStream.println("\"" + id + "\" [label=\"" + label + "\" shape=box, style=rounded];");
			this.addToIUCluster(iu);			
		}
	}
	
	private void printSLL(IU iu, IU sll) {
		if (this.iuTypes.isEmpty() || (this.iuTypes.contains(iu.getClass().getSimpleName()) && this.iuTypes.contains(sll.getClass().getSimpleName()))) {
			int id1 = iu.getID();
			int id2 = sll.getID();
			outStream.println("\"" + id1 + "\" -> \"" + id2 + "\" [style=dotted];");
		}
	}

	private void printGrin(IU iu, IU gr) {
		if (this.iuTypes.isEmpty() || (this.iuTypes.contains(iu.getClass().getSimpleName()) && this.iuTypes.contains(gr.getClass().getSimpleName()))) {
			int id1 = iu.getID();
			int id2 = gr.getID();
			boolean constraint = iu.getClass() == gr.getClass() ? true : false;
			outStream.println("\"" + id1 + "\" -> \"" + id2 + "\" [constraint=" + constraint + "];");			
		}
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
					System.err.println("BATCH: " + cmd);
					if (this.display) {
						p.waitFor();  // Not thread-safe!
						File imageFile = new File(this.out + "." + this.outputFormat);
						if (imageFile.exists()) {
							ImageIcon icon = new ImageIcon(imageFile.toString());
							Image image = icon.getImage();							
							image = image.getScaledInstance(l.getWidth(), -1, scale);
							image = image.getScaledInstance(-1, l.getHeight(), scale);
							icon.setImage(image);
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
