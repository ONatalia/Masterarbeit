package inpro.irmrsc.parser;

import inpro.irmrsc.simplepcfg.Grammar;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.junit.Test;

public class SITDBSParserTest {

	@Test
	public void test() {
		// parser properties
		boolean beRobust = false;
		double baseBeamFactor = 0.001;
		String grammarFile = "file:src/work/inpro/system/pentomino/resources/irmrsc-grammar.xml";
		Logger logger = Logger.getLogger("ParserTest");
	    logger.addAppender(new ConsoleAppender(new SimpleLayout()));
	    logger.setLevel(Level.DEBUG);
		
		
		// input string
	    // german input  :  nimm äh   das blaue teil  oben links und leg es   ins      mittlere feld  <S/>
	    // english gloss :  take ehm  the blue  piece top  left  and put it   into-the middle   field <S/>
	    // POS-tags      :  v    fill det adja  nn    adv  adv   kon v   pper apprart  adja     nn    S!
	    
		String[] input = {"v", "fill", "det", "adja", "nn", "adv", "adv", "kon", "v", "pper", "apprart", "adja", "nn", "S!"}; 
		
		// load grammar
		Grammar g = new Grammar();
		try {
			g.loadXML(new URL(grammarFile));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}		
		
		// initialize parser
		SITDBSParser p = new SITDBSParser(g, baseBeamFactor);
		SITDBSParser.setRobust(beRobust);
		p.setLogger(logger);
		
		// start parsing
		for (String token: input) {
			logger.info("feeding "+token);
			p.feed(token);
			//p.status();
		}
		
		logger.info("### Final derivations:");
		p.info();
		
		logger.info("### Parser statistics:");
		p.status();
	}

}
