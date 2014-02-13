package inpro.io.webspeech;

import inpro.incremental.IUModule;
import inpro.incremental.PushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
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

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
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
	
	@S4String(defaultValue = "en-UK")
	public final static String LANG = "lang";
	
	@S4String(defaultValue = "")
	public final static String SECRET_KEY = "secretKey";
	
	@S4Integer(defaultValue = 25)
	public final static String MAX_ALTERNATIVES = "maxAlternatives";
	
	@S4Boolean(defaultValue = true)
	public final static String CONTINUOUS = "continuous";
	
	@S4Boolean(defaultValue = true)
	public final static String INTERIM_RESULTS = "interimResults";
	
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
//	    tomcat.getServer().await();
  }

	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		try {
			this.run(ps);
		} 
		catch (LifecycleException e) {
			e.printStackTrace();
		}
		
	}
	
	public void setRightBuffer(ArrayList<AsrHyp> asrHyps) {
		System.out.println(asrHyps);
		
		 // we dont want a full hyp, rather a diff of the previous
        // what about partial words? if I say "testing" it sends "test" then "testing" but the diff would just be ing, 
        // what really needs to happen is test is revoked and replaced. 
        // also, I can't tell the order, if something has been bumped up or down....
		// all lower case?
		
//		At the moment, we are just working with the argmax, we don't have IU network stuff ready to handle nbest lattices
		AsrHyp argMax = asrHyps.get(0);
		List<WordIU> ius = new LinkedList<WordIU>();
		List<EditMessage<WordIU>> edits = new LinkedList<EditMessage<WordIU>>();
		WordIU prev = TextualWordIU.FIRST_ATOMIC_WORD_IU;
		for (String word : argMax.getWords()) {
			WordIU wiu = new WordIU(word, prev, null);
			prev = wiu;
			ius.add(wiu);
			edits.add(new EditMessage<WordIU>(EditType.ADD, wiu));
		}
		
		rightBuffer.setBuffer(ius, edits);
		super.notifyListeners();
	}

	
  @Override
  protected void leftBufferUpdate(Collection<? extends IU> ius,
		List<? extends EditMessage<? extends IU>> edits) {
	
	
  }

}
