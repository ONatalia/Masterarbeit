/**
 * Title:        CommViewer<p>
 * Description:  Graphical Log Analyzer for Communicator<p>
 * Copyright:    Copyright (c) Christophe Laprun<p>
 * Company:      NIST<p>
 * @author Christophe Laprun
 * @version 1.0
 */
package gov.nist.sphere;

public abstract class SphereHeaderItem {

  public static SphereHeaderItem createHeaderItemForType(String type, String name, String value) {
    if (type.equals("i")) {
      if (value.startsWith("+"))
        value = value.substring(1);
      return new IntHeaderItem(name, Integer.parseInt(value));
    }
    if (type.charAt(0) == 's') // Timo (20070919): strings are not denoted by 's' but by 's123' with '123' denoting the length of the string
      return new StringHeaderItem(name, value);
    if (type.equals("r"))
      return new FloatHeaderItem(name, Float.parseFloat(value));
    return null;
  }

  public String getName() {
    return name;
  }

  public abstract String getValueAsString();

  public abstract Object getValueAsObject();

  public String toString() {
    return new String(name + " " + getValueAsString());
  }

  SphereHeaderItem(String name) {
    this.name = name;
  }

  public abstract int getType();

  public static final int INT = 0;
  public static final int FLOAT = 1;
  public static final int STRING = 2;
  protected String name;
}




