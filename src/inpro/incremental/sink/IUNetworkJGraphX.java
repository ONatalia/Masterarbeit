package inpro.incremental.sink;

import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxMorphing;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;
import edu.cmu.sphinx.util.props.S4Boolean;

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
	/** Label for display of output */
	JLabel l;
	/** The graph */
	mxGraph graph;
	/** The graphical component to display the graph in */
	mxGraphComponent graphComponent;
	/** The layout type of the graph */
	mxIGraphLayout layout;
	
	public static final String CUSTOM_NODE_STYLE = "CUSTOM_NODE_STYLE";
    public static final String CUSTOM_SLL_EDGE_STYLE = "CUSTOM_SLL_EDGE_STYLE";
    public static final String CUSTOM_GRIN_EDGE_STYLE = "CUSTOM_GRIN_EDGE_STYLE";
	
	
    private static mxStylesheet getCustomStyleSheet() {
    	Map<String, Object> s = new HashMap<String, Object>();
    	mxStylesheet stylesheet = new mxStylesheet();
    	
        // base style
        Map<String, Object> baseStyle = new HashMap<String, Object>();
        baseStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");

        // custom node style
        s = new HashMap<String, Object>(baseStyle);
	    //s.put(mxConstants.STYLE_ROUNDED, true);
        s.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
	    //s.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
	    //s.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
	    //s.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
	    s.put(mxConstants.STYLE_STROKEWIDTH, 1);
        stylesheet.putCellStyle(CUSTOM_NODE_STYLE, s);

        // custom same level link style
        s = new HashMap<String, Object>(baseStyle);
//	    s.put(mxConstants.STYLE_ROUNDED, true);
//	    s.put(mxConstants.STYLE_ORTHOGONAL, false);
//	    s.put(mxConstants.STYLE_EDGE, "elbowEdgeStyle");
//	    s.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
//	    s.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
//	    s.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
//	    s.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        s.put(mxConstants.STYLE_STROKEWIDTH, 1);
        stylesheet.putCellStyle(CUSTOM_SLL_EDGE_STYLE, s);
        
        // custom grounded in link style
        s = new HashMap<String, Object>(baseStyle);
//	    s.put(mxConstants.STYLE_ROUNDED, true);
//	    s.put(mxConstants.STYLE_ORTHOGONAL, false);
//	    s.put(mxConstants.STYLE_EDGE, "elbowEdgeStyle");
//	    s.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
//	    s.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
//	    s.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
//	    s.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
	    s.put(mxConstants.STYLE_DASHED, true);
        s.put(mxConstants.STYLE_STROKEWIDTH, 1);
        stylesheet.putCellStyle(CUSTOM_GRIN_EDGE_STYLE, s);

        return stylesheet;
    }
    
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
		graph.setAllowDanglingEdges(false);
		graph.setAutoOrigin(true);
		graph.setAutoSizeCells(true);
		graph.setCellsEditable(false);
		graph.setCellsBendable(false);
		graph.setKeepEdgesInBackground(true);
		
		// setup styles
		graph.setStylesheet(getCustomStyleSheet());
		
		// setup layout
		layout = new mxHierarchicalLayout(graph); //mxCompactTreeLayout(graph); //mxHierarchicalLayout(graph); // mxFastOrganicLayout(graph); // new mxStackLayout(graph, true, 10, 0);
		layout.execute(graph.getDefaultParent());
		
		// setup frame
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				f.setLocation(0, 100);
				graphComponent = new mxGraphComponent(graph);
				f.getContentPane().add(BorderLayout.CENTER, graphComponent);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				f.setSize(600, 600);
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
				// first add a node for each iu (and store the links for second step)
				while (!iuProcessingQueue.isEmpty()) {
					// get next IU
					IU iu = iuProcessingQueue.remove();
					String id = Integer.toString(iu.getID());
					
					// if no corresponding node in the graph yet
					if (!insertedNodesByIUID.containsKey(id)) {
						// add node
						String iuType = iu.getClass().getSimpleName();
						String label = iuType + " " + id; //+ "\n" + iu.toPayLoad();
						Object node = graph.insertVertex(parent, id, label, 300, 300, 0, 0, CUSTOM_NODE_STYLE);
						graph.updateCellSize(node);
						
						// add node to group
						Object [] cells = { node };
						if (groupsByIUType.containsKey(iuType)) {
							// find group and add node to it
							Object group = groupsByIUType.get(iuType);
							graph.groupCells(group, 1, cells);
							System.out.println("add "+id+" to group "+iuType);
						} else {
							// make new group and register it
							mxCell group = (mxCell)graph.insertVertex(parent, null, iuType, 0, 0, 350, 200, "shape=swimlane;fontSize=9;fontStyle=1;startSize=20;horizontal=false;autosize=1;");
							// TODO: groups must be layouted internally
							graph.groupCells(group, 1, cells); // somehow this is needed, dunnow why.
							Object [] groups = { group };
							graph.setCellStyles(mxConstants.STYLE_OPACITY, "50", groups);
							graph.setCellStyles(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_SWIMLANE, groups);
							groupsByIUType.put(iuType, group);
							
							System.out.println("add "+id+" to new group "+iuType);
						}
						
						// add sll link
						IU sll = iu.getSameLevelLink();
						if (sll != null) {
							ConservedLinks.add(new ConservedLink(id, Integer.toString(sll.getID()), "sll"));
							iuProcessingQueue.add(sll);
						}
			
						// add grin links
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
					String style = (l.type.equals("sll")) ? CUSTOM_SLL_EDGE_STYLE : CUSTOM_GRIN_EDGE_STYLE ;
					graph.insertEdge(parent, null, null, from, to, style);
				}
			
				// refresh all
				graph.updateGroupBounds();
				
				// arrange the graph layout
				layout.execute(graph.getDefaultParent());
//				graph.refresh();
//				graph.getModel().validate();
			} finally {
				graph.getModel().endUpdate();
			}
		}
	}
}