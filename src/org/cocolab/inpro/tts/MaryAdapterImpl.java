package org.cocolab.inpro.tts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

public class MaryAdapterImpl extends MaryAdapter {

	public static final String DEFAULT_VOICE = System.getProperty("mary.voice", "de2");
	
	enum CompatibilityMode { MARY36EXTERNAL, MARY41EXTERNAL, MARY41INTERNAL;
		public static CompatibilityMode fromString(String mode) {
			if ("4.1".equals(mode)) return MARY41EXTERNAL; 
			if ("internal".equals(mode)) return MARY41INTERNAL;
			return MARY36EXTERNAL;
		}
	};
	CompatibilityMode compatibilityMode = CompatibilityMode.fromString(
									System.getProperty("mary.version", "3.6"));

	/** private constructor, this class is a singleton */
	MaryAdapterImpl() {
        try {
        	switch (compatibilityMode) {
        	case MARY36EXTERNAL:
        		compatibilityMode = CompatibilityMode.MARY36EXTERNAL;
                String serverHost = System.getProperty("mary.host", "localhost");
                int serverPort = Integer.getInteger("mary.port", 59125).intValue();
    			mc36 = new de.dfki.lt.mary.client.MaryClient(serverHost, serverPort);
        		break;
        	case MARY41EXTERNAL:
        		compatibilityMode = CompatibilityMode.MARY41EXTERNAL;
                serverHost = System.getProperty("mary.host", "localhost");
                serverPort = Integer.getInteger("mary.port", 59125).intValue();
        		mc41 = marytts.client.MaryClient.getMaryClient(
			               new marytts.client.http.Address(serverHost, serverPort)
			           );
        		break;
        	case MARY41INTERNAL:
        		break;
        	default:
        		throw new RuntimeException();
        	}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	protected ByteArrayOutputStream process(String query, String inputType, String outputType, String audioType) throws UnknownHostException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String defaultVoiceName = System.getProperty("inpro.tts.voice", DEFAULT_VOICE);
		switch (compatibilityMode) {
		case MARY36EXTERNAL:
			if (inputType.equals("TEXT"))
				inputType = "TEXT_DE";
			mc36.process(query, inputType, outputType, audioType,
					defaultVoiceName, baos);
			return baos;
		case MARY41EXTERNAL:
			String locale = "de";
			mc41.process(query, inputType, outputType, locale, audioType,
					defaultVoiceName, baos);
			return baos;
		default:
			throw new RuntimeException();
		}
	}
	
}
