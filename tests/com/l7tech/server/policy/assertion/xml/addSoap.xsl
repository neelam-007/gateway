<?xml version='1.0'?>
<!--
	Layer 7 Technologies, inc
	July 2006
	Sample XSLT which adds a SOAP Envelope around some XML
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
	<xsl:template match="/">
		<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
			<soap:Body>
				<xsl:copy-of select="." />
			</soap:Body>
		</soap:Envelope>			
	</xsl:template>
</xsl:stylesheet>