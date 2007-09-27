package gov.nist.sphere.jaudio;

import gov.nist.sphere.SphereException;

import javax.sound.sampled.*;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.*;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements the AudioFileReader class and provides an
 * SPHERE file reader for use with the Java Sound Service Provider Interface.
 * <br>
 *
 * As of now, this is a very simple implementation that only supports PCM-
 * and ULAW-encoded, uncompressed files with a maximum of 2 channels and
 * a 1024 bytes header.
 *
 * @author Christophe Laprun, NIST [chris.laprun@nist.gov]
 */
public class SphereFileReader extends AudioFileReader {

  private SphereHeader header;

  /** Return the AudioFileFormat from the given file. */
  public AudioFileFormat getAudioFileFormat(File file)
          throws UnsupportedAudioFileException, IOException {
    InputStream inputStream = new FileInputStream(file);
    try {
      return getAudioFileFormat(inputStream);
    } finally {
      inputStream.close();
    }
  }

  /** Return the AudioFileFormat from the given URL. */
  public AudioFileFormat getAudioFileFormat(URL url)
          throws UnsupportedAudioFileException, IOException {
    InputStream inputStream = url.openStream();
    try {
      return getAudioFileFormat(inputStream);
    } finally {
      inputStream.close();
    }
  }

  /** Return the AudioFileFormat from the given InputStream. */
  public AudioFileFormat getAudioFileFormat(InputStream inputStream)
          throws UnsupportedAudioFileException, IOException {
    return getAudioFileFormat(inputStream, null);
  }


  /** Return the AudioFileFormat from the given InputStream. Implementation. */
  protected AudioFileFormat getAudioFileFormat(InputStream inputStream, byte[] bytes)
          throws UnsupportedAudioFileException, IOException {
    InputStreamReader isr = new InputStreamReader(inputStream, "US-ASCII");
    LineNumberReader lnr = new LineNumberReader(isr);

    String line = lnr.readLine();
    String line2 = lnr.readLine();
    StringBuffer sb = new StringBuffer(Integer.parseInt(line2.trim()));
    this.addToByteHeader(sb, line);
    this.addToByteHeader(sb, line2);
    int size = this.checkHeaderStartAndGetSize(line, line2);

    this.header = new SphereHeader();

    line = lnr.readLine();
    this.addToByteHeader(sb, line);
    Matcher matcher = null;
    Pattern pattern = Pattern.compile("(.*)\\s-(\\w\\d*)\\s(.*)"); // Timo (20070919): fixed to also match string data
    while (line != null && !line.equals("end_head")) {
      matcher = pattern.matcher(line);
      if (matcher.matches()) {
        this.header.add(matcher.group(2), matcher.group(1), matcher.group(3));
      }
      line = lnr.readLine();
      this.addToByteHeader(sb, line);
    }
    bytes = new byte[size];
    int length = sb.length();
    System.arraycopy(sb.toString().getBytes("US-ASCII"), 0, bytes, 0, length);
    if (length < size)
      java.util.Arrays.fill(bytes, length, size, (byte) 0);
    //System.out.println(bytes.length + "\n" + new String(bytes, "US-ASCII"));

    AudioFormat format = null;
    if (header.isValid()) {
      try {
        String encodingType = header.getSampleCoding();
        AudioFormat.Encoding encoding = getEncoding(encodingType);
        int sampleRate = header.getSampleRate();
        int channelCount = header.getChannelCount();
        int resolution = header.getSampleNBytes() * 8;
        boolean isBigEndian = header.isDataBigEndian();
        format = new AudioFormat(encoding, (float) sampleRate, resolution,
                channelCount, channelCount * header.getSampleNBytes(), sampleRate, isBigEndian);
      } catch (SphereException e) {
        throw new UnsupportedAudioFileException(e.getMessage());
      }
    } else {
      throw new UnsupportedAudioFileException("incorrect header format");
    }

    AudioFileFormat.Type type = SphereFileFormatType.SPHERE;
    return new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
  } // getAudioFileFormat( InputStream, byte [] )


