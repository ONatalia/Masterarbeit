package inpro.apps;

import java.util.ArrayList;

public class BlockingQueueData implements Comparable<Object> {
	
	private String text;
	public String getText() {
		return text;
	}
	
	
	
	public void setText(String text) {
		this.text = text;
	}
	private ArrayList <Double> starttimes;
	public ArrayList<Double> getStarttimes() {
		return starttimes;
	}
	public void setStarttimes(ArrayList<Double> starttimes) {
		this.starttimes = starttimes;
	}
	private ArrayList <Double> endtimes;
	public ArrayList<Double> getEndtimes() {
		return endtimes;
	}
	public void setEndtimes(ArrayList<Double> endtimes) {
		this.endtimes = endtimes;
	}



	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

}
