package org.cocolab.inpro.annotation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LabelFile {

	/**
     * Returns a List of the lines in a label file.
     *
     * @param batchFile the batch file to read
     * @param skip the number of lines to skip between items
     *
     * @return a List of the lines in a label file
	 * @throws IOException 
     */
    public static List<String> getLines(String labelFile, int skip) throws IOException {
        int curCount = skip;
        List<String> list = new ArrayList<String>();
        try {
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
        } catch (FileNotFoundException ioe) {
        	System.err.println("File not found: " + labelFile);
//        	ioe.printStackTrace();
        }
        return list;
    }
    
    public static LinkedList<Label> getLabels(String labelFile) throws IOException {
    	LinkedList<Label> labels = new LinkedList<Label>();
    	List<String> lines = getLines(labelFile, 0);
    	double stopTime = 0.0;
    	for (String line : lines) {
    		stopTime = getStopTime(line);
    		labels.add(new Label(getStartTime(line), stopTime, getLabel(line)));
    	}
    	// automatically add a silence label in the end
    	labels.add(new Label(stopTime, Long.MAX_VALUE, "sil"));
    	return labels;
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
//    	int samples = Integer.parseInt(token); // FIXME: should be configurable if samples or time in seconds is used
//    	double time = ((double) samples) / 16000.f; // FIXME: should not be hardcoded  
    	double time = Double.parseDouble(token);
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
