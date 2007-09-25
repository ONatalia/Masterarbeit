package gov.nist.sphere;

public class StringHeaderItem extends SphereHeaderItem {
	  private String value;

	  StringHeaderItem(String name, String value) {
	    super(name);
	    this.value = value;
	  }

	  public final String getValue() {
	    return value;
	  }

	  public Object getValueAsObject() {
	    return getValue();
	  }

	  public String getValueAsString() {
	    return getValue();
	  }

	  public final int getType() {
	    return SphereHeaderItem.STRING;
	  }
	}