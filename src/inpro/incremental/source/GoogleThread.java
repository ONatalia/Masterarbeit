package inpro.incremental.source;

public class GoogleThread extends Thread {

	
	private final GoogleASR gasr;
	public GoogleThread(GoogleASR gasr) {
		// TODO Auto-generated constructor stub
		this.gasr=gasr;
	}
	public GoogleASR getGasr() {
		return gasr;
	}
	
	public void run (){
		gasr.recognize();
	}
	
	

}
