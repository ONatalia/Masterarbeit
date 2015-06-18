package demo.inpro.system.refres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sium.nlu.context.Context;
import sium.nlu.language.LingEvidence;
import sium.system.util.PentoSqlUtils;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;
import inpro.incremental.PushBuffer;
import inpro.incremental.processor.ResolutionModuleNgram;
import inpro.incremental.sink.FrameAwarePushBuffer;
import inpro.incremental.source.SphinxASR;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;

public class PentoNgramDemo {
	
	/*
	 * Make sure the sium.module.PredicateLogicModule's right buffer is set to sium_ngram!
	 */
	
	static Logger log = Logger.getLogger(PentoNgramDemo.class.getName());
	
	@S4Component(type = SphinxASR.class)
	public final static String PROP_ASR_HYP = "currentASRHypothesis";

	@S4ComponentList(type = PushBuffer.class)
	public final static String PROP_HYP_CHANGE_LISTENERS = SphinxASR.PROP_HYP_CHANGE_LISTENERS;
	
	ConfigurationManager cm;
	int currentFrame = 0;
	List<PushBuffer> hypListeners = null;
	PropertySheet ps;
	List<EditMessage<IU>> edits = new ArrayList<EditMessage<IU>>();
	private PentoSqlUtils pento;

	private int max = 1500;
	private int numFolds = 3;
	private int foldSize = 150;
	
	public PentoNgramDemo() {
		pento = new PentoSqlUtils();
		cm = new ConfigurationManager("src/demo/inpro/system/refres/config/config.xml");

		ps = cm.getPropertySheet(PROP_ASR_HYP);
		hypListeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
	}
	

	private void run() throws SQLException {
		
		pento.createConnection();
		
		ArrayList<String> episodes = pento.getAllEpisodes();
		
		double correct = 0.0;
		double total = 0.0;

		for (int i=1; i<=numFolds; i++) {
			int j = 1;
			
			cm = new ConfigurationManager("src/demo/inpro/system/refres//config/config.xml");
			ps = cm.getPropertySheet(PROP_ASR_HYP);
			hypListeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
			ResolutionModuleNgram resolution =  (ResolutionModuleNgram) cm.lookup("sium_ngram");
			
			System.out.println("Processing fold " + i + " out of " + numFolds);
			resolution.toggleTrainMode();
			
//			setup the training data
			for (String episode : episodes) {
				if (exceedsMax(j)) break;
			
				if (isTrainData(j, i)) {
//					System.out.println(episode);
					Context<String,String> context = pento.getContext(episode);
					resolution.setContext(context);
					ArrayList<LingEvidence> ling = pento.getLingEvidence(episode);
					String gold = pento.getGoldPiece(episode);
					resolution.setReferent(gold);
					processLine(ling);
				}
				j++;
			}
			
			resolution.train();
			resolution.toggleNormalMode();
			
			
//			now, run evaluation on this fold
			j = 0;
			for (String episode : episodes) {
				if (exceedsMax(j)) break;
			
				if (!isTrainData(j, i)) {
					
					Context<String,String> context = pento.getContext(episode);
					resolution.setContext(context);
					ArrayList<LingEvidence> ling = pento.getLingEvidence(episode);
					String gold = pento.getGoldPiece(episode);
					resolution.setReferent(gold);
					processLine(ling);
					if (!resolution.getGrounder().getPosterior().isEmpty()) {
						String guess = resolution.getGrounder().getPosterior().getArgMax().getEntity();
						if (gold.equals(guess)) 
							correct++;
					}
					total++;
				}
				j++;
			}
			
			resolution.clear();
		}
		
		pento.closeConnection();

		System.out.println("Accuracy: " + (correct / total) + " " + correct + "/" + total);
	}
	
	private void processLine(ArrayList<LingEvidence> ling) {
		
		ArrayList<WordIU> ius = new ArrayList<WordIU>();
		
		WordIU prev = WordIU.FIRST_WORD_IU;
		
		for (LingEvidence ev : ling) {
			String word = ev.getValue("w1");
			if (word.equals("<s>")) continue;
			WordIU wiu = new WordIU(word, prev, null);
			ius.add(wiu);
			edits.add(new EditMessage<IU>(EditType.ADD, wiu));
			notifyListeners(hypListeners);
			prev = wiu;
		}
		
		for (WordIU iu : ius) {
			edits.add(new EditMessage<IU>(EditType.COMMIT, iu));
		}
		notifyListeners(hypListeners);
		
	}

	private boolean isTrainData(int j, int i) {
		return (j < (i-1) * foldSize) || (j > i * foldSize);
	}


	private boolean exceedsMax(int j) {
		return j > max;
	}

	public void notifyListeners(List<PushBuffer> listeners) {
		if (edits != null && !edits.isEmpty()) {
			//logger.debug("notifying about" + edits);
			currentFrame += 100;
			for (PushBuffer listener : listeners) {
				if (listener instanceof FrameAwarePushBuffer) {
					((FrameAwarePushBuffer) listener).setCurrentFrame(currentFrame);
				}
				// notify
				listener.hypChange(null, edits);
				
			}
			edits = new ArrayList<EditMessage<IU>>();
		}
	}
	
	
	public static void main(String[] args) {
		try {
			new PentoNgramDemo().run();
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
