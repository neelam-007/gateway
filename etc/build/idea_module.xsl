<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to create an IDEA module file.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" encoding="utf-8" indent="yes"/>

  <xsl:param name="module"/> <!-- IDEA module file used to copy sources -->
  <xsl:param name="modulemeta"/> <!-- Javadoc URLs etc for libraries -->
  <xsl:param name="source">false</xsl:param>
  <xsl:param name="tests">false</xsl:param>
  <xsl:param name="source.resources">false</xsl:param>
  <xsl:param name="test.resources">false</xsl:param>
  <xsl:param name="runtime">false</xsl:param>

  <xsl:variable name="modulemetadoc" select="document($modulemeta)"/>
  <xsl:variable name="idea-modules" select="document($module)/module/component[@name = 'NewModuleRootManager']"/>

  <xsl:template match="/">
    <module relativePaths="true" type="JAVA_MODULE" version="4">
      <component name="NewModuleRootManager" inherit-compiler-output="true">
        <exclude-output />
        <content url="file://$MODULE_DIR$">
          <xsl:if test="'true' = $source">
          <sourceFolder url="file://$MODULE_DIR$/src/main/java" isTestSource="false" />
          </xsl:if>
          <xsl:if test="'true' = $source.resources">
          <sourceFolder url="file://$MODULE_DIR$/src/main/resources" isTestSource="false" />
          </xsl:if>
          <xsl:if test="'true' = $tests">
          <sourceFolder url="file://$MODULE_DIR$/src/test/java" isTestSource="true" />
          </xsl:if>
          <xsl:if test="'true' = $test.resources">
          <sourceFolder url="file://$MODULE_DIR$/src/test/resources" isTestSource="true" />
          </xsl:if>
          <xsl:if test="'true' = $source or 'true' = $tests or 'true' = $source.resources or 'true' = $test.resources">
          <excludeFolder url="file://$MODULE_DIR$/build" />
          </xsl:if>
        </content>
        <orderEntry type="inheritedJdk" />
        <orderEntry type="sourceFolder" forTests="false" />

        <xsl:if test="'true' = $runtime">
        <orderEntry type="module" module-name="UneasyRooster"/>
        </xsl:if>

        <xsl:for-each select="/ivy-report/dependencies/module">
          <xsl:sort select="revision/@position"/>
            <xsl:if test="revision/caller[@name = /ivy-report/info/@module and @organisation = /ivy-report/info/@organisation]">
              <xsl:choose>
                  <xsl:when test="@organisation = 'com.l7tech'">
                    <orderEntry type="module" module-name="{@name}"  exported=""/>
                  </xsl:when>
                  <xsl:otherwise>
                    <orderEntry type="module-library"  exported="">
                      <library name="{@organisation}.{@name}">
                        <CLASSES>
                          <xsl:choose>
                            <xsl:when test="revision/artifacts/artifact">
                                <xsl:for-each select="revision/artifacts/artifact">
                                  <root url="jar://{@location}!/" />
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:otherwise>
                                <!-- treat as a library -->
                                <xsl:call-template name="library">
                                    <xsl:with-param name="organisation"><xsl:value-of select="@organisation"/></xsl:with-param>
                                    <xsl:with-param name="name"><xsl:value-of select="@name"/></xsl:with-param>
                                </xsl:call-template>
                            </xsl:otherwise>
                          </xsl:choose>
                        </CLASSES>
                        <xsl:call-template name="javadoc">
                            <xsl:with-param name="libraryname"><xsl:value-of select="@organisation"/>.<xsl:value-of select="@name"/></xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="sources">
                            <xsl:with-param name="libraryname"><xsl:value-of select="@organisation"/>.<xsl:value-of select="@name"/></xsl:with-param>
                        </xsl:call-template>
                      </library>
                    </orderEntry>
                  </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
          </xsl:for-each>
        <orderEntryProperties />
      </component>
    </module>
  </xsl:template>

    <xsl:template name="library">
        <xsl:param name="organisation"/>
        <xsl:param name="name"/>

        <xsl:for-each select="/ivy-report/dependencies/module/revision[caller/@organisation = $organisation and caller/@name = $name]/artifacts/artifact">
          <root url="jar://{@location}!/" />
        </xsl:for-each>
    </xsl:template>                      

    <xsl:template name="javadoc">
        <xsl:param name="libraryname"/>

        <xsl:choose>
          <xsl:when test="$idea-modules/orderEntry/library[@name = $libraryname]/JAVADOC/*">
            <xsl:apply-templates select="$idea-modules/orderEntry/library[@name = $libraryname]/JAVADOC"/>
          </xsl:when>
          <xsl:otherwise>
            <JAVADOC>
              <xsl:if test="$modulemetadoc/layer7-project-info/library[@name = $libraryname and @javadoc-url]">
                  <root url="{$modulemetadoc/layer7-project-info/library[@name = $libraryname]/@javadoc-url}" />
              </xsl:if>
            </JAVADOC>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="sources">
        <xsl:param name="libraryname"/>

        <xsl:choose>
          <xsl:when test="$idea-modules/orderEntry/library[@name = $libraryname]/SOURCES">
            <xsl:apply-templates select="$idea-modules/orderEntry/library[@name = $libraryname]/SOURCES"/>
          </xsl:when>
          <xsl:otherwise>
            <SOURCES />
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Copy by default -->
   <xsl:template match="@*|*|processing-instruction()|comment()">
     <xsl:copy>
       <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
     </xsl:copy>
   </xsl:template>

</xsl:stylesheet>
