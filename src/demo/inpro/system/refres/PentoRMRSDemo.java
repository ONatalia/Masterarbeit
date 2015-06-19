package demo.inpro.system.refres;

import inpro.incremental.PushBuffer;
import inpro.incremental.processor.ResolutionModuleRMRS;
import inpro.incremental.sink.FrameAwarePushBuffer;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;
import inpro.incremental.source.SphinxASR;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import sium.nlu.context.Context;
import sium.nlu.grounding.Grounder;
import sium.nlu.language.LingEvidence;
import sium.system.util.PentoSqlUtils;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4ComponentList;

public class PentoRMRSDemo {
	
	static Logger log = Logger.getLogger(PentoRMRSDemo.class.getName());
	
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
	private double rankTotal = 0.0;
	
	public PentoRMRSDemo() {
		cm = new ConfigurationManager("src/demo/inpro/system/refres/config/config.xml");
		cm.setGlobalProperty("sium", "tagger");
		ps = cm.getPropertySheet(PROP_ASR_HYP);
		hypListeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
	}
	

	private void run() throws SQLException, InterruptedException {
		
//		context for possible actions that can be taken
		Context<String,String> actions = new Context<String,String>();
		actions.addPropertyToEntity("take", "take");
		actions.addPropertyToEntity("rotate", "rotate");
		actions.addPropertyToEntity("put", "put");
		actions.addPropertyToEntity("mirror", "mirror");
		actions.addPropertyToEntity("delete", "delete");
		actions.setContextID("e"); // this will map to entities of type e
		
		double correct = 0.0;
		double total = 0.0;
		double correct2 = 0.0;
		

		for (int i=1; i<=numFolds; i++) {
			int j = 1;
			
			Thread.sleep(1000);
			
			pento = new PentoSqlUtils();
			pento.createConnection();
			ArrayList<String> episodes = pento.getAllEpisodes();
			cm = new ConfigurationManager("src/demo/inpro/system/refres/config/config.xml");
			cm.setGlobalProperty("sium", "tagger");
			ps = cm.getPropertySheet(PROP_ASR_HYP);
			hypListeners = ps.getComponentList(PROP_HYP_CHANGE_LISTENERS, PushBuffer.class);
			ResolutionModuleRMRS resolution =  (ResolutionModuleRMRS) cm.lookup("sium_rmrs");
			
			System.out.println("Processing fold " + i + " out of " + numFolds);
			resolution.toggleTrainMode();
			
//			setup the training data
			for (String episode : episodes) {
				if (exceedsMax(j)) break;
			
				if (isTrainData(j, i)) {
//					System.out.println(episode);
					
					setContext(actions, resolution, episode);
					
					ArrayList<LingEvidence> ling = pento.getLingEvidence(episode);
					setGoldSlots(resolution, episode);
					
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
					String gold = pento.getGoldPiece(episode);
					setContext(actions, resolution, episode);
					
					ArrayList<LingEvidence> ling = pento.getLingEvidence(episode);
					setGoldSlots(resolution, episode);
					processLine(ling, resolution, gold);
					
//					Now, evaluate....
//					This, of course, isn't right, it goes through each slot with an x entity, looking for the gold
					int current_rank = 15;
					HashMap<String, Grounder<String, String>> grounders = resolution.getGrounders();
					double last = -1;
					LinkedList<String> inc_results = new LinkedList<String>();
					for (String gid : grounders.keySet()) {
						
						Grounder<String, String> grounder = grounders.get(gid);
						String guess = null, guess2 = null;
						if (!grounder.getPosterior().isEmpty()) {
							guess = grounder.getPosterior().getArgMax().getEntity();
							guess2 = grounder.getPosterior().getItem(1).getEntity();
						}

						
//						else System.out.println(guess)
						if (!grounder.getPosterior().isEmpty()) {
							double r = grounder.getPosterior().findRank(gold);
							boolean gotit = gold.equals(guess);
							if (r != -1)  {
								if (last != -1) {
									inc_results.removeLast();
								}
								current_rank = (int) r;
								last = 1.0/r;
								if (gotit) inc_results.add("True");
								else inc_results.add("False");
							}
							
							
							
							if (gotit) {
								correct++;
								break;
							}
							if (gold.equals(guess2)) {
								correct2++;
							}
						}
					}
					rankTotal += 1.0/current_rank;
					total++;
				}
				j++;
			}
			
			resolution.clear();
			pento.closeConnection();
//			break;
		}

		System.out.println("Accuracy: " + (correct / total) + "(" +correct+ "/" + total +") " +  correct2);
		System.out.println("Avg. Rank: " + (rankTotal / total));
	}
	
	private void setGoldSlots(ResolutionModuleRMRS resolution, String episode) throws SQLException {
		String gold = pento.getGoldPiece(episode);
		String action = pento.getGoldAction(episode);
		resolution.setReferent("x", gold);
		resolution.setReferent("e", action);		
	}

	private void setContext(Context<String, String> actions, ResolutionModuleRMRS resolution, String episode) throws SQLException {

		resolution.setContext(actions);
		Context<String,String> context = pento.getContext(episode);
		context.addPropertyToEntity("addressee", "addressee");
		context.setContextID("x");
		resolution.setContext(context);
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

	private void processLine(ArrayList<LingEvidence> ling, ResolutionModuleRMRS resolution, String gold) {
		
		ArrayList<WordIU> ius = new ArrayList<WordIU>();
		
		WordIU prev = WordIU.FIRST_WORD_IU;
		
		LinkedList<String> incResults = new LinkedList<String>();
		
		for (LingEvidence ev : ling) {
			String word = ev.getValue("w1");
			if (word.equals("<s>")) continue;
			WordIU wiu = new WordIU(word, prev, null);
			ius.add(wiu);
			edits.add(new EditMessage<IU>(EditType.ADD, wiu));
			notifyListeners(hypListeners);
			prev = wiu;
			
			
			double last = -1;
			for (String gid : resolution.getGrounders().keySet()) {
				
				Grounder<String, String> grounder = resolution.getGrounders().get(gid);			
				String guess = null;
				
				if (!grounder.getPosterior().isEmpty()) {
					guess = grounder.getPosterior().getArgMax().getEntity();
				}
				
				if (!grounder.getPosterior().isEmpty()) {
					int r = grounder.getPosterior().findRank(gold);
					boolean gotit = gold.equals(guess);
					if (r != -1)  {
						if (last != -1) {
							incResults.removeLast();
						}
						last = r;
						if (gotit) incResults.add("True");
						else incResults.add("False");
					}
					if (gotit) break;
				}
			}
			
							
		}
		if (incResults.isEmpty()) incResults.add("False");
		
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
			new PentoRMRSDemo().run();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
