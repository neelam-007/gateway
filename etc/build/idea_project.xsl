<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to update an IDEA project file.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Parameters -->
    <xsl:param name="data"/> <!-- file containing ivy dependency report -->
    <xsl:param name="idea.jdk">1.6</xsl:param>
    <xsl:param name="idea.javac.out">idea-classes</xsl:param>

    <!-- Globals -->
    <xsl:output method="xml" encoding="utf-8" indent="yes"/>
    <xsl:variable name="project" select="/*"/>
    <xsl:variable name="modules" select="document($data)/modules"/>

    <!-- Update if not initialized -->
    <xsl:template match="/project/component[@name = 'ProjectRootManager' and @project-jdk-name = '']">
        <xsl:element name="component">
            <xsl:attribute name="project-jdk-name"><xsl:value-of select="$idea.jdk"/></xsl:attribute>

            <xsl:apply-templates select="@*[local-name() != 'project-jdk-name']|text()|processing-instruction()|comment()"/>

            <output url="file://$PROJECT_DIR$/{$idea.javac.out}"/>            
        </xsl:element>
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
                        <xsl:otherwise>
                            <module group="Main" filepath="$PROJECT_DIR$/{@file}"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <!-- Copy by default -->
    <xsl:template match="@*|*|processing-instruction()|comment()">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

