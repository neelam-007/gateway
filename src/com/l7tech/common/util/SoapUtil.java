/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.common.util;

import com.l7tech.common.audit.Audit;
import com.l7tech.common.audit.CommonMessages;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.xml.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class SoapUtil {
    public static final Logger log = Logger.getLogger(SoapUtil.class.getName());
    public static final List<String> ENVELOPE_URIS = new ArrayList<String>();
    private static final QName[] EMPTY_QNAME_ARRAY = new QName[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    static {
        ENVELOPE_URIS.add(SOAPConstants.URI_NS_SOAP_ENVELOPE);
        ENVELOPE_URIS.add("http://www.w3.org/2001/06/soap-envelope");
        ENVELOPE_URIS.add("http://www.w3.org/2001/09/soap-envelope");
        ENVELOPE_URIS.add("http://www.w3.org/2003/05/soap-envelope");         
        ENVELOPE_URIS.add("urn:schemas-xmlsoap-org:soap.v1");
    }

    // Namespace prefix constants
    public static final String SOAP_ENV_PREFIX = "soapenv";
    public static final String XMLNS = "xmlns";
    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";

    // Namespace constants
    public static final String SECURITY_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";
    public static final String SECURITY_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2002/07/secext";
    public static final String SECURITY_NAMESPACE4 = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String SECURITY_NAMESPACE5 = "http://schemas.xmlsoap.org/ws/2003/06/secext";
    public static final String SECURITY11_NAMESPACE = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";
    //public static final String OLD_SECURITY11_NAMESPACE = "http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-wssecurity-secext-1.1.xsd";
    public static final String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String DIGSIG_URI = DsigUtil.DIGSIG_URI;
    public static final String WSU_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String WSU_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    public static final String WSU_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2003/06/utility";
    public static final String WSSC_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/04/sc";
    public static final String WSSC_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2005/02/sc";
    public static final String[] WSSC_NAMESPACE_ARRAY = {
        WSSC_NAMESPACE,
        WSSC_NAMESPACE2,
    };

    public static final String WST_NAMESPACE  = "http://schemas.xmlsoap.org/ws/2004/04/trust";
    public static final String WST_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2005/02/trust"; // FIM
    public static final String[] WST_NAMESPACE_ARRAY = {
        WST_NAMESPACE,
        WST_NAMESPACE2,   // Seen in Tivoli Fim example messages
    };
    public static final String WST_REQUESTSECURITYTOKEN = "RequestSecurityToken";
    public static final String WST_TOKENTYPE = "TokenType";
    public static final String WST_REQUESTTYPE = "RequestType";

    public static final String WSX_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/03/mex";

    public static final String WSP_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/12/policy";
    public static final String WSP_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2004/09/policy"; // FIM
    public static final String[] WSP_NAMESPACE_ARRAY = {
        WSP_NAMESPACE,
        WSP_NAMESPACE2,    // Seen in Tivoli Fim example messages
    };

    public static final String WSA_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/03/addressing";
    public static final String WSA_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2004/08/addressing"; // FIM
    public static final String[] WSA_NAMESPACE_ARRAY = {
        WSA_NAMESPACE,
        WSA_NAMESPACE2,    // Seen in Tivoli Fim example messages
    };

    public static final String L7_MESSAGEID_NAMESPACE = "http://www.layer7tech.com/ws/addr";
    public static final String L7_MESSAGEID_PREFIX = "L7a";
    public static final String L7_SERVICEID_ELEMENT = "ServiceId";
    public static final String L7_POLICYVERSION_ELEMENT = "PolicyVersion";

    public static final List<String> SECURITY_URIS = new ArrayList<String>();

    static {
        SECURITY_URIS.add(SECURITY_NAMESPACE);
        SECURITY_URIS.add(SECURITY_NAMESPACE2);
        SECURITY_URIS.add(SECURITY_NAMESPACE3);
        SECURITY_URIS.add(SECURITY_NAMESPACE4);
        SECURITY_URIS.add(SECURITY_NAMESPACE5);
    }

    public static final String[] SECURITY_URIS_ARRAY = (String[])SECURITY_URIS.toArray(EMPTY_STRING_ARRAY);

    public static final String WSU_PREFIX = "wsu";
    public static final List<String> WSU_URIS = new ArrayList<String>();

    static {
        WSU_URIS.add(WSU_NAMESPACE);
        WSU_URIS.add(WSU_NAMESPACE2);
        WSU_URIS.add(WSU_NAMESPACE3);
    }

    public static final String[] WSU_URIS_ARRAY = (String[])WSU_URIS.toArray(EMPTY_STRING_ARRAY);

    // Attribute names
    public static final String ID_ATTRIBUTE_NAME = "Id";
    public static final String ACTOR_ATTR_NAME = "actor";  // SOAP 1.1
    public static final String ROLE_ATTR_NAME = "role";    // SOAP 1.2
    public static final String MUSTUNDERSTAND_ATTR_NAME = "mustUnderstand"; // SOAP 1.1+
    public static final String REFERENCE_URI_ATTR_NAME = "URI";
    public static final String UNTOK_PSSWD_TYPE_ATTR_NAME = "Type";

    // Element names
    public static final String ENVELOPE_EL_NAME = "Envelope";
    public static final String BODY_EL_NAME = "Body";
    public static final String HEADER_EL_NAME = "Header";
    public static final String SECURITY_EL_NAME = "Security";
    public static final String SIGNATURE_EL_NAME = "Signature";
    public static final String SIGNED_INFO_EL_NAME = "SignedInfo";
    public static final String REFERENCE_EL_NAME = "Reference";
    public static final String SECURITY_CONTEXT_TOK_EL_NAME = "SecurityContextToken";
    public static final String SECURITYTOKENREFERENCE_EL_NAME = "SecurityTokenReference";
    public static final String BINARYSECURITYTOKEN_EL_NAME = "BinarySecurityToken";
    public static final String KEYIDENTIFIER_EL_NAME = "KeyIdentifier";
    public static final String ENCRYPTEDKEY_EL_NAME = "EncryptedKey";
    public static final String TIMESTAMP_EL_NAME = "Timestamp";
    public static final String CREATED_EL_NAME = "Created";
    public static final String EXPIRES_EL_NAME = "Expires";
    public static final String USERNAME_TOK_EL_NAME = "UsernameToken";
    public static final String UNTOK_USERNAME_EL_NAME = "Username";
    public static final String UNTOK_PASSWORD_EL_NAME = "Password";
    public static final String MESSAGEID_EL_NAME = "MessageID";
    public static final String RELATESTO_EL_NAME = "RelatesTo";
    public static final String WSSC_ID_EL_NAME = "Identifier";
    public static final String WSSC_DK_EL_NAME = "DerivedKeyToken";
    public static final String REFLIST_EL_NAME = "ReferenceList";
    public static final String DATAREF_EL_NAME = "DataReference";
    public static final String KINFO_EL_NAME = "KeyInfo";

    // Misc
    public static final String SOAPACTION = "SOAPAction";
    public static final String SUPPORTED_ENCRYPTEDKEY_ALGO = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
    public static final String SUPPORTED_ENCRYPTEDKEY_ALGO_2 = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";
    public static final String VALUETYPE_SKI_SUFFIX = "X509SubjectKeyIdentifier";
    public static final String VALUETYPE_SKI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#" + VALUETYPE_SKI_SUFFIX;
    public static final String VALUETYPE_SKI_2 = SECURITY_NAMESPACE_PREFIX + ":" + VALUETYPE_SKI_SUFFIX;
    public static final String VALUETYPE_X509_SUFFIX = "X509v3";
    public static final String VALUETYPE_X509 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#" + VALUETYPE_X509_SUFFIX;
    public static final String VALUETYPE_X509_2 = SECURITY_NAMESPACE_PREFIX + ":" + VALUETYPE_X509_SUFFIX;
    public static final String VALUETYPE_X509_THUMB_SHA1_SUFFIX = "ThumbprintSHA1";
    public static final String VALUETYPE_X509_THUMB_SHA1 =        "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1";
    public static final String VALUETYPE_ENCRYPTED_KEY = "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey";
    public static final String VALUETYPE_ENCRYPTED_KEY_SHA1_SUFFIX = "EncryptedKeySHA1";
    public static final String VALUETYPE_ENCRYPTED_KEY_SHA1 = "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKeySHA1";
    public static final String VALUETYPE_SAML = "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-saml-token-profile-1.0#SAMLAssertion-1.0"; // from a DRAFT spec?
    public static final String VALUETYPE_SAML2 = "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-saml-token-profile-1.0#SAMLAssertion-1.1"; // from a DRAFT spec?
    public static final String VALUETYPE_SAML3 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertion-1.1";
    public static final String VALUETYPE_SAML4 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    public static final String VALUETYPE_SAML5 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    public static final String[] VALUETYPE_SAML_ARRAY = {
        VALUETYPE_SAML,
        VALUETYPE_SAML2,
        VALUETYPE_SAML3, // SAML 1.1 assertion from STP 1.0
        VALUETYPE_SAML4, // SAML 1.1 assertion from STP 1.1
        VALUETYPE_SAML5, // SAML 2.0 assertion
    };
    public static final String VALUETYPE_SAML_ASSERTIONID = "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-saml-token-profile-1.0#SAMLAssertionID"; // from a DRAFT spec?
    public static final String VALUETYPE_SAML_ASSERTIONID2 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID";
    public static final String VALUETYPE_SAML_ASSERTIONID3 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID";
    public static final String[] VALUETYPE_SAML_ASSERTIONID_ARRAY = {
        VALUETYPE_SAML_ASSERTIONID,
        VALUETYPE_SAML_ASSERTIONID2, // SAML 1.1 assertion id ref (STP 1.0 and 1.1)
        VALUETYPE_SAML_ASSERTIONID3, // SAML 2.0 assertion id ref (STP 1.1)
    };

    public static final String VALUETYPE_DERIVEDKEY = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk";
    public static final String VALUETYPE_DERIVEDKEY2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk";
    public static final String VALUETYPE_SECURECONV = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";
    public static final String VALUETYPE_SECURECONV2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/sct";
    public static final String VALUETYPE_KERBEROS_GSS_AP_REQ = "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1#GSS_Kerberosv5_AP_REQ";
    public static final String VALUETYPE_KERBEROS_APREQ_SHA1 = "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1#Kerberosv5APREQSHA1";
    public static final String ENCODINGTYPE_BASE64BINARY_SUFFIX = "Base64Binary";
    public static final String ENCODINGTYPE_BASE64BINARY = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#" + ENCODINGTYPE_BASE64BINARY_SUFFIX;
    public static final String ENCODINGTYPE_BASE64BINARY_2 = SECURITY_NAMESPACE_PREFIX + ":" + ENCODINGTYPE_BASE64BINARY_SUFFIX;
    public static final String ALGORITHM_PSHA = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk/p_sha1";
    public static final String ALGORITHM_PSHA2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";
    public static final String TRANSFORM_STR = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform";

    // Well-known actors (SOAP 1.1)
    public static final String ACTOR_VALUE_NEXT = "http://schemas.xmlsoap.org/soap/actor/next";
    public static final String ACTOR_LAYER7_WRAPPED = "http://www.layer7tech.com/ws/actor-wrapped";

    // Well-known roles (SOAP 1.2)
    public static final String ROLE_VALUE_NONE = "http://www.w3.org/2003/05/soap-envelope/role/none";
    public static final String ROLE_VALUE_NEXT = "http://www.w3.org/2003/05/soap-envelope/role/next";
    public static final String ROLE_VALUE_ULTIMATE = "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";

    /**
     * soap envelope xpath '/soapenv:Envelope'
     */
    public static final String SOAP_ENVELOPE_XPATH = "/" + SOAP_ENV_PREFIX + ":" + ENVELOPE_EL_NAME;

    /**
     * soap body xpath '/soapenv:Envelope/soapenv:Body'
     */
    public static final String SOAP_BODY_XPATH = SOAP_ENVELOPE_XPATH + "/" + SOAP_ENV_PREFIX + ":Body";
    /**
     * soap header xpath '/soapenv:Envelope/soapenv:Header'
     */
    public static final String SOAP_HEADER_XPATH = SOAP_ENVELOPE_XPATH + "/" + SOAP_ENV_PREFIX + ":Header";

    public static final String C14N_EXCLUSIVE = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String C14N_EXCLUSIVEWC = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    /**
     * Get the SOAP envelope from the message
     *
     * @param message the message to examine
     * @return the SOAP:Envelope element.  never null
     * @throws MessageNotSoapException if the message isn't SOAP
     */
    public static Element getEnvelopeElement(Document message) throws MessageNotSoapException {
        Element env = message.getDocumentElement();
        if (!ENVELOPE_EL_NAME.equals(env.getLocalName()))
            throw new MessageNotSoapException("Document element is not " + ENVELOPE_EL_NAME);
        if (!SoapUtil.ENVELOPE_URIS.contains(env.getNamespaceURI()))
            throw new MessageNotSoapException(ENVELOPE_EL_NAME + " was present but the namespace was not recognized");
        return env;
    }

    /**
     * Get the Header Element, or null if it's not present
     *
     * @param soapMsg the message to examine
     * @return the Header element, or null if there wasn't one
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Header element
     */
    public static Element getHeaderElement(Document soapMsg) throws InvalidDocumentFormatException {
        Element envelope = getEnvelopeElement(soapMsg);
        String soapNs = envelope.getNamespaceURI();
        return XmlUtil.findOnlyOneChildElementByName(envelope, soapNs, HEADER_EL_NAME);
    }

    /**
     * Get the Body {@link Element}, or null if it's not present
     *
     * @param message the message to examine
     * @return the body, or null if there isn't one
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body element
     */
    public static Element getBodyElement(Document message) throws InvalidDocumentFormatException {
        Element env = getEnvelopeElement(message);
        String soapNs = env.getNamespaceURI();
        return XmlUtil.findOnlyOneChildElementByName(env, soapNs, BODY_EL_NAME);
    }

    /**
     * Get the payload element, or null if it's not present.  The payload is the first child element of the Body
     * element.  The Body is not permitted to have more than one child element, although this method
     * does not currently enforce this.
     *
     * @param message the SOAP message to examine
     * @return the payload element, or null if there isn't one
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
     */
    public static Element getPayloadElement(Document message) throws InvalidDocumentFormatException {
        Element body = getBodyElement(message);
        if (body != null)
            return XmlUtil.findFirstChildElement(body);
        return null;
    }

    public static MessageFactory getMessageFactory() {
        try {
            // bugzilla #2171, avoid 3rd party imposing their own implementations here
            if (SyspropUtil.getProperty("javax.xml.soap.MessageFactory") == null) {
                SyspropUtil.setProperty("javax.xml.soap.MessageFactory",
                                   "com.sun.xml.messaging.saaj.soap.ver1_1.SOAPMessageFactory1_1Impl");
            }
            return MessageFactory.newInstance();
        } catch (SOAPException e) {
            throw new RuntimeException(e); // can't happen
        }
    }


    public static SOAPMessage makeMessage() {
        try {
            return getMessageFactory().createMessage();
        } catch (SOAPException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * If the specified document is a valid SOAP message, this finds it's payload element's namespace URI.
     * The SOAP payload is the first and only child element of the mandatory SOAP Body element.
     *
     * @param request the Document to examine.  May not be null.
     * @return the SOAP payload namespace URIs if it's a SOAP Envelope and has one, or null if not found, or the document isn't valid SOAP.  Never empty.
     */
    public static QName[] getPayloadNames(Document request) {
        Element env = request.getDocumentElement();
        if (!ENVELOPE_EL_NAME.equals(env.getLocalName())) {
            log.finer("Request document element not " + ENVELOPE_EL_NAME + "; assuming non-SOAP request");
            return null; // not soap
        }
        if (!SoapUtil.ENVELOPE_URIS.contains(env.getNamespaceURI())) {
            log.finer("Request document element not in recognized SOAP namespace; assuming non-SOAP request");
            return null; // not soap
        }

        Element body;
        try {
            body = XmlUtil.findOnlyOneChildElementByName(env, env.getNamespaceURI(), BODY_EL_NAME);
        } catch (TooManyChildElementsException e) {
            // fla note, this is incorrent as a soap body is allowed to contain more than one element
            log.finer("Request is not a valid SOAP message (too many " + e.getName() + " elements); assuming non-SOAP request");
            return null;
        }

        if (body == null) {
            log.finer("Request does not contain a SOAP body; assuming non-SOAP request");
            return null;
        }

        NodeList children = body.getChildNodes();
        ArrayList<QName> payloadNames = new ArrayList<QName>();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String ns = n.getNamespaceURI();
                String ln = n.getLocalName();
                String pf = n.getPrefix();
                QName q = pf == null ? new QName(ns, ln) : new QName(ns, ln, pf);
                payloadNames.add(q);
            }
        }
        if (payloadNames.isEmpty()) {
            log.warning("There is no payload namespace");
            return null;
        }
        
        return payloadNames.toArray(EMPTY_QNAME_ARRAY);
    }

    /**
     * Finds or creates a Header element for a SOAP message, which may be partially-constructed and thus currently
     * invalid.
     * If the message does not have a header yet, one will be created and added to the envelope.
     * <p/>
     * If a body element exists, the header element will be inserted right before the body element.
     * <p/>
     * This method assumes that soapMsg is a partially-constructed SOAP message containing at least an Envelope.  No
     * validation is done on the message.
     * The existing envelope namespace is used for new Body or Header elements but is not checked for validity.
     *
     * @param soapMsg DOM document containing a SOAP Envelope.
     * @return the possibly-new header element.  Will never be null.
     * @throws IllegalArgumentException if there is no document element at all
     */
    public static Element getOrMakeHeader(Document soapMsg) {
        // use the soap flavor of this document
        Element env = soapMsg.getDocumentElement();
        if (env == null)
            throw new IllegalArgumentException("No document element");
        String soapNs = env.getNamespaceURI();
        String soapPrefix = env.getPrefix();
        Element header = XmlUtil.findFirstChildElementByName(env, soapNs, HEADER_EL_NAME);
        if (header != null)
            return header; // found it

        header = soapMsg.createElementNS(soapNs, HEADER_EL_NAME);
        header.setPrefix(soapPrefix);

        Element body = XmlUtil.findFirstChildElementByName(env, soapNs, BODY_EL_NAME);
        if (body == null)
            return (Element)env.appendChild(header);

        env.insertBefore(header, body);
        return header;
    }

    /**
     * Finds or creates a default Security element for a SOAP message, which may be partially-constructed and hence
     * not currently valid. If the message does not have a Header yet, one is created.
     * If the Header already contains a default Security element, it is returned; otherwise a new one is created,
     * added to the Header, and then returned.
     * <p/>
     * Namespaces and element cardinality are not checked or enforced by this method.
     *
     * @param soapMsg DOM document containing the soap message.
     * @return the security element, which will never be null.
     * @throws IllegalArgumentException if there is no document element at all.
     */
    public static Element getOrMakeSecurityElement(Document soapMsg) {
        Element header = getOrMakeHeader(soapMsg);
        List<Element> securityElements = XmlUtil.findChildElementsByName(header, SECURITY_URIS_ARRAY, SECURITY_EL_NAME);
        for (Element securityEl : securityElements) {
            if (isDefaultSecurityHeader(securityEl)) return securityEl;
        }
        return makeSecurityElement(soapMsg);
    }

    /**
     * Unconditionally creates a new Security element for a SOAP message, which may be partially-constructed
     * and hence not currently valid.  If the message does not yet have a Header, one is created.
     * A new Security element is created and added at the top of the Header.
     * Existing Security elements, if any, are ignored.
     *
     * @param soapMsg
     */
    public static Element makeSecurityElement(Document soapMsg) {
        return makeSecurityElement(soapMsg, SECURITY_NAMESPACE);
    }

    /**
     * Same as other makeSecurityElement but specifies a preferred security namesapce.
     */
    public static Element makeSecurityElement(Document soapMsg, String preferredWsseNamespace) {
        Element header = getOrMakeHeader(soapMsg);
        Element securityEl = soapMsg.createElementNS(preferredWsseNamespace, SECURITY_EL_NAME);
        securityEl.setPrefix(SECURITY_NAMESPACE_PREFIX);
        securityEl.setAttribute("xmlns:" + SECURITY_NAMESPACE_PREFIX, preferredWsseNamespace);
        setSoapAttr(securityEl, MUSTUNDERSTAND_ATTR_NAME, "1");

        Element existing = XmlUtil.findFirstChildElement(header);
        if (existing == null)
            header.appendChild(securityEl);
        else
            header.insertBefore(securityEl, existing);
        return securityEl;
    }

    public static Element makeSecurityElement(Document soapMsg, String preferredWsseNamespace, String actor) {
        Element header = getOrMakeHeader(soapMsg);
        Element securityEl = soapMsg.createElementNS(preferredWsseNamespace, SECURITY_EL_NAME);
        securityEl.setPrefix(SECURITY_NAMESPACE_PREFIX);
        securityEl.setAttribute("xmlns:" + SECURITY_NAMESPACE_PREFIX, preferredWsseNamespace);
        setSoapAttr(securityEl, MUSTUNDERSTAND_ATTR_NAME, "1");
        if (actor != null) {
            // todo, should we create this actor with a ns ?
            securityEl.setAttribute(SoapUtil.ACTOR_ATTR_NAME, actor);
        }
        Element existing = XmlUtil.findFirstChildElement(header);
        if (existing == null)
            header.appendChild(securityEl);
        else
            header.insertBefore(securityEl, existing);
        return securityEl;
    }

    /**
     * Remove any soap-specific attribute from the specified element.
     * Removes any attribute with a matching unqualified name, or a name fully
     * qualified in one of the known SOAP envelope namespaces.
     *
     * @param element
     * @param attrName
     */
    public static void removeSoapAttr(Element element, String attrName) {
        // todo - find out if soap-specific attrs can ever appear without a prefix.  if not, remove following line
        element.removeAttribute(attrName);

        for (String ns : ENVELOPE_URIS) {
            element.removeAttributeNS(ns, attrName);
        }

    }

    /**
     * Set a SOAP-specific attribute on the specified element.
     * The new attribute will be created using the namespace and prefix from the document element, which
     * is assumed to be a SOAP Envelope.
     *
     * @param elementNeedingAttr
     * @param attrName
     * @param attrValue
     */
    public static void setSoapAttr(Element elementNeedingAttr, String attrName, String attrValue) {
        Element env = elementNeedingAttr.getOwnerDocument().getDocumentElement();
        String soapNs = env.getNamespaceURI();
        String prefix = env.getPrefix();
        if (prefix == null) {
            // SOAP must be the default namespace.  We'll have to declare a prefix of our own.
            prefix = "soap8271"; // todo - find an unused prefix
            elementNeedingAttr.setAttributeNS(soapNs,
                                              prefix + ":" + attrName,
                                              attrValue);
            elementNeedingAttr.setAttribute("xmlns:" + prefix, soapNs);
        } else {
            elementNeedingAttr.setAttributeNS(soapNs,
                                              prefix + ":" + attrName,
                                              attrValue);
        }
    }

    /**
     * Creates a map of wsu:Id values to target elements in the specified document.
     *
     * @param doc  the Document to examine.
     * @return a Map of wsu:Id String to Element.  May be empty, but never null.
     */
    public static Map getElementByWsuIdMap(Document doc) throws InvalidDocumentFormatException {
        Map<String, Element> map = new HashMap<String, Element>();
        NodeList elements = doc.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element)elements.item(i);
            String id = getElementWsuId(element);
            Element existing = map.put(id, element);
            if (existing != null && id != null)
                throw new InvalidDocumentFormatException("Duplicate wsu:Id:" + id);
        }
        return map;
    }

    /**
     * Resolves the element in the passed document that has the id passed in elementId.
     * The id attributes can be of any supported WSU namespaces.
     * <p/>
     * This method just does a linear search over the entire document from
     * top to bottom.
     *
     * @return the leement or null if no such element exists
     */
    public static Element getElementByWsuId(Document doc, String elementId) {
        String url;
        if (elementId.charAt(0) == '#') {
            url = elementId.substring(1);
        } else
            url = elementId;
        NodeList elements = doc.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element)elements.item(i);
            if (url.equals(getElementWsuId(element))) {
                return element;
            }
        }
        return null;
    }

    /**
     * Gets the WSU:Id attribute of the passed element using all supported WSU namespaces.
     *
     * @return the string value of the attribute or null if the attribute is not present
     */
    public static String getElementWsuId(Element node) {
        String id = node.getAttributeNS(SoapUtil.WSU_NAMESPACE, ID_ATTRIBUTE_NAME);
        if (id == null || id.length() < 1) {
            id = node.getAttributeNS(SoapUtil.WSU_NAMESPACE2, ID_ATTRIBUTE_NAME);
            if (id == null || id.length() < 1) {
                id = node.getAttributeNS(SoapUtil.WSU_NAMESPACE3, ID_ATTRIBUTE_NAME);
                if (id == null || id.length() < 1) {
                    // This special handing for SAML is here for backwards compatibility.
                    // Any references to a SAML token should use a KeyIdentifier, not a
                    // Reference.
                    // SSG/SSB 4.0 onwards use only KeyIdentifier references.

                    // Special handling for saml:Assertion
                    if (SamlConstants.NS_SAML.equals(node.getNamespaceURI()) &&
                        SamlConstants.ELEMENT_ASSERTION.equals(node.getLocalName())) {
                        id = node.getAttribute(SamlConstants.ATTR_ASSERTION_ID);
                    }
                    // Special handling for saml2:Assertion
                    if (SamlConstants.NS_SAML2.equals(node.getNamespaceURI()) &&
                        SamlConstants.ELEMENT_ASSERTION.equals(node.getLocalName())) {
                        id = node.getAttribute(SamlConstants.ATTR_SAML2_ASSERTION_ID);
                    }
                    if (id == null || id.length() < 1)
                        id = node.getAttribute(ID_ATTRIBUTE_NAME);
                }
            }
        }
        // for some reason this is set to "" when not present.
        if (id.length() < 1) id = null;
        return id;
    }

    /**
     * Check if the specified Security element is a default security element.
     *
     * @param element the Security element to examine
     * @return True if this element has no role/actor, or a role/actor of "next"; false otherwise.
     */
    private static boolean isDefaultSecurityHeader(Element element) {
        // Check actor (SOAP 1.1)

        String actor = element.getAttribute(ACTOR_ATTR_NAME);
        if (actor != null && actor.length() > 0)
            return ACTOR_VALUE_NEXT.equals(actor);
        for (String ns : ENVELOPE_URIS) {
            actor = element.getAttributeNS(ns, ACTOR_ATTR_NAME);
            if (actor != null && actor.length() > 0)
                return ACTOR_VALUE_NEXT.equals(actor);
        }

        // No actor; check role (SOAP 1.2)
        String role = element.getAttribute(ROLE_ATTR_NAME);
        if (role != null && role.length() > 0)
            return ROLE_VALUE_NEXT.equals(role);
        for (String ns : ENVELOPE_URIS) {
            role = element.getAttributeNS(ns, ROLE_ATTR_NAME);
            if (role != null && role.length() > 0)
                return ROLE_VALUE_NEXT.equals(role);
        }

        // No actor or role
        return true;
    }

    /**
     * @return null if element not present or not addressed to us, the default security element if it's in the doc
     * @throws InvalidDocumentFormatException if there is more than one soap:Header or the message isn't SOAP
     */
    public static Element getSecurityElement(Document soapMsg) throws InvalidDocumentFormatException {
        List<Element> allofthem = getSecurityElements(soapMsg);
        if (allofthem == null || allofthem.size() < 1) return null;
        for (Element element : allofthem) {
            if (isDefaultSecurityHeader(element))
                return element;
        }
        return null; // no security header for us
    }

    public static void nukeActorAttribute(Element el) {
        el.removeAttribute(SoapUtil.ACTOR_ATTR_NAME);
        for (String ns : ENVELOPE_URIS) {
            el.removeAttributeNS(ns, SoapUtil.ACTOR_ATTR_NAME);
        }
    }

    public static String getActorValue(Element element) {
        String localactor = element.getAttribute(SoapUtil.ACTOR_ATTR_NAME);
        if (localactor == null || localactor.length() < 1) {
            for (String ns : ENVELOPE_URIS) {
                localactor = element.getAttributeNS(ns, SoapUtil.ACTOR_ATTR_NAME);
                if (localactor != null && localactor.length() > 0) {
                    return localactor;
                }
            }
        }
        if (localactor == null || localactor.length() < 1) return null;
        return localactor;
    }

    public static Element getSecurityElement(Document soapMsg, String actor) throws InvalidDocumentFormatException {
        if (actor == null) return getSecurityElement(soapMsg);
        List<Element> allofthem = getSecurityElements(soapMsg);
        if (allofthem == null || allofthem.size() < 1) return null;

        for (Element element : allofthem) {
            String localactor = SoapUtil.getActorValue(element);
            if (localactor != null && localactor.equals(actor)) {
                return element;
            }

        }
        return null;
    }

    /**
     * Returns all Security elements.
     *
     * @return never null
     * @throws InvalidDocumentFormatException if there is more than one soap:Header or the message isn't SOAP
     */
    public static List<Element> getSecurityElements(Document soapMsg) throws InvalidDocumentFormatException {
        Element header = getHeaderElement(soapMsg);
        if (header == null) return Collections.emptyList();
        return XmlUtil.findChildElementsByName(header, SECURITY_URIS_ARRAY, SECURITY_EL_NAME);
    }

    /**
     * Get the security element from a specific header instead of the entire soap message.
     * This alternative is used in the case of nested soap messages.
     *
     * @return null if element not present, the security element if it's in the doc
     */
    public static Element getSecurityElement(Element header) {
        List secElements = XmlUtil.findChildElementsByName(header, SECURITY_URIS_ARRAY, SECURITY_EL_NAME);
        // is it there ?
        if (secElements.size() < 1) return null;
        // we got it
        return (Element)secElements.get(0);
    }

    /**
     * Get the L7a:MesageID element from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @return the /Envelope/Header/L7a:MessageID element, or null if there isn't one.
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or MessageID
     */
    public static Element getL7aMessageIdElement(Document soapMsg) throws InvalidDocumentFormatException {
        Element header = getHeaderElement(soapMsg);
        if (header == null) return null;
        return XmlUtil.findOnlyOneChildElementByName(header, L7_MESSAGEID_NAMESPACE, MESSAGEID_EL_NAME);
    }

    /**
     * Get the L7a:MessageID URI from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @return the body text of the /Envelope/Header/L7a:MessageID field, which may be empty or an invalid URI; or,
     *         null if there was no /Envelope/Header/L7a:MessageID field.
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or MessageID
     */
    public static String getL7aMessageId(Document soapMsg) throws InvalidDocumentFormatException {
        Element messageId = getL7aMessageIdElement(soapMsg);
        if (messageId == null) return null;
        return XmlUtil.getTextValue(messageId);
    }

    /**
     * Set the L7a:MessageID URI for the specified message, which must not already have a L7a:MessageID element.
     *
     * @param soapMsg   the soap message to modify
     * @param messageId the new L7a:MessageID value
     * @throws InvalidDocumentFormatException if the message isn't soap, has more than one header, or already has
     *                                        a MessageID
     */
    public static void setL7aMessageId(Document soapMsg, String messageId) throws InvalidDocumentFormatException {
        Element idEl = getL7aMessageIdElement(soapMsg);
        if (idEl != null)
            throw new ElementAlreadyExistsException("This message already has a L7a:MessageID");

        Element header = getOrMakeHeader(soapMsg);
        idEl = XmlUtil.createAndPrependElementNS(header, MESSAGEID_EL_NAME, L7_MESSAGEID_NAMESPACE, L7_MESSAGEID_PREFIX);
        idEl.appendChild(XmlUtil.createTextNode(idEl, messageId));
    }

    /**
     * Set the L7a:RelatesTo URI for the specified message, which must not already have a L7a:RelatesTo element.
     *
     * @param soapMsg   the soap message to modify
     * @param relatesTo the new L7a:RelatesTo value
     * @return the L7a:RelatesTo element that was set.  Never null.
     * @throws InvalidDocumentFormatException if the message isn't soap, has more than one header, or already has
     *                                        a RelatesTo
     */
    public static Element setL7aRelatesTo(Document soapMsg, String relatesTo) throws InvalidDocumentFormatException {
        Element idEl = getL7aRelatesToElement(soapMsg);
        if (idEl != null)
            throw new ElementAlreadyExistsException("This message already has a L7a:RelatesTo");

        Element header = getOrMakeHeader(soapMsg);
        idEl = XmlUtil.createAndPrependElementNS(header, RELATESTO_EL_NAME, L7_MESSAGEID_NAMESPACE, L7_MESSAGEID_PREFIX);
        idEl.appendChild(XmlUtil.createTextNode(idEl, relatesTo));
        return idEl;
    }

    /**
     * Get the L7a:RelatesTo element from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @return the /Envelope/Header/L7a:RelatesTo element, or null if there wasn't one
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or RelatesTo
     */
    public static Element getL7aRelatesToElement(Document soapMsg) throws InvalidDocumentFormatException {
        Element header = getHeaderElement(soapMsg);
        if (header == null) return null;
        return XmlUtil.findOnlyOneChildElementByName(header, L7_MESSAGEID_NAMESPACE, RELATESTO_EL_NAME);
    }

    /**
     * Get the L7a:RelatesTo URI from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @return the body text of the /Envelope/Header/L7a:RelatesTo field, which may be empty or an invalid URI; or,
     *         null if there was no /Envelope/Header/L7a:RelatesTo field.
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or RelatesTo
     */
    public static String getL7aRelatesTo(Document soapMsg) throws InvalidDocumentFormatException {
        Element relatesTo = getL7aRelatesToElement(soapMsg);
        if (relatesTo == null) return null;
        return XmlUtil.getTextValue(relatesTo);
    }

    /**
     * Create the SOAP message from the DOM document. The caller must ensure
     * that the DOM source is valid SOAP message; the api <code>javax.xml.soap.*</code>
     * does not validate if the document is valid SOAP message or not.
     * <p/>
     *
     * @param doc the SOAP message as a DOM document
     * @return the corresponding SOAP message
     * @throws SOAPException on SOAP error
     */
    public static SOAPMessage asSOAPMessage(Document doc) throws SOAPException {
        SOAPMessage sm = SoapUtil.getMessageFactory().createMessage();
        sm.getSOAPPart().setContent(new DOMSource(doc));
        return sm;
    }

    /**
     * Reads the SOAP message from the input stream as a DOM document. Performs
     * some basic SOAP message structure validation
     *
     * @param in the input stream
     * @throws MessageNotSoapException if the xml document does not represent the soap messagr
     * @throws IOException on io parsing error
     * @throws SAXException on oarsing error
     */
    public static Document parseSoapMessage(InputStream in)
      throws MessageNotSoapException, IOException, SAXException {
        if (in == null) {
            throw new IllegalArgumentException();
        }
        final Document doc = XmlUtil.parse(in);
        if (isSoapMessage(doc)) {
            throw new MessageNotSoapException("Not Soap Message! \n"+XmlUtil.nodeToString(doc));
        }
        return doc;
    }

    /**
     * Tests wheteher a given document is a SOAP message.
     * Note: this should probably validate against SOAP message schema.
     *
     * @param doc the document to test whether it represents the SOAP message
     * @return true if soap message, false otherwise
     */
    public static boolean isSoapMessage(Document doc) {
        if (doc == null) {
            throw new IllegalArgumentException();
        }
        try {
            SoapUtil.getEnvelopeElement(doc);
            SoapUtil.getBodyElement(doc); // This returns null if there's no body, so Faults will still pass

            // Check for any processing instructions at the start of the document (Bug #3888)
            // We should only need to check for immediate children of the Document node itself,
            // and we can stop as soon as we encounter the document element.
            NodeList nodes = doc.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                    // Not soap
                    return false;
                } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                    // Don't bother looking any further for processing instructions
                    break;
                }
            }

            return true;
        } catch (InvalidDocumentFormatException e) {
            // not SOAP
        }
        return false;
    }

    /**
     * Tests wheteher a given element is a SOAP Body element.
     *
     * @param element the element to test whether it is SOAP Body element
     * @return true if SOAP Body, false otherwise
     * @throws InvalidDocumentFormatException if the message containing
     *         this element is not SOAP
     */
    public static boolean isBody(Element element) throws InvalidDocumentFormatException {
        if (element == null) {
            throw new IllegalArgumentException();
        }
        final Document doc = element.getOwnerDocument();
        if (doc == null) {
            throw new IllegalArgumentException("The element does not have Owner Document.");
        }
        return SoapUtil.getBodyElement(doc) !=null;
    }


    /**
     * There is no built-in provision in jax-rpc for adding a DOM document object (that
     * represents an XML document) as a SOAP body subelement in a SOAP message. The document
     * object needs to be 'unmarshalled' into a javax.xml.soap.SOAPElement object. In other
     * words a SOAPElement object is constructed from the contents of a DOM object. The
     * following method, domToSOAPElement(javax.xml.soap.SOAPEnvelope, org.w3c.dom.Node)
     * performs the 'unmarshalling' of the DOM object and creates an equivalent
     * javax.xml.soap.SOAPElement object. It basically performs a depth first traversal
     * of the DOM object's tree, and for each node in the tree creates a
     * javax.xml.soap.SOAPElement object and populates the SOAPElement with the contents
     * of the node.
     *
     * @param soapElement the soap element where the DOM Node is added
     * @param domNode     the DOM Node to add
     * @return the soap element containing the dom element marshalled into it
     * @throws SOAPException on soap error
     */
    public static SOAPElement domToSOAPElement(SOAPElement soapElement, Node domNode)
      throws SOAPException {

        //Test that domNode is of type org.w3c.dom.Node.ELEMENT_NODE.
        if ((domNode.getNodeType()) != Node.ELEMENT_NODE)
            throw new SOAPException("DOM Node must of type ELEMENT_NODE. received " + domNode.getNodeType());

        SOAPFactory sf = SOAPFactory.newInstance();
        return domNodeToSoapElement(domNode, soapElement, sf);
    }


    /**
     * Add the dom node, and its child nodes to the SOAP element.
     *
     * @param node        the node to add
     * @param soapElement the soap element (SOAPBody, SOAPHeader, SOAPElement etc)
     * @param soapFactory the soap factory
     * @throws SOAPException on soap error
     */
    private static SOAPElement domNodeToSoapElement(Node node, SOAPElement soapElement, SOAPFactory soapFactory)
      throws SOAPException {
        Name name = soapFactory.createName(node.getLocalName(), node.getPrefix(), node.getNamespaceURI());
        if (soapElement instanceof SOAPBody) {
            SOAPBody sb = (SOAPBody)soapElement;
            SOAPBodyElement sbe = sb.addBodyElement(name);
            return domChildrenToSoapElement(sbe, node, soapFactory);
        } else if (soapElement instanceof SOAPHeader) {
            SOAPHeader sh = (SOAPHeader)soapElement;
            SOAPHeaderElement she = sh.addHeaderElement(name);
            return domChildrenToSoapElement(she, node, soapFactory);
        } else {
            return soapElement.addChildElement(domChildrenToSoapElement(soapFactory.createElement(name), node, soapFactory));
        }
    }

    private static SOAPElement domChildrenToSoapElement(SOAPElement soapElement, Node parentNode, SOAPFactory sf)
      throws SOAPException {

        if (parentNode.hasAttributes()) {
            NamedNodeMap DOMAttributes = parentNode.getAttributes();
            int noOfAttributes = DOMAttributes.getLength();
            for (int i = 0; i < noOfAttributes; i++) {

                Node attr = DOMAttributes.item(i);
                String attrPrefix = attr.getPrefix();
                String attrLocalName = attr.getLocalName();
                if ((!XMLNS.equals(attrPrefix)) &&
                  (!XMLNS.equals(attrLocalName))) {
                    Name name = sf.createName(attr.getLocalName(), attr.getPrefix(), attr.getNamespaceURI());
                    soapElement.addAttribute(name, attr.getNodeValue());
                }
            }
        }

        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            switch (child.getNodeType()) {
                case Node.PROCESSING_INSTRUCTION_NODE:
                case Node.DOCUMENT_TYPE_NODE:
                case Node.CDATA_SECTION_NODE:
                case Node.COMMENT_NODE:
                    break;
                case Node.TEXT_NODE:
                    {
                        soapElement.addTextNode(child.getNodeValue());
                        break;
                    }
                default:
                    domNodeToSoapElement(child, soapElement, sf);
            }

        }
        return soapElement;
    }


    /**
     * Will import the importedDocument ointo the importingDocument under
     * the parentNode.
     *
     * @param importingDocument The document which will have a node import
     * @param importedDocument  The document that will be imported
     * @param parentNode        The node in importingDocument under which the node
     *                          is imported
     * @return The new version of importingDocument with imported document
     */
    public static Node importNode(Document importingDocument, Document importedDocument, Node parentNode) {

        //Create a documentFragment of the replacingDocument
        DocumentFragment docFrag = importedDocument.createDocumentFragment();
        Element rootElement = importedDocument.getDocumentElement();
        docFrag.appendChild(rootElement);


        //Import docFrag under the ownership of replacedDocument
        Node importNode = importingDocument.importNode(docFrag, true);


        //In order to replace the node need to retrieve replacedNode's parent
        parentNode.insertBefore(importNode, null);
        return importingDocument;
    }

    public static String soapMessageToString(SOAPMessage msg, String encoding) throws IOException, SOAPException {
        return new String(soapMessageToByteArray(msg), encoding);
    }

    public static byte[] soapMessageToByteArray(SOAPMessage msg) throws IOException, SOAPException {
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream(4096);
        try {
            msg.writeTo(baos);
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }

    /** @return a new unique URI in the form http://www.layer7tech.com/uuid/XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX, where the
     * Xes stand for random hexadecimal digits. */
    public static String generateUniqeUri() {
        String id;
        byte[] randbytes = new byte[16];
        rand.nextBytes(randbytes);
        id = "http://www.layer7tech.com/uuid/" + HexUtils.hexDump(randbytes);
        return id;
    }

    private static final SecureRandom rand = new SecureRandom();

    /**
     * Generate an id with the specified basename.
     *
     * <p>Uses the specified basename as the start of the Id.</p>
     *
     * <p>The generated id is in the form "id-1-b5084f383f3063ddf12f03325d5f3ac0".</p>
     *
     * @param basename The base name for the id (may be null)
     * @param basenumber The base number for the id
     * @return The generated id
     */
    public static String generateUniqueId(String basename, int basenumber) {
        if (basename == null) {
            basename = "id";
        }

        byte[] randbytes = new byte[16];
        rand.nextBytes(randbytes);

        return basename + "-" + basenumber + "-" + HexUtils.hexDump(randbytes);
    }

    /**
     * Append a wsu:Timestamp element to the specified parent element, showing the specified
     * time, or the current time if it isn't specified.
     * @param parent      element which will contain the new timestamp subelement
     * @param wsuUri      which wsu: namespace URI to use
     * @param timestamp   time that should be marked in this timestamp.  if null, uses current time
     * @param timestampNanos   number of nanoseconds to add to the millisecond-granular timestamp
     * @param timeoutSec  after how many seconds this timestamp should expirel.  If zero, uses 5 min.
     */
    public static Element addTimestamp(Element parent, String wsuUri, Date timestamp, long timestampNanos, int timeoutSec) {
        Document message = parent.getOwnerDocument();
        Element timestampEl = message.createElementNS(wsuUri,
                                                      TIMESTAMP_EL_NAME);
        parent.appendChild(timestampEl);
        timestampEl.setPrefix(XmlUtil.getOrCreatePrefixForNamespace(timestampEl, wsuUri, "wsu"));

        Calendar now = Calendar.getInstance();
        if (timestamp != null)
            now.setTime(timestamp);
        timestampEl.appendChild(makeTimestampChildElement(timestampEl, CREATED_EL_NAME, now.getTime(), timestampNanos));
        now.add(Calendar.MILLISECOND, timeoutSec != 0 ? timeoutSec : 300000);
        timestampEl.appendChild(makeTimestampChildElement(timestampEl, EXPIRES_EL_NAME, now.getTime(), -1));
        return timestampEl;
    }

    private static Element makeTimestampChildElement(Element parent, String createdElName, Date time, long nanos) {
        Document factory = parent.getOwnerDocument();
        Element element = factory.createElementNS(parent.getNamespaceURI(), createdElName);
        element.setPrefix(parent.getPrefix());
        element.appendChild(XmlUtil.createTextNode(factory, ISO8601Date.format(time, nanos)));
        return element;
    }

    /**
     * Set the wsu:Id on the specified element.
     *
     * @param element  the element whose ID to set
     * @param wsuNs    the wsu namespace URI to use
     * @param id       the ID to set
     */
    public static void setWsuId(Element element, final String wsuNs, String id) {
        // Check for special handling of dsig and xenc Ids
        String ns = element.getNamespaceURI();
        if (DIGSIG_URI.equals(ns) || XMLENC_NS.equals(ns)) {
            // hack hack hack - xenc and dsig elements aren't allowed to use wsu:Id, due to their inflexible schemas.
            // WSSE says they we required to (ab)use local namespace Id instead.
            element.setAttribute("Id", id);
        } else {
            // do normal handling
            String wsuPrefix = XmlUtil.getOrCreatePrefixForNamespace(element, wsuNs, "wsu");
            element.setAttributeNS(wsuNs, wsuPrefix == null ? "Id" : wsuPrefix + ":Id", id);
        }
    }

    /**
     * Test whether the valueType is one of the know X509v3 ValueType identifiers.
     *
     * @param valueType the value type to check
     * @return true if valueType is for X509v3
     */
    public static boolean isValueTypeX509v3(String valueType) {
        return VALUETYPE_X509.equals(valueType);
    }

    /**
     * Test whether the valueType is one of the know Saml ValueType identifiers.
     * @param valueType the saml valuetype
     * @return true if valueType matches one of known Saml valueType constants, false otherwise
     */
    public static boolean isValueTypeSaml(String valueType) {
        return ArrayUtils.contains(VALUETYPE_SAML_ARRAY,valueType) ||
               ArrayUtils.contains(VALUETYPE_SAML_ASSERTIONID_ARRAY,valueType);
    }

    /**
     * Test if a KeyIdentifier has a Kerberos ValueType.
     *
     * @param valueType the value type to check
     * @return true if valueType is a Kerberos KeyIdentifier
     */
    public static boolean isValueTypeKerberos(String valueType) {
        return VALUETYPE_KERBEROS_APREQ_SHA1.equals(valueType);
    }

    public static String findSoapAction(BindingOperation operation) {
        Iterator eels = operation.getExtensibilityElements().iterator();
        ExtensibilityElement ee;
        while ( eels.hasNext() ) {
            ee = (ExtensibilityElement)eels.next();
            if ( ee instanceof SOAPOperation ) {
                SOAPOperation sop = (SOAPOperation)ee;
                return sop.getSoapActionURI();
            }
        }
        return null;
    }

    public static String stripQuotes(String soapAction) {
        if (soapAction == null) return null;
        if (    ( soapAction.startsWith("\"") && soapAction.endsWith("\"") )
             || ( soapAction.startsWith("'") && soapAction.endsWith( "'" ) ) ) {
            return soapAction.substring( 1, soapAction.length()-1 );
        } else {
            return soapAction;
        }
    }

    private static boolean bothNullOrEqual(String s1, String s2) {
        return (s1 == null && s2 == null) || (s1 != null && s1.equals(s2));
    }

    /*public static Operation getOperation(Wsdl wsdl, Message request)
            throws IOException, SAXException, InvalidDocumentFormatException,
                   MessageNotSoapException
    {
        XmlKnob requestXml = (XmlKnob)request.getKnob(XmlKnob.class);
        if (requestXml == null) {
            log.info("Can't get operation for non-XML message");
            return null;
        }
        Document requestDoc = requestXml.getDocumentReadOnly();
        Element payload = getPayloadElement(requestDoc);
        if (payload == null) {
            log.info("Can't get operation for message with no payload element");
            return null;
        }

        Operation operation = null;

        Map bindings = wsdl.getDefinition().getBindings();
        if (bindings.isEmpty()) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
            return null;
        }

        boolean foundSoapBinding = false;
        bindings: for (Iterator h = bindings.keySet().iterator(); h.hasNext();) {
            QName bindingName = (QName)h.next();
            Binding binding = (Binding)bindings.get(bindingName);
            SOAPBinding soapBinding = null;
            List bindingEels = binding.getExtensibilityElements();
            for (Iterator bindit = bindingEels.iterator(); bindit.hasNext();) {
                ExtensibilityElement element = (ExtensibilityElement)bindit.next();
                if (element instanceof SOAPBinding) {
                    foundSoapBinding = true;
                    soapBinding = (SOAPBinding)element;
                }
            }

            if (soapBinding == null)
                continue bindings; // This isn't a SOAP binding; we don't care

            List bindingOperations = binding.getBindingOperations();

            for (Iterator i = bindingOperations.iterator(); i.hasNext();) {
                // Look for RPC (element name = operation name)
                BindingOperation bindingOperation = (BindingOperation)i.next();
                Operation candidateOperation = bindingOperation.getOperation();

                if ("rpc".equals(wsdl.getBindingStyle(bindingOperation))) {
                    BindingInput binput = bindingOperation.getBindingInput();
                    String ns = null;
                    List bindingInputEels = binput.getExtensibilityElements();
                    if (bindingInputEels != null) {
                        for (Iterator j = bindingInputEels.iterator(); j.hasNext();) {
                            ExtensibilityElement eel = (ExtensibilityElement)j.next();
                            if (eel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                                javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody)eel;
                                ns = body.getNamespaceURI();
                            } else if (eel instanceof MIMEMultipartRelated) {
                                MIMEMultipartRelated mime = (MIMEMultipartRelated)eel;
                                List parts = mime.getMIMEParts();
                                if (parts.size() >= 1) {
                                    MIMEPart firstPart = (MIMEPart)parts.get(0);
                                    List mimeEels = firstPart.getExtensibilityElements();
                                    for (Iterator k = mimeEels.iterator(); k.hasNext();) {
                                        ExtensibilityElement mimeEel = (ExtensibilityElement)k.next();
                                        if (mimeEel instanceof javax.wsdl.extensions.soap.SOAPBody ) {
                                            javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody)mimeEel;
                                            ns = body.getNamespaceURI();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (ns == null) ns = wsdl.getDefinition().getTargetNamespace();
                    if (payload.getLocalName().equals(bindingOperation.getName()) &&
                            bothNullOrEqual(payload.getNamespaceURI(), ns)) {
                        if (operation != null && operation != candidateOperation) {
                            warnMultipleOperations(payload, wsdl);
                        }
                        operation = candidateOperation;
                    }
                }

                // Try to match the abstract Operation's input message
                Input input = candidateOperation.getInput();
                javax.wsdl.Message inputMessage = null;
                if (input != null) {
                    inputMessage = input.getMessage();
                    QName expectedElementQname = inputMessage.getQName();
                    if (payload != null && expectedElementQname != null) {
                        if (bothNullOrEqual(payload.getNamespaceURI(), expectedElementQname.getNamespaceURI())) {
                            if (bothNullOrEqual(payload.getLocalName(), expectedElementQname.getLocalPart())) {
                                if (operation != null && operation != candidateOperation) {
                                    warnMultipleOperations(payload, wsdl);
                                }
                                operation = candidateOperation;
                            }
                        }
                    }

                    // Try to match message parts
                    Map parts = inputMessage.getParts();
                    for (Iterator j = parts.keySet().iterator(); j.hasNext();) {
                        String partName = (String)j.next();
                        Part part = (Part)inputMessage.getParts().get(partName);
                        QName elementName = part.getElementName();
                        if (elementName != null && payload != null &&
                                bothNullOrEqual(elementName.getLocalPart(), payload.getLocalName()) &&
                                bothNullOrEqual(elementName.getNamespaceURI(), payload.getNamespaceURI()) )
                        {
                            if (operation != null && operation != candidateOperation) {
                                warnMultipleOperations(payload, wsdl);
                            }
                            operation = candidateOperation;
                        }
                    }
                }
            }

            // Finally try to match based on SOAPAction
            if (operation == null) {
                HttpRequestKnob requestHttp = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
                String requestSoapAction = requestHttp == null ? null : stripQuotes(requestHttp.getHeaderSingleValue(SOAPACTION));
                List matchingOperationsBySoapAction = new ArrayList();
                for (Iterator i = bindingOperations.iterator(); i.hasNext();) {
                    BindingOperation bindingOperation = (BindingOperation)i.next();

                    String candidateSoapAction = stripQuotes(findSoapAction(bindingOperation));
                    Operation candidateOperation = bindingOperation.getOperation();
                    if (candidateSoapAction != null && candidateSoapAction.length() > 0 &&
                            candidateSoapAction.equals(requestSoapAction)) {
                        matchingOperationsBySoapAction.add(candidateOperation);
                    }
                }

                if (matchingOperationsBySoapAction.size() == 1) {
                    operation = (Operation)matchingOperationsBySoapAction.get(0);
                }
            }
        }

        if (!foundSoapBinding) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
            return null;
        }

        return operation;
    }*/

    /**
     * Narrows a list of candidate wsdl operations by matching them against a soapaction
     *
     * @param soapaction the soapaction to match against
     * @param candidates a list of candidate javax.wsdl.BindingOperation
     * @return a list of matching javax.wsdl.BindingOperation
     */
    private static List<BindingOperation> matchOperationsWithSoapaction(String soapaction, Collection<BindingOperation> candidates) {
        ArrayList<BindingOperation> output = new ArrayList<BindingOperation>();
        if (soapaction == null) soapaction = "";
        for (BindingOperation bindingOperation : candidates) {
            String candidateSoapAction = stripQuotes(findSoapAction(bindingOperation));
            if (candidateSoapAction == null) candidateSoapAction = "";
            if (candidateSoapAction.equals(soapaction)) {
                output.add(bindingOperation);
            }
        }
        return output;
    }

    private static List<BindingOperation> matchOperationsWithSoapBody(Element payload, Collection<BindingOperation> candidates, Wsdl wsdl) {
        if (payload == null) throw new NullPointerException();
        ArrayList<BindingOperation> output = new ArrayList<BindingOperation>();
        mainloop:
        for (BindingOperation bindingOperation : candidates) {
            // Look for RPC (element name = operation name)

            if ("rpc".equals(wsdl.getBindingStyle(bindingOperation))) {
                BindingInput binput = bindingOperation.getBindingInput();
                String ns = null;
                //noinspection unchecked
                List<ExtensibilityElement> bindingInputEels = binput.getExtensibilityElements();
                if (bindingInputEels != null) {
                    for (ExtensibilityElement eel : bindingInputEels) {
                        if (eel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                            javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody) eel;
                            ns = body.getNamespaceURI();
                        } else if (eel instanceof MIMEMultipartRelated) {
                            MIMEMultipartRelated mime = (MIMEMultipartRelated) eel;
                            List parts = mime.getMIMEParts();
                            if (parts.size() >= 1) {
                                MIMEPart firstPart = (MIMEPart) parts.get(0);
                                //noinspection unchecked
                                List<ExtensibilityElement> mimeEels = firstPart.getExtensibilityElements();
                                for (ExtensibilityElement mimeEel : mimeEels) {
                                    if (mimeEel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                                        javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody) mimeEel;
                                        ns = body.getNamespaceURI();
                                    }
                                }
                            }
                        }
                    }
                }

                if (ns == null) ns = wsdl.getDefinition().getTargetNamespace();
                if (payload.getLocalName().equals(bindingOperation.getName()) && bothNullOrEqual(payload.getNamespaceURI(), ns)) {
                    if (!output.contains(bindingOperation)) output.add(bindingOperation);
                    continue;
                }
            }

            // Try to match the abstract Operation's input message
            Input input = bindingOperation.getOperation().getInput();
            javax.wsdl.Message inputMessage;
            if (input != null) {
                inputMessage = input.getMessage();
                QName expectedElementQname = inputMessage.getQName();
                if (expectedElementQname != null) {
                    if (bothNullOrEqual(payload.getNamespaceURI(), expectedElementQname.getNamespaceURI())) {
                        if (bothNullOrEqual(payload.getLocalName(), expectedElementQname.getLocalPart())) {
                            if (!output.contains(bindingOperation)) output.add(bindingOperation);
                            continue;
                        }
                    }
                }

                // Try to match message parts
                //noinspection unchecked
                Map<String, Part> parts = inputMessage.getParts();
                for (String partName : parts.keySet()) {
                    Part part = (Part) inputMessage.getParts().get(partName);
                    QName elementName = part.getElementName();
                    if (elementName != null &&
                            bothNullOrEqual(elementName.getLocalPart(), payload.getLocalName()) &&
                            bothNullOrEqual(elementName.getNamespaceURI(), payload.getNamespaceURI())) {
                        if (!output.contains(bindingOperation)) output.add(bindingOperation);
                        continue mainloop;
                    }
                }
            }
        }
        return output;
    }

    private static Operation getOperationFromBinding(Binding soapbinding, OperationSearchContext context)
            throws IOException, SAXException, InvalidDocumentFormatException {
        //noinspection unchecked
        List<BindingOperation> bindingOperations = soapbinding.getBindingOperations();

        // only try to match soapaction for http requests (not for jms)
        if (context.hasHttpRequestKnob()) {
            String requestSoapAction = context.getSoapaction();
            List<BindingOperation> beforenarrowed = bindingOperations;
            bindingOperations = matchOperationsWithSoapaction(requestSoapAction, bindingOperations);
            if (bindingOperations == null || bindingOperations.size() < 1) {
                log.info("request's soapaction " + requestSoapAction + " did not match any operation in the wsdl." +
                         " will try to match using the document instead");
                bindingOperations = beforenarrowed;
            } else if (bindingOperations.size() == 1) {
                log.fine("operation identified using soapaction. bypassing further analysis");
                BindingOperation operation = bindingOperations.get(0);
                return operation.getOperation();
            }
        }

        bindingOperations = matchOperationsWithSoapBody(context.getPayload(), bindingOperations, context.getWsdl());
        if (bindingOperations == null || bindingOperations.size() < 1) {
            log.info("request payload did not match any operation in the wsdl." +
                     " perhaps the request is not valid for this wsdl");
            return null;
        } else if (bindingOperations.size() == 1) {
            log.fine("operation identified using payload");
            BindingOperation operation = bindingOperations.get(0);
            return operation.getOperation();
        } else {
            StringBuffer tmp = new StringBuffer();
            for (BindingOperation bindingOperation : bindingOperations) {
                tmp.append(bindingOperation.getOperation().getName()).append(", ");
            }
            log.info("this request payload yields more than one match during operation search: " + tmp.toString());
            return null;
        }
    }

    /**
     * @deprecated use {@link SoapUtil#getOperationPayloadQNames}, it's more accurate
     */
    @Deprecated
    public static String findTargetNamespace(Definition def, BindingOperation bindingOperation) {
        BindingInput bindingInput = bindingOperation.getBindingInput();
        if (bindingInput != null) {
            Iterator eels = bindingInput.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof javax.wsdl.extensions.soap.SOAPBody) {
                    javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody)ee;
                    String uri = body.getNamespaceURI();
                    if (uri != null) return uri;
                }
            }
        }

        Input input = bindingOperation.getOperation().getInput();
        javax.wsdl.Message inputMessage = input == null ? null : input.getMessage();
        if (inputMessage != null) {
            List parts = inputMessage.getOrderedParts(null);
            if (parts.size() > 0) {
                Part firstPart = (Part)parts.get(0);
                QName elementName = firstPart.getElementName();
                if (elementName != null) {
                    String uri = elementName.getNamespaceURI();
                    if (uri != null) return uri;
                }
            }
        }

        return def.getTargetNamespace();
    }

    /**
     * Gets the QName(s) of the element(s) that should appear as children of the SOAP Body element for messages destined
     * for the provided operation.
     * @param bindingOperation the operation to get QNames from. Must not be null.
     * @param bindingStyle     the style from the SOAP binding element; will be used if the operation doesn't specify its own style. May be null, indicating a default of "document".
     * @param audit            an Audit implementation to send error messages to.  May be null.
     * @return
     */
    public static List<QName> getOperationPayloadQNames(final BindingOperation bindingOperation,
                                               final String bindingStyle,
                                               final Audit audit)
    {
        //noinspection unchecked
        final List<ExtensibilityElement> bopEels = bindingOperation.getExtensibilityElements();
        SOAPOperation soapOperation = null;
        for (ExtensibilityElement eel : bopEels) {
            if (eel instanceof SOAPOperation) {
                soapOperation = (SOAPOperation) eel;
            }
        }

        final OperationType ot = bindingOperation.getOperation().getStyle();
        if (ot != null && ot != OperationType.REQUEST_RESPONSE && ot != OperationType.ONE_WAY)
            return null;

        String operationStyle = soapOperation == null ? null : soapOperation.getStyle();

        BindingInput input = bindingOperation.getBindingInput();
        //noinspection unchecked
        List<ExtensibilityElement> eels = input.getExtensibilityElements();
        String use = null;
        String namespace = null;
        for (ExtensibilityElement eel : eels) {
            if (eel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody)eel;
                use = body.getUse();
                namespace = body.getNamespaceURI();
            } else if (eel instanceof MIMEMultipartRelated) {
                MIMEMultipartRelated mime = (MIMEMultipartRelated) eel;
                //noinspection unchecked
                List<MIMEPart> parts = mime.getMIMEParts();
                MIMEPart part1 = parts.get(0);
                //noinspection unchecked
                List<ExtensibilityElement> partEels = part1.getExtensibilityElements();
                for (ExtensibilityElement partEel : partEels) {
                    if (partEel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                        javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody) partEel;
                        use = body.getUse();
                        namespace = body.getNamespaceURI();
                    }
                }
            }
        }

        if (use == null) return null;

        List<QName> operationQNames = new ArrayList<QName>();

        if (operationStyle == null) operationStyle = bindingStyle;
        if (operationStyle == null) {
            if (audit != null) audit.logAndAudit(CommonMessages.WSDL_OPERATION_NO_STYLE, bindingOperation.getName());
            operationStyle = Wsdl.STYLE_DOCUMENT;
        }

        if (Wsdl.STYLE_RPC.equalsIgnoreCase(operationStyle.trim())) {
            operationQNames.add(new QName(namespace, bindingOperation.getName()));
        } else if (Wsdl.STYLE_DOCUMENT.equalsIgnoreCase(operationStyle.trim())) {
            javax.wsdl.Message inputMessage = bindingOperation.getOperation().getInput().getMessage();
            if (inputMessage == null) return null;

            List<ExtensibilityElement> inputEels = input.getExtensibilityElements();
            List<String> partNames = null;
            for (ExtensibilityElement inputEel : inputEels) {
                // Headers are not relevant for service resolution (yet?)
                if (inputEel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                    javax.wsdl.extensions.soap.SOAPBody inputBody = (javax.wsdl.extensions.soap.SOAPBody) inputEel;
                    partNames = inputBody.getParts();
                } else if (inputEel instanceof SOAP12Body) {
                    SOAP12Body inputBody = (SOAP12Body) inputEel;
                    partNames = inputBody.getParts();
                }
            }

            List<Part> parts;
            if (partNames == null) {
                parts = inputMessage.getOrderedParts(null);
            } else {
                parts = new ArrayList<Part>();
                for (String name : partNames) {
                    parts.add((Part) inputMessage.getParts().get(name));
                }
            }

            //noinspection unchecked
            for (Part part : parts) {
                QName tq = part.getTypeName();
                QName eq = part.getElementName();
                if (tq != null && eq != null) {
                    if (audit != null) audit.logAndAudit(CommonMessages.WSDL_OPERATION_PART_TYPE, part.getName());
                    return null;
                } else if (tq != null) {
                    operationQNames.add(new QName(null, part.getName()));
                    if (audit != null) audit.logAndAudit(CommonMessages.WSDL_OPERATION_PART_TYPE, inputMessage.getQName().getLocalPart());
                } else if (eq != null) {
                    operationQNames.add(eq);
                }
            }
        } else {
            if (audit != null) audit.logAndAudit(CommonMessages.WSDL_OPERATION_BAD_STYLE, operationStyle, bindingOperation.getName());
            return null;
        }

        if (operationQNames.isEmpty()) {
            if (audit != null) audit.logAndAudit(CommonMessages.WSDL_OPERATION_NO_QNAMES_FOR_OP, bindingOperation.getName());
        }

        return operationQNames;
    }

    private interface OperationSearchContext {
        Element getPayload() throws InvalidDocumentFormatException, IOException, SAXException;
        String getSoapaction();
        boolean hasHttpRequestKnob() throws IOException;
        Wsdl getWsdl();
    }

    public static Collection getOperationNames(final Wsdl wsdl) {
        ArrayList<String> output = new ArrayList<String>();

        //noinspection unchecked
        Map<QName, Binding> bindings = wsdl.getDefinition().getBindings();
        if (bindings.isEmpty()) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
            return output;
        }
        boolean foundSoapBinding = false;
        for (QName bindingName : bindings.keySet()) {
            Binding binding = bindings.get(bindingName);
            SOAPBinding soapBinding = null;
            //noinspection unchecked
            List<ExtensibilityElement> bindingEels = binding.getExtensibilityElements();
            for (ExtensibilityElement element : bindingEels) {
                if (element instanceof SOAPBinding) {
                    foundSoapBinding = true;
                    soapBinding = (SOAPBinding) element;
                }
            }

            if (soapBinding == null)
                continue; // This isn't a SOAP binding; we don't care
            //noinspection unchecked
            List<BindingOperation> bindingOperations = binding.getBindingOperations();
            for (BindingOperation bindingOperation : bindingOperations) {
                String tmp = bindingOperation.getOperation().getName();
                if (!output.contains(tmp)) output.add(tmp);
            }

        }
        if (!foundSoapBinding) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
        }
        return output;
    }

    public static Operation getOperation(final Wsdl wsdl, final Message request)
            throws IOException, SAXException, InvalidDocumentFormatException {
        final XmlKnob requestXml = (XmlKnob)request.getKnob(XmlKnob.class);
        if (requestXml == null) {
            log.info("Can't get operation for non-XML message");
            return null;
        }
        OperationSearchContext context = new OperationSearchContext() {
            public Wsdl getWsdl() {
                return wsdl;
            }

            public Element getPayload() throws InvalidDocumentFormatException, IOException, SAXException {
                if (payload == null) {
                    Document requestDoc = requestXml.getDocumentReadOnly();
                    payload = getPayloadElement(requestDoc);
                }
                return payload;
            }

            public boolean hasHttpRequestKnob() throws IOException {
                if (hasHttpRequestKnob == null) {
                    HttpRequestKnob requestHttp = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
                    if (requestHttp == null) {
                        hasHttpRequestKnob = Boolean.FALSE;
                    } else {
                        hasHttpRequestKnob = Boolean.TRUE;
                        saction = stripQuotes(requestHttp.getHeaderSingleValue(SOAPACTION));
                    }
                }
                return hasHttpRequestKnob.booleanValue();
            }

            public String getSoapaction() {
                return saction;
            }

            private Element payload = null;
            Boolean hasHttpRequestKnob = null;
            String saction = null;
        };

        //noinspection unchecked
        Map<QName, Binding> bindings = wsdl.getDefinition().getBindings();
        if (bindings.isEmpty()) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
            return null;
        }

        boolean foundSoapBinding = false;
        for (QName bindingName : bindings.keySet()) {
            Binding binding = bindings.get(bindingName);
            SOAPBinding soapBinding = null;
            //noinspection unchecked
            List<ExtensibilityElement> bindingEels = binding.getExtensibilityElements();
            for (ExtensibilityElement element : bindingEels) {
                if (element instanceof SOAPBinding) {
                    foundSoapBinding = true;
                    soapBinding = (SOAPBinding) element;
                }
            }

            if (soapBinding == null)
                continue; // This isn't a SOAP binding; we don't care
            Operation res = getOperationFromBinding(binding, context);
            if (res != null) return res;
        }
        if (!foundSoapBinding) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
        } else {
            log.info("none of the binding could match exactly one operation from this request");
        }
        return null;
    }
}
