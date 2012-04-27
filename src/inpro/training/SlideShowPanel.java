package inpro.training;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A slide show panel to be used as part of DataCollector.
 * 
 * Given some XML (in an InputStream), this loads images  
 * and displays them in a slideshow. The user is responsible
 * for zapping through the slides, there are no automatic
 * slide changes.
 * 
 * The XML-format is as follows:
 * <code><pre>
 * &lt;slideshow description="a short description or name for the slideshow"
 *            root="file:/the/URL/root/for/the/slides/"%gt;
 *     &lt;slide url="image-url(relative to root).png" 
 *            description="the text that should be displayed together with this slide" /&gt;
 *     &lt;slide ...
 * &lt;/slideshow&gt;
 * </pre></code>
 * The object can be queried which data is currently being displayed 
 * 
 * @author timo
 */
@SuppressWarnings("serial")
public class SlideShowPanel extends JPanel implements ActionListener {
	/** description for this slideshow */
	private String description;
	
	/** list of currently available slides; initialized with one empty slide */
	private List<SlideIcon> images = Collections.singletonList(new SlideIcon());
	/** index to the currently displayed slide */ 
	private int current;

	private JLabel slideLabel;
	private JButton leftButton;
	private JButton rightButton;
	
	/**
	 * create a slide show panel without pre-loaded slides.
	 * Apart from the JLabel which displays the slides,
	 * two buttons are created to browse through the slide show
	 * 
	 */
	public SlideShowPanel(int textPosition) {
		super();
		setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		leftButton = new JButton("<");
		leftButton.addActionListener(this);
		panel.add(leftButton);
		rightButton = new JButton(">");
		rightButton.addActionListener(this);
		panel.add(rightButton);
		panel.setAlignmentX(0.5f);
		add(panel, (textPosition == SwingConstants.TOP
						? BorderLayout.NORTH : BorderLayout.SOUTH)
		);
		slideLabel = new JLabel();
		slideLabel.setHorizontalAlignment(SwingConstants.CENTER);
		slideLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		slideLabel.setVerticalAlignment(SwingConstants.CENTER);
		slideLabel.setVerticalTextPosition(textPosition);
		slideLabel.setAlignmentX(0.5f);
		slideLabel.setAlignmentY(1.0f);
		JPanel slideLabelPanel = new JPanel(new GridBagLayout());
		// anchor at top and avoid distribution of extra space
		GridBagConstraints gbconst = new GridBagConstraints();
		gbconst.anchor = GridBagConstraints.NORTH;
		gbconst.weightx = 1.0;  
		gbconst.weighty = 1.0;
		slideLabelPanel.add(slideLabel, gbconst);
		JScrollPane scrollPane = new JScrollPane(slideLabelPanel);

		add(scrollPane, BorderLayout.CENTER);
		current = 0;
		setImage(current);
	}
	
	/**
	 * create a slide show panel with slides from given slideshow XML.
	 * 
	 * try for example 
	 * <code><pre>
	 * new SlideShowPanel(SlideShowPanel.class.getResourceAsStream("slides.xml"));
	 * </pre></code>
	 * @param is an InputStream containing slideshow XML 
	 */
	public SlideShowPanel(InputStream is) {
		this(SwingConstants.TOP);
		setXML(is);
	}
	
	/**
	 * set the slide show images from a given URL.
	 * 
	 * try for example
	 * <code><pre>
	 * slideshow.setXML(new URL("http://www.ling.uni-potsdam.de/~timo/projekte/dc/slides1.xml"));
	 * </pre></code>
	 * 
	 * @param url URL to the XML
	 * @throws IOException when the URL cannot be accessed
	 */
	public void setXML(URL url) throws IOException {
		setXML(url.openStream());
	}
	
