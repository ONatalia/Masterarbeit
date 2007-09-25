package gov.nist.sphere;

public class FloatHeaderItem extends SphereHeaderItem {
	  private float value;

	  FloatHeaderItem(String name, float value) {
	    super(name);
	    this.value = value;
	  }

	  public String getValueAsString() {
	    return "" + value;
	  }

	  public Object getValueAsObject() {
	    return new Float(value);
	  }

	  public final float getValue() {
	    return value;
	  }

	  public final int getType() {
	    return SphereHeaderItem.FLOAT;
	  }
	}
