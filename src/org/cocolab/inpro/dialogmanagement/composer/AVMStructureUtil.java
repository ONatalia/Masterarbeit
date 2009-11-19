package org.cocolab.inpro.dialogmanagement.composer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Utility for AVM structure construction from file/input.
 * @author timo, okko
 */
public class AVMStructureUtil {

	private HashMap<String, HashMap<String, String>> avmStructures;

	AVMStructureUtil() {
		this("res/AVMStructure");
	}

	AVMStructureUtil(String filename) {
		this.avmStructures = this.parseStructureFile(filename);
	}

	private HashMap<String, HashMap<String, String>> parseStructureFile(String filename) {
		HashMap<String, HashMap<String, String>> avmStructures = new HashMap<String, HashMap<String, String>>();
		try {
			BufferedReader lbr;
			lbr = new BufferedReader(new FileReader(filename));
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
				avmStructures.put(type, attributes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return avmStructures;
	}
	
	public HashMap<String, HashMap<String, String>> getAVMStructures()  {
		return this.avmStructures;
	}	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		AVMStructureUtil su = new AVMStructureUtil();
		System.out.println(su.avmStructures.toString());
	}

}
