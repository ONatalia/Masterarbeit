package org.cocolab.inpro.incremental.unit;

public enum EditType {
	ADD, REVOKE, COMMIT //, SUBSTITUTE // this one does not play well with the other classes (yet?)
	// this could be extended by adding assert90, assert95, assert98, ...
	// which would then signify the likelihood in percent
}
