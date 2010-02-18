package org.cocolab.inpro.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Container for all the files in the current session.
 * An archive (in the form of a ZIP file) may be 
 * stored on disk or sent to a server via the network.
 * 
 * @author timo
 */
public class SessionData { // FIXME: improve naming

	/** the files contained in this archive */
	List<File> files = new ArrayList<File>(); 
	
	/** meta data for this archive */
	MetaData metaData; // FIXME: should this rather live somewhere else (somewhere more guiy?)

	/** 
	 * the boundary between multipart message parts. As it is highly unlikely  
	 * for this exact byte sequence to occur within the message, we ignore   
	 * that we should actually check for the sequence within the message. 
	 */
	final static String BOUNDARY = "---------------------------01234567890";
	/** size of the buffer when piping from input buffer to output buffer */
	static final int BUFFER_SIZE = 4096;

	/**
	 * add a file to the archive
	 * @param file the file to add
	 */
	void addFile(File file) {
		files.add(file);
	}
	
	/**
	 * add a file given its filename
	 */
	void addFile(String filename) {
		addFile(new File(filename));
	}
	
	/**
	 * add an input stream that will be added to the archive with the given name.
	 * TODO: this could be implemented by holding a separate list, similar to the list of files
	 * @param stream the data to be added
	 * @param filename the filename in the zipfile
	 */
	void addStream(InputStream stream, String filename) {
		/* TODO: implement */
	}
	
	/**
	 * clear the archive of previous entries
	 */
	void clear() {
		files = new ArrayList<File>();
	}

	/**
	 * pipe the content of an input stream into an output stream
	 * @param in the stream being drained
	 * @param out destination of the data
	 * @throws IOException if reading or writing goes wrong
	 */
	private static void pipe(InputStream in, OutputStream out) throws IOException {
		int count;
		byte data[] = new byte[BUFFER_SIZE];
		while((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
		   out.write(data, 0, count);
		}
		out.flush();
	}
	
	/**
	 * write the archive's content to an output stream
	 * @param outStream the stream that is written to
	 * @throws IOException when errors occur on reading the files contained in the archive
	 */
	void toOutputStream(OutputStream outStream) throws IOException {
		ZipOutputStream zipStream = new ZipOutputStream(outStream);
		for (File f : files) {
			FileInputStream fi = new FileInputStream(f);
			ZipEntry ze = new ZipEntry(f.getName());
			zipStream.putNextEntry(ze);
			pipe(fi, zipStream);
			fi.close();
			zipStream.closeEntry();
		}
		zipStream.close();
	}
	
	/**
	 * post the archive's content (encoded as a zip file) to a server.
	 * 
	 * in Swing, this should be called from a SwingWorker thread
	 * and some progress status should be displayed, as posting
	 * large archives can take quite a while. 
	 * 
	 * @param url the server's URL
	 * @throws IOException 
	 */
	InputStream postToServer(URL url) throws IOException {
		// TODO:IMPLEMENT
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true); // use this connection for output
		connection.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + BOUNDARY);
		StringBuilder sb = new StringBuilder("--");
		sb.append(BOUNDARY);
		sb.append("\r\n");
		sb.append("Content-Disposition: form-data; name=\"");
		sb.append("zipfile");
		sb.append("\"; filename=\"");
		sb.append("dc-archive.zip");
		sb.append("\"");
		sb.append("\r\n");
		sb.append("Content-Type: application/zip");
		sb.append("\r\n");
		sb.append("\r\n");
		OutputStream os = connection.getOutputStream();
		os.write(sb.toString().getBytes());
		PipedInputStream pin = new PipedInputStream();
		final PipedOutputStream pout = new PipedOutputStream(pin); 
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					toOutputStream(pout);
					pout.flush();
					pout.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}).start();
		pipe(pin, os);
		sb = new StringBuilder("\r\n");
		sb.append("--");
		sb.append(BOUNDARY);
		sb.append("--");
		sb.append("\r\n");
		os.write(sb.toString().getBytes());
		os.flush();
		os.close();
		return connection.getInputStream();
	}
	
	InputStream postToServer(String url) throws IOException {
		return postToServer(new URL(url));
	}
	
	/**
	 * write the archive's content to a local zip file
	 * @param filename name for the zip file
	 * @throws IOException when the file cannot be written to
	 */
	void saveToFile(String filename) throws IOException {
		File outFile = new File(filename);
		FileOutputStream outStream = new FileOutputStream(outFile);
		toOutputStream(outStream);
	}
	
	/**
	 * for testing only.
	 * @param args arguments are ignored
	 * @throws IOException when something goes wrong 
	 */
	public static void main(String[] args) throws IOException {
		SessionData a = new SessionData();
		a.addFile("/home/timo/IMG_1881.JPG");
		a.addFile("/home/timo/IMG_2459.JPG");
		a.saveToFile("/tmp/testfile.zip");
		InputStream is = a.postToServer("http://www.sfb632.uni-potsdam.de/cgi-timo/upload.pl");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = reader.readLine()) != null) {
			System.err.println(line);
		}

	}

}
