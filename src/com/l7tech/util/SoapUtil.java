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

import javax.xml.soap.SOAPConstants;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapUtil {
    public static List ENVELOPES = new ArrayList();
    static {
        ENVELOPES.add( SOAPConstants.URI_NS_SOAP_ENVELOPE );
        ENVELOPES.add( "http://www.w3.org/2001/06/soap-envelope" );
        ENVELOPES.add( "http://www.w3.org/2001/09/soap-envelope" );
        ENVELOPES.add( "urn:schemas-xmlsoap-org:soap.v1" );
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
        for ( int i = 0; i < ENVELOPES.size(); i++ ) {
            env = (String)ENVELOPES.get(i);
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
}
