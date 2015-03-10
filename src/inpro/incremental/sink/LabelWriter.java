package inpro.incremental.sink;

import inpro.apps.util.RecoCommandLineParser;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.util.TimeUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4String;

/**
 * An IU left buffer that prints its contents to STDOUT.
 * The format used resembles wavesurfer label files 
 * @author timo
 */
public class LabelWriter extends FrameAwarePushBuffer {
	
	@S4Boolean(defaultValue = false)
    public final static String PROP_WRITE_FILE = "writeToFile";
	
	@S4Boolean(defaultValue = false)
    public final static String PROP_COMMITS_ONLY = "commitsOnly";	
	
	@S4Boolean(defaultValue = true)
    public final static String PROP_WRITE_STDOUT = "writeToStdOut";
	
    @S4String(defaultValue = "")
    public final static String PROP_FILE_PATH= "filePath";    
    
    @S4String(defaultValue = "")
    public final static String PROP_FILE_NAME = "fileName";    
    
    private boolean writeToFile;
    private boolean commitsOnly;
    private boolean writeToStdOut = true;
    private static String fileName;
    int frameOutput = -1;
    
    ArrayList<IU> allIUs = new ArrayList<IU>();
    
	@Override	
	public void newProperties(PropertySheet ps) throws PropertyException {
		writeToFile = ps.getBoolean(PROP_WRITE_FILE);
		commitsOnly = ps.getBoolean(PROP_COMMITS_ONLY);
		writeToStdOut = ps.getBoolean(PROP_WRITE_STDOUT);
		fileName = RecoCommandLineParser.getLabelPath();
	}

	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		/* Get the time first */
		String toOut = String.format(Locale.US, "Time: %.2f", 
				currentFrame * TimeUtil.FRAME_TO_SECOND_FACTOR);
		/* Then go through all the IUs, ignoring commits */
		boolean added = false;
		for (EditMessage<? extends IU> edit : edits) {
			IU iu = edit.getIU();
			switch (edit.getType()) {
			case ADD:
				if (!commitsOnly) {
					added = true;
					allIUs.add(iu);
				}
				break;
			case COMMIT:
				if (commitsOnly) {
					added = true;
					allIUs.add(iu);
				}
				break;
			case REVOKE:
//				when revoking, we can assume that we are working with a stack;
//				hence, the most recent thing added is the most recent thing revoked
				if (!allIUs.isEmpty()) {
					added = true;
					allIUs.remove(allIUs.size()-1);
				}
				break;
			default:
				break;
			
			}

		}
		
		toOut = String.format(Locale.US, "Time: %.2f", 
		currentFrame * TimeUtil.FRAME_TO_SECOND_FACTOR);
		for (IU iu : allIUs) {
			toOut += "\n" + iu.toLabelLine();
		}
		
		toOut += "\n\n";
		/* If there were only commits, or if there are not IUs, then print out as specified */
		if (edits.size() > 0 && added) { // && frameOutput != currentFrame) {
			frameOutput = currentFrame;
			if (writeToFile) {
				try {
					FileWriter writer = new FileWriter( fileName + ".inc_reco", true);
					writer.write(toOut);
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (writeToStdOut) {
				System.out.println(toOut);
			}
		}
	}
	
	/** A file name can be specified here, if not specified in the config file */
	public void setFileName(String name) {
		fileName = name;
	}

	@Override
	public void reset() {
		super.reset();
		frameOutput = -1;
	}

	public void writeToFile() {
		// TODO Auto-generated method stub
		writeToFile = true;
	}
	
}
