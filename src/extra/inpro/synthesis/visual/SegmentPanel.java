package extra.inpro.synthesis.visual;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import extra.inpro.synthesis.visual.SegmentModel.PitchRange;
import extra.inpro.synthesis.visual.SegmentModel.Segment;
import extra.inpro.synthesis.visual.SegmentModel.SegmentBoundPitchMark;



/**
 * a {@link java.awt.Panel} that displays and handles manipulation of a SegmentModel.  
 * @author timo
 */
@SuppressWarnings("serial")
public class SegmentPanel extends JPanel {
	private static final int PANEL_HEIGHT = 140;
	private static final int TEXT_BASELINE = 23;
	private static final int PITCH_AREA_START = 30;
	private static final int PITCH_AREA_HEIGHT = PANEL_HEIGHT - PITCH_AREA_START;
	private static final int HEAD_ROOM = 30; // room to leave empty
	
	private static final Color TEXT_COLOR = Color.BLACK;
	private static final Color BORDER_COLOR = Color.BLACK;
	private static final Color PITCH_COLOR = Color.RED;
	private static final Color PITCH_BACKGROUND_COLOR = Color.WHITE;
	private static final float PITCH_HOTSPOT_SIZE = 4;
	
	/** the underlying segment model */
	SegmentModel segmentModel;
	/** the current pitch range, used for auto-scaling of pitch values */
	PitchRange pitchRange;
	
	final JPopupMenu segmentPopup;
	/** the point in which a popup event occurred */
	Point popupPoint;
	/** the zoom into the segment model */
	private float zoom = 1.0f;
	/** 
	 * the action to be triggered when something has changed in the segment model
	 * that justifies resynthesizing the represented speech
	 */
	Action synthesisAction;
	
	public SegmentPanel(Action synAction, SegmentModel segmentModel) {
		this.segmentModel = segmentModel;
		this.synthesisAction = synAction;
		addMouseListener(activateSegmentOrPMOnMousePressed);
		addMouseListener(deactiveSegmentOrPMOnMouseReleased);
		addMouseListener(popupHandler);
		addMouseMotionListener(cursorManipulator);
		addMouseMotionListener(mouseDragHandler);
		// generate the popup which we show when someone clicks into the segment part of the panel
		segmentPopup = new JPopupMenu();
		JMenu menu = new JMenu(insertSegmentAction.getValue(Action.NAME).toString());
		menu.add(new SegmentSelector(insertSegmentAction));
		segmentPopup.add(menu);
		menu = new JMenu(changeSegmentAction.getValue(Action.NAME).toString());
		menu.add(new SegmentSelector(changeSegmentAction));
		segmentPopup.add(menu);
		segmentPopup.add(new JMenuItem(deleteSegmentAction));
	}
	
	/** changes the mouse cursor depending on what can currently be done */
	MouseMotionListener cursorManipulator = new MouseMotionAdapter() {
		@Override
		public void mouseMoved(MouseEvent e) {
			Cursor c;
			if (hasPitchMarkAtPosition(e.getPoint())) {
				c = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
			} else if (hasSegmentBoundaryAtPosition(e.getX()))  
				c = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
			else
			    c = Cursor.getDefaultCursor();
			setCursor(c);
		}
	};
	
