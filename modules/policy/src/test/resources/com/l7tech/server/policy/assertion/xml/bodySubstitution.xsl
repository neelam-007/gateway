<?xml version="1.0" encoding="UTF-8"?>
<!--
	Layer 7 technology
	flascell 08/12/2005
	This transformation strips out wsse:Security elements
	from the header of a soap envelop.
-->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                             xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
	<xsl:template match="soapenv:Body">
        <xsl:element name="Body" namespace="http://schemas.xmlsoap.org/soap/envelope/">
            <xsl:text>
            </xsl:text>
            <xsl:element name="accountid" namespace="http://warehouse.acme.com/accountns">
                <xsl:text>5643249816516813216</xsl:text>
            </xsl:element>
		    <xsl:apply-templates/>
        </xsl:element>
	</xsl:template>
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>
</xsl:transform>
