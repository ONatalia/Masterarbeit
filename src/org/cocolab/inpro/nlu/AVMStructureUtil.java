package org.cocolab.inpro.nlu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

/**
 * Utility for AVM structure construction from file/input.
 * @author timo, okko
 */
public class AVMStructureUtil {
	
	// TODO: might make sense to keep previous structures around (in yet another hash map...)
	// to speed up parseStructureFile
	public static HashMap<String, HashMap<String, String>> parseStructureFile(URL fileURL) {
		HashMap<String, HashMap<String, String>> avmStructures = new HashMap<String, HashMap<String, String>>();
		try {
			BufferedReader lbr = new BufferedReader(new InputStreamReader(fileURL.openStream()));
			String line;
			while ((line = lbr.readLine()) != null) {
				if (line.startsWith("#") || line.matches("^\\s*$")) continue;
				String[] tokens = line.replaceAll("[\\[\\]]", "").split("\\s");
				String type = null;
				HashMap<String, String> attributes = new HashMap<String, String>(tokens.length + 1);
				for (String token : tokens) {
					String[] pair = token.split(":");
					if (pair[0].equals("type")) {
						type = pair[1];
					} else {
						attributes.put(pair[0], pair[1]);
					}
				}
				assert (type != null) : "Found AVM structure without type (" + attributes.toString() + ")!";
				assert (!attributes.isEmpty()) : "No structures found for type " + type  + "!";
				avmStructures.put(type, attributes);
			}
			lbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assert (avmStructures != null) : "Could not parse AVM structures in " + fileURL.toString() + ".";
		return avmStructures;
	}

}
