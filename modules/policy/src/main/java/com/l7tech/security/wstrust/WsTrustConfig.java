package com.l7tech.security.wstrust;

import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a version of WS-Trust, currently shared and immutable.
 */
public abstract class WsTrustConfig {
    private static final boolean deriveWsscNamespace = SyspropUtil.getBoolean( "com.l7tech.security.wstrust.deriveWsscNamespace", true ); // prior to 5.4.1 we did not derive the namespace
    private static final boolean useLegacyTokenUris = SyspropUtil.getBoolean( "com.l7tech.security.wstrust.useLegacyTokenUris", false ); // prior to 5.4.1 we used "old" token uris

    private final String wstNs;
    private final String wspNs;
    private final String wsaNs;
    private final String wsscNs;

    public WsTrustConfig( final String wstNs,
                          final String wspNs,
                          final String wsaNs) {
        this( wstNs, wspNs, wsaNs, deriveWsscNamespace ? deriveWsSecureConversationNamespace(wstNs) : SoapConstants.WSSC_NAMESPACE );
    }

    public WsTrustConfig( final String wstNs,
                          final String wspNs,
                          final String wsaNs,
                          final String wsscNs ) {
        this.wstNs = wstNs;
        this.wspNs = wspNs;
        this.wsaNs = wsaNs;
        this.wsscNs = wsscNs;
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

    public String getWsscNs() {
        return wsscNs;
    }

    /**
     * Derive the namespace of WS-Secure Conversation from a given namespace of WS-Trust.
     *
     * @param wstNamespace: the namespace of WS-Trust
     * @return a corresponding namespace of WS-Secure Conversation.
     * @throws IllegalArgumentException If the given namespace is null or unrecognized.
     */
    public static String deriveWsSecureConversationNamespace( final String wstNamespace ) {
        if (wstNamespace == null) throw new IllegalArgumentException("WS-Trust Namespace must be required.");

        String wscNamespace;
        if (SoapConstants.WST_NAMESPACE.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE;
        } else if (SoapConstants.WST_NAMESPACE2.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE2;
        } else if (SoapConstants.WST_NAMESPACE3.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE3;
        } else if (SoapConstants.WST_NAMESPACE4.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE3;
        } else {
            throw new IllegalArgumentException("Invalid WS-Trust namespace, " + wstNamespace);
        }

        return wscNamespace;
    }

    /**
     * @return the request type URI for the specified WsTrustRequestType for this WsTrustConfig's version of WS-Trust.
     *         Never null -- any valid WsTrustRequestType instance will produce a URI.
     */
    protected abstract String getRequestTypeUri(WsTrustRequestType requestType);

    protected Document makeRequestSecurityTokenResponseMessage(final String soapNs,
                                                               final String tokenString) throws SAXException {
        String start = "<soap:Envelope xmlns:soap=\"" + soapNs + "\">" +
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
    protected  Document makeRequestSecurityTokenMessage( final String soapNs,
                                                         final SecurityTokenType desiredTokenType,
                                                         final WsTrustRequestType requestType,
                                                         final String appliesToAddress,
                                                         final String wstIssuerAddress,
                                                         final byte[] entropy,
                                                         final int keySize,
                                                         final long lifetime,
                                                         final XmlSecurityToken base) throws SAXException {
        // TODO fix or remove this hack: if a saml: qname will be used, declare saml NS in root element
        String extraNs = "";
        if (desiredTokenType != null && SamlSecurityToken.class.isAssignableFrom(desiredTokenType.getInterfaceClass()))
            extraNs += " xmlns:saml=\"" + desiredTokenType.getWstPrototypeElementNs() + "\"";

        Document msg = XmlUtil.parse("<soap:Envelope xmlns:soap=\"" + soapNs + "\"" + extraNs + ">" +
                                                    "<soap:Header/><soap:Body>" +
                                                    "<wst:RequestSecurityToken xmlns:wst=\"" + getWstNs() + "\">" +
                                                    "</wst:RequestSecurityToken>" +
                                                    "</soap:Body></soap:Envelope>");
        Element env = msg.getDocumentElement();
        Element body = DomUtils.findFirstChildElementByName(env, env.getNamespaceURI(), "Body");
        Element rst = DomUtils.findFirstChildElement(body);

        // Add TokenType, if meaningful with this token type
        if (desiredTokenType != null) {
            final String tokenTypeUri = getTokenUri(desiredTokenType);
            if (tokenTypeUri != null) {
                // Add TokenType element
                Element tokenType = DomUtils.createAndAppendElementNS(rst, "TokenType", getWstNs(), "wst");
                tokenType.appendChild(DomUtils.createTextNode(msg, tokenTypeUri));
            }
        }

        // Add RequestType
        {
            final Element rt = DomUtils.createAndAppendElementNS(rst, "RequestType", rst.getNamespaceURI(), "wst");
            rt.appendChild(DomUtils.createTextNode(msg, getRequestTypeUri(requestType)));
        }

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

        // Add Entropy, if any
        if ( entropy != null ) {
            final Element entropyElement = DomUtils.createAndAppendElementNS(rst, "Entropy", getWstNs(), "wst");
            final Element binarySecretElement = DomUtils.createAndAppendElementNS(entropyElement, "BinarySecret", getWstNs(), "wst");
            String typeNonce = getWstNs() + "/Nonce";
            if ( SoapUtil.WST_NAMESPACE.equals( getWstNs() ) ) {
                typeNonce = SoapConstants.WST_BINARY_SECRET_NONCE_TYPE_URI;
            }
            binarySecretElement.setAttributeNS( null, "Type", typeNonce );
            DomUtils.setTextContent( binarySecretElement, HexUtils.encodeBase64( entropy, true ));
        }

        // Add KeySize
        if ( keySize > 64 ) {
            final Element keySizeElement = DomUtils.createAndAppendElementNS(rst, "KeySize", getWstNs(), "wst");
            DomUtils.setTextContent( keySizeElement, Integer.toString(keySize) );
        }

        // Add Lifetime
        if ( lifetime > 30000 ) {
            final Element lifetimeElement = DomUtils.createAndAppendElementNS(rst, "Lifetime", getWstNs(), "wst");
            final Element expiresElement = DomUtils.createAndAppendElementNS(lifetimeElement, "Expires", SoapConstants.WSU_NAMESPACE, "wsu");
            DomUtils.setTextContent( expiresElement, ISO8601Date.format(new Date(System.currentTimeMillis()+lifetime)));
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

        return msg;
    }

    protected String getTokenUri( final SecurityTokenType securityTokenType ) {
        String tokenUri;

        if ( useLegacyTokenUris ) {
            tokenUri = securityTokenType.getWstTokenTypeUri();
        } else if ( SecurityTokenType.SAML_ASSERTION == securityTokenType ) {
            tokenUri = SoapConstants.VALUETYPE_SAML4;
        } else if ( SecurityTokenType.SAML2_ASSERTION == securityTokenType ) {
            tokenUri = SoapConstants.VALUETYPE_SAML5;
        } else if ( SecurityTokenType.WSSC_CONTEXT == securityTokenType ) {
            if ( SoapConstants.WSSC_NAMESPACE.endsWith( getWsscNs() )) {
                tokenUri = SoapConstants.WSC_RST_SCT_TOKEN_TYPE;
            } else {
                tokenUri = getWsscNs() + "/sct";
            }
        } else {
            tokenUri = securityTokenType.getWstTokenTypeUri();
        }

        return tokenUri;
    }
}
