/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;
import com.l7tech.server.util.ServerSoapUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
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

    protected String getTargetValue( BindingOperation operation ) {
        BindingInput input = operation.getBindingInput();
        Iterator eels = input.getExtensibilityElements().iterator();
        ExtensibilityElement ee;
        while ( eels.hasNext() ) {
            ee = (ExtensibilityElement)eels.next();
            if ( ee instanceof SOAPBody ) {
                SOAPBody body = (SOAPBody)ee;
                return body.getNamespaceURI();
            }
        }
        return null;
    }

    protected Object getRequestValue( Request request ) throws ServiceResolutionException {
        try {
            Element body = ServerSoapUtil.getBodyElement( request );
            Node n = body.getFirstChild();
            while ( n != null ) {
                if ( n.getNodeType() == Node.ELEMENT_NODE )
                    return n.getNamespaceURI();

                n = n.getNextSibling();
            }
            return null;
        } catch ( SAXException se ) {
            throw new ServiceResolutionException( se.getMessage(), se );
        } catch ( IOException ioe ) {
            throw new ServiceResolutionException( ioe.getMessage(), ioe );
        }
    }

    public int getSpeed() {
        return SLOW;
    }
}
