package org.cocolab.inpro.incremental.unit;

public enum EditType {
	ADD, REVOKE, COMMIT; //, SUBSTITUTE // this one does not play well with the other classes (yet?)
	// how about a generic UPDATE -> that could subsume different confidences and even commit
	// this could be extended by adding ASSERT90, ASSERT95, ASSERT98, ...
	// which would then signify the likelihood in percent
	
	// FIXME: OAA-sending should be re-thought.
    public final static String ADD_WORD_GOAL = "addLastWord"; 
	public final static String REVOKE_WORD_GOAL = "revokeLastWord"; 
    public final static String COMMIT_WORD_GOAL = "commitFirstWord";

    public String oaaGoal() {
    	switch (this) {
    		case ADD: return ADD_WORD_GOAL;
    		case REVOKE: return REVOKE_WORD_GOAL;
    		case COMMIT: return COMMIT_WORD_GOAL;
    		default: throw new RuntimeException("If you implement new EditTypes, you should also implement their handling!");
    	}
    }
    
}
