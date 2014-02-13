package inpro.io.webspeech.servlets;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sun.media.sound.WaveFileWriter;

/**
 * Servlet implementation class DialogRecording
 */
@WebServlet("/DialogRecording")
@MultipartConfig
public class DialogRecording extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DialogRecording() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// This receives the wav data and writes it to file, if wanted
//		System.out.println("receiving recording...");
		String uttKey = getValue(request.getPart("utterance_key")).toString();
		String csrf = (String) request.getSession().getAttribute("csrf_token");
		
		Part part = request.getPart("data");
		InputStream in = new BufferedInputStream(part.getInputStream());
//		need to make sure it can append the file that has already started
		long unixTime = System.currentTimeMillis() / 1000L;
		String path = "wavs/" + csrf + ".wav";
		FileOutputStream out = new FileOutputStream(new File(path), true);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
		    out.write(buf, 0, len);
		}

		in.close();
		out.close();
	}
		
	
	private String getValue(Part part) throws IOException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream(), "UTF-8"));
	    StringBuilder value = new StringBuilder();
	    char[] buffer = new char[1024];
	    for (int length = 0; (length = reader.read(buffer)) > 0;) {
	        value.append(buffer, 0, length);
	    }
	    return value.toString();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// uncomment the line below when you want to use this "feature"
		
		//doGet(request, response);
	}

}
