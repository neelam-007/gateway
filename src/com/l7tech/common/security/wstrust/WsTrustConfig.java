/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.wstrust;

import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.WsTrustRequestType;
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
    private String wsscNs = SoapUtil.WSSC_NAMESPACE;

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

    /**
     * @return the request type URI for the specified WsTrustRequestType for this WsTrustConfig's version of WS-Trust.
     *         Never null -- any valid WsTrustRequestType instance will produce a URI.
     */

    protected abstract String getRequestTypeUri(WsTrustRequestType requestType);



    protected Document makeRequestSecurityTokenResponseMessage(String tokenString) throws SAXException {
        String start = "<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">" +
                "<soap:Body>" +
                "<wst:RequestSecurityTokenResponse xmlns:wst=\"" + getWstNs() + "\" " +
                "xmlns:wsu=\"" + SoapUtil.WSU_NAMESPACE + "\" " +
                "xmlns:wsse=\"" + SoapUtil.SECURITY_NAMESPACE + "\" " +
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

        Document msg = XmlUtil.stringToDocument("<soap:Envelope xmlns:soap=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\"" + extraNs + ">" +
                                                    "<soap:Header/><soap:Body>" +
                                                    "<wst:RequestSecurityToken xmlns:wst=\"" + getWstNs() + "\">" +
                                                    "</wst:RequestSecurityToken>" +
                                                    "</soap:Body></soap:Envelope>");
        Element env = msg.getDocumentElement();
        Element body = XmlUtil.findFirstChildElementByName(env, env.getNamespaceURI(), "Body");
        Element rst = XmlUtil.findFirstChildElement(body);

        // Add AppliesTo, if provided
        if (appliesToAddress != null && appliesToAddress.length() > 0) {
            Element appliesTo = XmlUtil.createAndAppendElementNS(rst, "AppliesTo", getWspNs(), "wsp");
            Element endpointRef = XmlUtil.createAndAppendElementNS(appliesTo, "EndpointReference", getWsaNs(), "wsa");
            Element address = XmlUtil.createAndAppendElementNS(endpointRef, "Address", getWsaNs(), "wsa");
            address.appendChild(XmlUtil.createTextNode(address, appliesToAddress));
        }

        // Add Issuer, if provided
        if (wstIssuerAddress != null && wstIssuerAddress.length() > 0) {
            Element issuer = XmlUtil.createAndAppendElementNS(rst, "Issuer", getWstNs(), "wst");
            Element address = XmlUtil.createAndAppendElementNS(issuer, "Address", getWsaNs(), "wsa");
            address.appendChild(XmlUtil.createTextNode(address, wstIssuerAddress));
        }

        // Add TokenType, if meaningful with this token type
        if (desiredTokenType != null) {
            final String tokenTypeUri = desiredTokenType.getWstTokenTypeUri();
            if (tokenTypeUri != null) {
                // Add TokenType element
                Element tokenType = XmlUtil.createAndPrependElementNS(rst, "TokenType", getWstNs(), "wst");
                tokenType.appendChild(XmlUtil.createTextNode(msg, tokenTypeUri));
            }
        }

        // Add Base, if provided.  Base is not required to be the same token type as the token type we are requesting.
        if (base != null) {
            Element baseEl = XmlUtil.createAndPrependElementNS(rst, "Base", getWstNs(), "wst");
            Element tokenEl = base.asElement();
            if (tokenEl == null) throw new IllegalStateException("Couldn't get Element for Base security token");

            // it is not possible to tell which of the declared namespaces are "used"
            // so we just ensure that all declared namespaces are available in the
            // token request
            Map requiredNamespaces = XmlUtil.getNamespaceMap(tokenEl);
            Map declaredNamespaces = XmlUtil.getNamespaceMap(baseEl);

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
                            Attr nsAttribute = factory.createAttribute(XMLConstants.XMLNS_ATTRIBUTE);
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
            Element rt = XmlUtil.createAndAppendElementNS(rst, "RequestType", rst.getNamespaceURI(), "wst");
            rt.appendChild(XmlUtil.createTextNode(msg, getRequestTypeUri(requestType)));
        }

        return msg;
    }


}
