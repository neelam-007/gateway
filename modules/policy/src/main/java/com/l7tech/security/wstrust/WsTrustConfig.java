/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.wstrust;

import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import javax.xml.XMLConstants;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a version of WS-Trust.
 */
public abstract class WsTrustConfig {
    private final String wstNs;
    private String wspNs;
    private String wsaNs;
    private String wsscNs = SoapConstants.WSSC_NAMESPACE;
    private String soapNs = SyspropUtil.getBoolean( "com.l7tech.security.wstrust.useSoap12", false ) ? 
            SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE :
            SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;

    public WsTrustConfig(String wstNs, String wspNs, String wsaNs) {
        this.wstNs = wstNs;
        this.wspNs = wspNs;
        this.wsaNs = wsaNs;
    }

    public String getWspNs() {
        return wspNs;
    }

    public String getWsaNs() {
        return wsaNs;
    }

    public String getWstNs() {
        return wstNs;
    }

    protected void setWsscNs(String wsscNs) {
        this.wsscNs = wsscNs;
    }

    public String getWsscNs() {
        return wsscNs;
    }

    public String getSoapNs() {
        return soapNs;
    }

    public void setSoapNs( String soapNs ) {
        this.soapNs = soapNs;
    }

    /**
     * @return the request type URI for the specified WsTrustRequestType for this WsTrustConfig's version of WS-Trust.
     *         Never null -- any valid WsTrustRequestType instance will produce a URI.
     */

    protected abstract String getRequestTypeUri(WsTrustRequestType requestType);



    protected Document makeRequestSecurityTokenResponseMessage(String tokenString) throws SAXException {
        String start = "<soap:Envelope xmlns:soap=\"" + getSoapNs() + "\">" +
                "<soap:Body>" +
                "<wst:RequestSecurityTokenResponse xmlns:wst=\"" + getWstNs() + "\" " +
                "xmlns:wsu=\"" + SoapConstants.WSU_NAMESPACE + "\" " +
                "xmlns:wsse=\"" + SoapConstants.SECURITY_NAMESPACE + "\" " +
                "xmlns:wsc=\"" + getWsscNs() + "\">" +
                "<wst:RequestedSecurityToken>";
        String middle = "</wst:RequestedSecurityToken>";
        String end = "</wst:RequestSecurityTokenResponse>" +
                "</soap:Body>" +
                "</soap:Envelope>";

        StringBuffer responseXml = new StringBuffer(start);
        responseXml.append(tokenString);
        responseXml.append(middle);
        responseXml.append(end);
        return XmlUtil.stringToDocument(responseXml.toString());
    }


    /** @return a SOAP envelope containing a RequestSecurityToken body. */
    protected  Document makeRequestSecurityTokenMessage(SecurityTokenType desiredTokenType,
                                                        WsTrustRequestType requestType,
                                                        String appliesToAddress,
                                                        String wstIssuerAddress,
                                                        XmlSecurityToken base)
            throws IOException, SAXException {
        // TODO fix or remove this hack: if a saml: qname will be used, declare saml NS in root element
        String extraNs = "";
        if (desiredTokenType != null && SamlSecurityToken.class.isAssignableFrom(desiredTokenType.getInterfaceClass()))
            extraNs += " xmlns:saml=\"" + desiredTokenType.getWstPrototypeElementNs() + "\"";

        Document msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + getSoapNs() + "\"" + extraNs + ">" +
                                                    "<soap:Header/><soap:Body>" +
                                                    "<wst:RequestSecurityToken xmlns:wst=\"" + getWstNs() + "\">" +
                                                    "</wst:RequestSecurityToken>" +
                                                    "</soap:Body></soap:Envelope>");
        Element env = msg.getDocumentElement();
        Element body = DomUtils.findFirstChildElementByName(env, env.getNamespaceURI(), "Body");
        Element rst = DomUtils.findFirstChildElement(body);

