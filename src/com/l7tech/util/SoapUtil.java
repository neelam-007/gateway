/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import com.l7tech.message.Request;
import com.l7tech.message.XmlRequest;
import org.w3c.dom.*;
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
        NodeList elementList;
        Element element = null;
        for ( int i = 0; i < ENVELOPE_URIS.size(); i++ ) {
            env = (String)ENVELOPE_URIS.get(i);
            elementList = envelope.getElementsByTagNameNS( env, elementName );
            int len = elementList.getLength();
            if ( len == 1 ) {
                element = (Element)elementList.item(0);
                break;
            } else if ( len == 0 )
                continue;
            else
                throw new IllegalArgumentException( "SOAP envelope contains more than one " + elementName + " element!" );
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
