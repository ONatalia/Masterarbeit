package inpro.pitch.notifier;

import inpro.incremental.util.TedAdapter;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

public class TEDviewNotifier implements SignalFeatureListener {

	TedAdapter tedAdapter = new TedAdapter("localhost", 2000);
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		tedAdapter.write("<control originator='pitch' action='new'/>");
	}

	@Override
	public void newSignalFeatures(int frame, double power, boolean voicing,
			double pitch) {
		send(frame, voicing, pitch);
	}

	private void send(int frame, boolean voicing, double pitch) {
		if (voicing) {
			StringBuilder sb = new StringBuilder("<point originator='pitch' time='");
			sb.append(frame * 10);
			sb.append("' height='");
			sb.append(pitch / 1200 + 0.5);
			sb.append("'/>");
			tedAdapter.write(sb.toString());
		}
	}
	
	@Override
	public void reset() { }

}
