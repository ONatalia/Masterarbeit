package inpro.dm.isu;

import inpro.incremental.unit.ContribIU;
import inpro.incremental.unit.IUList;
import edu.cmu.sphinx.util.props.Configurable;

/**
 * Abstract class for domain implementations. Builds
 * and returns ContribIU lists to serve as contributions
 * in a IUNetworkInformationState. 
 * @author okko
 *
 */
public abstract class IUNetworkDomainUtil implements Configurable {
	
	protected IUList<ContribIU> contributions = new IUList<ContribIU>();
	
	public IUList<ContribIU> getContributions() {
		return this.contributions;
	}
	
}
