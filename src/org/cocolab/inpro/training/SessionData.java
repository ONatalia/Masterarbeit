package org.cocolab.inpro.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Container for all data in the current session.
 * SessionData contains files in the file system,
 * and smaller information chunks (in SmallFile objects). 
 * 
 * An archive (in the form of a ZIP file) may be 
 * stored on disk or sent to a server via the network.
 * 
 * @author timo
 */
public class SessionData {

	/** the files contained in this archive */
	final List<URL> urls = new ArrayList<URL>(); 
	/** additional list of small information chunks to be stored in the archive */
	final List<SmallFile> smallFiles = new ArrayList<SmallFile>();

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
		try {
			addFromURL(file.toURI().toURL());
		} catch (MalformedURLException e) {
			// this is higly unlikely to happen, as the file must come from somewhere
			e.printStackTrace();
		}
	}
	
	/**
	 * add a file given its filename
	 */
	void addFile(String filename) {
		addFile(new File(filename));
	}
	
	/**
	 * add a short bit of information that will be added to the archive under the given name.
	 * @param filename the filename in the zipfile
	 * @param content the data to be added
	 */
	void addSmallFile(String filename, CharSequence content) {
		smallFiles.add(new SmallFile(filename, content));
	}

	/**
	 * add data that comes from a URL
	 */
	public void addFromURL(URL url) {
		urls.add(url);
	}

	/**
	 * clear the archive of previous entries
	 */
	void clear() {
		urls.clear();
		smallFiles.clear();
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
		if (!urls.isEmpty() || !smallFiles.isEmpty()) { 
			ZipOutputStream zipStream = new ZipOutputStream(outStream);
			// handle small files
			for (SmallFile sf : smallFiles) {
				String fileWOPath = new File(sf.filename).getName();
				ZipEntry ze = new ZipEntry(fileWOPath);
				zipStream.putNextEntry(ze);
				zipStream.write(sf.content.toString().getBytes());
				zipStream.closeEntry();
			}
			// handle stuff that comes from URLs
			for (URL url : urls) {
				InputStream is = url.openConnection().getInputStream();
				ZipEntry ze = new ZipEntry(url.getFile());
				zipStream.putNextEntry(ze);
				pipe(is, zipStream);
				is.close();
				zipStream.closeEntry();
			}
			zipStream.close();
		}
	}
	
	/**
	 * post the archive's content (packed as a zip file) to a server.
	 * 
	 * in Swing, this should be called from a SwingWorker thread
	 * and some progress status should be displayed, as posting
	 * large archives can take quite a while. 
	 * 
	 * @param url the server's URL
	 * @throws IOException 
	 */
	InputStream postToServer(URL url) throws IOException {
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
	 * keeps data bits that are not worth to write into files.
	 * a pair of filename (in archive) and bit of data
	 * to be put into the archive under this filename.
	 */
	public class SmallFile {
		private final String filename;
		private final CharSequence content;
		
		public SmallFile(String filename, CharSequence content) {
			this.filename = filename;
			this.content = content;
		}
	}
	
	/**
	 * for testing only.
	 * @param args arguments are ignored
	 * @throws IOException when something goes wrong 
	 */
	public static void main(String[] args) throws IOException {
		final SessionData a = new SessionData();
		a.addSmallFile("hallo", "lalalalalaa");
		a.addSmallFile("halla", "lololololoo");
//		a.saveToFile("/tmp/testfile.zip");
		InputStream is = a.postToServer("http://www.sfb632.uni-potsdam.de/cgi-timo/upload.pl");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = reader.readLine()) != null) {
			System.err.println(line);
		}
	}

}