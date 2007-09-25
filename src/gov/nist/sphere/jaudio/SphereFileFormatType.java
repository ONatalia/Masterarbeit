package gov.nist.sphere.jaudio;

import javax.sound.sampled.AudioFileFormat;

/**
 * Title:        CommViewer
 * Description:  Graphical Log Analyzer for Communicator
 * Copyright:    Copyright (c) Christophe Laprun
 * Company:      NIST
 * @author Christophe Laprun
 * @version 1.0
 */

public class SphereFileFormatType extends AudioFileFormat.Type {
  public static final AudioFileFormat.Type SPHERE = new SphereFileFormatType("SPHERE", "sph");

  public SphereFileFormatType(String fileFormatName, String fileExtension) {
    super(fileFormatName, fileExtension);
  }
}