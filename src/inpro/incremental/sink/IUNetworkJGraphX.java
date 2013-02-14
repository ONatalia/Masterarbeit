package inpro.incremental.sink;

import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.view.mxGraph;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

/**
 * A viewer for IU networks using the JGraphX library.
 * @author Andreas Peldszus
 */
public class IUNetworkJGraphX extends PushBuffer {

	/** String, comma-separated, for what iu types to display */
	@S4String(defaultValue = "")
	/** Property for string for iu types to display <br/> Beware that the iumodule that you hook this into must have links to the types you specify here (sll or grin). */
	public final static String PROP_IU_TYPES = "iuTypes";
	/** The string of comma separated iu types */
	private List<String> iuTypes = new ArrayList<String>();

	/** Frame for display of output */
	JFrame f = new JFrame("IU Network");
	/** The graph */
	mxGraph graph;
	/** The graphical component to display the graph in */
	mxGraphComponent graphComponent;
	/** The layout type of the graph */
	mxIGraphLayout layout;
	
	public static final String GROUP_STYLE = "shape=swimlane;fontSize=9;fontStyle=1;startSize=20;horizontal=false;padding=5;";
	public static final String NODE_STYLE = "shape=rectangle;rounded=true;fillColor=#FFFFFF;opacity=80;spacing=3;";
	public static final String SLL_EDGE_STYLE = "strokewidth=1;";
	public static final String GRIN_EDGE_STYLE = "strokewidth=1;dashed=1;";
    
	/**
	 * Sets up the IU Network listener
	 */
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		// setup list of IU types to display
		if (!ps.getString(PROP_IU_TYPES).isEmpty()) {
			String[] strings = ps.getString(PROP_IU_TYPES).split(",");
			for (String string : strings) {
				this.iuTypes.add(string);
				System.err.println("Displaying " + string);
			}
		}
		
		// setup graph
		graph = new mxGraph();
		graph.setMultigraph(true);
		graph.setCellsBendable(false);
		graph.setCellsCloneable(false);
		graph.setCellsEditable(false);
		graph.setCellsResizable(false);
		
		graph.setAllowDanglingEdges(false);
		graph.setKeepEdgesInBackground(true);
		
		// setup layout
		layout = new mxStackLayout(graph, false); //mxCompactTreeLayout(graph); //mxHierarchicalLayout(graph); // mxFastOrganicLayout(graph); // new mxStackLayout(graph, true, 10, 0);
		layout.execute(graph.getDefaultParent());
		
		// setup frame
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				f.setLocation(0, 0);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				f.setSize(1000, 800);
				
				graphComponent = new mxGraphComponent(graph);
				graphComponent.setConnectable(false);	
				f.getContentPane().add(BorderLayout.CENTER, graphComponent);
				
				// graphOutline in toolbar
				JPanel toolBar = new JPanel();
		        toolBar.setLayout(new BorderLayout());
		        final mxGraphOutline graphOutline = new mxGraphOutline(graphComponent);
		        graphOutline.setPreferredSize(new Dimension(100, 100));
		        toolBar.add(graphOutline, BorderLayout.WEST);
				f.getContentPane().add(BorderLayout.NORTH, toolBar);
				
				// make it so
				f.setVisible(true);
			}
		});
	}

	
	private class ConservedLink {
		public String from;
		public String to;
		public String type;
		
		public ConservedLink(String from, String to, String type) {
			this.from = from;
			this.to = to;
			this.type = type;
		}
	}
	
	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (edits != null && !edits.isEmpty()) {
			
			Map<String,Object> groupsByIUType = new HashMap<String,Object>();
			Map<String,Object> insertedNodesByIUID = new HashMap<String,Object>();
			Queue<IU> iuProcessingQueue = new ArrayDeque<IU>();
			iuProcessingQueue.addAll(new ArrayList<IU>(ius));
			List<ConservedLink> ConservedLinks = new ArrayList<ConservedLink>();
						
			// reset graph
			graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
			
			// fill graph
			graph.getModel().beginUpdate();
			Object parent = graph.getDefaultParent();
			try {
				// first step: add a node for each iu (and save the links for second step)
				while (!iuProcessingQueue.isEmpty()) {
					// get next IU
					IU iu = iuProcessingQueue.remove();
					String id = Integer.toString(iu.getID());
					String iuType = iu.getClass().getSimpleName();
					
					// make sure strange IUs don't enter the graph
					// TODO: this is the very first IU causing problems here. need to fix the IU bootstrap bug
					if (iuType == "" || iuType == null) { continue; }
					
					// build a new iuType group, if it doesn't exist already
					if (! groupsByIUType.containsKey(iuType)) {
						// make new group and register it
						mxCell group = (mxCell)graph.insertVertex(parent, null, iuType, 0, 0, 0, 0, GROUP_STYLE);
						groupsByIUType.put(iuType, group);
					}
					
					// if there is no node in the graph yet for this IU, make one and follow the IUs links 
					if (!insertedNodesByIUID.containsKey(id)) {
						// find the group to add this node to
						Object group = groupsByIUType.get(iuType);
						String label = iuType + " " + id+ "\n" + iu.toPayLoad().replace("\\n", "\n");
						Object node = graph.insertVertex(group, id, label, 0, 0, 0, 0, NODE_STYLE);
						graph.updateCellSize(node);
										
						// save sll link for later, queue linked IU
						IU sll = iu.getSameLevelLink();
						if (sll != null) {
							ConservedLinks.add(new ConservedLink(id, Integer.toString(sll.getID()), "sll"));
							iuProcessingQueue.add(sll);
						}
			
						// save grin links for later, queue linked IUs
						List<? extends IU> grin = iu.groundedIn();
						if (grin != null) {
							for (IU gr : grin) {			
								ConservedLinks.add(new ConservedLink(id, Integer.toString(gr.getID()), "grin"));
							}
							iuProcessingQueue.addAll(grin);
						}
										
						// store processed nodes
						insertedNodesByIUID.put(id, node);
					}		
				}
				
				// then add all links
				for (ConservedLink l : ConservedLinks) {
					// get nodes
					Object from = insertedNodesByIUID.get(l.from);
					Object to = insertedNodesByIUID.get(l.to);
					String style = (l.type.equals("sll")) ? SLL_EDGE_STYLE : GRIN_EDGE_STYLE ;
					graph.insertEdge(parent, null, null, from, to, style);
				}
				
			} finally {
				graph.getModel().endUpdate();
			}
				
			// arrange the group internal layout
			for (Object group : groupsByIUType.values()) {
//				mxCompactTreeLayout grouplayout = new mxCompactTreeLayout(graph);
//				grouplayout.setHorizontal(true);
				mxHierarchicalLayout grouplayout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
				grouplayout.execute(group);				
			}
			
			// arrange the overall graph layout
			layout.execute(graph.getDefaultParent());
		}
	}
}