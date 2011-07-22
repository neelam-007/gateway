<?xml version="1.0" encoding="UTF-8"?>
<!--
  XSL Transformation to generate the Gateway Management WSDL from a resource description XML.
-->
<xsl:stylesheet version="1.0"
                exclude-result-prefixes="exslt xalan"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exslt="http://exslt.org/common"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:wxf="http://schemas.xmlsoap.org/ws/2004/09/transfer"
                xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration"
                xmlns:wse="http://schemas.xmlsoap.org/ws/2004/08/eventing"
                xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                xmlns="http://schemas.xmlsoap.org/wsdl/"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:wsoap12="http://schemas.xmlsoap.org/wsdl/soap12/"
                xmlns:wsman="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd">

    <xsl:output
            indent="yes"
            xalan:indent-amount="2"
            method="xml"/>

    <xsl:key name="elementsKey" match="element" use="." />                                            

    <xsl:template match="resourceFactories">
        <definitions targetNamespace="{@namespace}">
            <!-- XSL 1.0 hack to add a dynamic NS declaration for tns (value is the targetNamespace)-->
            <xsl:attribute name="tns:targetNamespace" namespace="{@namespace}">
                <xsl:value-of select="@namespace"/>
            </xsl:attribute>

            <xsl:comment> Imports </xsl:comment>
            <import namespace="http://schemas.xmlsoap.org/ws/2004/09/transfer"
                    location="http://schemas.xmlsoap.org/ws/2004/09/transfer/transfer.wsdl"/>
            <import namespace="http://schemas.xmlsoap.org/ws/2004/09/enumeration"
                    location="http://schemas.xmlsoap.org/ws/2004/09/enumeration/enumeration.wsdl"/>
            <import namespace="http://schemas.xmlsoap.org/ws/2004/08/eventing"
                    location="http://schemas.xmlsoap.org/ws/2004/08/eventing/eventing.wsdl"/>

            <types>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                           elementFormDefault="qualified"
                           blockDefault="#all">
                    <xs:import namespace="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd"
                               schemaLocation="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd"/>
                </xs:schema>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                           elementFormDefault="qualified"
                           blockDefault="#all">
                    <xs:import namespace="{@namespace}"
                               schemaLocation="{@schemaLocation}"/>
                </xs:schema>
            </types>

            <xsl:comment> WS-Management Messages </xsl:comment>
            <message name="ResourceURIMessage">
               <part name="ResourceURIPart" element="wsman:ResourceURI"/>
            </message>
            <message name="SelectorSetMessage">
               <part name="SelectorSetPart"  element="wsman:SelectorSet"/>
            </message>
            <message name="OptionSetMessage">
               <part name="OptionSetPart" element="wsman:OptionSet"/>
            </message>
            <message name="OperationTimeoutMessage">
               <part name="OperationTimeoutPart" element="wsman:OperationTimeout"/>
            </message>
            <message name="RequestTotalItemsCountEstimateMessage">
               <part name="RequestTotalItemsCountEstimatePart" element="wsman:RequestTotalItemsCountEstimate"/>
            </message>
            <message name="TotalItemsCountEstimateMessage">
               <part name="TotalItemsCountEstimatePart" element="wsman:TotalItemsCountEstimate"/>
            </message>
            <message name="FragmentTransferMessage">
               <part name="FragmentTransferPart" element="wsman:FragmentTransfer"/>
            </message>
            <xsl:comment> Resource Messages </xsl:comment>
            <xsl:apply-templates mode="messages" select="(resourceFactory/resource | resourceFactory/resourceMethod/request | resourceFactory/resourceMethod/response)[generate-id(element) = generate-id(key('elementsKey',element)[1])]">
                <xsl:sort select="element"/>
            </xsl:apply-templates>

            <xsl:comment> PortTypes </xsl:comment>
            <portType name="GatewayManagementPortType">
                <xsl:comment>  WS-Transfer Operations </xsl:comment>
                <operation name="Create">
                    <input message="wxf:AnyXmlMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/Create"/>
                    <output message="wxf:CreateResponseMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse"/>
                </operation>
                <operation name="Get">
                    <input message="wxf:EmptyMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/Get"/>
                    <output message="wxf:AnyXmlMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse"/>
                </operation>
                <operation name="Put">
                    <input message="wxf:AnyXmlMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/Put"/>
                    <output message="wxf:OptionalXmlMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/PutResponse"/>
                </operation>
                <operation name="Delete">
                    <input message="wxf:EmptyMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete"/>
                    <output message="wxf:EmptyMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/DeleteResponse"/>
                </operation>
                <xsl:if test="resourceFactory/resourceMethod">
                    <xsl:comment> Resource Operations </xsl:comment>
                </xsl:if>
                <xsl:apply-templates mode="portTypes" select="resourceFactory/resourceMethod">
                    <xsl:sort select="concat(../name,name)"/>
                </xsl:apply-templates>
                <xsl:comment>  WS-Enumeration Operations </xsl:comment>
                <operation name="EnumerateOp">
                    <input message="wsen:EnumerateMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate"/>
                    <output message="wsen:EnumerateResponseMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse"/>
                </operation>
                <operation name="PullOp">
                    <input message="wsen:PullMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull"/>
                    <output message="wsen:PullResponseMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/PullResponse"/>
                </operation>
                <operation name="RenewOp">
                    <input message="wsen:RenewMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Renew"/>
                    <output message="wsen:RenewResponseMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/RenewResponse"/>
                </operation>
                <operation name="GetStatusOp">
                    <input message="wsen:GetStatusMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/GetStatus"/>
                    <output message="wsen:GetStatusResponseMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/GetStatusResponse"/>
                </operation>
                <operation name="ReleaseOp">
                    <input message="wsen:ReleaseMessage"
                           wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Release"/>
                    <output message="wsen:ReleaseResponseMessage"
                            wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/enumeration/ReleaseResponse"/>
                </operation>
            </portType>

            <xsl:comment> Bindings </xsl:comment>
            <binding name="GatewayManagementBinding" type="tns:GatewayManagementPortType">
                <wsoap12:binding transport="http://schemas.xmlsoap.org/soap/http" style="document"/>

                <xsl:comment>  WS-Transfer Operations </xsl:comment>
                <operation name="Get">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/transfer/Get"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:header message="tns:FragmentTransferMessage" part="FragmentTransferPart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <operation name="Put">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/transfer/Put"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:header message="tns:FragmentTransferMessage" part="FragmentTransferPart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <operation name="Delete">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <operation name="Create">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/transfer/Create"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <xsl:if test="resourceFactory/resourceMethod">
                    <xsl:comment> Resource Operations </xsl:comment>
                </xsl:if>
                <xsl:apply-templates mode="bindings" select="resourceFactory/resourceMethod">
                    <xsl:sort select="concat(../name,name)"/>
                </xsl:apply-templates>
                <xsl:comment> WS-Enumeration Operations </xsl:comment>
                <operation name="EnumerateOp">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:header message="tns:RequestTotalItemsCountEstimateMessage" part="RequestTotalItemsCountEstimatePart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:header message="tns:TotalItemsCountEstimateMessage" part="TotalItemsCountEstimatePart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <operation name="PullOp">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:header message="tns:RequestTotalItemsCountEstimateMessage" part="RequestTotalItemsCountEstimatePart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:header message="tns:TotalItemsCountEstimateMessage" part="TotalItemsCountEstimatePart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <operation name="RenewOp">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Renew"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <operation name="GetStatusOp">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/enumeration/GetStatus"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
                <operation name="ReleaseOp">
                    <wsoap12:operation soapAction="http://schemas.xmlsoap.org/ws/2004/09/enumeration/Release"/>
                    <input>
                        <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                        <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                        <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                        <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                        <wsoap12:body use="literal"/>
                    </input>
                    <output>
                        <wsoap12:body use="literal"/>
                    </output>
                </operation>
            </binding>

            <xsl:comment> Services </xsl:comment>
            <service name="GatewayManagementService">
                <xsl:comment>
                   Service for management of the following resources:
                   <xsl:for-each select="resourceFactory">
                        <xsl:sort select="name"/>
                        <xsl:text>
                        </xsl:text><xsl:value-of select="../@namespace"/>/<xsl:value-of select="name"/>
                   </xsl:for-each><xsl:text>
    </xsl:text>
                </xsl:comment>
                <port name="GatewayManagementPort" binding="tns:GatewayManagementBinding">
                    <wsoap12:address location="{@address}"/>
                </port>
            </service>
        </definitions>
    </xsl:template>
    
    <xsl:template match="resource | request | response" mode="messages">
        <message name="{element}Message">
            <part name="{element}Part" element="tns:{element}"/>
        </message>
    </xsl:template>

    <xsl:template match="resourceMethod" mode="portTypes">
        <operation name="{../name}{name}">
            <xsl:choose>
                <xsl:when test="request/element">
                    <input message="tns:{request/element}Message"
                           wsa:Action="{../../@namespace}/{../name}/{name}" />
                </xsl:when>
                <xsl:otherwise>
                    <input message="wxf:EmptyMessage"
                           wsa:Action="{../../@namespace}/{../name}/{name}" />
                </xsl:otherwise>
            </xsl:choose>
            <output message="tns:{response/element}Message"
                   wsa:Action="{../../@namespace}/{../name}/{name}Response" />
       </operation>
    </xsl:template>
    
    <xsl:template match="resourceMethod" mode="bindings">
        <operation name="{../name}{name}">
            <wsoap12:operation soapAction="{../../@namespace}/{../name}/{name}"/>
            <input>
                <wsoap12:header message="tns:ResourceURIMessage" part="ResourceURIPart" use="literal" />
                <wsoap12:header message="tns:SelectorSetMessage" part="SelectorSetPart" use="literal" />
                <wsoap12:header message="tns:OptionSetMessage" part="OptionSetPart" use="literal" />
                <wsoap12:header message="tns:OperationTimeoutMessage" part="OperationTimeoutPart" use="literal" />
                <wsoap12:body use="literal"/>
            </input>
            <output>
                <wsoap12:body use="literal"/>
            </output>
        </operation>
    </xsl:template>

</xsl:stylesheet>