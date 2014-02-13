package inpro.io.webspeech;

import inpro.incremental.IUModule;
import inpro.incremental.PushBuffer;
import inpro.incremental.deltifier.ASRWordDeltifier;
import inpro.incremental.source.CurrentASRHypothesis;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IUList;
import inpro.incremental.unit.TextualWordIU;
import inpro.incremental.unit.WordIU;
import inpro.io.webspeech.model.AsrHyp;
import inpro.io.webspeech.servlets.Dialog;
import inpro.io.webspeech.servlets.DialogAsrResult;
import inpro.io.webspeech.servlets.DialogEnd;
import inpro.io.webspeech.servlets.DialogRecording;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class WebSpeech extends IUModule {
	
	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = "hypChangeListeners";
	
	@S4Component(type = ASRWordDeltifier.class, defaultClass = ASRWordDeltifier.class)
	public final static String PROP_ASR_DELTIFIER = "asrFilter"; // NO_UCD (use private)
	protected ASRWordDeltifier asrDeltifier;
	
	@S4String(defaultValue = "en-UK")
	public final static String LANG = "lang";
	
	@S4Integer(defaultValue = 25)
	public final static String MAX_ALTERNATIVES = "maxAlternatives";
	
	@S4Boolean(defaultValue = true)
	public final static String CONTINUOUS = "continuous";
	
	@S4Boolean(defaultValue = true)
	public final static String INTERIM_RESULTS = "interimResults";
	
	IUList<WordIU> prevList;
	
  public void run(PropertySheet ps) throws LifecycleException {
	  	Tomcat tomcat = new Tomcat();
	    tomcat.setPort(8080);

	    String domainPath = WebSpeech.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "inpro/io/webspeech/";
	    tomcat.addWebapp(tomcat.getHost(), "", new File(domainPath).getAbsolutePath()); 
	    
	    Context ctx = tomcat.addContext("/", new File(".").getAbsolutePath());

	    Tomcat.addServlet(ctx, "dialog", new Dialog(ps));
	    ctx.addServletMapping("/dialog", "dialog");
	    
	    Tomcat.addServlet(ctx, "dialogASRResult", new DialogAsrResult(this));
	    ctx.addServletMapping("/dialog/asr_result", "dialogASRResult");
	    
	    Tomcat.addServlet(ctx, "dialogRecording", new DialogRecording());
	    ctx.addServletMapping("/dialog/recording", "dialogRecording");
	    
	    Tomcat.addServlet(ctx, "dialogEnd", new DialogEnd());
	    ctx.addServletMapping("/dialog/end", "dialogEnd");
	    
	    tomcat.start();
	    
	    System.out.println("Using Google Web Speech: open Chrome/Chromium at localhost:8080/dialog");
//	    tomcat.getServer().await();
  }

	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		prevList = new IUList<WordIU>();
		asrDeltifier = (ASRWordDeltifier) ps.getComponent(PROP_ASR_DELTIFIER);
		if (asrDeltifier == null) {
			asrDeltifier = new ASRWordDeltifier();
		}
		
		try {
			this.run(ps);
		} 
		catch (LifecycleException e) {
			e.printStackTrace();
		}
		
	}
	
	public void setRightBuffer(ArrayList<AsrHyp> asrHyps) {
		System.out.println(asrHyps);
		
//		At the moment, we are just working with the argmax, we don't have IU network stuff ready to handle nbest lattices
		AsrHyp argMax = asrHyps.get(0);
	
		IUList<WordIU> list = new IUList<WordIU>();
		
//		This splits the words and makes the IU list
		List<WordIU> ius = new LinkedList<WordIU>();
		WordIU prev = TextualWordIU.FIRST_ATOMIC_WORD_IU;
		for (String word : argMax.getWords()) {
			WordIU wiu = new WordIU(word, prev, null);
			prev = wiu;
			ius.add(wiu);
			list.add(wiu);
		}
		
//		This calculates the differences between the current IU list and the previous, based on payload
		List<EditMessage<WordIU>> diffs = prevList.diffByPayload(list);
		prevList = list;
		
//		The diffs represents what edits it takes to get from prevList to list, send that to the right buffer
		rightBuffer.setBuffer(diffs);
		System.out.println(diffs);
		super.notifyListeners();
	}

	@Override
	protected void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		
	}
}
