/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.message.Request;
import com.l7tech.message.XmlRequest;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
            if (request instanceof XmlRequest) {
                Document doc = ((XmlRequest)request).getDocument();
                String uri = SoapUtil.getPayloadNamespaceUri(doc);
                return uri;
            }
            return null;
        } catch (SAXException se) {
            throw new ServiceResolutionException(se.getMessage(), se);
        } catch (IOException ioe) {
            throw new ServiceResolutionException(ioe.getMessage(), ioe);
        }
    }

    public int getSpeed() {
        return SLOW;
    }
}
