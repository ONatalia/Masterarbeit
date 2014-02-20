package inpro.apps.util;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;

import org.junit.Test;

public class RecoCommandLineParserUnitTest {

	@Test
	public void testParserLmWithURL() {
		String[] args = {"-c","file:src/inpro/apps/config.xml", "-F", "file:res/DE_1234.wav", "-lm", "file:test/data/empty.lm"};
		RecoCommandLineParser pars = new RecoCommandLineParser(args);
		assertEquals("res/DE_1234.wav",pars.audioURL.getPath());
		assertEquals("src/inpro/apps/config.xml", pars.configURL.getPath());
		assertEquals("test/data/empty.lm", pars.languageModelURL.getPath());
		assertTrue(pars.parsedSuccessfully());
	}
	@Test
	public void testParserGrWithURL() {
		String[] args = {"-c","file:src/inpro/apps/config.xml", "-F", "file:res/DE_1234.wav", "-gr", "file:test/data/empty.gram"};
		RecoCommandLineParser pars = new RecoCommandLineParser(args);
		assertEquals("res/DE_1234.wav",pars.audioURL.getPath());
		assertEquals("src/inpro/apps/config.xml", pars.configURL.getPath());
		assertEquals("test/data/empty.gram", pars.getLanguageModelURL().getPath());
		assertTrue(pars.parsedSuccessfully());
	}
	@Test
	public void testParserLmWithPath() {
		String[] args = {"-c","src/inpro/apps/config.xml", "-F", "res/DE_1234.wav","-lm", "test/data/empty.lm"};
		RecoCommandLineParser pars = new RecoCommandLineParser(args);
		String root = new File("").getAbsolutePath();
		assertEquals(root + "/res/DE_1234.wav",pars.audioURL.getPath());
		assertEquals(root + "/src/inpro/apps/config.xml", pars.configURL.getPath());
		assertEquals(root + "/test/data/empty.lm", pars.languageModelURL.getPath());
		assertTrue(pars.parsedSuccessfully());
	}
	
	@Test
	public void testParserGrWithPath() {
		String[] args = {"-c","src/inpro/apps/config.xml", "-F", "res/DE_1234.wav","-gr", "test/data/empty.gram"};
		RecoCommandLineParser pars = new RecoCommandLineParser(args);
		String root = new File("").getAbsolutePath();
		assertEquals(root + "/res/DE_1234.wav",pars.audioURL.getPath());
		assertEquals(root + "/src/inpro/apps/config.xml", pars.configURL.getPath());
		assertEquals(root + "/test/data/empty.gram", pars.languageModelURL.getPath());
		assertTrue(pars.parsedSuccessfully());
	}
	
	@Test
	public void testOtherArguments()
	{
		String[] args = {"-M", "-O", "-v", "-f", "-N"};
		RecoCommandLineParser pars = new RecoCommandLineParser(args);
		assertEquals(CommonCommandLineParser.MICROPHONE_INPUT,pars.getInputMode());
		assertEquals(CommonCommandLineParser.DISPATCHER_OBJECT_OUTPUT, pars.outputMode);
		assertTrue(pars.verbose());
		assertTrue(pars.ignoreErrors());
		assertEquals(RecoCommandLineParser.NON_INCREMENTAL, pars.incrementalMode);
		assertTrue(pars.parsedSuccessfully());
		
		String[] args2 = {"-R", "22", "-T", "-Is", "5", "-rt"};
		RecoCommandLineParser pars2 = new RecoCommandLineParser(args2);
		assertEquals(CommonCommandLineParser.RTP_INPUT, pars2.getInputMode());
		assertEquals(CommonCommandLineParser.TED_OUTPUT, pars2.outputMode);
		assertEquals(22, pars2.rtpPort);
		assertFalse(pars2.verbose());
		assertFalse(pars2.ignoreErrors());
		assertEquals(RecoCommandLineParser.SMOOTHED_INCREMENTAL, pars2.incrementalMode);
		assertEquals(5, pars2.incrementalModifier);
		assertTrue(pars2.parsedSuccessfully());
		
		String[] args3 = {"-M", "-L", "-If", "5"};
		RecoCommandLineParser pars3 = new RecoCommandLineParser(args3);
		assertEquals(CommonCommandLineParser.LABEL_OUTPUT,pars3.outputMode);
		assertEquals(RecoCommandLineParser.FIXEDLAG_INCREMENTAL, pars3.incrementalMode);
		assertEquals(5, pars3.incrementalModifier);
		assertTrue(pars.parsedSuccessfully());
		
	}

}