        // Add AppliesTo, if provided
        if (appliesToAddress != null && appliesToAddress.length() > 0) {
            Element appliesTo = DomUtils.createAndAppendElementNS(rst, "AppliesTo", getWspNs(), "wsp");
            Element endpointRef = DomUtils.createAndAppendElementNS(appliesTo, "EndpointReference", getWsaNs(), "wsa");
            Element address = DomUtils.createAndAppendElementNS(endpointRef, "Address", getWsaNs(), "wsa");
            address.appendChild(DomUtils.createTextNode(address, appliesToAddress));
        }

        // Add Issuer, if provided
        if (wstIssuerAddress != null && wstIssuerAddress.length() > 0) {
            Element issuer = DomUtils.createAndAppendElementNS(rst, "Issuer", getWstNs(), "wst");
            Element address = DomUtils.createAndAppendElementNS(issuer, "Address", getWsaNs(), "wsa");
            address.appendChild(DomUtils.createTextNode(address, wstIssuerAddress));
        }

        // Add TokenType, if meaningful with this token type
        if (desiredTokenType != null) {
            final String tokenTypeUri = desiredTokenType.getWstTokenTypeUri();
            if (tokenTypeUri != null) {
                // Add TokenType element
                Element tokenType = DomUtils.createAndPrependElementNS(rst, "TokenType", getWstNs(), "wst");
                tokenType.appendChild(DomUtils.createTextNode(msg, tokenTypeUri));
            }
        }

        // Add Base, if provided.  Base is not required to be the same token type as the token type we are requesting.
        if (base != null) {
            Element baseEl = DomUtils.createAndPrependElementNS(rst, "Base", getWstNs(), "wst");
            Element tokenEl = base.asElement();
            if (tokenEl == null) throw new IllegalStateException("Couldn't get Element for Base security token");

            // it is not possible to tell which of the declared namespaces are "used"
            // so we just ensure that all declared namespaces are available in the
            // token request
            Map requiredNamespaces = DomUtils.getNamespaceMap(tokenEl);
            Map declaredNamespaces = DomUtils.getNamespaceMap(baseEl);

            // import token to rst doc
            Element importedTokenElement = (Element) msg.importNode(tokenEl, true);

            // add any missing namespace decls
            NamedNodeMap attrs = importedTokenElement.getAttributes();
            Document factory = importedTokenElement.getOwnerDocument();
            for (Map.Entry<String,String> entry : (Set<Map.Entry<String,String>>) requiredNamespaces.entrySet()) {
                String prefix = entry.getKey();
                String namespace = entry.getValue();

                if ( !namespace.equals( declaredNamespaces.get(prefix) ) ) {
                    // then prefix is not in scope or is declared but for a
                    // different namespace
                    if ("".equals(prefix)) {
                        // Add NS if not redeclared on token element (default NS)
                        if (attrs.getNamedItem(XMLConstants.XMLNS_ATTRIBUTE) == null) {
                            Attr nsAttribute = factory.createAttributeNS(DomUtils.XMLNS_NS, XMLConstants.XMLNS_ATTRIBUTE);
                            nsAttribute.setValue(namespace);
                            attrs.setNamedItem(nsAttribute);    
                        }
                    } else {
                        // Add NS if not redeclared on token element
                        if (attrs.getNamedItemNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix) == null) {
                            Attr nsAttribute = factory.createAttributeNS(
                                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                    XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix);
                            nsAttribute.setValue(namespace);
                            attrs.setNamedItem(nsAttribute);
                        }
                    }
                }
            }

            baseEl.appendChild(importedTokenElement);
        }

        // Add RequestType
        {
            Element rt = DomUtils.createAndAppendElementNS(rst, "RequestType", rst.getNamespaceURI(), "wst");
            rt.appendChild(DomUtils.createTextNode(msg, getRequestTypeUri(requestType)));
        }

        return msg;
    }


}
