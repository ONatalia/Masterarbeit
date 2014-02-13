package inpro.io.webspeech;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
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
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class WebSpeech extends IUModule {
	
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
	    
	    Tomcat.addServlet(ctx, "dialogASRResult", new DialogAsrResult());
	    ctx.addServletMapping("/dialog/asr_result", "dialogASRResult");
	    
	    Tomcat.addServlet(ctx, "dialogRecording", new DialogRecording());
	    ctx.addServletMapping("/dialog/recording", "dialogRecording");
	    
	    Tomcat.addServlet(ctx, "dialogEnd", new DialogEnd());
	    ctx.addServletMapping("/dialog/end", "dialogEnd");
	    
	    tomcat.start();
	    tomcat.getServer().await();
  }

	public void newProperties(PropertySheet ps) throws PropertyException {
		try {
			this.run(ps);
		} 
		catch (LifecycleException e) {
			e.printStackTrace();
		}
		
	}

  @Override
  protected void leftBufferUpdate(Collection<? extends IU> ius,
		List<? extends EditMessage<? extends IU>> edits) {
	
	
  }

}
