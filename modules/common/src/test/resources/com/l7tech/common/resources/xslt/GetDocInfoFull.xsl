<?xml version='1.0'?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output encoding="UTF-8" cdata-section-elements="Name Headline Analyst PrimaryTicker Tickers" method="xml"/>
	<xsl:template match="/">
		<Clara>
			<Documents>
				<xsl:apply-templates/>
			</Documents>
		</Clara>
	</xsl:template>


	<xsl:template match="docSearch_diDef_0_0/diDef">
		<Document ID="{@docID}" clientID="{@localCode}">
			<Headline><xsl:value-of select="headline"/></Headline>
			<Dates>
				<Release><xsl:call-template name="FormatDateTime"><xsl:with-param name="thedate" select="@releaseDate"/></xsl:call-template></Release>
				<Submit><xsl:call-template name="FormatDateTime"><xsl:with-param name="thedate" select="@submitDate"/></xsl:call-template></Submit>
				<Update></Update>
			</Dates>
			<Contributor ID="{@ctbID}">
				<Name><xsl:value-of select="@companyName"/></Name>
			</Contributor>
			<Entitlement>
				<xsl:for-each select="grp">
					<DocumentGroups><xsl:value-of select="."/></DocumentGroups>
				</xsl:for-each>
			</Entitlement>
			<Analysts>
				<xsl:for-each select="author">
					<xsl:if test="not(text() = '**********')">
						<Analyst ID="{@c}"><xsl:value-of select="."/></Analyst>
					</xsl:if>
				</xsl:for-each>
			</Analysts>
			<Tags>
				<Type><xsl:value-of select="@docClass"/></Type>
				<PrimaryTicker><xsl:value-of select="pTkr"/></PrimaryTicker>
				<Tickers>
					<xsl:for-each select="tkr"><xsl:value-of select="."/><xsl:if test="position() != last()">,</xsl:if></xsl:for-each>
				</Tickers>
				<Industries IDs=""></Industries>
			</Tags>
		</Document>
	</xsl:template>
	
	<xsl:template name="FormatDateTime">
		<xsl:param name="thedate"/>
		<xsl:variable name="rYear" select="substring-before($thedate,'-')"/>
		<xsl:variable name="rMonth" select="substring-before(substring-after($thedate,concat($rYear,'-')),'-')"/>
		<xsl:variable name="rDay" select="substring-before(substring-after($thedate,concat($rYear,'-',$rMonth,'-')),'T')"/>
		
		<xsl:variable name="timebit" select="substring-after($thedate,'T')"/>
		<xsl:variable name="rHour" select="substring-before($timebit,':')"/>
		<xsl:variable name="rMin" select="substring-before(substring-after($timebit,concat($rHour,':')),':')"/>
		<xsl:variable name="rSec" select="substring-after($timebit, concat($rHour,':',$rMin,':'))"/>
		
		<xsl:value-of select="concat($rMonth,'/',$rDay,'/',$rYear,' ',$rHour,':',$rMin,':',$rSec)"/>
	</xsl:template>
</xsl:stylesheet>