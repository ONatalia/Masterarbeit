package inpro.incremental.listener;

import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inpro.incremental.util.ResultUtil;

import java.util.Collection;
import java.util.List;
import java.util.Locale;


public class LabelWriter extends FrameAwarePushBuffer {

	@Override
	public void hypChange(Collection<? extends IU> ius, List<? extends EditMessage<? extends IU>> edits) {
		if (ius.size() > 0) {
			System.out.println(String.format(Locale.US, "Time: %.2f", 
					currentFrame * ResultUtil.FRAME_TO_SECOND_FACTOR));
			for (IU iu : ius) {
				System.out.println(iu.toLabelLine());
			}
			System.out.println();
		}
	}

}
