package org.cocolab.inpro.tts;
/**
 * (C) Timo Baumann, 2010, portions copyright 2000-2006 DFKI GmbH.
 * released under the terms of the GNU LGPL version >= 3
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;

import marytts.Version;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.NoSuchPropertyException;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.Synthesis;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.server.Request;
import marytts.util.MaryUtils;
import marytts.util.Pair;

public class MaryAdapterImpl extends MaryAdapter {

	public static final String DEFAULT_VOICE = System.getProperty("mary.voice", "de2");
	
    private static Logger logger = Logger.getLogger(MaryAdapterImpl.class);

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
        		try {
        			startupInternalMary();
        		} catch (Exception e) {
        			System.err.println("Error setting up internal mary:");
        			e.printStackTrace();
        			throw new RuntimeException(e);
        		}
        		break;
        	default:
        		throw new RuntimeException();
        	}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	// startup-code mostly copied from marytts.server.Mary
	private void startupInternalMary() throws Exception {
        MaryProperties.readProperties();
        // configure logging
        logger.info("Mary starting up...");
        logger.info("Specification version " + Version.specificationVersion());
        logger.info("Implementation version " + Version.implementationVersion());
        logger.info("Running on a Java " + System.getProperty("java.version")
                + " implementation by " + System.getProperty("java.vendor")
                + ", on a " + System.getProperty("os.name") + " platform ("
                + System.getProperty("os.arch") + ", " + System.getProperty("os.version")
                + ")");
        logger.debug("MARY_BASE: "+MaryProperties.maryBase());
        // setup feature processors
        String featureProcessorManagers = MaryProperties.getProperty("featuremanager.classes.list");
        if (featureProcessorManagers == null) {
            throw new NoSuchPropertyException("Expected list property 'featuremanager.classes.list' is missing.");
        }
        StringTokenizer st = new StringTokenizer(featureProcessorManagers);
        while (st.hasMoreTokens()) {
            String fpmInitInfo = st.nextToken();
            try {

                FeatureProcessorManager mgr = (FeatureProcessorManager) MaryUtils.instantiateObject(fpmInitInfo);
                Locale locale = mgr.getLocale();
                if (locale != null) {
                    FeatureRegistry.setFeatureProcessorManager(locale, mgr);
                } else {
                    logger.debug("Setting fallback feature processor manager to '"+fpmInitInfo+"'");
                    FeatureRegistry.setFallbackFeatureProcessorManager(mgr);
                }
            } catch (Throwable t) {
                throw new Exception("Cannot instantiate feature processor manager '"+fpmInitInfo+"'", t);
            }
        }
        // start modules
        for (String moduleClassName : MaryProperties.moduleInitInfo()) {
            MaryModule m = ModuleRegistry.instantiateModule(moduleClassName);
            // Partially fill module repository here; 
            // TODO: voice-specific entries will be added when each voice is loaded.
            ModuleRegistry.registerModule(m, m.getLocale(), null);
        }
        ModuleRegistry.setRegistrationComplete();
        
        List<Pair<MaryModule, Long>> startupTimes = new ArrayList<Pair<MaryModule,Long>>();
        
        // Separate loop for startup allows modules to cross-reference to each
        // other via Mary.getModule(Class) even if some have not yet been
        // started.
        for (MaryModule m : ModuleRegistry.getAllModules()) {
            // Only start the modules here if in server mode: 
            if (((!MaryProperties.getProperty("server").equals("commandline")) || m instanceof Synthesis) 
                    && m.getState() == MaryModule.MODULE_OFFLINE) {
                long before = System.currentTimeMillis();
                try {
                    m.startup();
                } catch (Throwable t) {
                    throw new Exception("Problem starting module "+ m.name(), t);
                }
                long after = System.currentTimeMillis();
                startupTimes.add(new Pair<MaryModule, Long>(m, after-before));
            }
            if (MaryProperties.getAutoBoolean("modules.poweronselftest", false)) {
                m.powerOnSelfTest();
            }
        }
        
        if (startupTimes.size() > 0) {
            Collections.sort(startupTimes, new Comparator<Pair<MaryModule, Long>>() {
                public int compare(Pair<MaryModule, Long> o1, Pair<MaryModule, Long> o2) {
                    return -o1.getSecond().compareTo(o2.getSecond());
                }
            });
            logger.debug("Startup times:");
            for (Pair<MaryModule, Long> p : startupTimes) {
                logger.debug(p.getFirst().name()+": "+p.getSecond()+" ms");
            }
        }
        logger.info("Startup complete.");
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
		case MARY41INTERNAL:
	        MaryDataType mInputType = MaryDataType.get(inputType);
	        MaryDataType mOutputType = MaryDataType.get(outputType);
	        Locale mLocale = MaryUtils.string2locale("de");
	        Voice voice = Voice.getVoice(DEFAULT_VOICE);
	        AudioFormat audioFormat = voice.dbAudioFormat();
	        logger.info("audioFormat is " + audioFormat);
	        AudioFileFormat.Type audioFileFormatType = //MaryAudioUtils.getAudioFileFormatType(audioType);
	        						AudioFileFormat.Type.WAVE;
	        logger.info("audioFileFormatType is " + audioFileFormatType);
	        AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);
	        logger.info("audioFileFormat is " + audioFileFormat);
	        Request request = new Request(mInputType, mOutputType, mLocale, voice, 
	        							  (String) null, (String) null, 
	        							  // the following true ↓ is experimental (switches on streaming)
	        							  1, audioFileFormat, true, (String) null);
	        try {
		        request.setInputData(query);
		        request.process();
		        request.writeOutputData(baos);
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	throw new RuntimeException(e);
	        }
	        return baos;
		default:
			throw new RuntimeException();
		}
	}
	
}
