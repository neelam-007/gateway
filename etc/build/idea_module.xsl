<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to update an IDEA module.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Parameters for customizing options -->
    <xsl:param name="build"/> <!-- file containing ANT build for module -->
    <xsl:param name="data"/> <!-- file containing ivy dependency report -->
    <xsl:param name="modulemeta"/> <!-- Javadoc URLs etc for libraries -->
    <xsl:param name="source">false</xsl:param>
    <xsl:param name="tests">false</xsl:param>
    <xsl:param name="source.resources">false</xsl:param>
    <xsl:param name="test.resources">false</xsl:param>
    <xsl:param name="build.output">false</xsl:param>
    <xsl:param name="scope"/><!-- default scope for library dependencies -->

    <!-- Global settings -->
    <xsl:output method="xml" encoding="utf-8" indent="yes"/>
    <xsl:variable name="ivy-report" select="document($data)/ivy-report"/>
    <xsl:variable name="ivy-module-report" select="document($data)/modules"/>
    <xsl:variable name="modulemetadoc" select="document($modulemeta)"/>
    <xsl:variable name="idea-modules" select="/module/component[@name = 'NewModuleRootManager']"/>
    <xsl:variable name="build-properties" select="document($build)/project"/>

    <!-- Process the main module component -->
    <xsl:template match="/module/component[@name = 'NewModuleRootManager']">
        <!-- copy existing -->
        <xsl:copy>
            <xsl:if test="$build-properties/property[@name = 'module.compile.source']">
                <xsl:choose>
                    <xsl:when test="$build-properties/property[@name = 'module.compile.source' and @value = '1.3']">
                        <xsl:attribute name="LANGUAGE_LEVEL">JDK_1_3</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="$build-properties/property[@name = 'module.compile.source' and @value = '1.4']">
                        <xsl:attribute name="LANGUAGE_LEVEL">JDK_1_4</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="$build-properties/property[@name = 'module.compile.source' and @value = '1.5']">
                        <xsl:attribute name="LANGUAGE_LEVEL">JDK_1_5</xsl:attribute>
                    </xsl:when>
                </xsl:choose>
            </xsl:if>

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
                <xsl:if test="'true' = $source or 'true' = $tests or 'true' = $source.resources or 'true' = $test.resources or 'true' = $build.output">
                    <excludeFolder url="file://$MODULE_DIR$/build" />
                </xsl:if>
            </content>
            <orderEntry type="inheritedJdk" />
            <orderEntry type="sourceFolder" forTests="false" />

            <xsl:choose>
                <xsl:when test="$ivy-report">
                    <!-- Use module libraries this uses the regular IVY dependency report -->

                    <!-- Direct Module Dependencies -->
                    <xsl:for-each select="$ivy-report/dependencies/module">
                        <xsl:sort select="revision/@position"/>
                        <xsl:if test="revision/caller[@name = $ivy-report/info/@module and @organisation = $ivy-report/info/@organisation]">
                          <xsl:choose>
                              <xsl:when test="@organisation = 'com.l7tech'">
                                <orderEntry type="module" module-name="{@name}"  exported=""/>
                              </xsl:when>
                              <xsl:otherwise>
                                <orderEntry type="module-library"  exported="">
                                  <xsl:if test="$scope and $scope != ''">
                                      <xsl:attribute name="scope"><xsl:value-of select="$scope"/></xsl:attribute>
                                  </xsl:if>
                                  <library name="{@organisation}.{@name}">
                                    <CLASSES>
                                        <!-- direct jars -->
                                        <xsl:for-each select="revision/artifacts/artifact">
                                          <root url="jar://{@location}!/" />
                                        </xsl:for-each>
                                        <!-- dependency jars -->
                                        <xsl:call-template name="library">
                                            <xsl:with-param name="organisation"><xsl:value-of select="@organisation"/></xsl:with-param>
                                            <xsl:with-param name="name"><xsl:value-of select="@name"/></xsl:with-param>
                                        </xsl:call-template>
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

                    <!-- Runtime Dependencies -->
                    <!--<xsl:for-each select="$ivy-report/dependencies/module">-->
                        <!--<xsl:sort select="revision/@position"/>-->
                        <!--<xsl:if test="revision/caller[@name = $ivy-report/info/@module and @organisation = $ivy-report/info/@organisation]">-->
                        <!--<xsl:if test="not(revision[@default = 'false']/caller[@name = $ivy-report/info/@module and @organisation = $ivy-report/info/@organisation])">-->
                            <!--<xsl:call-template name="runtime-module-library">-->
                                <!--<xsl:with-param name="organisation"><xsl:value-of select="@organisation"/></xsl:with-param>-->
                                <!--<xsl:with-param name="name"><xsl:value-of select="@name"/></xsl:with-param>-->
                            <!--</xsl:call-template>-->
                        <!--</xsl:if>-->
                    <!--</xsl:for-each>-->
                </xsl:when>
                <xsl:otherwise>
                    <!-- Reference libraries in the project, this uses the IVY artifact report -->
                    <xsl:for-each select="$ivy-module-report/module">
                      <xsl:choose>
                          <xsl:when test="@organisation = 'com.l7tech'">
                            <orderEntry type="module" module-name="{@name}" exported=""/>
                          </xsl:when>
                      </xsl:choose>
                    </xsl:for-each>

                    <xsl:for-each select="$ivy-module-report/module[artifact/@type = 'jar']">
                      <xsl:choose>
                          <xsl:when test="@organisation != 'com.l7tech'">
                              <orderEntry type="library" name="{@organisation}.{@name}.{@rev}" level="project">
                                  <xsl:if test="$scope and $scope != ''">
                                      <xsl:attribute name="scope"><xsl:value-of select="$scope"/></xsl:attribute>
                                  </xsl:if>
                              </orderEntry>
                          </xsl:when>
                      </xsl:choose>
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="library">
        <xsl:param name="organisation"/>
        <xsl:param name="name"/>

        <xsl:for-each select="$ivy-report/dependencies/module">
            <xsl:if test="revision[caller/@organisation = $organisation and caller/@name = $name]">
                <!-- direct jars -->
                <xsl:for-each select="revision/artifacts/artifact">
                    <root url="jar://{@location}!/" />
                </xsl:for-each>

                <!-- dependency jars -->
                <xsl:call-template name="library">
                    <xsl:with-param name="organisation"><xsl:value-of select="@organisation"/></xsl:with-param>
                    <xsl:with-param name="name"><xsl:value-of select="@name"/></xsl:with-param>
                </xsl:call-template>
            </xsl:if>
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

        <orderEntry type="module-library">
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
