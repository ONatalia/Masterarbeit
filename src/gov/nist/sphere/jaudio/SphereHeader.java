/**
 * Title:        CommViewer<br>
 * Description:  Graphical Log Analyzer for Communicator<br>
 * Copyright:    Copyright (c) 2000 Christophe Laprun for NIST<br>
 * Company:      NIST<br>
 * @author Christophe Laprun
 * @version 1.0
 */
package gov.nist.sphere.jaudio;

import gov.nist.sphere.IntHeaderItem;
import gov.nist.sphere.SphereException;
import gov.nist.sphere.SphereHeaderItem;
import gov.nist.sphere.StringHeaderItem;

import java.util.HashMap;

public class SphereHeader {

  private HashMap<String, SphereHeaderItem> items = new HashMap<String, SphereHeaderItem>(37);
  private int sampleCount = -1;
  private int sampleNBytes = -1;
  private int channelCount = -1;
  private int sampleRate = -1;
  private String sampleCoding = null;

  private final static String SAMPLE_COUNT = "sample_count";
  private final static String SAMPLE_N_BYTES = "sample_n_bytes";
  private final static String CHANNEL_COUNT = "channel_count";
  public final static String SAMPLE_CODING_PCM = "pcm";
  public final static String SAMPLE_CODING_ULAW = "ulaw";
  public final static String SAMPLE_CODING_ALAW = "alaw";
  public final static String SAMPLE_CODING_DEFAULT = SAMPLE_CODING_PCM;

	  
  public SphereHeader() {
  }

  public void add(SphereHeaderItem item) {
    items.put(item.getName(), item);
  }

  public void add(String type, String name, String value) {
    items.put(name, SphereHeaderItem.createHeaderItemForType(type, name, value));
  }

  public Object getAsObject(String name) {
    SphereHeaderItem item = get(name);
    return item.getValueAsObject();
  }

  public int getTypeOfItem(String name) throws SphereException {
    try {
      return get(name).getType();
    } catch (NullPointerException e) {
      throw new SphereException("no header item named " + name);
    }
  }

  public String getStringItem(String name) throws SphereException {
    return (String) this.getAsTypeCheckedObject(name, SphereHeaderItem.STRING, "String");
  }

  public int getIntItem(String name) throws SphereException {
    Integer i = (Integer) getAsTypeCheckedObject(name, SphereHeaderItem.INT, "integer");
    return i.intValue();
  }

  public float getRealItem(String name) throws SphereException {
    Float f = (Float) getAsTypeCheckedObject(name, SphereHeaderItem.FLOAT, "real");
    return f.floatValue();
  }

  public boolean containsItemNamed(String name) {
    return items.containsKey(name);
  }

  public final int getSampleCount() throws SphereException {
    if (sampleCount > 1)
      return sampleCount;
    else {
      sampleCount = this.getRequiredIntField("sample_count");
      if (sampleCount < 1)
        throw new SphereException("Incorrect header: sample_count must be greater or equal to 1");
      return sampleCount;
    }
  }

  public final int getSampleNBytes() throws SphereException {
    if (sampleNBytes == 1 || sampleNBytes == 2)
      return sampleNBytes;
    else {
      sampleNBytes = this.getRequiredIntField("sample_n_bytes");
      if (sampleNBytes != 1 && sampleNBytes != 2)
        throw new SphereException("Incorrect header: sample_n_bytes must be either 1 or 2");
      return sampleNBytes;
    }
  }

  public final int getChannelCount() throws SphereException {
    if (channelCount > 0 && channelCount < 33)
      return channelCount;
    else {
      channelCount = this.getRequiredIntField("channel_count");
      if (channelCount < 1 || channelCount > 32)
        throw new SphereException("Incorrect header: channel_count must be comprised between 1 and 32");
      return channelCount;
    }
  }


  public final String getSampleCoding() throws SphereException {
    if (sampleCoding != null)
      return sampleCoding;
    else {
      StringHeaderItem shi = (StringHeaderItem) get("sample_coding");
      if (shi == null) {
        sampleCoding = SAMPLE_CODING_DEFAULT;
      } else {
        String value = shi.getValue();
        if (value.equals(SAMPLE_CODING_PCM) || value.equals(SAMPLE_CODING_ULAW)
        									|| value.equals(SAMPLE_CODING_ALAW))
          sampleCoding = value;
        else
          throw new SphereException("Incorrect header: illegal value for sample_coding");
      }
      return sampleCoding;
    }
  }

  /**
   * Returns the value of sample_rate.
   */
  public final int getSampleRate() throws SphereException {
    // sampleRate == 0 => sampleRate not present but not needed
    if (sampleRate >= 0)
      return sampleRate;
    else {
      if (isSampleRateNeededFor(getSampleCoding())) {
        sampleRate = this.getRequiredIntField("sample_rate");
        if (sampleRate < 1)
          throw new SphereException("Incorrect header: sample_rate must be greater than 1");
      } else
        sampleRate = 0;
      return sampleRate;
    }
  }

  public final boolean isDataBigEndian() throws SphereException {
    if (getSampleNBytes() == 1)
      return false;
    StringHeaderItem shi = (StringHeaderItem) get("sample_byte_format");
    if (shi == null)
    // This can happen when sample_byte_format defaults to platform default which
    // is not portable
      throw new SphereException("Incorrect header: couldn't determine byte format`");
    else {
      String value = shi.getValue();
      if (value.equals("01")) {
        return false;
      } else if (value.equals("10")) {
        return true;
      } else {
        throw new SphereException("Incorrect header: sample_byte_format");
      }
    }
  }

  /** NOT FINISHED */
  public boolean isValid() {
    boolean required =
            containsItemNamed(SAMPLE_COUNT) &&
            this.containsItemNamed(CHANNEL_COUNT) &&
            this.containsItemNamed(SAMPLE_N_BYTES);
    boolean optional = isSampleRateValid();

    return required && optional;
  }

  public Object[] getItemsAsArray() {
    return items.values().toArray();
  }

  public String toString() {
    return items.toString() + "\n" + items.size();
  }

  public final boolean isSampleRateMeaningful() {
    try {
      return (getSampleRate() > 0);
    } catch (SphereException e) {
      return false;
    }
  }

  protected boolean isSampleRateValid() {
    try {
      if (getSampleRate() < 0)
        return false;
      else
        return true;
    } catch (SphereException e) {
      return false;
    }
  }

  protected boolean isSampleRateNeededFor(String encoding) {
    return encoding.equals(SAMPLE_CODING_PCM) || encoding.equals(SAMPLE_CODING_ULAW);
  }

  protected int getRequiredIntField(String name) throws SphereException {
    IntHeaderItem ihi = (IntHeaderItem) get(name);
    if (ihi == null)
      throw new SphereException("Incorrect header: " + name + " is missing");
    return ihi.getValue();
  }

  private final Object getAsTypeCheckedObject(String name, int iType, String sType)
          throws SphereException {
    if (getTypeOfItem(name) != iType)
      throw new SphereException("type of item named " + name + " is not " + sType);
    return getAsObject(name);
  }

  protected SphereHeaderItem get(String name) {
    return items.get(name);
  }

}