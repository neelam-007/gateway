/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;

import javax.wsdl.BindingOperation;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapActionResolver extends WsdlOperationServiceResolver {
    public int getSpeed() {
        return FAST;
    }

    protected String getParameterName() {
        return Request.PARAM_HTTP_SOAPACTION;
    }

    protected String getTargetValue( BindingOperation operation ) {
        Iterator eels = operation.getExtensibilityElements().iterator();
        ExtensibilityElement ee;
        while ( eels.hasNext() ) {
            ee = (ExtensibilityElement)eels.next();
            if ( ee instanceof SOAPOperation ) {
                SOAPOperation sop = (SOAPOperation)ee;
                return sop.getSoapActionURI();
            }
        }
        return null;
    }

    protected Object getRequestValue( Request request ) {
        String soapAction = (String)request.getParameter( request.PARAM_HTTP_SOAPACTION );
        // Strip leading and trailing quotes
        if ( soapAction.startsWith("\"") && soapAction.endsWith("\"") ) {
            return soapAction.substring( 1, soapAction.length()-1 );
        } else {
            return soapAction;
        }

    }

}
