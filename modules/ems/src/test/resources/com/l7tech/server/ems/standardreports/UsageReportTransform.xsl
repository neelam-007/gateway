<?xml version="1.0" encoding="UTF-8"?>
<!--
	Layer 7 technology
	darmstrong 22/10/2008
	Transform a template JRXML usage report into a specific jrxml file for the supplied parameters.
	This involves creating a set of variables and textfields for the mapping values supplied as parameters
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:param name="RuntimeDoc"/>

    <xsl:output method="xml" indent="yes" version="1.0" encoding="UTF-8" omit-xml-declaration="no"
                doctype-public="//JasperReports//DTD Report Design//EN"
                doctype-system="http://jasperreports.sourceforge.net/dtds/jasperreport.dtd"/>


    <xsl:template match="jasperReport">

        <xsl:element name="jasperReport">
            <xsl:attribute name="columnWidth">
                <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/width"/>
            </xsl:attribute>
            <xsl:attribute name="leftMargin">
                <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/leftMargin"/>
            </xsl:attribute>
            <xsl:attribute name="rightMargin">
                <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/rightMargin"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()|@*[local-name()!='columnWidth' | local-name()!='leftMargin' | local-name()!='rightMargin']"/>
        </xsl:element>

    </xsl:template>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="variable[@name='SERVICE_ONLY_TOTAL']">
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


    <xsl:template match="/jasperReport/group[@name='CONSTANT']/groupHeader/band/frame[2]">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:text>
            </xsl:text>
            <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/constantHeader/textField">
                <xsl:element name="textField">
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        </xsl:copy>
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

    <xsl:template match="/jasperReport/group[@name='SERVICE_ID']/groupFooter/band/frame">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:text>
            </xsl:text>
            <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/serviceIdFooter/textField">
                <xsl:element name="textField">
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT']/groupFooter/band/frame">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            <xsl:text>
            </xsl:text>
            <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/constantFooter/textField">
                <xsl:element name="textField">
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>

</xsl:transform>

