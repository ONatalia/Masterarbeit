package inpro.synthesis;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import inpro.incremental.unit.IU;
import inpro.synthesis.MaryAdapter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MaryAdapterIUUnitTest {

	@Test
	public void testText2maryxml() throws SAXException, IOException, ParserConfigurationException {
		SAXParser saxp = SAXParserFactory.newInstance().newSAXParser();
		saxp.parse(MaryAdapter.getInstance().text2maryxml("Hallo"), (DefaultHandler) null);
	}

	@Test
	public void testText2IUs() {
		MaryAdapter ma = MaryAdapter.getInstance();
		List<IU> ius = ma.text2IUs("eins zwei drei vier fünf sechs sieben acht");
		assertEquals(ius.size(), 8);
	}

}
