package inpro.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.log4j.Logger;

/**
 * Output given messages to a running TEDview instance.
 * @author timo
 */
public class TedAdapter {
	private Socket tedSocket;
    private PrintWriter tedWriter;
    
    private boolean connected = false;

    public TedAdapter(String tedAddress, int tedPort) {
		try {
			tedSocket = new Socket(tedAddress, tedPort);
			tedWriter = new PrintWriter(tedSocket.getOutputStream());
			connected = true;
		} catch (IOException e) {
			Logger.getLogger(TedAdapter.class).info("Cannot connect to TEDview. I will not retry.");
			connected = false;
		}
	}
    
    public boolean isConnected() {
    	return connected;
    }
    
	public void write(String message) {
		if (isConnected()) {
			tedWriter.print(message + "\n\n");
			tedWriter.flush();
		}
	}

	protected void finalize() {
    	tedWriter.close();
    	try {
			tedSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}