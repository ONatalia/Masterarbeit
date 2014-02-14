package inpro.io.instantio;

import junit.framework.Assert;

import org.instantreality.InstantIO.InSlot;
import org.junit.Test;

public class InstantIOListenerTest implements InSlot.Listener {
	
	/**
	 * This just does a simple check, it doesn't actually receive data from the InstantIO network.
	 */
	@Test 
	public void test() {
		InstantIOListener listener = InstantIOListener.getInstance();
		Assert.assertNotNull(listener);
		listener.addInSlotListener("fake_name", this);
	}

	@Override
	public void newData(InSlot arg0) {
		
	}

	@Override
	public void startInSlot(InSlot arg0) {
		
	}

	@Override
	public void stopInSlot(InSlot arg0) {
		
	}

}
