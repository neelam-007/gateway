<?xml version="1.0" encoding="UTF-8"?>
<!--
	Layer 7 technology
	darmstrong 14/11/2008
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="yes" version="1.0" encoding="UTF-8" omit-xml-declaration="no"
                doctype-public="//JasperReports//DTD Report Design//EN"
                doctype-system="http://jasperreports.sourceforge.net/dtds/jasperreport.dtd"/>

    <xsl:param name="RuntimeDoc"/>

    <xsl:variable name="isContextMapping">
        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/isContextMapping" />
    </xsl:variable>


    <xsl:template match="jasperReport">
        <xsl:element name="jasperReport">
            <xsl:attribute name="pageHeight">
                <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/pageHeight"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()|@*[local-name()!='pageHeight']"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band">
        <xsl:choose>
            <xsl:when test="$isContextMapping = 1" >
                <xsl:element name="band">
                    <xsl:attribute name="height">0</xsl:attribute>
                    <xsl:apply-templates select="@*[local-name()!='height']"/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="band">
                    <xsl:attribute name="height"><xsl:value-of
                            select="$RuntimeDoc/JasperRuntimeTransformation/bandHeight" />
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--CONSTANT_MAPPING band height-->
    <xsl:template match="/jasperReport/group[@name='CONSTANT_MAPPING']/groupHeader/band">
        <xsl:choose>
            <xsl:when test="$isContextMapping = 0" >
                <xsl:element name="band">
                    <xsl:attribute name="height">0</xsl:attribute>
                    <xsl:apply-templates select="@*[local-name()!='height']"/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="band">
                    <xsl:attribute name="height"><xsl:value-of
                            select="$RuntimeDoc/JasperRuntimeTransformation/bandHeight" />
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--CONSTANT_MAPPING chart height-->
    <xsl:template match="/jasperReport/group[@name='CONSTANT_MAPPING']/groupHeader/band/frame[2]/reportElement">
        <xsl:if test="$isContextMapping = 1">
            <xsl:element name="reportElement">
                <xsl:attribute name="height"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartFrameHeight" />
                </xsl:attribute>
                <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_MAPPING']/groupHeader/band/frame[2]/stackedBarChart/chart/reportElement">
        <xsl:if test="$isContextMapping = 1">
            <xsl:element name="reportElement">
                <xsl:attribute name="height"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartHeight" />
                </xsl:attribute>
                <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_MAPPING']/groupHeader/band/frame[2]/frame/reportElement">
        <xsl:if test="$isContextMapping = 1">
            <xsl:element name="reportElement">
                <xsl:attribute name="height"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartLegendHeight" />
                </xsl:attribute>
                <xsl:attribute name="y"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartLegendFrameYPos" />
                </xsl:attribute>
                <xsl:apply-templates select="node()|@*[local-name()!='height' and local-name()!='y']"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_MAPPING']/groupHeader/band/frame[2]/frame/box">
        <xsl:if test="$isContextMapping = 1">
            <xsl:copy>
                <xsl:apply-templates select="node()|@*"/>
            </xsl:copy>
            <xsl:text>
            </xsl:text>
            <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/chartLegend/textField">
                <xsl:element name="textField">
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

    <!--CONSTANT_CHART chart height-->
    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/reportElement">
        <xsl:if test="$isContextMapping = 0">
            <xsl:element name="reportElement">
                <xsl:attribute name="height"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartFrameHeight" />
                </xsl:attribute>
                <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/stackedBarChart/chart/reportElement">
        <xsl:if test="$isContextMapping = 0">
            <xsl:element name="reportElement">
                <xsl:attribute name="height"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartHeight" />
                </xsl:attribute>
                <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/frame/reportElement">
        <xsl:if test="$isContextMapping = 0">
            <xsl:element name="reportElement">
                <xsl:attribute name="height"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartLegendHeight" />
                </xsl:attribute>
                <xsl:attribute name="y"><xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartLegendFrameYPos" />
                </xsl:attribute>
                <xsl:apply-templates select="node()|@*[local-name()!='height' and local-name()!='y']"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/frame/box">
        <xsl:if test="$isContextMapping = 0">
            <xsl:copy>
                <xsl:apply-templates select="node()|@*"/>
            </xsl:copy>
            <xsl:text>
            </xsl:text>
            <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/chartLegend/textField">
                <xsl:element name="textField">
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

</xsl:transform>