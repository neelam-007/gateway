<?xml version="1.0" encoding="UTF-8"?>
<!--

	FILE wsil2xhtml.xslt
	LAYER 7 TECHNOLOGIES
	Author: flascell
	$Id$
	
	This transform takes a wsil doc as input and
	reformats it in html format so that recipient
	can click a <a> link to go to the wsdl for each
	service described in wsil.

-->
<xsl:transform version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:wilsns="http://schemas.xmlsoap.org/ws/2001/10/inspection/">
        
	<xsl:template match="wilsns:inspection">
		<html>
			<head>
				<title>Formatted WSIL</title>
			</head>
			<body>
				<xsl:apply-templates select="wilsns:service" />
			</body>
		</html>
	</xsl:template>
	
	<xsl:template match="wilsns:service">
		<h3><xsl:value-of select="wilsns:abstract" /></h3>
		<a>
			<xsl:attribute name="href">
				<xsl:value-of select="wilsns:description/@location" />
			</xsl:attribute>
			<xsl:text>wsdl</xsl:text>
		</a>
	</xsl:template>
	
</xsl:transform>
