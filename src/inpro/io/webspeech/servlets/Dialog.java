package inpro.io.webspeech.servlets;

import inpro.io.webspeech.WebSpeech;

import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * Servlet implementation class Dialog
 */
@WebServlet("/Dialog")
public class Dialog extends HttpServlet {
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
        setLang(ps.getString(WebSpeech.LANG));
        setMaxAlternatives(ps.getInt(WebSpeech.MAX_ALTERNATIVES));
        setInterimResults(ps.getBoolean(WebSpeech.INTERIM_RESULTS));
        setContinuous(ps.getBoolean(WebSpeech.CONTINUOUS));
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//this is used to set the initial session variables
		HttpSession session = request.getSession();
		session.setAttribute("timeout", 86400*30); // thirty days
		session.setAttribute("secret_key", "inprotk");
		//session.setAttribute("utterance_key", 0);
		session.setAttribute("csrf_token", UUID.randomUUID().toString());
		//then redirect to index.html via the web.xml mappings (default goes to index.html)

		session.setAttribute("lang", this.getLang());
//		ja, de-de, en_uk
//		http://www.science.co.il/language/locale-codes.asp
		session.setAttribute("maxAlternatives", this.getMaxAlternatives());
		session.setAttribute("interimResults", this.isInterimResults());
		session.setAttribute("continuous", this.isContinuous());
		
		request.getRequestDispatcher("dialog.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public int getMaxAlternatives() {
		return maxAlternatives;
	}

	public void setMaxAlternatives(int maxAlternatives) {
		this.maxAlternatives = maxAlternatives;
	}

	public boolean isInterimResults() {
		return interimResults;
	}

	public void setInterimResults(boolean interimResults) {
		this.interimResults = interimResults;
	}

	public boolean isContinuous() {
		return continuous;
	}

	public void setContinuous(boolean continuous) {
		this.continuous = continuous;
	}
}
