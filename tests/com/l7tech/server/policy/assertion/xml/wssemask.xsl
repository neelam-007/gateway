<?xml version="1.0" encoding="UTF-8"?>
<!--
	Layer 7 technology
	flascell 01/08/2002
	This transformation strips out wsse:Security elements
	from the header of a soap envelop.
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                             xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/04/secext"
                             xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
	<xsl:template match="/">
		<xsl:copy>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="wsse:Security">
		<xsl:comment>a wsse:Security element was stripped out</xsl:comment>
	</xsl:template>
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>
</xsl:transform>
