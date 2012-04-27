package inpro.incremental.unit;

import weka.core.FastVector;

public enum EditType {
	ADD, REVOKE, COMMIT; //, SUBSTITUTE // this one does not play well with the other classes (yet?)
	// how about a generic UPDATE -> that could subsume different confidences and even commit
	// this could be extended by adding ASSERT90, ASSERT95, ASSERT98, ...
	// which would then signify the likelihood in percent
	
	public boolean isCommit() {
		return this.equals(COMMIT);
	}
	
	public static FastVector typesForWEKA() {
		FastVector v = new FastVector(3);
		v.addElement(ADD.toString());
		v.addElement(REVOKE.toString());
		v.addElement(COMMIT.toString());
		return v;
	}
	
}
