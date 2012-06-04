package demo.inpro.system.greifarm;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.WordIU;
import inpro.nlu.AVPairMappingUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * GreifarmActor is the NLU and action management component for the Greifarm prototype
 * 
 * (as the Greifarm prototype is not really a "dialogue system", but rather
 * a speech-controlled application, we avoid the term "dialog manager")
 * 
 * This class consists of the following components, which all live in their 
 * own subclasses:
 * <p>
 * GreifarmActor: the incremental processor (PushBuffer) which handles incoming
 * IUs and initiates the corresponding processing. This class handles the global
 * list of performed actions and of the words yet to process, as well as references
 * to the NLU component, the greifarm handling, and the game score.
 * It also provides a global logger.  
 * <p>
 * NLU: processes incoming words and (depending on what has been done before)
 * initiates actions (in the form of ActionIUs)
 * <p>
 * ActionIU: these {@link inpro.incremental.unit.IU}s are generated,
 * executed and put into the list of performed actions by the NLU. 
 * <p>
 * Greifarm: handles concurrency stuff related to the greifarm GUI (which itself
 * lives in {@link demo.inpro.system.greifarm.gui.GreifArmGUI} 
 * <p>
 * 
 * 
 * @author timo
 *
 */
public class GreifarmActor extends IUModule {
	
	private static final Logger logger = Logger.getLogger(GreifarmActor.class);

	/** the little score counter */
	protected final GameScore gameScore = new GameScore();
	
	/** encapsulates greifarmController GUI and exposes only the task-dependent actions */
	public final GreifarmController greifarmController = new GreifarmController(gameScore);
	
	/* incrementality/add/revoke related stuff here: */
	/** most recent words that are not yet part of an interpretation */
	private final Deque<WordIU> unusedWords = new ArrayDeque<WordIU>(); 
	/** actions already performed since the last call to processorReset() */
	private final Deque<ActionIU> performedActions = new ArrayDeque<ActionIU>();
	
	private final NLU nlu = new NLU(unusedWords, performedActions);
	
	@SuppressWarnings("unchecked")
	@Override
	public void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		boolean commitFlag = false; // set to true if there are commit messages
		for (EditMessage<? extends IU> em : edits) {
			logger.debug("received edit message " + em);
			switch (em.getType()) {
			case REVOKE:
				// on revoke, check that this is either the last element of
				// unusedWords, or that unusedWords is empty and
				if (unusedWords.isEmpty()) {
				//	logger.debug("I have to revert an action");
					if (!performedActions.isEmpty()) {
						ActionIU previousAction = performedActions.peekLast();
						if (!(previousAction instanceof ActionIU.StartActionIU)) {
							performedActions.pollLast();
							previousAction.revoke();
							unusedWords.addAll((List<WordIU>) previousAction.groundedIn());
						} else {
							logger.warn("something's wrong: " + performedActions + previousAction + unusedWords);
						}
					} else {
						assert false : "Must not revoke when no word has been input.";
					}
				}
				unusedWords.pollLast();
				// check that the correct word was revoked
			//	assert revokedWord.wordEquals((WordIU) em.getIU()) : "Expected " + revokedWord + "\n but I got " + em.getIU();
			//	logger.debug("unused words are now " + unusedWords);
				break;
			case ADD:
				// on add, add this word to unusedWords;
				WordIU addedWord = (WordIU) em.getIU();
				if (addedWord.isSilence() && addedWord.duration() > 0.1) {
					if (!performedActions.isEmpty())
						performedActions.getLast().precedesPause(true);
				} else {
					unusedWords.addLast(addedWord);
				}
				break;
			case COMMIT: 
				commitFlag = true;
			}
		}
		nlu.incrementallyUnderstandUnusedWords();
		if (commitFlag) {
			nlu.understandUnusedWordsOnCommit();
			// on commit, we want to notify the corresponding performedAction
			if (!performedActions.isEmpty())
				performedActions.getLast().precedesPause(true);
		}
		rightBuffer.setBuffer(performedActions);
	}
	
	@Override
	/** called after every commit (when recognition is complete) */
	public void reset() { /* ignore */ 
	// clear list of unused words? (or not?)
	//		-> we should probably keep it, to allow for ... hesitations
	// clear list of performed actions? (or not?) 
	//		-> maybe, but not the most recent action (as it can be of use in NLU)
	}
	
	/**
	 * reset the IU lists, called when the processor should be restarted
	 * (unlike reset() which is called from Sphinx after every commit)
	 */
	public void processorReset() {
		unusedWords.clear();
		for (ActionIU aiu : performedActions) {
			aiu.commit();
		}
		performedActions.clear();
		performedActions.add(new ActionIU.StartActionIU(greifarmController));
	}
	
	public void gameReset() {
		greifarmController.reset();
		processorReset();
	}
	
	
	@SuppressWarnings("serial")
	private void constructGUI() {
		JFrame f = new JFrame("Greifarm");
		JPanel p = new JPanel(new BorderLayout());
		JComponent grGUI = greifarmController.getVisual();
		grGUI.setBorder(new LineBorder(Color.GRAY));
		p.add(grGUI, BorderLayout.NORTH);
		p.add(gameScore.scoreLabel, BorderLayout.CENTER);
		p.add(new JButton(new AbstractAction("neu") {
			@Override
			public void actionPerformed(ActionEvent e) {
				gameReset();
			}
		}), BorderLayout.SOUTH);
		greifarmController.reset();
		processorReset();
		f.add(p);
		f.pack();
		f.setResizable(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		try {
			SwingUtilities.invokeAndWait(new Runnable() {@Override
				public void run() {
					constructGUI();
				}});
			WordIU.setAVPairs(AVPairMappingUtil.readAVPairs(GreifarmActor.class.getResourceAsStream("GreifarmAVMapping")));
		} catch (Exception e) {
			e.printStackTrace();
		}
		new Thread(gameScore, "game score thread").start();
	}

}
