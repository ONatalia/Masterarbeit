package inpro.apps;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import javax.sound.sampled.AudioInputStream;

import com.sun.istack.internal.logging.Logger;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.language.grammar.AlignerGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import inpro.apps.util.RecognizerInputStream;
import inpro.incremental.sink.LabelWriter;
import inpro.incremental.source.SphinxASR;

public class SphinxThread extends Thread {
	
	private Recognizer recognizer;
	private boolean running = true;
	private static final Logger logger = Logger.getLogger(SphinxThread.class);
	private PriorityBlockingQueue<BlockingQueueData> bq;
	private ConfigurationManager cm;
	private RecognizerInputStream rais;
	private double offset=0;
	private long start=0;
	//alignemnt+recognition with jsgf grammar
	private boolean alRecJsgf=false;
	//alignment+recognition with language model
	private boolean alRecLM=false;
	


	public SphinxThread(Recognizer recognizer,PriorityBlockingQueue<BlockingQueueData> bq, ConfigurationManager cm,RecognizerInputStream rais) {
	
		this.recognizer=recognizer;
		this.bq=bq;
		this.cm=cm;
		this.rais=rais;
		
	}
	
	public void run (){
	
	
		
	while (running)	{
		
		
		BlockingQueueData data=null;
		
		
		logger.info ("queue status:"+bq.isEmpty());
		
		
		
		
		try {
			data=bq.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("take data:+"+data.getText());
		
		//set Text;
		setText(data.getText());
		
		
		
		//updateInputStream
		updateInputStream(data.getStarttimes(),data.getEndtimes());
		
		
		
		Result result = null;
		
		
			do {
				
				
				
				if (start==0){
					start=System.currentTimeMillis();
				}
				result = recognizer.recognize();
				 
			
				
				
				if (result != null) {
					// Normal Output
					String phones=result.getBestPronunciationResult();
					result.toString();
					
					
					
				
					
					
				} else {
					logger.info("result null");
					
				}
				
			} while ((result != null) && (result.getDataFrames() != null)
					&& (result.getDataFrames().size() > 4));;
			
			recognizer.resetMonitors();	
			
			logger.info("reset monitors");
			long duration=System.currentTimeMillis()-start;
			logger.info("duration"+duration);
	}	
			
			
		
	}
	
	private void updateInputStream(ArrayList<Double> starttimes, ArrayList<Double> endtimes) {
		
		int endInBytes = 0;
		
		offset=0.400;
		
	   
		double endInSec=0;
		
		
			
		
		endInSec=endtimes.get(endtimes.size()-1)-offset;
	
		logger.info("stimes:first"+starttimes.get(0));
		logger.info("etimes:last "+endtimes.get(endtimes.size()-1));
		
		logger.info("endInSec"+endInSec);
		
		logger.info("sound data length"+rais.getSoundData().length);
			
			
		endInBytes=(int)((rais.getChannels()*rais.getSiteInBits()* rais.getSampleRate()/8.0f)*(endInSec));
		logger.info("endInBytes"+endInBytes);
			
		
		StreamDataSource sdsFA = (StreamDataSource) cm.lookup("streamDataSourceFA");
		sdsFA.initialize();
		
		if (endInBytes>rais.getSoundData().length-1){
			endInBytes=rais.getSoundData().length-1;
		}
			
		byte [] buffer =Arrays.copyOfRange(rais.getSoundData(), 0, endInBytes);
		
				
		AudioInputStream aisS = new AudioInputStream(new ByteArrayInputStream(buffer),rais.getFormat(), buffer.length);;
				
		sdsFA.setInputStream(aisS, "sphinxFA");
		
		
				
		
	}

	private void setText(String text) {
		
		if (alRecJsgf==true) {
			MyJSGFGrammar jsgf= (MyJSGFGrammar) cm.lookup("myjsgfGrammar");
			jsgf.setText(text);
		}else if (alRecLM==true){
			MyLMGrammar lm=(MyLMGrammar) cm.lookup("ngramGrammar");
			lm.setText(text);
		}else {
			AlignerGrammar forcedAligner = (AlignerGrammar) cm.lookup("forcedAligner");
			//MyAlignerGrammar forcedAligner = (MyAlignerGrammar) cm.lookup("forcedAligner");
		    forcedAligner.setText(text);
		}
		
		
		
		
		
		
	}

	public void terminate (){
		running=false;
	}

}