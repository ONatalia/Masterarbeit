/* 
 * Copyright 2009, Timo Baumann and the Inpro project
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
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
