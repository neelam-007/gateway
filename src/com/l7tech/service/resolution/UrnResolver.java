/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;
import com.l7tech.util.SoapUtil;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.apache.log4j.Category;

import javax.wsdl.*;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.ExtensibilityElement;
import java.io.IOException;
import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public class UrnResolver extends WsdlOperationServiceResolver {

    protected String getParameterName() {
        return Request.PARAM_SOAP_URN;
    }

    protected String doGetValue( BindingOperation operation ) {
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
            Element body = SoapUtil.getBodyElement( request );
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

    protected Category _log = Category.getInstance( getClass() );
}
