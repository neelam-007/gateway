<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to remove xml declaration and any processing instructions and comments
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   
    <!-- Globals -->
    <xsl:output method="xml" omit-xml-declaration="yes" encoding="utf-8" indent="yes"/>

    <!-- Copy by default -->
    <xsl:template match="@*|*">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

