/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapUtil {
    public static final List ENVELOPE_URIS = new ArrayList();
    static {
        ENVELOPE_URIS.add( SOAPConstants.URI_NS_SOAP_ENVELOPE );
        ENVELOPE_URIS.add( "http://www.w3.org/2001/06/soap-envelope" );
        ENVELOPE_URIS.add( "http://www.w3.org/2001/09/soap-envelope" );
        ENVELOPE_URIS.add( "urn:schemas-xmlsoap-org:soap.v1" );
    }

    public static final String SOAP_ENV_PREFIX = "soapenv";

    public static final String BODY_EL_NAME   = "Body";
    public static final String HEADER_EL_NAME = "Header";
    public static final String SECURITY_EL_NAME = "Security";
    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";
    public static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";

    public static Element getEnvelope( Document request ) {
        Element env = request.getDocumentElement();
        return env;
    }

    public static Element getHeaderElement( Document request ) {
        return getEnvelopePart( request, HEADER_EL_NAME );
    }


    public static Element getBodyElement( Document request ) {
        return getEnvelopePart( request, BODY_EL_NAME );
    }

    protected static Element getEnvelopePart( Document request, String elementName ) {
        Element envelope = getEnvelope( request );
        String env;
        Node node;
        Element element = null;
        for ( int i = 0; i < ENVELOPE_URIS.size(); i++ ) {
            env = (String)ENVELOPE_URIS.get(i);

            node = envelope.getFirstChild();
            while ( node != null ) {
                if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                    element = (Element)node;
                    String ln = element.getLocalName();
                    if ( ln.equals( elementName ) ) {
                        String uri = element.getNamespaceURI();
                        if ( uri.equals( env ) ) return element;
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

    public static SOAPFault addFaultTo( SOAPMessage message, String faultCode, String faultString ) throws SOAPException {
        SOAPPart spart = message.getSOAPPart();
        SOAPEnvelope senv = spart.getEnvelope();
        SOAPBody body = senv.getBody();
        SOAPFault fault = body.addFault();
        fault.setFaultCode( faultCode );
        fault.setFaultString( faultString);
        return fault;
    }

    /**
     * Find the Namespace URI of the given document, which is assumed to contain a SOAP Envelope.
     * @param request  the SOAP envelope to examine
     * @return the body's namespace URI, or null if not found.
     */
    public static String getNamespaceUri( Document request ) {
        Element body = SoapUtil.getBodyElement( request );
        Node n = body.getFirstChild();
        while ( n != null ) {
            if ( n.getNodeType() == Node.ELEMENT_NODE )
                return n.getNamespaceURI();
            n = n.getNextSibling();
        }
        return null;
    }

    /**
     * Returns the Header element from a soap message. If the message does not have a header yet, it creates one and
     * adds it to the envelope, and returns it back to you. If a body element exists, the header element will be inserted right before the body element.
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
        }
        else return (Element)list.item(0);
    }

    public static Element getBody(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        // if the body is there, get it so that the header can be inserted before it
        NodeList bodylist = soapMsg.getElementsByTagNameNS(soapEnvNS, BODY_EL_NAME);
        if (bodylist.getLength() > 0) {
            return (Element)bodylist.item(0);
        } else return null;
    }

    /**
     * Returns the Security element from the header of a soap message. If the message does not have a header yet, it
     * creates one and a child Security element and adds it all to the envelope, and returns back the Security element
     * to you. If a body element exists, the header element will be inserted right before the body element.
     * @param soapMsg DOM document containing the soap message
     * @return the security element (never null)
     */
    public static Element getOrMakeSecurityElement(Document soapMsg) {
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE2, SECURITY_EL_NAME);
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
     * @return null if element not present, the security element if it's in the doc
     */
    public static Element getSecurityElement(Document soapMsg) {
        // look for the element
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE2, SECURITY_EL_NAME);
        }
        // is it there ?
        if (listSecurityElements.getLength() < 1) return null;
        // we got it
        return (Element)listSecurityElements.item(0);
    }

    public static void cleanEmptySecurityElement(Document soapMsg) {
        Element secEl = getSecurityElement(soapMsg);
        while (secEl != null) {
            if (elHasChildrenElements(secEl)) {
                return;
            } else secEl.getParentNode().removeChild(secEl);
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

    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";
}
