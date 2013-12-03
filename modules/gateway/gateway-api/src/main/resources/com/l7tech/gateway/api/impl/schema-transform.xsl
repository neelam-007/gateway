<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL-T for modification to JAX-B generated XML Schema
-->
<xsl:stylesheet version="1.0" 
                exclude-result-prefixes="xsl xalan"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xalan="http://xml.apache.org/xalan">

    <xsl:output method="xml" encoding="utf-8" indent="yes" xalan:indent-amount="2"/>
    <xsl:strip-space elements="*"/>
    
    <!--
      ExtensionType should only allow targetNamespace for elements.
      JAXB does not allow the namespace for @XmlAnyElement to be specified.
    -->
    <xsl:template match="xs:any[@namespace = '##other' and ../../@name = 'ExtensionType' and local-name(../..) = 'complexType']">
        <xs:any processContents="lax" namespace="##targetNamespace" minOccurs="1" maxOccurs="unbounded"/>
    </xsl:template>
                                     
    <!--
      Translate attribute wildcard to allow any namespace rather than other.
      JAXB does not allow the namespace to be specified for @XmlAnyAttribute.
    -->
    <xsl:template match="xs:anyAttribute[@namespace = '##other']">
        <xs:anyAttribute namespace="##any" processContents="skip"/>
    </xsl:template>

    <!--
      Transform property types to allow attribute extension.
      A JAXB bug omits the anyAttribute output when an @XmlValue is used.
      (https://jaxb.dev.java.net/issues/show_bug.cgi?id=738)
    -->
    <xsl:template match="xs:simpleType[@name = 'StringPropertyType' or
                                       @name = 'BooleanPropertyType' or
                                       @name = 'IntegerPropertyType' or
                                       @name = 'LongPropertyType' or
                                       @name = 'BigIntegerPropertyType' or
                                       @name = 'BinaryPropertyType' or
                                       @name = 'StringValueType' or
                                       @name = 'BooleanValueType' or
                                       @name = 'IntegerValueType' or
                                       @name = 'LongValueType' or
                                       @name = 'ValidationStatusPropertyType' or
                                       @name = 'IdentityProviderTypePropertyType' or
                                       @name = 'PolicyTypePropertyType' or
                                       @name = 'JMSProviderTypePropertyType' or
                                       @name = 'ClientAuthenticationPropertyType']">
        <xs:complexType name="{@name}">
            <xs:simpleContent>
                <xs:extension base="{xs:restriction/@base}">
                    <xs:anyAttribute namespace="##other" processContents="skip"/>
                </xs:extension>
            </xs:simpleContent>
        </xs:complexType>
    </xsl:template>

    <xsl:template match="xs:extension[local-name(..) = 'simpleContent' and local-name(../..) = 'complexType' and (
                         ../../@name = 'AssertionDetailType' or
                         ../../@name = 'ResourceType')]">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
            <xs:anyAttribute namespace="##any" processContents="skip"/>
        </xsl:copy>
    </xsl:template>
    
    <!--
      Sort by name elements first then types. Import item need to come before Element items
    -->
    <xsl:template match="/*">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="xs:import" mode="spaced"/>
            <xsl:apply-templates select="xs:element" mode="spaced">
                <xsl:sort select="@name"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="node()[local-name() != 'element' and local-name() != 'import']" mode="spaced">
                <xsl:sort select="@name"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!--
      Add space between top level elements
    -->
    <xsl:template match="*" mode="spaced">
        <xsl:text>
            
  </xsl:text>
        <xsl:apply-templates select="."/>
    </xsl:template>

    <!--Remove FioranoMQ from schema in 7.1-->
    <xsl:template match="xs:enumeration[@value = 'FioranoMQ']" />

    <!--
      Copy by default
    -->
    <xsl:template match="@*|*|processing-instruction()|comment()">
        <xsl:if test="local-name(.) = 'simpleType' and not(xs:restriction)">
            <xsl:message>Detected non-extensible simple type '<xsl:value-of select="@name"/>'</xsl:message>
        </xsl:if>
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()|processing-instruction()|comment()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>