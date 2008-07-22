<?xml version="1.0" encoding="UTF-8"?>
<!--
	Layer 7 technology
	flascell 20/12/2005

	this allows you to appreciate a transformation by using the acme client and sending
	get details to gateway and get details for a different product than the one asked for
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                             xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                             xmlns:acme="http://warehouse.acme.com/ws">
	<!--<xsl:template match="acme:productid">
		<xsl:element name="productid" namespace="http://warehouse.acme.com/ws">
			<xsl:text>192822745</xsl:text>
		</xsl:element>
	</xsl:template>-->
    <xsl:template match="acme:productid/text()">
        <xsl:text>192822745</xsl:text>
	</xsl:template>

    <xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>
</xsl:transform>
