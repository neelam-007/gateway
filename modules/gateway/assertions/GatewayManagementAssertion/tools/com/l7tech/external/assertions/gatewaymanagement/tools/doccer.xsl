<!--
    This is used to transform a wadl into an html document.
-->
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:wadl="http://wadl.dev.java.net/2009/02">
    <!-- HTML 5 output -->
    <xsl:output
            method="html"
            encoding="UTF-8"
            indent="yes"
            omit-xml-declaration="yes"/>

    <!-- matches the root application -->
    <xsl:template match="wadl:application">
        <div id="rest-api-docs">
            <h1>Resources</h1>
            <xsl:call-template name="table-of-contents"/>
            <xsl:apply-templates select="wadl:resources/wadl:resource">
                <!-- order by the resource name -->
                <xsl:sort select="concat(wadl:doc[@title='title-javadoc'], wadl:doc[@title='title-src'], @path)"/>
            </xsl:apply-templates>
        </div>
    </xsl:template>

    <xsl:template name="table-of-contents">
        <div class="table-of-contents">
            <p><strong>Page Contents</strong></p>
            <ul>
                <xsl:for-each select="wadl:resources/wadl:resource">
                    <!-- order by the resource name -->
                    <xsl:sort select="concat(wadl:doc[@title='title-javadoc'], wadl:doc[@title='title-src'], @path)"/>
                    <li>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="concat('#', @path)"/>
                            </xsl:attribute>
                            <xsl:call-template name="extract-name"/>
                        </a>
                    </li>
                </xsl:for-each>
            </ul>
        </div>
    </xsl:template>

    <!-- This template is used to extract the name from docs. It first looks for a name from a javadoc title, then a code title. Then it used the path -->
    <xsl:template name="extract-name">
        <xsl:param name="path" select="string(@path)"/>
        <xsl:choose>
            <xsl:when test="wadl:doc[@title='title-javadoc']">
                <xsl:value-of select="wadl:doc[@title='title-javadoc']"/>
            </xsl:when>
            <xsl:when test="wadl:doc[@title='title-src']">
                <xsl:value-of select="wadl:doc[@title='title-src']"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$path"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- This matched root resources -->
    <xsl:template match="wadl:resources/wadl:resource">
        <div class="resource">
            <a href="#" class="scrollToTop">Top</a>
            <a class="anchor">
                <xsl:attribute name="id">
                    <xsl:value-of select="@path"/>
                </xsl:attribute>
                <xsl:value-of select="@path"/>
            </a>
            <h3 class="resource-name">
                <xsl:call-template name="extract-name"/>
            </h3>
            <div class="resource-content">
                <div class="doc">
                    <xsl:apply-templates select="wadl:doc"/>
                </div>
                <xsl:apply-templates select="wadl:method|wadl:resource">
                    <xsl:with-param name="path" select="@path"/>
                    <!-- order by the method name -->
                    <xsl:sort select="concat(wadl:doc[@title='title-javadoc'], wadl:doc[@title='title-src'], @path)"/>
                </xsl:apply-templates>
            </div>
        </div>
    </xsl:template>

    <!-- Matches sub-resources. Here nothing needs to be output but the path needs to be calculated and passed on and params do too. -->
    <xsl:template match="wadl:resource/wadl:resource">
        <xsl:param name="path"/>
        <xsl:param name="path-params"/>
        <!-- collect any params-->
        <xsl:variable name="path-params-updated">
            <xsl:copy-of select="$path-params"/>
            <xsl:apply-templates select="wadl:param[@style='template']"/>
        </xsl:variable>
        <!-- Update the path and delegate to the method template. -->
        <xsl:apply-templates select="wadl:method|wadl:resource">
            <xsl:with-param name="path" select="concat($path, '/', @path)"/>
            <xsl:with-param name="path-params" select="$path-params-updated"/>
            <!-- order by the method name -->
            <xsl:sort select="concat(wadl:doc[@title='title-javadoc'], wadl:doc[@title='title-src'], @path)"/>
        </xsl:apply-templates>
    </xsl:template>

    <!-- This is the main template for an individual api call.-->
    <xsl:template match="wadl:resource/wadl:method">
        <xsl:param name="path"/>
        <xsl:param name="path-params"/>
        <div class="api-call">
            <h3 class="call-title">
                <xsl:call-template name="extract-name">
                    <xsl:with-param name="path" select="concat(@name, ' ', $path)"/>
                </xsl:call-template>
            </h3>
            <div class="api-call-content">
                <div class="doc">
                    <xsl:apply-templates select="wadl:doc"/>
                </div>
                <div class="request">
                    <h5 class="request-title">Request</h5>
                    <div class="request-body">
                        <div class="resource-path">
                            <xsl:value-of select="concat(@name, ' ', $path)"/>
                        </div>
                        <xsl:if test="wadl:request/wadl:param or $path-params != ''">
                            <xsl:if test="wadl:request/wadl:param[@style='template'] or $path-params != ''">
                                <table class="params-table" cellpadding="0" cellspacing="0">
                                    <caption>Path Parameters</caption>
                                    <tr>
                                        <th>Param</th>
                                        <th>Type</th>
                                        <th>Description</th>
                                    </tr>
                                    <xsl:if test="$path-params != ''">
                                        <xsl:copy-of select="$path-params"/>
                                    </xsl:if>
                                    <xsl:if test="wadl:request/wadl:param[@style='template']">
                                        <xsl:apply-templates select="wadl:request/wadl:param[@style='template']"/>
                                    </xsl:if>
                                </table>
                            </xsl:if>
                            <xsl:if test="wadl:request/wadl:param[@style='query']">
                                <table class="params-table" cellpadding="0" cellspacing="0">
                                    <caption>Query Parameters</caption>
                                    <tr>
                                        <th>Param</th>
                                        <th>Type</th>
                                        <th>Description</th>
                                    </tr>
                                    <xsl:apply-templates select="wadl:request/wadl:param[@style='query']"/>
                                </table>
                            </xsl:if>
                        </xsl:if>
                        <xsl:apply-templates select="wadl:request/wadl:representation"/>
                    </div>
                </div>
                <xsl:if test="wadl:response">
                    <xsl:apply-templates select="wadl:response"/>
                </xsl:if>
                <xsl:if test="not(wadl:response)">
                    <h5 class="response-title">Response</h5>
                    <div class="response-body">No Response Body</div>
                </xsl:if>
            </div>
        </div>
    </xsl:template>

    <!-- outputs documentation -->
    <xsl:template match="wadl:doc">
        <xsl:if test="not(starts-with(@title, 'title'))">
            <xsl:value-of select="text()" disable-output-escaping="yes"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="wadl:representation">
        <xsl:param name="body-doc"/>
        <h6 class="body-title">Body</h6>
        <xsl:if test="wadl:doc or $body-doc">
            <div class="body-doc">
                <xsl:copy-of select="$body-doc"/>
                <xsl:apply-templates select="wadl:doc"/>
            </div>
        </xsl:if>
        <xsl:if test="@element or @mediaType">
            <table class="body-info-table">
                <xsl:if test="@element">
                    <tr>
                        <td class="body-info">Element</td>
                        <td><xsl:apply-templates select="@element"/></td>
                    </tr>
                </xsl:if>
                <xsl:if test="@mediaType">
                    <tr>
                        <td class="body-info">Content-Type</td>
                        <td><xsl:apply-templates select="@mediaType"/></td>
                    </tr>
                </xsl:if>
            </table>
        </xsl:if>
    </xsl:template>

    <!-- Adds a parameter to the parameter table. -->
    <xsl:template match="wadl:param">
        <tr>
            <td>
                <xsl:value-of select="@name"/>
            </td>
            <td>
                <xsl:if test="wadl:option">
                    <ul class="param-choice-list">
                        <xsl:for-each select="wadl:option">
                            <li>
                                <xsl:value-of select="@value"/>
                            </li>
                        </xsl:for-each>
                    </ul>
                </xsl:if>
                <xsl:if test="not(wadl:option)">
                    <xsl:value-of select="substring-after(@type,':')"/>
                </xsl:if>
            </td>
            <td>
                <xsl:apply-templates select="wadl:doc"/>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="wadl:response">
        <h5 class="response-title">Response</h5>
        <xsl:if test="wadl:representation or wadl:doc">
            <div class="response-body">
                <xsl:apply-templates select="wadl:representation">
                    <xsl:with-param name="body-doc" select="wadl:doc"/>
                </xsl:apply-templates>
                <xsl:if test="not(wadl:representation)">
                    <h6 class="body-title">Body</h6>
                    <div class="body-doc">
                        <xsl:apply-templates select="wadl:doc"/>
                    </div>
                </xsl:if>
            </div>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>