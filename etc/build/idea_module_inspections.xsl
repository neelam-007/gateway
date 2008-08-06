<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to create an IDEA module file.

  <module-paths>
    <path file="modules/common/src/main/java"/>
    ...
  </module-paths>
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <!-- Parameters for customizing options -->
  <xsl:param name="data"></xsl:param> <!-- file containing ivy dependency report -->

  <!-- Globals -->
  <xsl:output method="xml" encoding="utf-8" indent="yes"/>
  <xsl:variable name="dependencies" select="document($data)/project-dependencies/ivy-report/dependencies"/>

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
        <orderEntry type="jdk" jdkName="1.6" jdkType="JavaSDK" />
        <xsl:for-each select="$dependencies/module">
            <xsl:sort select="revision/@position"/>
              <xsl:choose>
                  <xsl:when test="@organisation = 'com.l7tech'">
                    <!-- Ignore it -->
                  </xsl:when>
                  <xsl:otherwise>
                    <orderEntry type="module-library"  exported="">
                      <library name="{@organisation}.{@name}">
                        <CLASSES>
                            <xsl:for-each select="revision/artifacts/artifact">
                              <root url="jar://{@location}!/" />
                            </xsl:for-each>
                        </CLASSES>
                        <JAVADOC />
                        <SOURCES />
                      </library>
                    </orderEntry>
                  </xsl:otherwise>
              </xsl:choose>
          </xsl:for-each>
      </component>
    </module>
  </xsl:template>

</xsl:stylesheet>
