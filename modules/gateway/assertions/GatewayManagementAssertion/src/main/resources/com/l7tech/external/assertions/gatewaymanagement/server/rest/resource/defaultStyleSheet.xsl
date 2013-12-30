<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:l7="http://ns.l7tech.com/2010/04/gateway-management"
                version="1.0">
    <xsl:output method="html"/>

    <xsl:template match="l7:reference[@href]">
        <li><a href="{@href}"><xsl:apply-templates/></a></li>
    </xsl:template>

    <xsl:template match="/">
        <html><body><ul>
            <xsl:apply-templates/>
        </ul></body></html>
    </xsl:template>

</xsl:stylesheet>