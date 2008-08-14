<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transform to update an IDEA module containing only runtime dependencies.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Parameters for customizing options -->
    <xsl:param name="data"/> <!-- file containing ivy dependency report -->

    <!-- Global settings -->
    <xsl:output method="xml" encoding="utf-8" indent="yes"/>
    <xsl:variable name="module" select="/*"/>
    <xsl:variable name="dependencies" select="document($data)/ivy-report/dependencies"/>

    <!-- Process the main module component -->
    <xsl:template match="/module/component[@name = 'NewModuleRootManager']">
        <!-- copy existing -->
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>

            <!-- Add any new libraries -->
            <xsl:for-each select="$dependencies/module">
                <xsl:sort select="revision/@position"/>
                <xsl:if test="revision[@default = 'false' and contains(@conf,'runtime') and not(contains(@conf,'default'))]">
                    <xsl:call-template name="process-library">
                        <xsl:with-param name="library-name" select="concat(@organisation,'.',@name)"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:for-each>

        </xsl:copy>
    </xsl:template>

    <!-- Add module library only if it not present -->
    <xsl:template name="process-library">
        <xsl:param name="library-name"/>

        <!-- Ensure this does not exist in the current list of modules -->
        <xsl:if test="not($module/component[@name = 'NewModuleRootManager']/orderEntry[@type = 'module-library']/library[@name = $library-name])">
            <orderEntry type="module-library">
                <library name="{$library-name}">
                    <CLASSES>
                        <xsl:for-each select="revision[@default = 'false' and contains(@conf,'runtime') and not(contains(@conf,'default'))]/artifacts/artifact">
                            <root url="jar://{@location}!/"/>
                        </xsl:for-each>
                    </CLASSES>
                    <JAVADOC/>
                    <SOURCES/>
                </library>
            </orderEntry>
        </xsl:if>
    </xsl:template>

    <!-- Copy by default -->
    <xsl:template match="@*|*|processing-instruction()|comment()">
      <xsl:copy>
        <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
      </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
