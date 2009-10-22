package org.cocolab.inpro.dialogmanagement.composer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author timo
 * Utility for AVPair construction from file/input.
 */
public class AVPairMappingUtil {

	private static Map<String, List<AVPair>> readAVPairs(String filename) throws IOException {
		BufferedReader lbr = new BufferedReader(new FileReader(filename));
		Map<String, List<AVPair>> avPairs = new HashMap<String, List<AVPair>>();
		String line;
		while ((line = lbr.readLine()) != null) {
			if (line.startsWith("#") || line.matches("^\\s*$")) continue;
			String[] tokens = line.split("\\s*\t\\s*");
			assert (tokens.length == 2) : "Error parsing " + line;
			String[] words = tokens[0].split("\\s*,\\s*");
			String[] avArray = tokens[1].split("\\s*,\\s*");
			List<AVPair> avList = new ArrayList<AVPair>(avArray.length);
			for (String av : avArray) {
				avList.add(new AVPair(av));
			}
			for (String word : words) {
				assert (!avPairs.containsKey(word)) : "You are (by mistake?) trying to redefine the mapping for " + word;
				avPairs.put(word, avList);
			}
		}
		return avPairs;
	}	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Map<String, List<AVPair>> avPairs = readAVPairs("res/AVMapping");
		System.out.println(avPairs.toString());
	}

}
