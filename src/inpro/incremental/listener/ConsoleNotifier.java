package inpro.incremental.listener;

import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.util.ResultUtil;

import java.util.Collection;
import java.util.List;


public class ConsoleNotifier extends FrameAwarePushBuffer {

	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (edits.size() > 0) {
			System.out.print("\nThe Hypothesis has changed at time: ");
			System.out.println(currentFrame * ResultUtil.FRAME_TO_SECOND_FACTOR);
			System.out.println("Edits since last hypothesis:");
			for (EditMessage<? extends IU> edit : edits) {
				System.out.println(edit.toString());
			}
			System.out.println("Current hypothesis is now:");
			for (IU iu : ius) {
				System.out.println(iu.deepToString());
			} 
		} else {
/*			System.out.print("."); */
		}
	}

}