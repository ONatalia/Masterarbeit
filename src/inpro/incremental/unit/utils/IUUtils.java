package inpro.incremental.unit.utils;

import inpro.incremental.unit.CandidateAnalysisIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.TagIU;
import inpro.incremental.unit.WordIU;

public class IUUtils {

	
	public static boolean isFirstWordIU(IU wordIU) {
		return wordIU.toPayLoad().equals(WordIU.FIRST_IU.toPayLoad());
	}
	
	public static WordIU getWordIU(IU iu) {
		while (! (iu instanceof WordIU)) {
			iu = iu.groundedIn().get(0);
		}
		return (WordIU)iu;
	}
	
	public static boolean isFirst(IU iu) {
		return IUUtils.isFirstWordIU(IUUtils.getWordIU(iu).getSameLevelLink());
	}

	public static TagIU getPOS(IU iu) {
		while (! (iu instanceof TagIU)) {
			iu = iu.groundedIn().get(0);
		}
		return (TagIU)iu;
	}

	public static CandidateAnalysisIU getSyntax(IU iu) {
		while (! (iu instanceof CandidateAnalysisIU)) {
			iu = iu.groundedIn().get(0);
		}
		return (CandidateAnalysisIU)iu;
	}
	
}
