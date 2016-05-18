package inpro.incremental.processor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.IOUtils;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.language.grammar.AlignerGrammar;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.emory.mathcs.backport.java.util.TreeSet;
import inpro.apps.util.GoogleSphinxRCLP;
import inpro.audio.AudioUtils;
import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordSpotterIU;

public class FAModule  extends IUModule  {
	
	
	private ConfigurationManager cm;
	private final GoogleSphinxRCLP clp;
	private byte [] soundData=null;
	private Linguist linguist;
	
	
	
	public FAModule(ConfigurationManager cm,GoogleSphinxRCLP clp, Linguist linguist) {
		// TODO Auto-generated constructor stub
		this.cm=cm;
		this.clp=clp;
		this.linguist=linguist;
		
		
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		// TODO Auto-generated method stub
		System.err.println("This is left buffer update");
		//clp.setReferenceText(new String ());
		Map <Integer,String> hyps=new TreeMap <Integer,String>();
		String text=new String ();
		
		//linguist.stopRecognition();
		
		
		if (!edits.isEmpty()) {
			
			
			
			/*logger.info("\nThe Hypothesis has changed at time: ");
			
			logger.info("Edits since last hypothesis:");
			for (EditMessage<? extends IU> edit : edits) {
				System.out.println(edit.toString());
			}*/
			logger.info("Current hypothesis is now:");
			for (IU iu : ius) {
				logger.info("IU:"+iu.toString());
				
				//setText(iu);
				
				hyps.put(Integer.parseInt(iu.toString().split("\\s+")[0].replace("IU:", "").replace(",", "")), iu.toString().split("\\s+")[3]+" ");
				
					
						
				//updateInputStream(0, 60000);
				
				/*ArrayList<EditMessage<IU>> newIUs = new ArrayList<EditMessage<IU>>();
				newIUs.add(new EditMessage<IU> (null, iu));
				this.rightBuffer.setBuffer(newIUs);*/
				
			} 
		} else {
			logger.info("empty"); 
		} 
		
		try {
			//updateInputStream(0, 3000);
			for (Entry<Integer, String> entry : hyps.entrySet()) {
				  text+=entry.getValue();
				}
			
			logger.info ("Set text:"+text);
			
			setText(text);
			//linguist.startRecognition();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//updateInputStream(0, 3000);
		//linguist.startRecognition();
	}
	
	public String []iUToStringArray (IU iu){
		return iu.toString().split("\\s+");
	}
	
	
	public int getEnd(IU iu){
		
		return (int)(Float.valueOf(iUToStringArray(iu) [2])*1000);
	}
	
	public int getStart(IU iu){
		return (int)(Float.valueOf(iUToStringArray(iu) [1])*1000);
	}
	
	public void setText (String text) throws IOException{
		
		
		
		
		
	
		AlignerGrammar forcedAligner = (AlignerGrammar) cm
				.lookup("forcedAligner");
		
		//linguist.getSearchGraph().getInitialState();
		//logger.info ("Initial state"+ linguist.getSearchGraph().getInitialState().toString());
		
		clp.setReferenceText(text);
		forcedAligner.setText(clp.getReference());
		forcedAligner.getInitialNode().dump();
		
		//logger.info ("Text:"+clp.getReference());
		
		
	}
	
	public void updateInputStream (float startInMillies, float endInMillies){
		//StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSourceGoogle");
		StreamDataSource sds1 = (StreamDataSource) cm.lookup("streamDataSource");
		//sds.initialize();
		sds1.initialize();
		URL audioURL = clp.getAudioURL();
		AudioInputStream ais = null;
		int startInBytes = 0;
		int endInBytes = 0;
		try {
			ais = AudioUtils.getAudioStreamForURL(audioURL);
			AudioFormat format=ais.getFormat();
			
			//int buffersize = (int) (format.getFrameSize()*format.getFrameRate()/2.0f);
			//int buffersize = (int) (format.getFrameSize()*format.getFrameRate());
			int buffersize = (int) (ais.getFrameLength()*format.getFrameSize());
			soundData = new byte [buffersize];
			
			soundData=IOUtils.toByteArray(ais);		
			
			//startInBytes=(int) (((format.getChannels()*format.getSampleSizeInBits()*format.getSampleRate())/8.0f)*((startInMillies)/1000));			
			//endInBytes=(int) (((format.getChannels()*format.getSampleSizeInBits()*format.getSampleRate())/8.0f)*((endInMillies)/1000));
			//startInBytes=(int) ((int) ((startInMillies)/1000))/(((format.getChannels()*format.getSampleSizeInBits()*format.getSampleRate())*8.0f));
			float startInSec=startInMillies/1000.0f;
			float endInSec=endInMillies/1000.0f;
			startInBytes=(int) (startInSec/(format.getSampleRate()*format.getChannels()*format.getSampleSizeInBits()*8.0f));
			
			endInBytes=(int) (endInSec/(format.getSampleRate()*format.getChannels()*format.getSampleSizeInBits()*8.0f));
			
			
			
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		InputStream is1 = new ByteArrayInputStream(Arrays.copyOfRange(soundData, startInBytes, endInBytes));
		//InputStream is1 = new ByteArrayInputStream(Arrays.copyOfRange(soundData,startInBytes, soundData.length-1));
		//sds.setInputStream(is1, audioURL.getFile());
		sds1.setInputStream(is1, audioURL.getFile());
		

	}
	
	
	

	public ConfigurationManager getCm() {
		return cm;
	}

	

}
