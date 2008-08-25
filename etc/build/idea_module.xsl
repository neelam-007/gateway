<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to update an IDEA module.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Parameters for customizing options -->
    <xsl:param name="data"/> <!-- file containing ivy dependency report -->
    <xsl:param name="modulemeta"/> <!-- Javadoc URLs etc for libraries -->
    <xsl:param name="source">false</xsl:param>
    <xsl:param name="tests">false</xsl:param>
    <xsl:param name="source.resources">false</xsl:param>
    <xsl:param name="test.resources">false</xsl:param>

    <!-- Global settings -->
    <xsl:output method="xml" encoding="utf-8" indent="yes"/>
    <xsl:variable name="ivy-report" select="document($data)/ivy-report"/>
    <xsl:variable name="modulemetadoc" select="document($modulemeta)"/>
    <xsl:variable name="idea-modules" select="/module/component[@name = 'NewModuleRootManager']"/>

    <!-- Process the main module component -->
    <xsl:template match="/module/component[@name = 'NewModuleRootManager']">
        <!-- copy existing -->
        <xsl:copy>
            <xsl:apply-templates select="*[local-name() != 'content' and local-name() != 'orderEntry']|@*|text()|processing-instruction()|comment()"/>

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

            <!-- Direct Module Dependencies -->
            <xsl:for-each select="$ivy-report/dependencies/module">
                <xsl:sort select="revision/@position"/>
                <xsl:if test="revision[contains(@conf,'default') or contains(@conf,'core')]">
                    <xsl:if test="revision/caller[@name = $ivy-report/info/@module and @organisation = $ivy-report/info/@organisation]">
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
                  </xsl:if>
              </xsl:for-each>

            <!-- Runtime Module Dependencies -->
            <xsl:for-each select="$ivy-report/dependencies/module">
                <xsl:sort select="revision/@position"/>
                <xsl:if test="revision[@default = 'false' and contains(@conf,'runtime') and not(contains(@conf,'default'))]">
                    <xsl:call-template name="runtime-module-library">
                        <xsl:with-param name="organisation"><xsl:value-of select="@organisation"/></xsl:with-param>
                        <xsl:with-param name="name"><xsl:value-of select="@name"/></xsl:with-param>
                    </xsl:call-template>
                </xsl:if>
              </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="library">
        <xsl:param name="organisation"/>
        <xsl:param name="name"/>

        <xsl:for-each select="$ivy-report/dependencies/module/revision[caller/@organisation = $organisation and caller/@name = $name]/artifacts/artifact">
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

    <!--
      Recursively process all runtime dependencies for the given module.
    -->
    <xsl:template name="runtime-module-library">
        <xsl:param name="organisation"/>
        <xsl:param name="name"/>

        <xsl:if test="$ivy-report/dependencies/module/revision[contains(@conf,'runtime') and not(contains(@conf,'default'))]">
            <orderEntry type="module-library"> <!-- TODO [steve] Is this correct for unexported ?-->
              <library name="{@organisation}.{@name}">
                <CLASSES>
                    <xsl:for-each select="$ivy-report/dependencies/module[@organisation = $organisation and @name = $name]/revision/artifacts/artifact">
                      <root url="jar://{@location}!/" />
                    </xsl:for-each>
                </CLASSES>
                <xsl:call-template name="javadoc">
                    <xsl:with-param name="libraryname"><xsl:value-of select="@organisation"/>.<xsl:value-of select="@name"/></xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="sources">
                    <xsl:with-param name="libraryname"><xsl:value-of select="@organisation"/>.<xsl:value-of select="@name"/></xsl:with-param>
                </xsl:call-template>
              </library>
            </orderEntry>
        </xsl:if>

        <!-- This select is not quite right, we should be choosing modules there the organisation/name are found for the same revision/caller -->
        <!--
        <xsl:for-each select="$ivy-report/dependencies/module/revision/caller[@organisation = $organisation and @name = $name]/../..">
            <xsl:sort select="revision/@position"/>
            <xsl:call-template name="runtime-module-library">
                <xsl:with-param name="organisation"><xsl:value-of select="@organisation"/></xsl:with-param>
                <xsl:with-param name="name"><xsl:value-of select="@name"/></xsl:with-param>
            </xsl:call-template>          
        </xsl:for-each>
        -->
    </xsl:template>

    <!-- Copy by default -->
    <xsl:template match="@*|*|processing-instruction()|comment()">
      <xsl:copy>
        <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
      </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
