<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                             xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
	<xsl:template match="/">
		<xsl:copy>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="soapenv:Body">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
        <xsl:comment>SSG WAS HERE</xsl:comment>
	</xsl:template>
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>
</xsl:transform>
