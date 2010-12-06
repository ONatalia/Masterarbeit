<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mary="http://mary.dfki.de/2002/MaryXML"
>
<xsl:output method="xml" indent="no"/>

<xsl:template match="/">
 <xsl:element name="s"><xsl:apply-templates /></xsl:element>
</xsl:template>

<xsl:template match="mary:t">
 <xsl:element name="t">
  <xsl:apply-templates select="text()" /> 
  <xsl:element name="syllable">
   <xsl:apply-templates />
  </xsl:element>
 </xsl:element>
</xsl:template>

<xsl:template match="mary:boundary">
 <xsl:element name="t">
  <xsl:text>&lt;sil&gt;</xsl:text>
  <xsl:element name="syllable">
   <xsl:element name="ph">
    <xsl:attribute name="d"><xsl:apply-templates select="@duration" /></xsl:attribute>
    <xsl:attribute name="p"><xsl:text>_</xsl:text></xsl:attribute>
   </xsl:element>
  </xsl:element>
 </xsl:element>
</xsl:template>

<xsl:template match="mary:ph">
 <xsl:element name="ph">
  <xsl:attribute name="d"><xsl:apply-templates select="@d" /></xsl:attribute>
  <xsl:attribute name="end"><xsl:apply-templates select="@end" /></xsl:attribute>
  <xsl:attribute name="f0"><xsl:apply-templates select="@f0" /></xsl:attribute>
  <xsl:attribute name="p"><xsl:apply-templates select="@p" /></xsl:attribute>
  <!--xsl:apply-templates select="node()|@*" /-->
 </xsl:element>
</xsl:template>
</xsl:stylesheet>