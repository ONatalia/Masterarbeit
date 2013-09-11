package inpro.incremental.sink;

import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.util.TimeUtil;

import java.io.FileWriter;
import java.io.IOException;
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
 * 
 * @author timo
 */
public class LabelWriter extends FrameAwarePushBuffer {
	
	@S4Boolean(defaultValue = false)
    public final static String PROP_WRITE_FILE = "writeToFile";
	
	@S4Boolean(defaultValue = true)
    public final static String PROP_WRITE_STDOUT = "writeToStdOut";
	
    @S4String(defaultValue = "")
    public final static String PROP_FILE_PATH= "filePath";    
    
    @S4String(defaultValue = "")
    public final static String PROP_FILE_NAME = "fileName";    
    
    private boolean writeToFile;
    private boolean writeToStdOut = true;
    private String filePath;
    private String fileName;
    int frameOutput = -1;

	
	@Override	
	public void newProperties(PropertySheet ps) throws PropertyException {
		writeToFile = ps.getBoolean(PROP_WRITE_FILE);
		writeToStdOut = ps.getBoolean(PROP_WRITE_STDOUT);
		filePath = ps.getString(PROP_FILE_PATH);
		fileName = ps.getString(PROP_FILE_NAME);
	}

	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		
		/*
		 * Get the time first
		 */
		String toOut = String.format(Locale.US, "Time: %.2f", 
				currentFrame * TimeUtil.FRAME_TO_SECOND_FACTOR);
		
		
		/*
		 * Then go through all the IUs, ignoring commits 
		 */
		boolean added = false;
		for (IU iu : ius) {
			if (iu.isCommitted()) continue;
			added = true;
			toOut += "\n" + iu.toLabelLine();
		}
		toOut += "\n\n";
		
		/*
		 * If there were only commits, or if there are not IUs, then print out as specified
		 */
		if (ius.size() > 0 && added) { // && frameOutput != currentFrame) {
			frameOutput = currentFrame;

			if (writeToFile) {
				try {
				FileWriter writer = new FileWriter(filePath + "/" + fileName + ".inc_reco", true);
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
	
	/*
	 * A file name can be specified here, if not specified in the config file
	 */
	public void setFileName(String name) {
		fileName = name;
	}

	@Override
	public void reset() {
		super.reset();
		frameOutput = -1;
	}
	
}
