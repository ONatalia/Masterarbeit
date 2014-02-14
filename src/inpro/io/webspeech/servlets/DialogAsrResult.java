package inpro.io.webspeech.servlets;

import inpro.io.webspeech.WebSpeech;
import inpro.io.webspeech.model.AsrHyp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author casey
 *
 */
@WebServlet("/DialogAsrResult")
public class DialogAsrResult extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(DialogAsrResult.class.getName());
       
	
	protected WebSpeech webSpeech;
    /**
     * @param webSpeech 
     * @see HttpServlet#HttpServlet()
     */
    public DialogAsrResult(WebSpeech webSpeech) {
        super();
        log.info("initialising");
        this.webSpeech = webSpeech;
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
//		get received JSON data from request
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";
        if(br != null){
            json = br.readLine();
        }
        
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
            	value = value.substring(2,value.length()-2);
//            	Somewhat hacky, but we get the hyps and confidence scores from the string itself
            	String hyps = value.substring(value.indexOf("hyps=")+5);
            	hyps = hyps.substring(hyps.indexOf("[")+1,hyps.indexOf("]"));
            	String confs = value.substring(value.indexOf("confidence=")+11);
            	confs = confs.substring(confs.indexOf("[")+1,confs.indexOf("]"));
            	String[] confSplit = confs.split(", ");
            	String[] hypSplit = hyps.split(", ");
//            	step through each hyp and add a new AsrHyp object for each
            	for (int i=0; i<hypSplit.length; i++) {
            		AsrHyp hyp = new AsrHyp(hypSplit[i], Double.parseDouble(confSplit[i]));
            		asrHyps.add(hyp);
            	}
            }
            	
          } // end of while
          
//        set some of the shared information for each utterance hyp
          for (AsrHyp hyp : asrHyps) {
        	  log.debug("new hyp: " + hyp);
        	  hyp.setUtteranceKey(utteranceKey);
        	  hyp.setTimestamp(timestamp);
        	  hyp.setFinal(isFinal);
          }
          
//        this is where you would send the results along to something else, in this case the WebSpeech that makes IUs out of them
          webSpeech.setNewHyps(asrHyps);
        
        }
        catch(ParseException pe){
          pe.printStackTrace();
        }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
