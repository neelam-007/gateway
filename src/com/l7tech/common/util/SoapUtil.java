/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.xml.ElementAlreadyExistsException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import org.w3c.dom.*;
import org.w3c.dom.Node;

import javax.xml.rpc.NamespaceConstants;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapUtil {

    public static final List ENVELOPE_URIS = new ArrayList();

    static {
        ENVELOPE_URIS.add(SOAPConstants.URI_NS_SOAP_ENVELOPE);
        ENVELOPE_URIS.add("http://www.w3.org/2001/06/soap-envelope");
        ENVELOPE_URIS.add("http://www.w3.org/2001/09/soap-envelope");
        ENVELOPE_URIS.add("urn:schemas-xmlsoap-org:soap.v1");
    }

    // Namespace prefix constants
    public static final String SOAP_ENV_PREFIX = NamespaceConstants.NSPREFIX_SOAP_ENVELOPE;
    public static final String XMLNS = "xmlns";
    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";

    // Namespace constants
    public static final String SECURITY_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";
    public static final String SECURITY_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2002/07/secext";
    public static final String SECURITY_NAMESPACE4 = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String DIGSIG_URI = "http://www.w3.org/2000/09/xmldsig#";
    public static final String WSU_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String WSU_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    public static final String WSSC_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/04/sc";

    public static final String L7_MESSAGEID_NAMESPACE = "http://www.layer7tech.com/ws/addr";
    public static final String L7_MESSAGEID_PREFIX = "L7a";

    public static final List SECURITY_URIS = new ArrayList();

    static {
        SECURITY_URIS.add(SECURITY_NAMESPACE);
        SECURITY_URIS.add(SECURITY_NAMESPACE2);
        SECURITY_URIS.add(SECURITY_NAMESPACE3);
        SECURITY_URIS.add(SECURITY_NAMESPACE4);
    }

    public static final String[] SECURITY_URIS_ARRAY = (String[])SECURITY_URIS.toArray(new String[0]);

    public static final String WSU_PREFIX = "wsu";
    public static final List WSU_URIS = new ArrayList();

    static {
        WSU_URIS.add(WSU_NAMESPACE);
        WSU_URIS.add(WSU_NAMESPACE2);
    }

    public static final String[] WSU_URIS_ARRAY = (String[])WSU_URIS.toArray(new String[0]);

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
    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";
    public static final String SOAPACTION = "SOAPAction";
    public static final String SUPPORTED_ENCRYPTEDKEY_ALGO = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
    public static final String VALUETYPE_SKI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier";
    public static final String VALUETYPE_X509 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3";
    public static final String VALUETYPE_DERIVEDKEY = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk";
    public static final String VALUETYPE_SECURECONV = "http://schemas.xmlsoap.org/ws/2004/04/sc";
    public static final String ENCODINGTYPE_BASE64BINARY = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary";
    public static final String ALGORITHM_PSHA = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk/p_sha1";

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
    public static final String SOAP_ENVELOPE_XPATH = "/" + NamespaceConstants.NSPREFIX_SOAP_ENVELOPE + ":" + ENVELOPE_EL_NAME;

    /**
     * soap body xpath '/soapenv:Envelope/soapenv:Body'
     */
    public static final String SOAP_BODY_XPATH = SOAP_ENVELOPE_XPATH + "/" + NamespaceConstants.NSPREFIX_SOAP_ENVELOPE + ":Body";

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
     * The Header Element, or null if it's not present
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
     * The Body {@link Element}, or null if it's not present
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

    public static SOAPMessage makeMessage() {
        MessageFactory mf = null;
        try {
            mf = MessageFactory.newInstance();
            SOAPMessage smsg = mf.createMessage();
            return smsg;
        } catch (SOAPException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public static SOAPFault addFaultTo(SOAPMessage message, String faultCode, String faultString, String faultActor) throws SOAPException {
        SOAPPart spart = message.getSOAPPart();
        SOAPEnvelope senv = spart.getEnvelope();
        SOAPBody body = senv.getBody();
        SOAPFault fault = body.addFault();
        fault.setFaultCode(faultCode);
        fault.setFaultString(faultString);
        if (faultActor != null)
            fault.setFaultActor(faultActor);
        return fault;
    }

    /**
     * Find the Namespace URI of the given document, which is assumed to contain a SOAP Envelope.
     *
     * @param request the SOAP envelope to examine
     * @return the body's namespace URI, or null if not found or the document isn't SOAP.
     */
    public static String getNamespaceUri(Document request) throws InvalidDocumentFormatException {
        Element body = SoapUtil.getBodyElement(request);
        if (body == null) return null;
        Node n = body.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE)
                return n.getNamespaceURI();
            n = n.getNextSibling();
        }
        return null;
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
        List securityElements = XmlUtil.findChildElementsByName(header, SECURITY_URIS_ARRAY, SECURITY_EL_NAME);
        for (Iterator i = securityElements.iterator(); i.hasNext();) {
            Element securityEl = (Element)i.next();
            if (isDefaultSecurityHeader(securityEl))
                return securityEl;
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
     * @return
     */
    public static Element makeSecurityElement(Document soapMsg) {
        Element header = getOrMakeHeader(soapMsg);
        Element securityEl = soapMsg.createElementNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
        securityEl.setPrefix(SECURITY_NAMESPACE_PREFIX);
        securityEl.setAttribute("xmlns:" + SECURITY_NAMESPACE_PREFIX, SECURITY_NAMESPACE);
        setSoapAttr(soapMsg, securityEl, MUSTUNDERSTAND_ATTR_NAME, "true");

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

        for (Iterator i = ENVELOPE_URIS.iterator(); i.hasNext();) {
            String ns = (String)i.next();
            element.removeAttributeNS(ns, attrName);
        }

    }

    /**
     * Set a SOAP-specific attribute on the specified element.
     * The new attribute will be created using the namespace and prefix from the document element, which
     * is assumed to be a SOAP Envelope.
     *
     * @param soapMsg
     * @param elementNeedingAttr
     * @param attrName
     * @param attrValue
     */
    public static void setSoapAttr(Document soapMsg, Element elementNeedingAttr, String attrName, String attrValue) {
        Element env = soapMsg.getDocumentElement();
        String soapNs = env.getNamespaceURI();
        String prefix = env.getPrefix();
        if (prefix == null) {
            // SOAP must be the default namespace.  We'll have to declare a prefix of our own.
            prefix = "soap8271"; // todo - find an unused prefix
            elementNeedingAttr.setAttributeNS(soapNs,
              prefix + ":" + attrName,
              attrValue);
            if (elementNeedingAttr.getAttribute("xmlns:" + prefix).length() < 1)
                elementNeedingAttr.setAttribute("xmlns:" + prefix, soapNs);
        } else {
            elementNeedingAttr.setAttributeNS(soapNs,
              prefix + ":" + attrName,
              attrValue);
        }
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
        String url = null;
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
                id = node.getAttribute(ID_ATTRIBUTE_NAME);
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
        for (Iterator i = ENVELOPE_URIS.iterator(); i.hasNext();) {
            String ns = (String)i.next();
            actor = element.getAttributeNS(ns, ACTOR_ATTR_NAME);
            if (actor != null && actor.length() > 0)
                return ACTOR_VALUE_NEXT.equals(actor);
        }

        // No actor; check role (SOAP 1.2)
        String role = element.getAttribute(ROLE_ATTR_NAME);
        if (role != null && role.length() > 0)
            return ROLE_VALUE_NEXT.equals(role);
        for (Iterator i = ENVELOPE_URIS.iterator(); i.hasNext();) {
            String ns = (String)i.next();
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
        List allofthem = getSecurityElements(soapMsg);
        if (allofthem == null || allofthem.size() < 1) return null;
        for (Iterator i = allofthem.iterator(); i.hasNext();) {
            Element element = (Element)i.next();
            if (isDefaultSecurityHeader(element))
                return element;
        }
        return null; // no security header for us
    }

    /**
     * Returns all Security elements.
     *
     * @return never null
     * @throws InvalidDocumentFormatException if there is more than one soap:Header or the message isn't SOAP
     */
    public static List getSecurityElements(Document soapMsg) throws InvalidDocumentFormatException {
        Element header = getHeaderElement(soapMsg);
        if (header == null) return Collections.EMPTY_LIST;
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
        idEl.appendChild(idEl.getOwnerDocument().createTextNode(messageId));
    }

    /**
     * Set the L7a:RelatesTo URI for the specified message, which must not already have a L7a:RelatesTo element.
     *
     * @param soapMsg   the soap message to modify
     * @param relatesTo the new L7a:RelatesTo value
     * @throws InvalidDocumentFormatException if the message isn't soap, has more than one header, or already has
     *                                        a RelatesTo
     */
    public static void setL7aRelatesTo(Document soapMsg, String relatesTo) throws InvalidDocumentFormatException {
        Element idEl = getL7aRelatesToElement(soapMsg);
        if (idEl != null)
            throw new ElementAlreadyExistsException("This message already has a L7a:RelatesTo");

        Element header = getOrMakeHeader(soapMsg);
        idEl = XmlUtil.createAndPrependElementNS(header, RELATESTO_EL_NAME, L7_MESSAGEID_NAMESPACE, L7_MESSAGEID_PREFIX);
        idEl.appendChild(idEl.getOwnerDocument().createTextNode(relatesTo));
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
        SOAPMessage sm = MessageFactory.newInstance().createMessage();
        sm.getSOAPPart().setContent(new DOMSource(doc));
        return sm;
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
        return domNodeToSoapElement(domNode,soapElement, sf);

    }


    /**
     * Add the dom node, and its child nodes to the SOAP element.
     *
     * @param node the node to add
     * @param soapElement the soap element (SOAPBody, SOAPHeader, SOAPElement etc)
     * @param soapFactory the soap factory
     *
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

    public static SOAPMessage makeFaultMessage(String faultCode, String faultString, String faultActor) {
        SOAPMessage msg = makeMessage();
        try {
            SoapUtil.addFaultTo(msg, faultCode, faultString, faultActor);
            return msg;
        } catch (SOAPException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public static String soapMessageToString(SOAPMessage msg, String encoding) throws IOException, SOAPException {
        return new String(soapMessageToByteArray(msg), encoding);
    }

    public static byte[] soapMessageToByteArray(SOAPMessage msg) throws IOException, SOAPException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        msg.writeTo(baos);
        return baos.toByteArray();
    }
}
