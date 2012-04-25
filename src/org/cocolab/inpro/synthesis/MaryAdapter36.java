package org.cocolab.inpro.synthesis;
/**
 * (C) Timo Baumann, 2010, portions copyright 2000-2006 DFKI GmbH.
 * released under the terms of the GNU LGPL version >= 3
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

public class MaryAdapter36 extends MaryAdapter {

	de.dfki.lt.mary.client.MaryClient mc36;

	public static final String DEFAULT_VOICE = System.getProperty("mary.voice", "de2");
	
	/** private constructor, this class is a singleton 
	 * @throws IOException */
	MaryAdapter36() throws IOException {
		compatibilityMode = CompatibilityMode.MARY36EXTERNAL;
        String serverHost = System.getProperty("mary.host", "localhost");
        int serverPort = Integer.getInteger("mary.port", 59125).intValue();
		mc36 = new de.dfki.lt.mary.client.MaryClient(serverHost, serverPort);
	}
	
	@Override
	protected ByteArrayOutputStream process(String query, String inputType, String outputType, String audioType) throws UnknownHostException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String defaultVoiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
		if (inputType.equals("TEXT"))
			inputType = "TEXT_DE";
		mc36.process(query, inputType, outputType, audioType,
				defaultVoiceName, baos);
		return baos;
	}
	
}
