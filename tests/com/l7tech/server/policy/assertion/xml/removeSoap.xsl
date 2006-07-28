<?xml version='1.0'?>
<!--
	Layer 7 Technologies, inc
	July 2006
	Sample XSLT which removes a SOAP Envelope around some XML
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
	<xsl:template match="soap:Body">
			<xsl:copy-of select="./*[1]" />
	</xsl:template>
</xsl:stylesheet>