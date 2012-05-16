package extra.inpro.synthesis.visual;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

/**
 * A MenuItem that shows the relevant parts of an IPA table and returns the
 * selected value via a registered Action (segmentAction) 
 * @author timo
 */
@SuppressWarnings("serial")
public class SegmentSelector extends JMenuItem {

	private static final int HOTSPOT_SIZE = 10;
	
	Dimension size = new Dimension(522, 200);
	
	Image vowelImage = Toolkit.getDefaultToolkit().createImage(SegmentSelector.class.getResource("vowels-small.png"));
	List<StringHotSpot> vowelHotSpots = Arrays.<StringHotSpot>asList(
			new StringHotSpot("i:", 6, 7),
			new StringHotSpot("y:", 27, 9),
			new StringHotSpot("I", 56, 21),
			new StringHotSpot("Y", 79, 21),
			new StringHotSpot("e:", 31, 48),
			new StringHotSpot("2:", 55, 48),
			new StringHotSpot("E", 60, 88),
			new StringHotSpot("9", 84, 88),
			new StringHotSpot("E:", 73, 108),
			new StringHotSpot("a:", 88, 127),
			new StringHotSpot("a", 161, 127),
			new StringHotSpot("6", 131, 108),
			new StringHotSpot("@", 119, 76),
			new StringHotSpot("aI", 105, 91),
			new StringHotSpot("aU", 151, 87),
			new StringHotSpot("OY", 114, 43),
			new StringHotSpot("u:", 183, 7),
			new StringHotSpot("U", 160, 21),
			new StringHotSpot("o:", 183, 48),
			new StringHotSpot("O", 183, 88)
		);
	Point vowelImageOffset = new Point(15, 55);
	Image consonantImage = Toolkit.getDefaultToolkit().createImage(SegmentSelector.class.getResource("consonants-small.png"));
	List<StringHotSpot> consonantHotSpots = Arrays.<StringHotSpot>asList(
			new StringHotSpot("m", 33, 15),
			new StringHotSpot("n", 115,  15),
			new StringHotSpot("N", 238, 15),
			new StringHotSpot("p", 12, 42),
			new StringHotSpot("b", 33, 41),
			new StringHotSpot("t", 94, 41),
			new StringHotSpot("d", 115, 41),
			new StringHotSpot("k", 217, 41),
			new StringHotSpot("g", 238, 41),
			new StringHotSpot("f", 53, 67),
			new StringHotSpot("v", 73, 69),
			new StringHotSpot("s", 94, 69),
			new StringHotSpot("z", 115, 69),
			new StringHotSpot("S", 136, 69),
			new StringHotSpot("Z", 156, 69),
			new StringHotSpot("C", 177, 69),
			new StringHotSpot("x", 218, 69),
			new StringHotSpot("h", 260, 69),
			new StringHotSpot("pf", 56, 93),
			new StringHotSpot("ts", 95, 93),
			new StringHotSpot("tS", 136, 93),
			new StringHotSpot("l", 115, 119),
			new StringHotSpot("j", 196, 121),
			new StringHotSpot("R", 238, 119)
		); 
	Point consonantImageOffset = new Point(215, 55);
	
	Rectangle silenceRect; 
	
	String mostRecentSound = null;
	
	private final Action segmentAction;
	
	public SegmentSelector(Action segmentAction) {
		this.segmentAction = segmentAction;
	}
	
	public SegmentSelector() {
		this(null);
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			mostRecentSound = getSoundAt(e.getPoint());
			if (segmentAction != null) {
				ActionEvent event = new ActionEvent(this, 0, mostRecentSound);
				segmentAction.actionPerformed(event);
			}			
		}
	}
	
	private String getSoundAt(Point p) {
		if (inImageArea(p, vowelImageOffset, vowelImage)) {
			return getHotSpotAt(p, vowelImageOffset, vowelHotSpots);
		}
		if (inImageArea(p, consonantImageOffset, consonantImage)) {			
			return getHotSpotAt(p, consonantImageOffset, consonantHotSpots);
		}
		if (silenceRect.contains(p)) {
			return "_";
		}
		return null;
	}
	
	private boolean inImageArea(Point p, Point imageOffset, Image image) {
		return p.x >= imageOffset.x && p.x <= imageOffset.x + image.getWidth(null)
		    && p.y >= imageOffset.y && p.y <= imageOffset.y + image.getHeight(null);
	}
	
	private String getHotSpotAt(Point p, Point imageOffset, List<StringHotSpot> hotspots) {
		Point relativePosition = new Point(p.x - imageOffset.x, p.y - imageOffset.y);
		for (StringHotSpot hs : hotspots) {
			if (hs.matches(relativePosition)) {
				return hs.getItem();
			}
		}
		return null;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return size;
	}

	private void printCentered(Graphics g, String str, int x, int y) {
		int width = g.getFontMetrics().stringWidth(str);
		g.drawString(str, x - width / 2, y);		
	}
	
	private void drawSilenceSpot(Graphics g) {
		g.setColor(Color.WHITE);
		String cmd = "Stille";
		int width = g.getFontMetrics().stringWidth(cmd);
		int height = g.getFontMetrics().getHeight();
		silenceRect = new Rectangle(200 - 1, 20 - height, width + 1, height + 4);
		g.fillRect(silenceRect.x, silenceRect.y, silenceRect.width, silenceRect.height);
		g.setColor(Color.BLACK);
		g.drawString(cmd, 200, 20);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (segmentAction != null) {
			String name = segmentAction.getValue(Action.NAME).toString();
			g.drawString(name, 0, 20);
		}
		printCentered(g, "Vokale", 110, 48);
		printCentered(g, "Konsonanten", 343, 48);
		drawSilenceSpot(g);
		if (!g.drawImage(vowelImage, vowelImageOffset.x, vowelImageOffset.y, null))
			repaint();
		if (!g.drawImage(consonantImage, consonantImageOffset.x, consonantImageOffset.y, null)) 
			repaint();
	}
	
	/** a hostspot with a fixed size, containing a string */
	class StringHotSpot extends HotSpot<String> {
		StringHotSpot(String item, int x, int y) {
			super(new Point(x, y), item, HOTSPOT_SIZE);
		}
	}
	
	private static void createAndShowGUI() {
		JFrame frame = new JFrame("SegmentSelector Test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// add our object
		JComponent mainPanel = new SegmentSelector();
		frame.setContentPane(mainPanel);
		// Display the window.
        frame.pack();
		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
