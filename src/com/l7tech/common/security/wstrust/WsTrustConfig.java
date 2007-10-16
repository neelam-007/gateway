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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

            // Ensure all prefixes inherited from token's original context are available to the token
            Node n = tokenEl;
            Map<String, String> declaredTokenNamespaces = new HashMap<String, String>();
            Map<String, String> usedTokenNamespaces = new HashMap<String, String>();
            while (n != null) {
                if (n.getPrefix() != null && n.getNamespaceURI() != null)
                    usedTokenNamespaces.put(n.getPrefix(), n.getNamespaceURI());
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    NamedNodeMap attrs = n.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Attr attr = (Attr) attrs.item(i);
                        if ("xmlns".equalsIgnoreCase(attr.getPrefix())) {
                            declaredTokenNamespaces.put(attr.getLocalName(), attr.getValue());
                        } else if (attr.getPrefix() != null && attr.getNamespaceURI() != null) {
                            usedTokenNamespaces.put(attr.getPrefix(), attr.getNamespaceURI());
                        }
                    }
                }
                Node next = n.getNextSibling();
                if (next == null) next = n.getFirstChild();
                n = next;
            }

            for (String prefix : usedTokenNamespaces.keySet()) {
                String uri = usedTokenNamespaces.get(prefix);
                if (declaredTokenNamespaces.containsKey(prefix) && declaredTokenNamespaces.get(prefix).equals(uri)) {
                    // Already there
                } else {
                    String newPrefix = XmlUtil.findUnusedNamespacePrefix(tokenEl, prefix);
                    tokenEl.setAttribute("xmlns:" + newPrefix, uri);
                }
            }

            baseEl.appendChild(msg.importNode(tokenEl, true));
        }

        // Add RequestType
        {
            Element rt = XmlUtil.createAndAppendElementNS(rst, "RequestType", rst.getNamespaceURI(), "wst");
            rt.appendChild(XmlUtil.createTextNode(msg, getRequestTypeUri(requestType)));
        }

        return msg;
    }


}
