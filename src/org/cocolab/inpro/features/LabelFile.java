package org.cocolab.inpro.features;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LabelFile {

	/**
     * Returns a List of the lines in a label file.
     *
     * @param batchFile the batch file to read
     * @param skip the number of lines to skip between items
     *
     * @return a List of the lines in a label file
     */
    public static List<String> getLines(String labelFile, int skip) throws IOException {
        int curCount = skip;
        List<String> list = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(labelFile));
        
        String line = null;
        
        while ((line = reader.readLine()) != null) {
            if (line.length() > 0) {
                if (++curCount >= skip) {
                    list.add(line);
                    curCount = 0;
                }
            }
		}
		reader.close();
        return list;
    }
    
    public static String getLastLine(String labelFile) throws IOException {
    	List<String> lines = getLines(labelFile, 0);
    	return lines.get(lines.size() - 1);
    }
    
    private static String getTokenInLine(String labelLine, int token) {
    	String[] tokens = labelLine.split("\\s+");
    	return tokens[token];
    }
    
    private static double getTokenAsTime(String labelLine, int tokenID) {
    	String token = getTokenInLine(labelLine, tokenID);
    	int samples = Integer.parseInt(token);
    	double time = ((double) samples) / 16000.f; // FIXME: should not be hardcoded  
    	return time;
    }

    public static double getStartTime(String labelLine) {
    	return getTokenAsTime(labelLine, 0);
    }
    
    public static double getStopTime(String labelLine) {
    	return getTokenAsTime(labelLine, 1);
    }
    
    public static String getLabel(String labelLine) {
    	return getTokenInLine(labelLine, 2);
    }
    
}
