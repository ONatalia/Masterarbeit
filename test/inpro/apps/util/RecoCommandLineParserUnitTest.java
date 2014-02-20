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

}
