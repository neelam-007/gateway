/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.w3c.dom.*;
import org.w3c.dom.Node;

import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.rpc.NamespaceConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
    public static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";
    public static final String SECURITY_NAMESPACE3 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String DIGSIG_URI = "http://www.w3.org/2000/09/xmldsig#";
    public static final String WSU_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    public static final String WSU_NAMESPACE2 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    // Attribute names
    public static final String ID_ATTRIBUTE_NAME = "Id";

    // Element names
    public static final String BODY_EL_NAME = "Body";
    public static final String HEADER_EL_NAME = "Header";
    public static final String SECURITY_EL_NAME = "Security";
    public static final String SIGNATURE_EL_NAME = "Signature";
    public static final String SIGNED_INFO_EL_NAME = "SignedInfo";
    public static final String REFERENCE_EL_NAME = "Reference";

    public static final String REFERENCE_URI_ATTR_NAME = "URI";

    // Misc
    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";
    public static final String SOAPACTION = "SOAPAction";

    /** soap envelope xpath '/soapenv:Envelope' */
    public static final String SOAP_ENVELOPE_XPATH = "/" + NamespaceConstants.NSPREFIX_SOAP_ENVELOPE + ":Envelope";

    /** soap body xpath '/soapenv:Envelope/soapenv:Body' */
    public static final String SOAP_BODY_XPATH = SOAP_ENVELOPE_XPATH+"/"+NamespaceConstants.NSPREFIX_SOAP_ENVELOPE + ":Body";


    public static Element getEnvelope(Document request) {
        Element env = request.getDocumentElement();
        return env;
    }

    public static Element getHeaderElement(Document soapMsg) {
        Element envelope = soapMsg.getDocumentElement();
        if (!"Envelope".equals(envelope.getLocalName())) {
            throw new IllegalArgumentException("Invalid SOAP envelope: document element is not named 'Envelope'");
        }
        String envelopeNs = envelope.getNamespaceURI();
        if (!SoapUtil.ENVELOPE_URIS.contains(envelopeNs)) {
            throw new IllegalArgumentException("Invalid SOAP message: unrecognized envelope namespace \"" + envelopeNs + "\"");
        }
        return XmlUtil.findFirstChildElementByName(envelope, envelopeNs, HEADER_EL_NAME);
    }


    public static Element getBodyElement(Document request) {
        return getEnvelopePart(request, BODY_EL_NAME);
    }

    protected static Element getEnvelopePart(Document request, String elementName) {
        Element envelope = getEnvelope(request);
        String env;
        Node node;
        Element element = null;
        for (int i = 0; i < ENVELOPE_URIS.size(); i++) {
            env = (String)ENVELOPE_URIS.get(i);

            node = envelope.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    element = (Element)node;
                    String ln = element.getLocalName();
                    if (ln.equals(elementName)) {
                        String uri = element.getNamespaceURI();
                        if (uri.equals(env)) return element;
                    }
                }
                node = node.getNextSibling();
            }
        }
        return element;
    }

    public static SOAPMessage makeMessage() throws SOAPException {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage smsg = mf.createMessage();
        return smsg;
    }

    public static SOAPFault addFaultTo(SOAPMessage message, String faultCode, String faultString) throws SOAPException {
        SOAPPart spart = message.getSOAPPart();
        SOAPEnvelope senv = spart.getEnvelope();
        SOAPBody body = senv.getBody();
        SOAPFault fault = body.addFault();
        fault.setFaultCode(faultCode);
        fault.setFaultString(faultString);
        return fault;
    }

    /**
     * Find the Namespace URI of the given document, which is assumed to contain a SOAP Envelope.
     * 
     * @param request the SOAP envelope to examine
     * @return the body's namespace URI, or null if not found.
     */
    public static String getNamespaceUri(Document request) {
        Element body = SoapUtil.getBodyElement(request);
        Node n = body.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE)
                return n.getNamespaceURI();
            n = n.getNextSibling();
        }
        return null;
    }

    /**
     * Returns the Header element from a soap message. If the message does not have a header yet, it creates one and
     * adds it to the envelope, and returns it back to you. If a body element exists, the header element will be inserted right before the body element.
     * 
     * @param soapMsg DOM document containing the soap message
     * @return the header element (never null)
     */
    public static Element getOrMakeHeader(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        NodeList list = soapMsg.getElementsByTagNameNS(soapEnvNS, HEADER_EL_NAME);
        if (list.getLength() < 1) {
            String soapEnvNamespacePrefix = soapMsg.getDocumentElement().getPrefix();

            // create header element
            Element header = soapMsg.createElementNS(soapEnvNS, HEADER_EL_NAME);
            header.setPrefix(soapEnvNamespacePrefix);

            // if the body is there, get it so that the header can be inserted before it
            Element body = getBody(soapMsg);
            if (body != null)
                soapMsg.getDocumentElement().insertBefore(header, body);
            else
                soapMsg.getDocumentElement().appendChild(header);
            return header;
        } else
            return (Element)list.item(0);
    }

    public static Element getBody(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        // if the body is there, get it so that the header can be inserted before it
        NodeList bodylist = soapMsg.getElementsByTagNameNS(soapEnvNS, BODY_EL_NAME);
        if (bodylist.getLength() > 0) {
            return (Element)bodylist.item(0);
        } else
            return null;
    }

    /**
     * Returns the Security element from the header of a soap message. If the message does not have a header yet, it
     * creates one and a child Security element and adds it all to the envelope, and returns back the Security element
     * to you. If a body element exists, the header element will be inserted right before the body element.
     * 
     * @param soapMsg DOM document containing the soap message
     * @return the security element (never null)
     */
    public static Element getOrMakeSecurityElement(Document soapMsg) {
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE2, SECURITY_EL_NAME);
        }
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE3, SECURITY_EL_NAME);
        }
        if (listSecurityElements.getLength() < 1) {
            // element does not exist
            Element header = SoapUtil.getOrMakeHeader(soapMsg);
            Element securityEl = soapMsg.createElementNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
            securityEl.setPrefix(SECURITY_NAMESPACE_PREFIX);
            securityEl.setAttribute("xmlns:" + SECURITY_NAMESPACE_PREFIX, SECURITY_NAMESPACE);
            header.insertBefore(securityEl, null);
            return securityEl;
        } else {
            return (Element)listSecurityElements.item(0);
        }
    }

    /**
     * Resolves the element in the passed document that has the id passed in elementId.
     * The id attributes can be of any supported WSU namespaces.
     * @return the leement or null if no such element exists
     */
    public static Element getElementById(Document doc, String elementId) {
        NodeList elements = doc.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element)elements.item(i);
            if (elementId.equals(getElementId(element))) {
                return element;
            }
        }
        return null;
    }

    /**
     * Gets the WSU:Id attribute of the passed element using all supported WSU namespaces.
     * @return the string value of the attribute or null if the attribute is not present
     */
    public static String getElementId(Element node) {
        String id = node.getAttribute(ID_ATTRIBUTE_NAME);
        if (id == null || id.length() < 1) {
            id = node.getAttributeNS(SoapUtil.WSU_NAMESPACE, ID_ATTRIBUTE_NAME);
        }
        if (id == null || id.length() < 1) {
            id = node.getAttributeNS(SoapUtil.WSU_NAMESPACE2, ID_ATTRIBUTE_NAME);
        }
        // for some reason this is set to "" when not present.
        if (id.length() < 1) id = null;
        return id;
    }

    /**
     * @return null if element not present, the security element if it's in the doc
     */
    public static Element getSecurityElement(Document soapMsg) {
        // look for the element
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE2, SECURITY_EL_NAME);
        }
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE3, SECURITY_EL_NAME);
        }
        // is it there ?
        if (listSecurityElements.getLength() < 1) return null;
        // we got it
        return (Element)listSecurityElements.item(0);
    }

    /**
     * Get the security element from a specific header instead of the entire soap message.
     * This alternative is used in the case of nested soap messages.
     * @return null if element not present, the security element if it's in the doc
     */
    public static Element getSecurityElement(Element header) {
        List secElements = XmlUtil.findChildElementsByName(header,
                                                           new String[] {SECURITY_NAMESPACE,
                                                                         SECURITY_NAMESPACE2,
                                                                         SECURITY_NAMESPACE3},
                                                           SECURITY_EL_NAME);
        // is it there ?
        if (secElements.size() < 1) return null;
        // we got it
        return (Element)secElements.get(0);
    }

    public static void cleanEmptySecurityElement(Document soapMsg) {
        Element secEl = getSecurityElement(soapMsg);
        while (secEl != null) {
            if (elHasChildrenElements(secEl)) {
                return;
            } else
                secEl.getParentNode().removeChild(secEl);
            secEl = getSecurityElement(soapMsg);
        }
    }

    public static void cleanEmptyHeaderElement(Document soapMsg) {
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        NodeList list = soapMsg.getElementsByTagNameNS(soapEnvNS, HEADER_EL_NAME);
        // is it there ?
        if (list.getLength() < 1) return;
        // we got it
        Element headerEl = (Element)list.item(0);
        if (!elHasChildrenElements(headerEl)) {
            headerEl.getParentNode().removeChild(headerEl);
        }
    }

    /**
     * checks whether the passed element has any Element type children
     * 
     * @return true if it does false if not
     */
    public static boolean elHasChildrenElements(Element el) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node thisChild = children.item(i);
            // only consider element types
            if (thisChild.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Load the DOM document into the SOAP message
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

        if (domNode.hasAttributes()) {
            NamedNodeMap DOMAttributes = domNode.getAttributes();
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

        if (domNode.hasChildNodes()) {
            NodeList children = domNode.getChildNodes();
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
                        Name name = sf.createName(child.getLocalName(), child.getPrefix(), child.getNamespaceURI());
                        soapElement.addChildElement(domToSOAPElement(sf.createElement(name), child));
                }

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
        Node importNode = ((importingDocument).importNode(docFrag, true));

    
        //In order to replace the node need to retrieve replacedNode's parent
        parentNode.insertBefore(importNode, null);
        return importingDocument;
    }

    public static SOAPMessage makeFaultMessage( String faultCode, String faultString ) throws SOAPException {
        SOAPMessage msg = makeMessage();
        SoapUtil.addFaultTo(msg, faultCode, faultString);
        return msg;
    }

    public static String soapMessageToString( SOAPMessage msg, String encoding ) throws IOException, SOAPException {
        return new String( soapMessageToByteArray( msg ), encoding );
    }

    public static byte[] soapMessageToByteArray( SOAPMessage msg ) throws IOException, SOAPException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        msg.writeTo(baos);
        return baos.toByteArray();
    }

    public static void cleanEmptyRefList(Document soapMsg) {
         List refs = new ArrayList();
         {
             NodeList listRefElements = soapMsg.getElementsByTagNameNS(XMLENC_NS, "ReferenceList");
             if (listRefElements.getLength() < 1)
                 return;
             for (int i = 0; i < listRefElements.getLength(); ++i)
                 refs.add(listRefElements.item(i));
             listRefElements = null;
         }

         for (Iterator iterator = refs.iterator(); iterator.hasNext();) {
             Element refListEl = (Element)iterator.next();
             if (!elHasChildrenElements(refListEl))
                 refListEl.getParentNode().removeChild(refListEl);
         }
     }

}