  /** Return the AudioInputStream from the given InputStream.
   * The stream is uninterpreted and is ready to read at byte 0.
   */
  public AudioInputStream getAudioInputStream(InputStream inputStream)
          throws UnsupportedAudioFileException, IOException {
    // Save byte header since this method must return the stream opened at byte 0.
    byte[] bytes = null;
    AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, bytes);
//			SequenceInputStream sequenceInputStream =
//			new SequenceInputStream( new ByteArrayInputStream( bytes ), inputStream );
    return new AudioInputStream(inputStream,
            audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
  }

  /** Return the AudioInputStream from the given File. */
  public AudioInputStream getAudioInputStream(File file)
          throws UnsupportedAudioFileException, IOException {
    InputStream inputStream = new FileInputStream(file);
    try {
      return getAudioInputStream(inputStream);
    } catch (UnsupportedAudioFileException e) {
      inputStream.close();
      throw e;
    } catch (IOException e) {
      inputStream.close();
      throw e;
    }
  }

  /** Return the AudioInputStream from the given URL. */
  public AudioInputStream getAudioInputStream(URL url)
          throws UnsupportedAudioFileException, IOException {
    InputStream inputStream = url.openStream();
    try {
      return getAudioInputStream(inputStream);
    } catch (UnsupportedAudioFileException e) {
      inputStream.close();
      throw e;
    } catch (IOException e) {
      inputStream.close();
      throw e;
    }
  }

  public final SphereHeader getSphereHeader() {
    return header;
  }

  private AudioFormat.Encoding getEncoding(String type) throws SphereException {
    if (type.equals(SphereHeader.SAMPLE_CODING_PCM))
      return AudioFormat.Encoding.PCM_SIGNED;
    if (type.equals(SphereHeader.SAMPLE_CODING_ULAW))
      return AudioFormat.Encoding.ULAW;
    throw new SphereException("unsupported encoding");
  }

  private int checkHeaderStartAndGetSize(String line1, String line2)
          throws UnsupportedAudioFileException {
    if (!line1.equals("NIST_1A"))
      throw new UnsupportedAudioFileException("incorrect header format: 1st line should be 'NIST_1A'");
    int length;
    try {
      if (line2.length() != 7)
        throw new UnsupportedAudioFileException("incorrect header format: 2nd line should be 8 bytes long");
      length = Integer.parseInt(line2.trim());
      if (length < 0 || (length % 1024) != 0)
        throw new UnsupportedAudioFileException("incorrect header format: 2nd line should be a multiple of 1024");
    } catch (NumberFormatException e) {
      System.out.println(line1 + "\n" + line2);
      throw new UnsupportedAudioFileException("incorrect header format");
    }
    return length;
  }

  private void addToByteHeader(StringBuffer buffer, String line) {
    int start = buffer.length();
    int end = start + line.length() + 1;
    buffer.replace(start, end, line + "\n");
  }
  

  public static void main(String[] args) {
//    File file = new File("res/sample_vm.nis");
    File file = new File("res/sample_swbd.sph");
    SphereFileReader sfr = new SphereFileReader();
    try {
      System.out.println(sfr.getAudioFileFormat(file));
      sfr.audioInputStream = sfr.getAudioInputStream(file);
      sfr.audioFormat = sfr.audioInputStream.getFormat();
      DataLine.Info dli = new DataLine.Info(SourceDataLine.class, sfr.audioFormat);
      sfr.sourceDataLine = (SourceDataLine) AudioSystem.getLine(dli);
      sfr.new PlayThread().start();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println(sfr.header);
  }

  AudioInputStream audioInputStream;
  AudioFormat audioFormat;
  SourceDataLine sourceDataLine;
  
//=============================================//
//Inner class to play back the data from the
// audio file.
class PlayThread extends Thread{
  byte tempBuffer[] = new byte[10000];

  public void run(){
    try{
      sourceDataLine.open(audioFormat);
      sourceDataLine.start();

      int cnt;
      //Keep looping until the input read method
      // returns -1 for empty stream or the
      // user clicks the Stop button causing
      // stopPlayback to switch from false to
      // true.
      while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
        if(cnt > 0){
          //Write data to the internal buffer of
          // the data line where it will be
          // delivered to the speaker.
          sourceDataLine.write(tempBuffer, 0, cnt);
        }//end if
      }//end while
      //Block and wait for internal buffer of the
      // data line to empty.
      sourceDataLine.drain();
      sourceDataLine.close();

      //Prepare to playback another file
    }catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }//end catch
  }//end run
}//end inner class PlayThread
//===================================//


}