	/**
	 * sets the slide show images from given slideshow XML.
	 * 
	 * try for example 
	 * <code><pre>
	 * slideshow.setXML(MyClass.getResourceAsStream("slides.xml"));
	 * </pre></code>
	 * 
	 * This code uses JAXB to convert the XML contained in the
	 * InputStream to SlideShowXML and SlideXML objects, which
	 * are then passed to <pre>setXML(SlideShowXML ssx)</pre> 
	 * 
	 * @param is an InputStream containing slideshow XML
	 */
	public void setXML(InputStream is) {
		SlideShowXML ssx = null;
		try {
			JAXBContext context = JAXBContext.newInstance(SlideShowXML.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(null);
			ssx = (SlideShowXML) unmarshaller.unmarshal(is);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		setXML(ssx);
	}
	
	/**
	 * set the slide show images from given slideshow XML.
	 * 
	 * This creates a new images list from the given XML. 
	 * @param ssx simple object representing the XML structure read by JAXB  
	 */
	private void setXML(SlideShowXML ssx) {
		if (ssx != null) {
			this.description = ssx.description;
			images = new ArrayList<SlideIcon>();
			for (SlideXML sx : ssx.slides) {
				try {
					SlideIcon ii = new SlideIcon(ssx.root + sx.url, sx.description);
					images.add(ii);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				
			}
		}
		setImage(current);
	}
	
	/**
	 * Handles slide forwarding / backwarding depending
	 * on which button this action was performed on. 
	 */
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == leftButton) {
			current--;
			if (current < 0) current += images.size(); // avoid negative numbers
		} else if (ae.getSource() == rightButton) {
			current++;
		}
		current = current % images.size();
		setImage(current);
	}
	
	/**
	 * change to a specific slide.
	 * @param i
	 */
	public void setImage(int i) {
		assert i >= 0;
		assert i < images.size();
		slideLabel.setIcon(images.get(i));
		slideLabel.setDisabledIcon(images.get(i));
		slideLabel.setText(images.get(i).getDescription());
	}
	
	/**
	 * get information about the currently displayed slide.
	 * @return short textual info about the current slide.
	 */	
	public String getCurrentSlideInfo() {
		return "slideshow:\t" + this.description + "\n" + images.get(current).getInfoString() + "\n";
	}
	
	/**
	 * used in the demo application.
	 */
	public static void createAndShowGUI() {
		JFrame frame = new JFrame("Test Application");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// add our object
		SlideShowPanel contentPane = new SlideShowPanel(SlideShowPanel.class.getResourceAsStream("slides.xml"));
		contentPane.setOpaque(true);
		//JScrollPane scrollPane = new JScrollPane(contentPane);
		frame.setContentPane(contentPane);
		//Display the window.	
        frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * main method used for debugging.
	 * @param args arguments are ignored
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();	
			}
		});
	}
	
	/**
	 * simple container class for slideshow XML root elements.
	 * This describes complete slideshows
	 * @author timo
	 */
	@XmlRootElement(name = "slideshow")
	private static class SlideShowXML {
		@XmlAttribute(name = "description")
		String description;
		@XmlAttribute(name = "root")
		String root;
		@XmlElement(name = "slide")
		List<SlideXML> slides;
	}
	
	/**
	 * simple container class for slideshow XML slides.
	 * this describes indiviual slides.
	 * @author timo
	 *
	 */
	@XmlRootElement(name = "slide")
	private static class SlideXML {
		@XmlAttribute(name = "url")
		String url;
		@XmlAttribute(name = "description")
		String description;
	}
	
	/**
	 * Simple extension to ImageIcon with an additional infoString field.
	 * 
	 * TODO: it would be much nicer if lazy image loading were implemented
	 * so that the initial delay would be lower. All images that are
	 * not needed immediately could be loaded in the background from
	 * a swing worker thread.
	 * 
	 * @author timo
	 */
	private class SlideIcon extends ImageIcon {
		/** url and description for this slide */
		String infoString;
		
		SlideIcon() {
			super();
			infoString = "<nothing was displayed>\n";
		}
		
		SlideIcon(String url, String description) throws MalformedURLException {
			super(new URL(url));
			setDescription(description);
			infoString = "URL:\t" + url + "\nDescription:\t" + description + "\n";
		}
		
		String getInfoString() {
			return infoString;
		}
	}
	
}
