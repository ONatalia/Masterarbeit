package inpro.irmrsc.rmrs;

public class Entity implements Comparable {
	
	private String entity;
	private String ep;
	private String type;
	private boolean isFilled = false;
	
	public Entity(String entity, String name, String type) {
		this.setEntity(entity);
		this.setEp(name);
		this.setType(type);
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}
	
	/*
	 * At the moment, entity types are simply represented as the first char, e.g., x14 or e2
	 */
	public String getEntityType() {
		return getEntity().substring(0,1);
	}
	
	/*
	 * Number that comes after the ID, i.e., x14 -> 14
	 */
	public String getEntityID() {
		return getEntity().substring(1);
	}
	
	public boolean equals(Entity other) {
		return other.getEntity().equals(this.getEntity());
	}

	@Override
	public int compareTo(Object o) {
		Entity other = (Entity) o;
		return this.getEntity().compareTo(other.getEntity());
	}
	
	public String toString() {
		return getEp() + " " +getEntity() ;
	}

	public boolean isFilled() {
		return isFilled;
	}
	
	public void entityHasBeenFilled() {
		this.setFilled(true);
	}

	public void setFilled(boolean isFilled) {
		this.isFilled = isFilled;
	}

	public String getEp() {
		return ep;
	}

	public void setEp(String ep) {
		this.ep = ep;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	

}
