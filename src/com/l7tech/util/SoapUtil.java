/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import com.l7tech.message.Request;
import com.l7tech.message.XmlRequest;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.soap.*;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapUtil {
    public static List ENVELOPE_URIS = new ArrayList();
    static {
        ENVELOPE_URIS.add( SOAPConstants.URI_NS_SOAP_ENVELOPE );
        ENVELOPE_URIS.add( "http://www.w3.org/2001/06/soap-envelope" );
        ENVELOPE_URIS.add( "http://www.w3.org/2001/09/soap-envelope" );
        ENVELOPE_URIS.add( "urn:schemas-xmlsoap-org:soap.v1" );
    }

    public static final String HEADER = "Header";
    public static final String BODY   = "Body";

    public static Element getEnvelope( Request request ) throws SAXException, IOException {
        if ( request instanceof XmlRequest ) {
            XmlRequest xreq = (XmlRequest)request;
            Document doc = xreq.getDocument();
            Element env = doc.getDocumentElement();
            return env;
        } else {
            throw new IllegalArgumentException( "Can't find a URN in a non-XML request!" );
        }
    }

    public static Element getHeaderElement( Request request ) throws SAXException, IOException {
        return getEnvelopePart( request, HEADER );
    }

    public static Element getBodyElement( Request request ) throws SAXException, IOException {
        return getEnvelopePart( request, BODY );
    }

    static Element getEnvelopePart( Request request, String elementName ) throws SAXException, IOException {
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
        // TODO: Match namespace to request's SOAP version?
        SOAPFault fault = body.addFault();
        fault.setFaultCode( faultCode );
        fault.setFaultString( faultString);
        return fault;
    }

    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";
}
