package inpro.incremental.unit;

import inpro.irmrsc.parser.CandidateAnalysis;
import inpro.irmrsc.rmrs.Formula;

import java.util.Collections;
import java.util.List;

/**
 * An IU representing a syntactic derivation. The IU simply wraps around the
 * structure defined in {@link CandidateAnalysis}.
 * @author Andreas Peldszus
 */
public class CandidateAnalysisIU extends IU {

	private CandidateAnalysis candidateAnalysis;

	// this could better be a CAIU with the usual first CA: with stack(startsymbol)
	public static final CandidateAnalysisIU FIRST_CA_IU = new CandidateAnalysisIU();
	
	@SuppressWarnings("unchecked")
	public CandidateAnalysisIU() {
		this(FIRST_CA_IU, Collections.EMPTY_LIST, null);		
	}

	public CandidateAnalysisIU(IU sll, List<IU> groundedIn, CandidateAnalysis CandidateAnalysis) {
		super(sll, groundedIn);
		this.candidateAnalysis = CandidateAnalysis;
	}

	public CandidateAnalysisIU(IU sll, IU groundedIn, CandidateAnalysis CandidateAnalysis) {
		super(sll, Collections.singletonList(groundedIn));
		this.candidateAnalysis = CandidateAnalysis;
	}

	@Override
	public String toPayLoad() {
		if (this.candidateAnalysis == null) {
			return "null";
		} 
		return this.candidateAnalysis.toString();
	}
	
	public CandidateAnalysis getCandidateAnalysis () {
		return candidateAnalysis;
	}

}
