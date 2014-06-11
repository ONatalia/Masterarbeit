package inpro.io.webspeech.servlets;

import inpro.io.webspeech.WebSpeech;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * @author casey
 */
@WebServlet("/Dialog")
public class Dialog extends HttpServlet {
	
	static Logger log = Logger.getLogger(Dialog.class.getName());
	
	private static final long serialVersionUID = 1L;
       
	private String lang;
	private int maxAlternatives;
	private boolean interimResults;
	private boolean continuous;
	
    /**
     * @param ps 
     * @see HttpServlet#HttpServlet()
     */
    public Dialog(PropertySheet ps) {
        super();
        log.info("Starting Dialog class under Tomcat using the following session vars");
        setLang(ps.getString(WebSpeech.LANG));
        setMaxAlternatives(ps.getInt(WebSpeech.MAX_ALTERNATIVES));
        setInterimResults(ps.getBoolean(WebSpeech.INTERIM_RESULTS));
        setContinuous(ps.getBoolean(WebSpeech.CONTINUOUS));
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//this is used to set the initial session variables, some are read from the config file
		HttpSession session = request.getSession();
		session.setAttribute("timeout", 86400*30); // thirty days
		session.setAttribute("secret_key", "inprotk"); // this probably isn't really necessary
		session.setAttribute("csrf_token", UUID.randomUUID().toString()); // unique token for the session
		log.debug("setting csrf_token: " + session.getAttribute("csrf_token"));
		session.setAttribute("lang", this.getLang());
		session.setAttribute("maxAlternatives", this.getMaxAlternatives());
		session.setAttribute("interimResults", this.isInterimResults());
		session.setAttribute("continuous", this.isContinuous());
		
//		forward now to the main jsp file that houses all of the javascript code
		request.getRequestDispatcher("dialog.jsp").forward(request, response);
	}

	/**
	 * Allow POST and GET requests, process in the same way. 
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * @return String representing the language ID
	 */
	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		log.info("language: " + lang);
		this.lang = lang;
	}

	/**
	 * @return　number of maximum alternatives (nbest)
	 */
	public int getMaxAlternatives() {
		return maxAlternatives;
	}

	public void setMaxAlternatives(int maxAlternatives) {
		log.info("max alernatives: " + maxAlternatives);
		this.maxAlternatives = maxAlternatives;
	}

	/**
	 * @return boolean of interim results (incremental = true)
	 */
	public boolean isInterimResults() {
		return interimResults;
	}

	public void setInterimResults(boolean interimResults) {
		log.info("interim results: " + interimResults);
		this.interimResults = interimResults;
	}

	/**
	 * @return boolean of continuous recognition (don't stop after endpointing = true)
	 */
	public boolean isContinuous() {
		return continuous;
	}

	public void setContinuous(boolean continuous) {
		log.info("is continuous: " + continuous);
		this.continuous = continuous;
	}
}
