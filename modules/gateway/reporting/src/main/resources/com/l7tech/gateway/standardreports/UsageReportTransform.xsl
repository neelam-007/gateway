<?xml version="1.0" encoding="UTF-8"?>
<!--
	Layer 7 technology
	darmstrong 22/10/2008
	Transform a template JRXML usage report into a specific jrxml file for the supplied parameters.
	This involves creating a set of variables and textfields for the mapping values supplied as parameters, and setting
	width parameters which are determined at runtime.
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:param name="RuntimeDoc"/>
    <xsl:param name="FrameMinWidth"/>
    <xsl:param name="PageMinWidth"/>
    <!-- Frames within the title frame are slightly shorter due to formatting. Currently this value is 7-->
    <xsl:param name="TitleInnerFrameBuffer"/>

    <xsl:variable name="useDynamicWidths">
        <xsl:choose>
            <xsl:when test="$RuntimeDoc/JasperRuntimeTransformation/pageWidth &gt; $PageMinWidth ">
                <xsl:value-of select="1"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="0"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:output method="xml" indent="yes" version="1.0" encoding="UTF-8" omit-xml-declaration="no"
                doctype-public="//JasperReports//DTD Report Design//EN"
                doctype-system="http://jasperreports.sourceforge.net/dtds/jasperreport.dtd"/>


    <xsl:template match="jasperReport">

        <xsl:element name="jasperReport">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="columnWidth">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/columnWidth"/>
                    </xsl:attribute>
                    <xsl:attribute name="pageWidth">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/pageWidth"/>
                    </xsl:attribute>
                    <xsl:attribute name="leftMargin">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/leftMargin"/>
                    </xsl:attribute>
                    <xsl:attribute name="rightMargin">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/rightMargin"/>
                    </xsl:attribute>
                    <xsl:attribute name="pageHeight">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/pageHeight"/>
                    </xsl:attribute>
                    <xsl:apply-templates
                            select="node()|@*[local-name()!='columnWidth' and local-name()!='pageWidth' and local-name()!='leftMargin' and local-name()!='rightMargin' and local-name()!='pageHeight']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="pageHeight">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/pageHeight"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='pageHeight']"/>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:element>

    </xsl:template>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!-- Set the title width to match that of the data section-->
    <xsl:template match="/jasperReport/title/band/frame">
        <xsl:copy>
            <xsl:for-each select="reportElement">
                <xsl:element name="reportElement">
                    <xsl:choose>
                        <xsl:when test="$useDynamicWidths = 1">
                            <xsl:attribute name="width">
                                <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                            </xsl:attribute>
                            <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="node()|@*"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </xsl:for-each>
            <xsl:apply-templates select="node()[local-name()!='reportElement']|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/jasperReport/title/band/frame[2]/frame">
        <xsl:copy>
            <xsl:for-each select="reportElement">
                <xsl:element name="reportElement">
                    <xsl:choose>
                        <xsl:when test="$useDynamicWidths = 1">
                            <xsl:attribute name="width">
                                <xsl:value-of
                                        select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth - $TitleInnerFrameBuffer"/>
                            </xsl:attribute>
                            <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="node()|@*"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </xsl:for-each>
            <xsl:apply-templates select="node()[local-name()!='reportElement']|@*"/>
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

    <!-- Copy in the key info into the Usage Summary frame-->
    <xsl:template match="/jasperReport/group[@name='CONSTANT']/groupHeader/band/frame[1]/textField/textFieldExpression">
        <xsl:element name="textFieldExpression">
            <xsl:text>"Usage Summary data grouped by</xsl:text>
            <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/keyInfo"/>
            <xsl:text>"</xsl:text>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT']/groupHeader/band/frame[1]/textField/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <!-- //todo [Donal] turn this template into a function, as it's repeated several times in this file-->
    <xsl:template match="/jasperReport/group[@name='CONSTANT']/groupHeader/band/frame[*]/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[*]/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='SERVICE_ID']/groupHeader/band/frame/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
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

    <xsl:template match="/jasperReport/group[@name='SERVICE_AND_OPERATION']/groupFooter/band/frame/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
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

    <xsl:template match="/jasperReport/group[@name='SERVICE_ID']/groupFooter/band/frame/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
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

    <xsl:template match="/jasperReport/group[@name='CONSTANT']/groupFooter/band/frame/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <!-- Chart transforms-->
    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band">
        <xsl:element name="band">
            <xsl:attribute name="height">
                <xsl:value-of
                        select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/bandHeight"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
        </xsl:element>
    </xsl:template>

    <!--CONSTANT_MAPPING chart height-->
    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:attribute name="height">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartFrameHeight"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='width' and local-name()!='height']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="height">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartFrameHeight"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:element>
    </xsl:template>


    <xsl:template
            match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/barChart/chart/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:attribute name="height">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartHeight"/>
                    </xsl:attribute>

                    <xsl:apply-templates select="node()|@*[local-name()!='height' and local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="height">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartHeight"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='height']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/frame/reportElement">
        <xsl:element name="reportElement">
            <xsl:choose>
                <xsl:when test="$useDynamicWidths = 1">
                    <xsl:attribute name="width">
                        <xsl:value-of select="$RuntimeDoc/JasperRuntimeTransformation/frameWidth"/>
                    </xsl:attribute>
                    <xsl:attribute name="height">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartLegendHeight"/>
                    </xsl:attribute>
                    <xsl:attribute name="y">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartLegendFrameYPos"/>
                    </xsl:attribute>
                    <xsl:apply-templates
                            select="node()|@*[local-name()!='height' and local-name()!='y' and local-name()!='width']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="height">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartLegendHeight"/>
                    </xsl:attribute>
                    <xsl:attribute name="y">
                        <xsl:value-of
                                select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartLegendFrameYPos"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="node()|@*[local-name()!='height' and local-name()!='y']"/>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:element>
    </xsl:template>

    <xsl:template match="/jasperReport/group[@name='CONSTANT_CHART']/groupHeader/band/frame[2]/frame/box">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
        <xsl:text>
        </xsl:text>
        <xsl:for-each select="$RuntimeDoc/JasperRuntimeTransformation/chartElement/chartLegend/textField">
            <xsl:element name="textField">
                <xsl:apply-templates select="node()|@*"/>
            </xsl:element>
            <xsl:text>
            </xsl:text>
        </xsl:for-each>
    </xsl:template>

</xsl:transform>