	/** setup everything so that dragging works */
	MouseListener activateSegmentOrPMOnMousePressed = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				Point mouse = e.getPoint();
				if (hasPitchMarkAtPosition(mouse))
					getSegmentModel().setActivePitchMark(getPitchMarkAtPosition(mouse));
				else if (hasSegmentBoundaryAtPosition(mouse.x)) {
					SegmentModel sm = getSegmentModel();
					getSegmentModel().setActiveLabel(sm.getSegmentAt(timeAtPosition(mouse.x - 3)));
				}
			}
		}
	};
	
	/** teardown after something has been dragged */
	MouseListener deactiveSegmentOrPMOnMouseReleased = new MouseAdapter() {
		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) { 
				getSegmentModel().setActiveLabel(null);
				getSegmentModel().setActivePitchMark(null);
				pitchRange = getSegmentModel().getPitchRange();
				if (synthesisAction != null)
					synthesisAction.actionPerformed(new ActionEvent(this, 0, "synthesize"));
				repaint();
				revalidate();
			}
		}	
	};
	
	/** handles dragging of segment boundaries and pitch marks */
	MouseMotionListener mouseDragHandler = new MouseMotionAdapter() {
		@Override
		public void mouseDragged(MouseEvent e) {
			Point mouse = e.getPoint();
			if (!inSegmentArea(e)) {
				getSegmentModel().setPitchOfActiveMarkTo(pitchForPosition(mouse.y));					
			}
			boolean isShiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == 1;
			if (isShiftPressed)
				getSegmentModel().moveRightBoundaryOfActiveLabelTo(timeAtPosition(mouse.x));
			else
				getSegmentModel().moveAllBoundariesRightOfActiveLabel(timeAtPosition(mouse.x));
			repaint();
		}
	};

	/** show the right popup which may trigger one of the following actions */
	MouseListener popupHandler = new MouseAdapter() {
		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3) {
				popupPoint = e.getPoint();
				if (inSegmentArea(e)) {
					segmentPopup.show(e.getComponent(), e.getX(), e.getY());
				} else {
					JPopupMenu pitchPopup = new JPopupMenu();
					if (hasPitchMarkAtPosition(popupPoint)) {
						pitchPopup.add(new JMenuItem(deletePitchMarkAction));
					} else {
						pitchPopup.add(new JMenuItem(insertPitchMarkAction));
					}
					pitchPopup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}	
	};
	
	/** the action handling the insertion of a segment */
	Action insertSegmentAction = new AbstractAction("Segment einfügen") {
		@Override
		public void actionPerformed(ActionEvent e) {
			String newSegment = e.getActionCommand();
			int time = timeAtPosition(popupPoint.x);
			segmentModel.insertSegment(newSegment, time);
			callSynthesis();
		}
	};
	
	/** the action handling the label-text change of a segment */
	Action changeSegmentAction = new AbstractAction("Segment ändern") {
		@Override
		public void actionPerformed(ActionEvent e) {
			String newSegment = e.getActionCommand();
			if (newSegment != null) {
				getSegmentAtPosition(popupPoint.x).setText(newSegment);
				callSynthesis();
			}
		}
	};
	
	/** the action handling the deletion of a segment */
	Action deleteSegmentAction = new AbstractAction("Segment löschen") {
		@Override
		public void actionPerformed(ActionEvent e) {
			Segment l = getSegmentAtPosition(popupPoint.x);
			getSegmentModel().removeSegment(l);
			callSynthesis();
		}
	};
	
	/** the action handling the insertion of a pitch mark */
	Action insertPitchMarkAction = new AbstractAction("Punkt hier einfügen") {
		@Override
		public void actionPerformed(ActionEvent e) {
			segmentModel.insertPitchMark(timeAtPosition(popupPoint.x), pitchForPosition(popupPoint.y));
			callSynthesis();
		}
	};
	
	/** the action handling the deletion of a pitch mark */
	Action deletePitchMarkAction = new AbstractAction("Punkt löschen") {
		@Override
		public void actionPerformed(ActionEvent e) {
			SegmentBoundPitchMark pm = getPitchMarkAtPosition(popupPoint);
			SegmentModel.remove(pm);
			callSynthesis();
		}
	};
	
	/** determine whether we're in the segment area or in the pitchmark area */
	boolean inSegmentArea(MouseEvent e) {
		return e.getY() < PITCH_AREA_START;
	}
	
	void callSynthesis() {
		this.pitchRange = segmentModel.getPitchRange();
		repaint(); // if we have to re-synthesize, then we're likely to have to repaint as well
		if (synthesisAction != null)
			synthesisAction.actionPerformed(new ActionEvent(this, 0, "synthesize"));		
	}
	
	public void setZoom(float zoom) {
		this.zoom  = zoom;
		revalidate();
		repaint();
	}

	SegmentModel getSegmentModel() {
		return segmentModel;
	}
	
	void setSegmentModel(SegmentModel sm) {
		this.segmentModel = sm;
		this.pitchRange = segmentModel.getPitchRange();
		this.repaint();
	}
	
	private Segment getSegmentAtPosition(int mouseX) {
		return getSegmentModel().getSegmentAt(timeAtPosition(mouseX));
	}
		
	private boolean hasSegmentBoundaryAtPosition(int mouseX) {
		for (Segment l : getSegmentModel().getSegments()) {
			if (Math.abs(mouseX - getRightBoundary(l)) < PITCH_HOTSPOT_SIZE) {
				return true;
			}
		}
		return false;
	}
	
	private SegmentBoundPitchMark getPitchMarkAtPosition(Point p1) {
		for (HotSpot<SegmentBoundPitchMark> p2 : getPitchPoints()) {
			if (p2.matches(p1))
				return p2.getItem();
		}
		return null;		
	}
	
	private boolean hasPitchMarkAtPosition(Point p1) {
		return getPitchMarkAtPosition(p1) != null;
	}
	
	@Override
    public Dimension getPreferredSize() {
        return new Dimension((int) (segmentModel.getDuration() * zoom), PANEL_HEIGHT);
    }
    
    private List<HotSpot<SegmentBoundPitchMark>> getPitchPoints() {
    	List<HotSpot<SegmentBoundPitchMark>> pointList = new ArrayList<HotSpot<SegmentBoundPitchMark>>();
        List<Segment> segments = getSegmentModel().getSegments();
        for (Segment l : segments) {
        	for (SegmentBoundPitchMark pm : l.getPitchMarks()) {
        		int x = positionAtTime(pm.getTime());
        		int y = positionForPitch(pm.getPitch());
        		pointList.add(new HotSpot<SegmentBoundPitchMark>(x, y, pm, PITCH_HOTSPOT_SIZE));
        	}
        }
        return Collections.unmodifiableList(pointList);
    }
    
    /** paint the background */
    private void paintPitchBackground(Graphics g) {
        g.setColor(PITCH_BACKGROUND_COLOR);
        g.fillRect(0, PITCH_AREA_START, getWidth(), PITCH_AREA_HEIGHT);
    }
    
    /** handle painting of the pitchmark area */
    private void paintPitchMarks(Graphics g) {
    	g.setColor(PITCH_COLOR);
    	for (Point p : getPitchPoints()) {
    		g.drawOval(p.x - 2, p.y - 2, 4, 4);    		
    	}
    }
    
    /** handle painting of the segment area */
    private void paintSegments(Graphics g) {
        List<Segment> segments = getSegmentModel().getSegments();
        for (Segment l : segments) {
        	int x = getLeftBoundary(l);
        	g.setColor(BORDER_COLOR);
        	if (x != 0) 
        		g.drawLine(x, 0, x, PANEL_HEIGHT);
        	x = getCenter(l); 
        	g.setColor(TEXT_COLOR);
        	g.setFont(VisualTTS.DEFAULT_FONT);
        	String labelText = l.getText();
        	int width = g.getFontMetrics().charsWidth(labelText.toCharArray(), 0, labelText.length());
        	g.drawString(labelText, x - width / 2, TEXT_BASELINE);
	    }
        int x = getRightBoundary(segments.get(segments.size() - 1));
    	g.setColor(BORDER_COLOR);
   		g.drawLine(x, 0, x, PANEL_HEIGHT);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintPitchBackground(g);
        paintPitchMarks(g);
        paintSegments(g);
    }
    
    private int getLeftBoundary(Segment l) {
    	return positionAtTime(l.getStartTime());
    }
    
    private int getRightBoundary(Segment l) {
    	return positionAtTime(l.getEndTime());
    }
    
    private int getCenter(Segment l) {
    	return positionAtTime(l.getCenter());
    }
    
    /** convert position in pixels to time in ms */ 
    private int timeAtPosition(int mouseX) {
		return (int) (mouseX / zoom);
	}
    
    /** convert time in ms to position in pixels */ 
    private int positionAtTime(int time) {
    	return (int) (time * zoom);
    }
    
    private int positionForPitch(int pitch) {
    	final float pixPerPitch = ((PITCH_AREA_HEIGHT - 2 * HEAD_ROOM) / (float) pitchRange.range());
    	final int relPosition = (int) ((pitch - pitchRange.getMin()) * pixPerPitch);
    	return PITCH_AREA_START + PITCH_AREA_HEIGHT - HEAD_ROOM - relPosition;
    }
    
    private int pitchForPosition(int mouseY) {
    	final float pitchPerPix = pitchRange.range() / (float) ((PITCH_AREA_HEIGHT - 2 * HEAD_ROOM));
    	final int relPitch = (int) ((PITCH_AREA_START + PITCH_AREA_HEIGHT - HEAD_ROOM - mouseY) * pitchPerPix);
    	return pitchRange.getMin() + relPitch;
    }

}