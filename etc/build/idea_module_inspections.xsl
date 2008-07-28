<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to create an IDEA module file.

  <module-paths>
    <path file="modules/common/src/main/java"/>
    ...
  </module-paths>
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" encoding="utf-8" indent="yes"/>

  <xsl:template match="/">
    <module relativePaths="true" type="JAVA_MODULE" version="4">
      <component name="NewModuleRootManager" inherit-compiler-output="true">
        <exclude-output />
        <content url="file://$MODULE_DIR$">
          <xsl:for-each select="module-paths/path">
            <xsl:sort select="@file"/>
            <sourceFolder url="file://$MODULE_DIR$/{@file}" isTestSource="false" />
          </xsl:for-each>
        </content>
      </component>
    </module>
  </xsl:template>

</xsl:stylesheet>
