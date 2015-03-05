<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to update an IDEA project file.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Parameters -->
    <xsl:param name="data"/> <!-- file containing modules -->
    <xsl:param name="datalibs"/> <!-- file containing ivy module dependencies -->
    <xsl:param name="projectmeta"/> <!-- Javadoc URLs etc for libraries -->    
    <xsl:param name="idea.jdk">1.8</xsl:param>
    <xsl:param name="idea.javac.out">idea-classes</xsl:param>
    <xsl:param name="idea.ant.integration">true</xsl:param>

    <!-- Globals -->
    <xsl:output method="xml" encoding="utf-8" indent="yes"/>
    <xsl:variable name="project" select="/*"/>
    <xsl:variable name="projectmetadoc" select="document($projectmeta)"/>
    <xsl:variable name="modules" select="document($data)/modules"/>
    <xsl:variable name="module-libs" select="document($datalibs)/modules/modules"/>
    <xsl:key name="moduleid" match="module" use="concat(@organisation, '.', @name, '.', @rev)"/> 

    <!-- Enable/Disable ANT integration -->
    <xsl:template match="/project/component[@name = 'AntConfiguration']/buildFile[@url = 'file://$PROJECT_DIR$/etc/build/idea_build.xml']/executeOn[@event = 'afterCompilation']">
        <xsl:if test="$idea.ant.integration = 'true'">
            <xsl:copy>
                <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
            </xsl:copy>
        </xsl:if>
    </xsl:template>

    <!-- Update if not initialized -->
    <xsl:template match="/project/component[@name = 'ProjectRootManager' and @project-jdk-name = '']">
        <xsl:element name="component">
            <xsl:attribute name="project-jdk-name"><xsl:value-of select="$idea.jdk"/></xsl:attribute>

            <xsl:apply-templates select="@*[local-name() != 'project-jdk-name']|text()|processing-instruction()|comment()"/>

            <output url="file://$PROJECT_DIR$/{$idea.javac.out}"/>            
        </xsl:element>
    </xsl:template>

    <!-- Add global project libraries -->
    <xsl:template match="/project/component[@name = 'libraryTable']">
        <xsl:choose>
            <xsl:when test="$module-libs/module">
                <xsl:copy>
                    <xsl:apply-templates select="@*"/>

                    <xsl:for-each select="$module-libs/module[artifact/@type = 'jar']">
                      <xsl:variable name="module-name" select="concat(@organisation, '.', @name, '.', @rev)"/>
                      <xsl:choose>
                          <xsl:when test="@organisation != 'com.l7tech' and generate-id() = generate-id(key('moduleid',$module-name)[artifact/@type = 'jar'])">
                              <xsl:choose>
                                  <xsl:when test="/project/component[@name = 'libraryTable']/library[@name=$module-name]">
                                      <!--  Copy existing entry to preserve any configuration for Javadoc/Sources, etc -->
                                      <xsl:apply-templates select="/project/component[@name = 'libraryTable']/library[@name=$module-name]"/>
                                  </xsl:when>
                                  <xsl:otherwise>
                                      <!--  New entry with values from config -->
                                      <library name="{@organisation}.{@name}.{@rev}">
                                        <CLASSES>
                                            <root url="jar://{artifact/origin-location}!/" />
                                        </CLASSES>
                                        <xsl:call-template name="javadoc">
                                            <xsl:with-param name="libraryname"><xsl:value-of select="@organisation"/>.<xsl:value-of select="@name"/></xsl:with-param>
                                        </xsl:call-template>
                                      </library>
                                  </xsl:otherwise>
                              </xsl:choose>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <!-- If there are no modules then use whatever is currently in the library table -->
                <xsl:copy>
                    <xsl:apply-templates select="*[@name != 'scala-compiler']|@*|text()|processing-instruction()|comment()"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Add any new modules -->
    <xsl:template match="/project/component[@name = 'ProjectModuleManager']/modules">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>

            <xsl:for-each select="$modules/module">
                <xsl:sort select="@file"/>
                <xsl:variable name="file" select="@file"/>

                <xsl:if test="not($project/component[@name = 'ProjectModuleManager']/modules/module[@filepath = concat('$PROJECT_DIR$/',$file)])">
                    <xsl:choose>
                        <xsl:when test="starts-with(@file, 'modules/gateway/assertions/')">
                            <module group="Modular Assertions" filepath="$PROJECT_DIR$/{@file}"/>
                        </xsl:when>
                        <xsl:when test="starts-with(@file, 'modules\gateway\assertions\')">
                            <module group="Modular Assertions" filepath="$PROJECT_DIR$/{@file}"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <module group="Main" filepath="$PROJECT_DIR$/{@file}"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <!-- If there is a javadoc URL for a library then use it -->
    <xsl:template name="javadoc">
        <xsl:param name="libraryname"/>

        <xsl:if test="$projectmetadoc/layer7-project-info/library[@name = $libraryname and @javadoc-url]">
          <JAVADOC>
              <root url="{$projectmetadoc/layer7-project-info/library[@name = $libraryname]/@javadoc-url}" />
          </JAVADOC>
        </xsl:if>
    </xsl:template>

    <!-- Copy by default -->
    <xsl:template match="@*|*|processing-instruction()|comment()">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

