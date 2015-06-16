package inpro.io.webspeech.servlets;

import inpro.io.webspeech.WebSpeech;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * @author casey
 *
 */
@WebServlet("/DialogAsrResult")
public class DialogAsrResult extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(DialogAsrResult.class.getName());
	// TODO: the following is NOT good style.
	public static double previousTimestamp = -1; 
	private Thread prevResetThread;
       
	
	protected WebSpeech webSpeech;
    /**
     * @param webSpeech 
     * @see HttpServlet#HttpServlet()
     */
    public DialogAsrResult(WebSpeech webSpeech) {
        super();
        log.info("initialising");
        this.webSpeech = webSpeech;
        this.updateResetThread();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	/*
		if (prevResetThread != null) {
			prevResetThread.interrupt();
		}
//		get received JSON data from request
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";
        json = br.readLine();
        
//      convert the JSON object into Java objects
        JSONParser parser = new JSONParser();
        ContainerFactory containerFactory = new ContainerFactory(){
        @SuppressWarnings("rawtypes")
		public List<?> creatArrayContainer() {
            return new LinkedList();
          }
          public Map<Object, Object> createObjectContainer() {
            return new LinkedHashMap<Object, Object>();
          }
        };
                      
        try{
          Map<?, ?> jsonMap = (Map<?, ?>)parser.parse(json, containerFactory);
          Iterator<?> iter = jsonMap.entrySet().iterator();
          ArrayList<AsrHyp> asrHyps = new ArrayList<AsrHyp>();
          double timestamp = -1;
          boolean isFinal = false;
          int utteranceKey = -1;
//        step through the items in the list, keep track of things we need
          while(iter.hasNext()){
            @SuppressWarnings("rawtypes")
			Map.Entry entry = (Map.Entry)iter.next();
//            System.out.println(entry.getKey() + " " + entry.getValue());
//          save the timestamp 
            if (entry.getKey().equals("timeStamp")) {
            	timestamp = Double.parseDouble(entry.getValue().toString());
            }
//			utterance key to check ordering            
            if (entry.getKey().equals("utteranace_key")) {
            	utteranceKey = Integer.parseInt(entry.getValue().toString());
            }
//          the results has the isFinal information
            if (entry.getKey().equals("results")) {
            	int ind = entry.getValue().toString().indexOf("isFinal=") + 8;
            	String f = entry.getValue().toString().substring(ind, ind+1);
            	if ("t".equals(f)) isFinal = true;
            }
//			Now, the hyplist, which is the list of             
            if (entry.getKey().equals("hypList")) {
            	String value = entry.getValue().toString();
            	System.out.println(value);
            	asrHyps = getHyps(value);
            }
            	
          } // end of while
          
//        set some of the shared information for each utterance hyp
          if (DialogAsrResult.previousTimestamp == -1) 
        	  DialogAsrResult.previousTimestamp = timestamp;
          
          for (AsrHyp hyp : asrHyps) {
        	  log.debug("new hyp: " + hyp);
        	  hyp.setUtteranceKey(utteranceKey);
        	  hyp.setTimestamp(timestamp/1000.0 - TimeUtil.startupTime/1000.0);
        	  hyp.setPreviousTimestamp(Math.abs(DialogAsrResult.previousTimestamp/1000.0 - TimeUtil.startupTime/1000.0));
        	  hyp.setFinal(isFinal);
          }
          
          updateResetThread();
          
          DialogAsrResult.previousTimestamp = timestamp;
//          if (isFinal) webSpeech.setStartTime(); // reset start time if endpointing detected the end of an utterance
//        this is where you would send the results along to something else, in this case the WebSpeech that makes IUs out of them
          webSpeech.setNewHyps(asrHyps);
        
        }
        catch(ParseException pe){
          pe.printStackTrace();
        }
        */
	}

	private void updateResetThread() {
		
		prevResetThread = new Thread() {
			public void run() {
				try{
					while (!Thread.currentThread().isInterrupted()) {
						Thread.sleep(3000);
						previousTimestamp = System.currentTimeMillis();	
//						webSpeech.setStartTime();
					}
				}
				catch (InterruptedException e) {
					// ...
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
        };
        prevResetThread.start();		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
