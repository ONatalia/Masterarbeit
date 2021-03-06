package inpro.nlu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility for building AVM worldList from file/input.
 * @author okko
 */
public class AVMWorldUtil {
	
	public static ArrayList<AVM> setAVMsFromFile(String fileURL, HashMap<String, HashMap<String, String>> avmStructures) {
		ArrayList<AVM> list = new ArrayList<AVM>();
		try {
			BufferedReader lbr = new BufferedReader(new InputStreamReader(new URL(fileURL).openStream()));
			String line;
			while ((line = lbr.readLine()) != null) {
				if (line.startsWith("#") || line.matches("^\\s*$")) continue;
				// {type:X name:kreuz color:green }
				String[] tokens = line.replaceAll("[\\{\\}]", "").split("\\s+");
				ArrayList<AVPair> avpairs = new ArrayList<AVPair>();
				String type = "";
				for (String token : tokens) {
					String[] pair = token.split(":");
					assert pair.length == 2 : "Cannot parse file " + fileURL + ". Failing line is: " + line;
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
	
}