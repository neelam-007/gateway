<?xml version="1.0" encoding="UTF-8"?>
<!--
	Layer 7 technology
	darmstrong 22/10/2008
	Transform a template JRXML usage report into a specific jrxml file for the supplied parameters.
	This involves creating a set of variables and textfields for the mapping values supplied as parameters, and setting
	width parameters which are determined at runtime.
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="yes" version="1.0" encoding="UTF-8" omit-xml-declaration="no"
                doctype-public="//JasperReports//DTD Report Design//EN"
                doctype-system="http://jasperreports.sourceforge.net/dtds/jasperreport.dtd"/>

    <xsl:param name="RuntimeDoc"/>
    <xsl:param name="PageMinWidth"/>

    <xsl:variable name="useDynamicWidths">
        <xsl:choose>
            <xsl:when test="$RuntimeDoc/JasperRuntimeTransformation/pageWidth &gt; $PageMinWidth ">
                <xsl:value-of select="1" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="0" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:template match="jasperReport">
        <xsl:element name="jasperReport">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1" >
                    <xsl:attribute name="columnWidth">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/pageWidth"/>
                    </xsl:attribute>
                    <xsl:attribute name="pageWidth">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/pageWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='columnWidth' and local-name()!='pageWidth']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="variable[@name='TOTAL']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
        <xsl:text>
        </xsl:text>
        <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/variables/variable">
            <xsl:element name="variable">
                <xsl:apply-templates select="node()|@*"/>
            </xsl:element>
            <xsl:text>
            </xsl:text>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='SERVICE_AND_OPERATION']/groupFooter/band/frame">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:text>
            </xsl:text>
            <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/serviceAndOperationFooter/textField">
                <xsl:element name="textField">
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/jasperReport/noData/band/frame">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:text>
            </xsl:text>
            <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/noData/staticText">
                <xsl:element name="staticText">
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="frame/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1" >
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/pageWidth" />
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    
</xsl:transform>