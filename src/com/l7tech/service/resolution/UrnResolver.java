/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;
import com.l7tech.message.SoapRequest;
import com.l7tech.server.util.ServerSoapUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBody;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class UrnResolver extends WsdlOperationServiceResolver {
    protected String getParameterName() {
        return Request.PARAM_SOAP_URN;
    }

    protected String getTargetValue(Definition def, BindingOperation operation) {
        BindingInput input = operation.getBindingInput();
        if (input != null) {
            Iterator eels = input.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof SOAPBody) {
                    SOAPBody body = (SOAPBody)ee;
                    String uri = body.getNamespaceURI();
                    if ( uri != null ) return uri;
                }
            }
        }
        return def.getTargetNamespace();
    }

    protected Object getRequestValue(Request request) throws ServiceResolutionException {
        try {
            if ( request instanceof SoapRequest ) {
                SoapRequest sreq = (SoapRequest)request;
                XmlPullParser xpp = sreq.getPullParser();
                String tag;
                String ns;
                boolean gotBody = false;
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if( eventType == XmlPullParser.START_TAG ) {
                        tag = xpp.getName();
                        if ( gotBody ) {
                            ns = xpp.getNamespace();
                            return ns;
                        } else if ( tag.equalsIgnoreCase("body") ) {
                            gotBody = true;
                        }
                    }
                    eventType = xpp.next();
                }
            } else {
                Element body = ServerSoapUtil.getBodyElement(request);
                if ( body == null ) return null;
                Node n = body.getFirstChild();
                while (n != null) {
                    if (n.getNodeType() == Node.ELEMENT_NODE)
                        return n.getNamespaceURI();

                    n = n.getNextSibling();
                }
            }
            return null;
        } catch (SAXException se) {
            throw new ServiceResolutionException(se.getMessage(), se);
        } catch (IOException ioe) {
            throw new ServiceResolutionException(ioe.getMessage(), ioe);
        } catch ( XmlPullParserException xppe ) {
            throw new ServiceResolutionException(xppe.getMessage(), xppe);
        }
    }

    public int getSpeed() {
        return SLOW;
    }
}
