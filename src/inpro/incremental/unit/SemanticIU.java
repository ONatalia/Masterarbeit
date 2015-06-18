package inpro.incremental.unit;

import inpro.incremental.unit.IU;

public class SemanticIU extends IU {
	
	private String predicate;
	private String left;
	private String right;
	private String semID;
	private String leftID;
	private String rightID;
	private Boolean isHead;
	
	public SemanticIU(String semID, String predicate, String left, String leftID, String right, String rightID, Boolean isHead) {
		super();
		setSemID(semID);
		setPredicate(predicate);
		setLeft(left);
		setRight(right);
		setLeftID(leftID);
		setRightID(rightID);
		setIsHead(isHead);
	}

	private void setIsHead(Boolean isHead) {
		this.isHead = isHead;
	}
	
	public boolean isHead() {
		return this.isHead;
	}

	public SemanticIU() { 
		
		
	}

	@Override
	public String toPayLoad() {
		if (getPredicate() == null) return "EmptyIU";
		if (getLeftID() == null) 
			return String.format("%s:%s()", getSemID(), getPredicate());
		if (getRightID() == null)
			return String.format("%s:%s(%s:%s)", getSemID(), getPredicate(), getLeftID(), getLeft());
		return String.format("%s:%s(%s:%s,%s:%s)", getSemID(), getPredicate(), getLeftID(), getLeft(), getRightID(), getRight()); 
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getLeft() {
		return left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}

	public String getSemID() {
		return semID;
	}

	public void setSemID(String semID2) {
		this.semID = semID2;
	}

	public String getLeftID() {
		return leftID;
	}

	public void setLeftID(String leftID) {
		this.leftID = leftID;
	}

	public String getRightID() {
		return rightID;
	}

	public void setRightID(String rightID) {
		this.rightID = rightID;
	}

	public String getEntityType() {
		return getSemID().substring(0,1);
	}

	public String getLeftEntityType() {
		return getLeftID().substring(0,1);
	}
	
	public String getRightEntityType() {
		return getRightID().substring(0,1);
	}

	public boolean hasLeft() {
		return getLeft() != null && !getLeft().isEmpty();
	}

	public boolean hasRight() {
		return getRight() != null && !getRight().isEmpty();
	}
}
