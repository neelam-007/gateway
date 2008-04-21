<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns="http://l7tech.com/xmlbench"
        >
<xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>

    <xsl:template match="/">
        <transformed>
            <xsl:copy-of select="."/>
        </transformed>
    </xsl:template>
</xsl:stylesheet>
