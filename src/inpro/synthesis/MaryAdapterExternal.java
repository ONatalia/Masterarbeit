package inpro.synthesis;
/**
 * (C) Timo Baumann, 2010, portions copyright 2000-2006 DFKI GmbH.
 * released under the terms of the GNU LGPL version >= 3
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import marytts.client.http.MaryHttpClient;
import marytts.util.http.Address;

public class MaryAdapterExternal extends MaryAdapter {

	marytts.client.MaryClient mc41;

	public static final String DEFAULT_VOICE = System.getProperty("inpro.tts.voice", "de6");
	
	/** private constructor, this class is a singleton 
	 * @throws IOException */
	MaryAdapterExternal() throws IOException {
        String serverHost = System.getProperty("inpro.tts.mary.host", "localhost");
        int serverPort = Integer.getInteger("inpro.tts.mary.port", 59125).intValue();
        mc41 = new MaryHttpClient(new Address(serverHost, serverPort), false, false);
//		mc41 = marytts.client.MaryClient.getMaryClient(
//	               new marytts.client.http.Address(serverHost, serverPort)
//	           );
	}
	
	@Override
	protected ByteArrayOutputStream process(String query, String inputType, String outputType, String audioType) throws UnknownHostException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String defaultVoiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
		String locale = "de";
		mc41.process(query, inputType, outputType, locale, audioType,
				defaultVoiceName, baos);
		return baos;
	}
	
}

