package org.cocolab.inpro.nlu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility for building AVM worldList from file/input.
 * @author okko
 */
public class AVMWorldUtil {
	
	public static ArrayList<AVM> setAVMsFromFile(String filename, HashMap<String, HashMap<String, String>> avmStructures) {
		ArrayList<AVM> list = new ArrayList<AVM>();
		try {
			BufferedReader lbr;
			lbr = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = lbr.readLine()) != null) {
				if (line.startsWith("#") || line.matches("^\\s*$")) continue;
				// {type:X name:kreuz color:green }
				String[] tokens = line.replaceAll("[\\{\\}]", "").split("\\s+");
				ArrayList<AVPair> avpairs = new ArrayList<AVPair>();
				String type = "";
				for (String token : tokens) {
					String[] pair = token.split(":");
					assert pair.length == 2 : "Cannot parse file " + filename + ". Failing line is: " + line;
					if (pair[0].equals("type")) {
						type = pair[1];
					} else {
						avpairs.add(new AVPair(pair[0], pair[1]));
					}
				}
				assert (!type.equals("")) : "No type def found for AVM structure " + avpairs.toString();
				AVM avm = new AVM(type, avmStructures);
				for (AVPair avp : avpairs) {
					avm.setAttribute(avp);
				}
				list.add(avm);
			}
			lbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String worldFile = "res/AVMWorldList";
		String structureFile = "res/AVMStructure";
		System.out.println(
				AVMWorldUtil.setAVMsFromFile(worldFile, 
											 AVMStructureUtil.parseStructureFile(structureFile))
				);
	}

}