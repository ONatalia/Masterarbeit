package org.cocolab.inpro.incremental.unit;

public enum EditType {
	add, substitute, revoke, commit
	// this could be extended by adding assert90, assert95, assert98, ...
	// which would then signify the likelihood in percent
}
