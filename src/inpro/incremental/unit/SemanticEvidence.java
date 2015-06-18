package inpro.incremental.unit;

public class SemanticEvidence implements Comparable{
	
	private String relation;
	private String arg1;
	private String arg2;
	private String leftEntity;
	private String rightEntity;

	public SemanticEvidence(String name, String anchor, String argument) {
		setRelation(name);
		setArg1(anchor);
		setArg2(argument);
	}

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getArg2() {
		return arg2;
	}

	public void setArg2(String arg2) {
		this.arg2 = arg2;
	}
	
	public String toString() {
		return getRelation() + "("+this.getLeftEntity()+"."+getArg1()+","+this.getRightEntity()+"."+getArg2()+")";
	}

	@Override
	public int compareTo(Object o) {
		SemanticEvidence semEv = (SemanticEvidence) o;
		return this.toString().compareTo(semEv.toString());
	}

	public String getLeftEntity() {
		return leftEntity;
	}

	public void setLeftEntity(String leftEntity) {
		this.leftEntity = leftEntity;
	}

	public String getRightEntity() {
		return rightEntity;
	}

	public void setRightEntity(String rightEntity) {
		this.rightEntity = rightEntity;
	}
	
	

}
