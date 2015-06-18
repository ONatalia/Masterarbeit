package inpro.incremental.source;

import java.util.Collection;
import java.util.List;

import inpro.incremental.IUModule;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;

public abstract class IUSourceModule extends IUModule {

	@Override
	protected final void leftBufferUpdate(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		throw new RuntimeException("IU source modules do not expect to be anything but the source of IUs; they cannot be fed IUs.");
	}

}
