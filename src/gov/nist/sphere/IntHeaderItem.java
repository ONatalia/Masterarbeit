package gov.nist.sphere;

public class IntHeaderItem extends SphereHeaderItem {
  private int value;

  IntHeaderItem(String name, int value) {
    super(name);
    this.value = value;
  }

  public String getValueAsString() {
    return "" + value;
  }

  public Object getValueAsObject() {
    return new Integer(value);
  }


  public final int getValue() {
    return value;
  }

  public final int getType() {
    return SphereHeaderItem.INT;
  }
}