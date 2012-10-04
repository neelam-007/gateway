/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.xml.soap;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.MessageNotSoapException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.xml.soap.SoapVersion.SOAP_1_1;

/**
 * @author alex
 */
@SuppressWarnings({"JavaDoc"})
public class SoapUtil extends SoapConstants {
    public static final Logger log = Logger.getLogger(SoapUtil.class.getName());

    public static final String PROPERTY_DISCLOSE_ELEMENT_NAME_IN_WSU_ID = "com.l7tech.xml.soap.discloseElementNameInWsuId";
    public static final boolean DISCLOSE_ELEMENT_NAME_IN_WSU_ID = ConfigFactory.getBooleanProperty( PROPERTY_DISCLOSE_ELEMENT_NAME_IN_WSU_ID, false );

    // Bug #6478
    public static final String PROPERTY_MUSTUNDERSTAND = "com.l7tech.common.security.xml.decorator.secHdrMustUnderstand";

    public static final String PROPERTY_ID_CONFIG = "com.l7tech.common.security.xml.idAttributeConfig";

    private static final String DEFAULT_UUID_PREFIX = "http://www.layer7tech.com/uuid/";

    private static final AtomicReference<Pair<String,IdAttributeConfig>> idAttributeConfig = new AtomicReference<Pair<String, IdAttributeConfig>>();

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
        if (!SoapConstants.ENVELOPE_URIS.contains(env.getNamespaceURI()))
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
        return DomUtils.findOnlyOneChildElementByName(envelope, soapNs, HEADER_EL_NAME);
    }

    /**
     * Get the Body {@link Element}, or null if it's not present
     *
     * @param message the message to examine
     * @return the body, or null if there isn't one
     * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body element
     */
    @Nullable
    public static Element getBodyElement(Document message) throws InvalidDocumentFormatException {
        Element env = getEnvelopeElement(message);
        String soapNs = env.getNamespaceURI();
        return DomUtils.findOnlyOneChildElementByName(env, soapNs, BODY_EL_NAME);
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
            return DomUtils.findFirstChildElement(body);
        return null;
    }

    public static MessageFactory getMessageFactory() {
        return getMessageFactory(SOAPConstants.SOAP_1_1_PROTOCOL);
    }

    public static MessageFactory getMessageFactory(String soapProtocol) {
        try {
            return MessageFactory.newInstance(soapProtocol);
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
        if (!SoapConstants.ENVELOPE_URIS.contains(env.getNamespaceURI())) {
            log.finer("Request document element not in recognized SOAP namespace; assuming non-SOAP request");
            return null; // not soap
        }

        Element body;
        try {
            body = DomUtils.findOnlyOneChildElementByName(env, env.getNamespaceURI(), BODY_EL_NAME);
        } catch ( TooManyChildElementsException e) {
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

        return payloadNames.toArray(new QName[payloadNames.size()]);
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
        Element header = DomUtils.findFirstChildElementByName(env, soapNs, HEADER_EL_NAME);
        if (header != null)
            return header; // found it

        header = soapMsg.createElementNS(soapNs, HEADER_EL_NAME);
        header.setPrefix(soapPrefix);

        Element body = DomUtils.findFirstChildElementByName(env, soapNs, BODY_EL_NAME);
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
        List<Element> securityElements = DomUtils.findChildElementsByName(header, SECURITY_URIS_ARRAY, SECURITY_EL_NAME);
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
        return makeSecurityElement(soapMsg, preferredWsseNamespace, null, isSecHdrDefaultsToMustUnderstand(), true);
    }

    /**
     * @return true if Security headers should be created with mustUnderstand asserted, if a preference
     *         is not explicitly made at the time of creation.
     */
    public static boolean isSecHdrDefaultsToMustUnderstand() {
        return ConfigFactory.getBooleanProperty(PROPERTY_MUSTUNDERSTAND, true);
    }

    public static Element makeSecurityElement(Document soapMsg, String preferredWsseNamespace, String actor, Boolean mustUnderstand) {
        return makeSecurityElement(soapMsg, preferredWsseNamespace, actor, mustUnderstand, true);
    }

    public static Element makeSecurityElement(Document soapMsg, String preferredWsseNamespace, String actor, Boolean mustUnderstand, boolean namespaceActor) {
        Element header = getOrMakeHeader(soapMsg);
        Element securityEl = soapMsg.createElementNS(preferredWsseNamespace, SECURITY_EL_NAME);
        securityEl.setPrefix(SECURITY_NAMESPACE_PREFIX);
        securityEl.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:" + SECURITY_NAMESPACE_PREFIX, preferredWsseNamespace);

        boolean isSoap12 = SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(soapMsg.getDocumentElement().getNamespaceURI());
        if ( mustUnderstand != null ) {
            if(isSoap12) {
                setSoapAttr(securityEl, MUSTUNDERSTAND_ATTR_NAME, mustUnderstand ? "true" : "false");
            } else {
                setSoapAttr(securityEl, MUSTUNDERSTAND_ATTR_NAME, mustUnderstand ? "1" : "0");
            }
        }
        if (actor != null) {
            if(isSoap12) {
                setSoapAttr(securityEl, SoapUtil.ROLE_ATTR_NAME, actor);
            } else if (namespaceActor) {
                setSoapAttr(securityEl, SoapUtil.ACTOR_ATTR_NAME, actor);
            } else {
                securityEl.setAttribute(SoapUtil.ACTOR_ATTR_NAME, actor);
            }
        }
        Element existing = DomUtils.findFirstChildElement(header);
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
            prefix = DomUtils.getOrCreatePrefixForNamespace(elementNeedingAttr, soapNs, "soap");
            elementNeedingAttr.setAttributeNS(soapNs,
                                              prefix + ":" + attrName,
                                              attrValue);
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
     * @throws InvalidDocumentFormatException  if the element contained more than one attribute recognized as an ID attribute.
     */
    public static Element getElementByWsuId(Document doc, String elementId) throws InvalidDocumentFormatException {
        return DomUtils.getElementByIdValue(doc, elementId, getDefaultIdAttributeConfig());
    }

    /**
     * Gets the WSU:Id attribute of the passed element using all supported WSU namespaces, and, as a special case,
     * SAML1 AssertionID and SAML2 ID values too.
     *
     * @return the string value of the attribute or null if the attribute is not present
     * @throws InvalidDocumentFormatException  if the element contained more than one attribute recognized as an ID attribute.
     */
    public static String getElementWsuId(Element node) throws InvalidDocumentFormatException {
        return DomUtils.getElementIdValue(node, getDefaultIdAttributeConfig());
    }

    /**
     * Get the current system-wide default ID attribute config.
     *
     * @return the ID attribute config curently configured via system property.
     */
    public static IdAttributeConfig getDefaultIdAttributeConfig() {
        String idConfig = ConfigFactory.getProperty(PROPERTY_ID_CONFIG, null);
        if (idConfig == null)
            return DEFAULT_ID_ATTRIBUTE_CONFIG;

        Pair<String, IdAttributeConfig> p = idAttributeConfig.get();
        if (p != null && p.left.equals(idConfig))
            return p.right;

        synchronized (idAttributeConfig) {
            p = idAttributeConfig.get();
            if (p != null && p.left.equals(idConfig))
                return p.right;

            IdAttributeConfig cfg = null;
            try {
                cfg = IdAttributeConfig.fromString(idConfig);
            } catch (ParseException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                log.log(Level.WARNING, "Invalid ID attribute config string: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            p = new Pair<String, IdAttributeConfig>(idConfig, cfg);
            idAttributeConfig.set(p);
            return cfg;
        }
    }

    /**
     * Get a wsu:Id attribute from the specified element, creating a new one if needed.
     * <p/>
     * This method should be used as a last resort since it is slow, has a tiny probability
     * of generating non-unique IDs, and provides no way to configure which wsu namespace to use.
     *
     * @param node the element to examine.  May be modified if a wsu:Id needs to be added.  Required.
     * @return the wsu:Id value from this element, possibly newly-created.
     * @throws InvalidDocumentFormatException  if the element contained more than one attribute recognized as an ID attribute.
     */
    public static String getOrCreateElementWsuId(Element node) throws InvalidDocumentFormatException {
        return getOrCreateElementWsuId(node, 0);
    }

    public static String getOrCreateElementWsuId(Element node, int baseNumber) throws InvalidDocumentFormatException {
        String id = getElementWsuId(node);
        if (id != null)
            return id;

        final String prefix = DISCLOSE_ELEMENT_NAME_IN_WSU_ID ? node.getLocalName() : null;
        id = generateUniqueId(prefix, baseNumber);
        setWsuId(node, SoapConstants.WSU_NAMESPACE, id);
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

    /**
     * Get the security header for a Layer 7 actor.
     *
     * <p>If there are multiple headers that target Layer 7 actors then this
     * will find the header for the "preferred" actor, not the first matching
     * security header in the message.</p>
     *
     * @param soapMsg The soap message to process (must not be null)
     * @return The element or null if not found
     * @throws InvalidDocumentFormatException If the document is not soap
     */
    public static Element getSecurityElementForL7( final Document soapMsg ) throws InvalidDocumentFormatException {
        Element l7secheader = null;

        if ( soapMsg == null || soapMsg.getDocumentElement()==null ) {
            throw new InvalidDocumentFormatException( "Document missing or empty." );            
        }

        SoapVersion soapVersion = SoapVersion.namespaceToSoapVersion( soapMsg.getDocumentElement().getNamespaceURI() );
        String actors = soapVersion == SOAP_1_1 || soapVersion == SoapVersion.UNKNOWN ?
                ConfigFactory.getProperty( "com.l7tech.xml.soap.actors", SoapUtil.L7_SOAP_ACTORS ) :
                ConfigFactory.getProperty( "com.l7tech.xml.soap.roles", SoapUtil.L7_SOAP_ACTORS );

        for ( String actor : actors.split("\\s{1,1024}") ) {
           l7secheader = SoapUtil.getSecurityElement( soapMsg, actor );
            if ( l7secheader != null ) {
                break;
            }
        }

        return l7secheader;
    }

    /**
     * Is the given element addressed to the Layer7 actor/role.
     *
     * <p>Note that this method does not validate that the given element is a
     * child of the SOAP header.</p>
     *
     * <p>An element with the "next" role/actor is NOT counted as addressed to
     * layer 7.</p>
     *
     * @param element The element to check.
     * @return true if the element is for Layer7
     */
    public static boolean isElementForL7( final Element element ) {
        boolean isL7ActorOrRole = false;

        Document document = element.getOwnerDocument();
        if ( document != null ) {
            final String actorValue = getActorValue( element );
            if ( actorValue != null ) {
                SoapVersion soapVersion = SoapVersion.namespaceToSoapVersion( document.getDocumentElement().getNamespaceURI() );
                String actors = soapVersion == SOAP_1_1 || soapVersion == SoapVersion.UNKNOWN ?
                        ConfigFactory.getProperty( "com.l7tech.xml.soap.actors", SoapUtil.L7_SOAP_ACTORS ) :
                        ConfigFactory.getProperty( "com.l7tech.xml.soap.roles", SoapUtil.L7_SOAP_ACTORS );

                for ( String actor : actors.split("\\s{1,1024}") ) {
                    if ( actor.equals(actorValue) ) {
                        isL7ActorOrRole = true;
                        break;
                    }
                }
            }
        }

        return isL7ActorOrRole;
    }

    public static void nukeActorAttribute(Element el) {
        String attrName = SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(el.getOwnerDocument().getDocumentElement().getNamespaceURI()) ? SoapUtil.ROLE_ATTR_NAME : SoapUtil.ACTOR_ATTR_NAME;
        el.removeAttribute(attrName);
        for (String ns : ENVELOPE_URIS) {
            el.removeAttributeNS(ns, attrName);
        }
    }

    public static String getActorValue(Element element) {
        String localactor;
        if(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(element.getOwnerDocument().getDocumentElement().getNamespaceURI())) {
            localactor = element.getAttributeNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, ROLE_ATTR_NAME);
        } else {
            localactor = element.getAttribute(SoapUtil.ACTOR_ATTR_NAME);
            if (localactor == null || localactor.length() < 1) {
                for (String ns : ENVELOPE_URIS) {
                    localactor = element.getAttributeNS(ns, SoapUtil.ACTOR_ATTR_NAME);
                    if (localactor != null && localactor.length() > 0) {
                        return localactor;
                    }
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
        return DomUtils.findChildElementsByName(header, SECURITY_URIS_ARRAY, SECURITY_EL_NAME);
    }

    /**
     * Get the security element from a specific header instead of the entire soap message.
     * This alternative is used in the case of nested soap messages.
     *
     * @return null if element not present, the security element if it's in the doc
     */
    public static Element getSecurityElement(Element header) {
        List secElements = DomUtils.findChildElementsByName(header, SECURITY_URIS_ARRAY, SECURITY_EL_NAME);
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
        return DomUtils.findOnlyOneChildElementByName(header, L7_MESSAGEID_NAMESPACE, MESSAGEID_EL_NAME);
    }


    /**
     * Get the wsa:MesageID element from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @param otherWsaNamespaceUri an additional WS-Addressing namespace URI to use in the search, or null to use the default URIs
     * @return the /Envelope/Header/wsa:MessageID element, or null if there isn't one.
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or MessageID
     */
    public static Element getWsaMessageIdElement(Document soapMsg, String otherWsaNamespaceUri) throws InvalidDocumentFormatException {
        final String elName = MESSAGEID_EL_NAME;
        return getWsaElement(soapMsg, elName, otherWsaNamespaceUri);
    }

    private static Element getWsaElement(Document soapMsg, String elName, String otherWsaNamespaceUri) throws InvalidDocumentFormatException {
        Element header = getHeaderElement(soapMsg);
        if (header == null) return null;
        final String[] namespaces;
        if (otherWsaNamespaceUri == null) {
            namespaces = SoapConstants.WSA_NAMESPACE_ARRAY;
        } else {
            namespaces = new String[SoapConstants.WSA_NAMESPACE_ARRAY.length+1];
            System.arraycopy( SoapConstants.WSA_NAMESPACE_ARRAY, 0, namespaces, 0, SoapConstants.WSA_NAMESPACE_ARRAY.length);
            namespaces[SoapConstants.WSA_NAMESPACE_ARRAY.length] = otherWsaNamespaceUri;
        }
        return DomUtils.findOnlyOneChildElementByName(header, namespaces, elName);
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
        return DomUtils.getTextValue(messageId);
    }

    public static String getWsaMessageId(Document soapMsg, String otherWsaNamespaceUri) throws InvalidDocumentFormatException {
        Element messageId = getWsaMessageIdElement(soapMsg, otherWsaNamespaceUri);
        if (messageId == null) return null;
        return DomUtils.getTextValue(messageId);
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
        idEl = DomUtils.createAndPrependElementNS(header, MESSAGEID_EL_NAME, L7_MESSAGEID_NAMESPACE, L7_MESSAGEID_PREFIX);
        idEl.appendChild(DomUtils.createTextNode(idEl, messageId));
    }

    /**
     * Set the wsaa:MessageID URI for the specified message, which must not already have a wsa:MessageID element.
     *
     * @param soapMsg   the soap message to modify
     * @param wsaNamespaceUri the WS-Addressing namespace URI to use for the element, or null to use {@link #WSA_NAMESPACE2}.
     * @param messageId the new wsa:MessageID value
     * @throws InvalidDocumentFormatException if the message isn't soap, has more than one header, or already has
     *                                        a MessageID
     */
    public static Element setWsaMessageId(Document soapMsg, String wsaNamespaceUri, String messageId) throws InvalidDocumentFormatException {
        Element idEl = getWsaMessageIdElement(soapMsg, wsaNamespaceUri);
        if (idEl != null)
            throw new ElementAlreadyExistsException("This message already has a wsa:MessageID");

        Element header = getOrMakeHeader(soapMsg);
        final String uri = wsaNamespaceUri == null ? WSA_NAMESPACE2 : wsaNamespaceUri;
        idEl = DomUtils.createAndPrependElementNS(header, MESSAGEID_EL_NAME, uri, "wsa");
        idEl.appendChild(DomUtils.createTextNode(idEl, messageId));
        return idEl;
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
        idEl = DomUtils.createAndPrependElementNS(header, RELATESTO_EL_NAME, L7_MESSAGEID_NAMESPACE, L7_MESSAGEID_PREFIX);
        idEl.appendChild(DomUtils.createTextNode(idEl, relatesTo));
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
        return DomUtils.findOnlyOneChildElementByName(header, L7_MESSAGEID_NAMESPACE, RELATESTO_EL_NAME);
    }

    /**
     * Get the L7a:RelatesTo element from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @return the /Envelope/Header/L7a:RelatesTo element, or null if there wasn't one
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or RelatesTo
     */
    public static Element getWsaRelatesToElement(Document soapMsg) throws InvalidDocumentFormatException {
        return getWsaRelatesToElement(soapMsg, null);
    }

    /**
     * Get the L7a:RelatesTo element from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @param otherNs an additional WS-Addressing namespace URI to recognize (optional, may be null)
     * @return the /Envelope/Header/L7a:RelatesTo element, or null if there wasn't one
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or RelatesTo
     */
    public static Element getWsaRelatesToElement(Document soapMsg, String otherNs) throws InvalidDocumentFormatException {
        return getWsaElement(soapMsg, RELATESTO_EL_NAME, otherNs);
    }

    /**
     * Get all WS-Addressing elements from the specified message.
     *
     * <p>The elements can be from any (single) supported addressing namespace.</p>
     *
     * @param soapMsg The soap envelope to examine
     * @return The addressing elements (may be empty, never null)
     */
    public static List<Element> getWsaAddressingElements(Document soapMsg) throws InvalidDocumentFormatException {
        return getWsaAddressingElements(soapMsg, null);
    }

    /**
     * Get all WS-Addressing elements from the specified message.
     *
     * <p>The elements can be from any (single) supported addressing namespace.</p>
     *
     * @param soapMsg The soap envelope to examine
     * @param otherNamespaces The namespaces to check instead of the default addressing namespaces
     * @return The addressing elements (may be empty, never null)
     */
    public static List<Element> getWsaAddressingElements(Document soapMsg, String[] otherNamespaces) throws InvalidDocumentFormatException {
        Element header = getHeaderElement(soapMsg);
        if (header == null) return Collections.emptyList();

        final List<Element> addressingElements = new ArrayList<Element>();
        final String[] namespaces;
        if (otherNamespaces == null) {
            namespaces = SoapConstants.WSA_NAMESPACE_ARRAY;
        } else {
            namespaces = otherNamespaces;
        }

        for ( final String namespace : namespaces ) {
            Node childNode = header.getFirstChild();
            while ( childNode != null ) {
                if ( childNode.getNodeType() == Node.ELEMENT_NODE ) {
                    if ( namespace.equals( childNode.getNamespaceURI() )) {
                        addressingElements.add( (Element) childNode );
                    }
                }
                childNode = childNode.getNextSibling();
            }

            // break if found
            if ( !addressingElements.isEmpty() ) {
                break;
            }
        }

        return addressingElements;
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
        return DomUtils.getTextValue(relatesTo);
    }

    /**
     * Get the wsa:RelatesTo URI from the specified message, or null if there isn't one.
     *
     * @param soapMsg the soap envelope to examine
     * @return the body text of the /Envelope/Header/wsa:RelatesTo field, which may be empty or an invalid URI; or,
     *         null if there was no /Envelope/Header/wsa:RelatesTo field.
     * @throws InvalidDocumentFormatException if the message isn't soap, or there is more than one Header or RelatesTo
     */
    public static String getWsaRelatesTo(Document soapMsg) throws InvalidDocumentFormatException {
        Element relatesTo = getWsaRelatesToElement(soapMsg);
        if (relatesTo == null) return null;
        return DomUtils.getTextValue(relatesTo);
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
            throw new MessageNotSoapException("Not Soap Message! \n"+ XmlUtil.nodeToString(doc));
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
     * Quickly check whether the given document has a recognized SOAP envelope and body, without
     * attempting to validate it any further than that.
     *
     * @param doc the document to check.  Required.
     * @return true if this document looks like it wants to be a SOAP envelope
     */
    public static boolean isSoapMessageMaybe(Document doc) {
        Element env = doc.getDocumentElement();
        return ENVELOPE_EL_NAME.equals(env.getLocalName()) &&
               SoapConstants.ENVELOPE_URIS.contains(env.getNamespaceURI()) &&
               null != DomUtils.findFirstChildElementByName(env, env.getNamespaceURI(), BODY_EL_NAME);
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
        return SoapUtil.getBodyElement(doc)==element;
    }

    /**
     * Tests whether a given element is the SOAP Header element.
     *
     * @param node the element to test whether it is the SOAP Header element (may be null)
     * @return true if SOAP Header, false otherwise
     * @throws InvalidDocumentFormatException if the message containing
     *         this element is not SOAP
     */
    public static boolean isHeader( final Node node ) throws InvalidDocumentFormatException {
        boolean header = false;

        if ( node instanceof Element ) { // ensures not null
            final Document document = node.getOwnerDocument();
            if ( document == null ) {
                throw new IllegalArgumentException("The node does not have Owner Document.");
            }

            header = SoapUtil.getHeaderElement( document ) == node;
        }

        return header;
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
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream(4096);
        try {
            msg.writeTo(baos);
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }

    /** @return a new unique URI in the form &lt;prefix&gt; + XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX, where the
     * Xes stand for random hexadecimal digits.  The default prefix is {@link #DEFAULT_UUID_PREFIX}. @param messageIdPrefix an alternate message ID prefix, or null to use {@link #DEFAULT_UUID_PREFIX}.
     * @param includeUuidDashes
     */
    public static String generateUniqueUri(final String messageIdPrefix, boolean includeUuidDashes) {
        StringBuilder sb = new StringBuilder(messageIdPrefix == null ? DEFAULT_UUID_PREFIX : messageIdPrefix);
        byte[] randbytes = new byte[16];
        rand.nextBytes(randbytes);
        final String hex = HexUtils.hexDump(randbytes);
        if (includeUuidDashes) {
            // 8 4 4 4 12
            sb.append(hex.substring(0, 8));
            sb.append("-");
            sb.append(hex.substring(8, 12));
            sb.append("-");
            sb.append(hex.substring(12, 16));
            sb.append("-");
            sb.append(hex.substring(16, 20));
            sb.append("-");
            sb.append(hex.substring(20, 32));
        } else {
            sb.append(hex);
        }
        return sb.toString();
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
     *
     * @param parent      element which will contain the new timestamp subelement
     * @param wsuUri      which wsu: namespace URI to use
     * @param timestamp   time that should be marked in this timestamp.  if null, uses current time
     * @param timestampNanos   number of nanoseconds to add to the millisecond-granular timestamp
     * @param timeoutSec  after how many seconds this timestamp should expirel.  If zero, uses 5 min.
     * @return the timestamp element
     */
    public static Element addTimestamp(Element parent, String wsuUri, Date timestamp, long timestampNanos, int timeoutSec) {
        return addTimestamp( parent, wsuUri, timestamp, true, timestampNanos, timeoutSec );
    }

    /**
     * Append a wsu:Timestamp element to the specified parent element, showing the specified
     * time, or the current time if it isn't specified.
     *
     * @param parent      element which will contain the new timestamp subelement
     * @param wsuUri      which wsu: namespace URI to use
     * @param timestamp   time that should be marked in this timestamp.  if null, uses current time
     * @param millis      true to include milliseconds (and possibly nanoseconds)
     * @param timestampNanos   number of nanoseconds to add to the millisecond-granular timestamp
     * @param timeoutSec  after how many seconds this timestamp should expirel.  If zero, uses 5 min.
     * @return the timestamp element
     */
    private static final long MILLIS_1_DAY =  86400L * 1000L;
    private static final long MILLIS_1_YEAR = 365L * MILLIS_1_DAY;

    public static Element addTimestamp( final Element parent,
                                        final String wsuUri,
                                        final Date timestamp,
                                        final boolean millis,
                                        final long timestampNanos,
                                        final long timeoutSec ) {
        Document message = parent.getOwnerDocument();
        Element timestampEl = message.createElementNS(wsuUri,
                                                      TIMESTAMP_EL_NAME);
        parent.appendChild(timestampEl);
        timestampEl.setPrefix(DomUtils.getOrCreatePrefixForNamespace(timestampEl, wsuUri, "wsu"));

        Calendar now = Calendar.getInstance();
        if (timestamp != null)
            now.setTime(timestamp);
        timestampEl.appendChild(makeTimestampChildElement(timestampEl, CREATED_EL_NAME, now.getTime(), millis, timestampNanos));

        if(timeoutSec == 0)
        {                      
            now.add(Calendar.MILLISECOND,  300000);
        }
        else
        {
            // todo year, days, ms
            long years = timeoutSec/ MILLIS_1_YEAR;
            long days = (timeoutSec - (years * MILLIS_1_YEAR)) / MILLIS_1_DAY;
            long sec  = timeoutSec - (years * MILLIS_1_YEAR) - (days * MILLIS_1_DAY);

            now.add(Calendar.YEAR, (int)years);
            now.add(Calendar.DATE, (int)days);
            now.add(Calendar.MILLISECOND, (int)sec);
        }
        
        timestampEl.appendChild(makeTimestampChildElement(timestampEl, EXPIRES_EL_NAME, now.getTime(), millis, -1));
        return timestampEl;
    }

    private static Element makeTimestampChildElement( final Element parent,
                                                      final String createdElName,
                                                      final Date time,
                                                      final boolean millis,
                                                      final long nanos) {
        Document factory = parent.getOwnerDocument();
        Element element = factory.createElementNS(parent.getNamespaceURI(), createdElName);
        element.setPrefix(parent.getPrefix());
        element.appendChild(DomUtils.createTextNode(factory, ISO8601Date.format(time, millis, nanos)));
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
            String wsuPrefix = DomUtils.getOrCreatePrefixForNamespace(element, wsuNs, "wsu");
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

    public static String findSoapAction( BindingOperation operation) {
        Iterator eels = operation.getExtensibilityElements().iterator();
        ExtensibilityElement ee;
        while ( eels.hasNext() ) {
            ee = (ExtensibilityElement)eels.next();
            if ( ee instanceof SOAPOperation ) {
                SOAPOperation sop = (SOAPOperation)ee;
                return sop.getSoapActionURI();
            } else if( ee instanceof SOAP12Operation) {
                SOAP12Operation sop = (SOAP12Operation)ee;
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
                        } else if (eel instanceof SOAP12Body) {
                            SOAP12Body body = (SOAP12Body) eel;
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
                                    } else if (mimeEel instanceof SOAP12Body) {
                                        SOAP12Body body = (SOAP12Body) mimeEel;
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

    private static Operation getOperationFromBinding( Binding soapbinding, OperationSearchContext context)
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

        if ( context.getPayload() != null ) {
            bindingOperations = matchOperationsWithSoapBody(context.getPayload(), bindingOperations, context.getWsdl());
        }
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
    public static String findTargetNamespace( Definition def, BindingOperation bindingOperation) {
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
                } else if (ee instanceof SOAP12Body) {
                    SOAP12Body body = (SOAP12Body)ee;
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
     * Creates an empty SOAP document and returns the Body element, ready to have the payload added to it.
     *
     * @return the Body element of a newly-created empty SOAP document (created with XmlUtil document factory).  Never null.
     */
    public static Element createSoapEnvelopeAndGetBody(SoapVersion version) {
        String nsUri = SoapVersion.SOAP_1_1.equals(version) ? SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE : SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE;
        Document doc = XmlUtil.createEmptyDocument("Envelope", "s", nsUri);
        Element env = doc.getDocumentElement();
        return XmlUtil.createAndAppendElementNS(env, "Body", env.getNamespaceURI(), env.getPrefix());
    }

    public interface OperationListener {
        void notifyNoStyle( String name );
        void notifyPartName( String name );
        void notifyPartInvalid( String name );
        void notifyBadStyle( String operationStyle, String name );
        void notifyNoNames( String name );
    }

    /**
     * Interface for resolving an XML Schema type to its related elements.
     *
     * <p>Technically the resolved elements will be a set, which may be
     * empty. The resulting collection may contain a null QName if the type
     * has (only) optional content.</p>
     */
    public interface SchemaTypeResolver {
        
        /**
         * Find elements that satisfy the given type requirement.
         *
         * @param type The type QName
         * @return The collection of element QNames
         */
        Collection<QName> resolveType( QName type );
    }

    /**
     * Gets the QName(s) of the element(s) that should appear as children of the SOAP Body element for messages destined
     * for the provided operation.
     *
     * <p>The result is a set of lists of element QNames. Each item in the set
     * is an option for the operation and each item in the list is a required
     * part of that option.</p>
     *
     * <p>If an operation has no payload then this is indicated by an empty
     * QName list.</p>
     * 
     * @param bindingOperation the operation to get QNames from. Must not be null.
     * @param bindingStyle     the style from the SOAP binding element; will be used if the operation doesn't specify its own style. May be null, indicating a default of "document".
     * @param listener         a listener implementation to notify of errors / warnings to.  May be null.
     * @return The set of qname lists
     */
    public static Set<List<QName>> getOperationPayloadQNames( final BindingOperation bindingOperation,
                                                              final String bindingStyle,
                                                              final OperationListener listener )
    {
        return getOperationPayloadQNames( bindingOperation, bindingStyle, listener, null );
    }

    /**
     * Gets the QName(s) of the element(s) that should appear as children of the SOAP Body element for messages destined
     * for the provided operation.
     *
     * <p>The result is a set of lists of element QNames. Each item in the set
     * is an option for the operation and each item in the list is a required
     * part of that option.</p>
     *
     * <p>If an operation has no payload then this is indicated by an empty
     * QName list.</p>
     *
     * @param bindingOperation the operation to get QNames from. Must not be null.
     * @param bindingStyle     the style from the SOAP binding element; will be used if the operation doesn't specify its own style. May be null, indicating a default of "document".
     * @param listener         a listener implementation to notify of errors / warnings to.  May be null.
     * @param schemaTypeResolver the XML Schema type resolver.  May be null.
     * @return The set of qname lists
     */
    @SuppressWarnings({ "unchecked" })
    public static Set<List<QName>> getOperationPayloadQNames( final BindingOperation bindingOperation,
                                                              final String bindingStyle,
                                                              final OperationListener listener,
                                                              final SchemaTypeResolver schemaTypeResolver )
    {
        final List<ExtensibilityElement> bopEels = bindingOperation.getExtensibilityElements();
        String operationStyle = null;
        for (ExtensibilityElement eel : bopEels) {
            if (eel instanceof SOAPOperation) {
                operationStyle = ((SOAPOperation) eel).getStyle();
            } else if (eel instanceof SOAP12Operation) {
                operationStyle = ((SOAP12Operation) eel).getStyle();
            }
        }

        final OperationType ot = bindingOperation.getOperation().getStyle();
        if (ot != null && ot != OperationType.REQUEST_RESPONSE && ot != OperationType.ONE_WAY)
            return null;

        final BindingInput input = bindingOperation.getBindingInput();
        if ( input == null ) {
            return null;
        }

        final List<ExtensibilityElement> eels = input.getExtensibilityElements();
        String use = null;
        String namespace = null;
        for (ExtensibilityElement eel : eels) {
            if (eel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody)eel;
                use = body.getUse();
                namespace = body.getNamespaceURI();
            }  else if (eel instanceof javax.wsdl.extensions.soap12.SOAP12Body) {
                javax.wsdl.extensions.soap12.SOAP12Body body = (javax.wsdl.extensions.soap12.SOAP12Body)eel;
                use = body.getUse();
                namespace = body.getNamespaceURI();
            } else if (eel instanceof MIMEMultipartRelated) {
                MIMEMultipartRelated mime = (MIMEMultipartRelated) eel;
                List<MIMEPart> parts = mime.getMIMEParts();
                MIMEPart part1 = parts.get(0);
                List<ExtensibilityElement> partEels = part1.getExtensibilityElements();
                for (ExtensibilityElement partEel : partEels) {
                    if (partEel instanceof javax.wsdl.extensions.soap.SOAPBody) {
                        javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody) partEel;
                        use = body.getUse();
                        namespace = body.getNamespaceURI();
                    } else if (partEel instanceof javax.wsdl.extensions.soap12.SOAP12Body) {
                        javax.wsdl.extensions.soap12.SOAP12Body body = (javax.wsdl.extensions.soap12.SOAP12Body) partEel;
                        use = body.getUse();
                        namespace = body.getNamespaceURI();
                    }
                }
            }
        }

        if (use == null) {
            if (listener != null) listener.notifyNoNames(bindingOperation.getName());
            return null;
        }

        Set<List<QName>> operationQNames = new HashSet<List<QName>>();
        operationQNames.add( new ArrayList<QName>() );

        if (operationStyle == null) operationStyle = bindingStyle;
        if (operationStyle == null) {
            if (listener != null) listener.notifyNoStyle(bindingOperation.getName());
            operationStyle = Wsdl.STYLE_DOCUMENT;
        }

        if (Wsdl.STYLE_RPC.equalsIgnoreCase(operationStyle.trim())) {
            for ( List<QName> list : operationQNames ) {
                list.add(new QName(namespace, bindingOperation.getName()));
            }
        } else if (Wsdl.STYLE_DOCUMENT.equalsIgnoreCase(operationStyle.trim())) {
            javax.wsdl.Message inputMessage = bindingOperation.getOperation().getInput().getMessage();
            if (inputMessage == null) {
                if (listener != null) listener.notifyNoNames(bindingOperation.getName());
                return null;
            }

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
                    Part part = (Part) inputMessage.getParts().get(name);
                    if (part != null) {
                        parts.add( part );
                    } else {
                        if (listener != null) listener.notifyPartInvalid(name);
                    }
                }
            }

            for ( final Part part : parts ) {
                QName tq = part.getTypeName();
                QName eq = part.getElementName();
                if (tq != null && eq != null) {
                    if (listener != null) listener.notifyPartName(part.getName());
                    return null;
                } else if (tq != null) {
                    if ( schemaTypeResolver != null ) {
                        final Collection<QName> partAlternatives = schemaTypeResolver.resolveType( tq );
                        final Set<List<QName>> newOperationQNames = new HashSet<List<QName>>();
                        for ( final QName alternative : partAlternatives ) {
                            for ( final List<QName> list : operationQNames ) {
                                final List<QName> names = new ArrayList<QName>( list );
                                names.add( alternative );
                                newOperationQNames.add( names );
                            }
                        }
                        operationQNames = newOperationQNames;
                    } else {
                        for ( List<QName> list : operationQNames ) {
                            list.add(new QName(null, part.getName()));
                        }
                    }
                } else if (eq != null) {
                    for ( List<QName> list : operationQNames ) {
                        list.add(eq);
                    }
                }
            }
        } else {
            if (listener != null) listener.notifyBadStyle(operationStyle, bindingOperation.getName());
            return null;
        }

        return operationQNames;
    }

    /**
     * Get the value of the mustUnderstand global attribute for the specified element.
     *
     * @param element the element to examine.  Usually this would be an immediate child of a SOAP Header element.  Required.
     * @return the value of a mustUnderstand attribute (supposed to be either "1" or "0"), or null if no such attribute was found.
     * @throws InvalidDocumentFormatException if there is more than one recognized mustUnderstand attribute
     * but the values do not all exactly match
     */
    public static String getMustUnderstandAttributeValue(Element element) throws InvalidDocumentFormatException {
        String value = null;
        NamedNodeMap attrs = element.getAttributes();
        int len = attrs.getLength();
        for (int i = 0; i < len; i++) {
            Node attr = attrs.item(i);
            if ("mustUnderstand".equals(attr.getLocalName()) || "mustUnderstand".equals(attr.getNodeName())) {
                final String nodeValue = attr.getNodeValue();
                if (value != null) {
                    // If we see more than one, they must all match up
                    if (!value.equals(nodeValue))
                        throw new InvalidDocumentFormatException("Element has more than one mustUnderstand attribute and their values do not exactly match");
                }
                value = nodeValue;
            }
        }
        return value;
    }

    /**
     * Remove any empty SOAP Header from the specified SOAP document.
     *
     * @param doc the document to examine and possibly modify.  Required.
     * @throws InvalidDocumentFormatException if the document is not a SOAP envelope or if there is more than one Header element
     * @return true if an empty Header element was removed; or, false if the document was not modified.
     */
    public static boolean removeEmptySoapHeader(Document doc) throws InvalidDocumentFormatException {
        Element header = getHeaderElement(doc);
        if (header != null && DomUtils.elementIsEmpty(header)) {
            header.getParentNode().removeChild(header);
            return true;
        }
        return false;
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
        Collection<Binding> bindings = wsdl.getBindings();
        if (bindings.isEmpty()) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
            return output;
        }
        boolean foundSoapBinding = false;
        for ( Binding binding : bindings ) {
            SOAPBinding soapBinding = null;
            SOAP12Binding soap12Binding = null;
            //noinspection unchecked
            List<ExtensibilityElement> bindingEels = binding.getExtensibilityElements();
            for (ExtensibilityElement element : bindingEels) {
                if (element instanceof SOAPBinding) {
                    foundSoapBinding = true;
                    soapBinding = (SOAPBinding) element;
                } else if (element instanceof SOAP12Binding) {
                    foundSoapBinding = true;
                    soap12Binding = (SOAP12Binding) element;
                }
            }

            if (soapBinding == null && soap12Binding == null)
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

    public static Pair<Binding, Operation> getBindingAndOperation(final Wsdl wsdl, final Message request)
            throws IOException, SAXException, InvalidDocumentFormatException {

        return getBindingAndOperation(wsdl, request, null);
    }
    /**
     * Get the Binding and Operation the XML message is targeted at from the supplied WSDL.
     * @param wsdl WSDL to search
     * @param request XML Message to match to a Binding + Operation
     * @param soapVersion Can be null. If not null and not UNKNOWN, then it will determine what Bindings are searched.
     * @return Pair of Binding and Operation. Null if no match is found. If not null, then both the Binding and
     * Operation will be not null. Binding and Operation will be from a Binding which matches the supplied SoapVersion,
     * if supplied and is not UNKNOWN.
     * @throws IOException
     * @throws SAXException
     * @throws InvalidDocumentFormatException
     */
    public static Pair<Binding, Operation> getBindingAndOperation(final Wsdl wsdl, final Message request, final SoapVersion soapVersion)
            throws IOException, SAXException, InvalidDocumentFormatException {
        final XmlKnob requestXml = request.getKnob(XmlKnob.class);
        if (requestXml == null) {
            log.fine("Can't get operation for non-XML message");
            return null;
        }
        if (wsdl == null) {
            log.fine("Can't get operation without WSDL");
            return null;
        }
        OperationSearchContext context = new OperationSearchContext() {
            @Override
            public Wsdl getWsdl() {
                return wsdl;
            }

            @Override
            public Element getPayload() throws InvalidDocumentFormatException, IOException, SAXException {
                if (payload == null) {
                    Document requestDoc = requestXml.getDocumentReadOnly();
                    payload = getPayloadElement(requestDoc);
                }
                return payload;
            }

            @Override
            public boolean hasHttpRequestKnob() throws IOException {
                if (hasHttpRequestKnob == null) {
                    HttpRequestKnob requestHttp = request.getKnob(HttpRequestKnob.class);
                    if (requestHttp == null) {
                        hasHttpRequestKnob = Boolean.FALSE;
                    } else {
                        hasHttpRequestKnob = Boolean.TRUE;
                        saction = stripQuotes(requestHttp.getSoapAction());
                    }
                }
                return hasHttpRequestKnob;
            }

            @Override
            public String getSoapaction() {
                return saction;
            }

            private Element payload = null;
            Boolean hasHttpRequestKnob = null;
            String saction = null;
        };

        //noinspection unchecked
        Collection<Binding> bindings = wsdl.getBindings();
        if (bindings.isEmpty()) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
            return null;
        }

        boolean includeSoap1_1 = true;
        boolean includeSoap1_2 = true;
        if(soapVersion != null){
            switch (soapVersion){
                case SOAP_1_1:
                    includeSoap1_2 = false;
                    break;
                case SOAP_1_2:
                    includeSoap1_1 = false;
                    break;
            }
        }

        boolean foundSoapBinding = false;
        for ( Binding binding : bindings ) {
            SOAPBinding soapBinding = null;
            SOAP12Binding soap12Binding = null;
            //noinspection unchecked
            List<ExtensibilityElement> bindingEels = binding.getExtensibilityElements();
            for (ExtensibilityElement element : bindingEels) {
                if (element instanceof SOAPBinding && includeSoap1_1) {
                    foundSoapBinding = true;
                    soapBinding = (SOAPBinding) element;
                } else if (element instanceof SOAP12Binding && includeSoap1_2) {
                    foundSoapBinding = true;
                    soap12Binding = (SOAP12Binding) element;
                }
            }

            if (soapBinding == null && soap12Binding == null)
                continue; // This isn't a SOAP binding; we don't care
            Operation res = getOperationFromBinding(binding, context);
            if (res != null) return new Pair<Binding, Operation>(binding, res);
        }
        if (!foundSoapBinding) {
            log.info("Can't get operation; WSDL " + wsdl.getDefinition().getDocumentBaseURI() + " has no SOAP port");
        } else {
            log.info("none of the binding could match exactly one operation from this request");
        }
        return null;
    }

    public static String extractSoapAction(BindingOperation bindingOperation, SoapVersion soapVersion){
        String soapAction = null;
        boolean includeSoap1_1 = true;
        boolean includeSoap1_2 = true;
        if(soapVersion != null){
            switch (soapVersion){
                case SOAP_1_1:
                    includeSoap1_2 = false;
                    break;
                case SOAP_1_2:
                    includeSoap1_1 = false;
                    break;
            }
        }

        //noinspection unchecked
        final List<ExtensibilityElement> extElements = bindingOperation.getExtensibilityElements();
        for (ExtensibilityElement element : extElements) {
            if (element instanceof SOAPOperation && includeSoap1_1) {
                SOAPOperation soapOperation = (SOAPOperation) element;
                soapAction = soapOperation.getSoapActionURI();

            } else if (element instanceof SOAP12Operation && includeSoap1_2) {
                SOAP12Operation soap12Operation = (SOAP12Operation) element;
                soapAction = soap12Operation.getSoapActionURI();
            }
        }

        return soapAction;
    }
}
