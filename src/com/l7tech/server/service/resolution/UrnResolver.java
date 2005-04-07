/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.common.xml.MessageNotSoapException;
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

    protected int getMaxLength() {
        return ResolutionParameters.MAX_LENGTH_RES_PARAMETER;
    }

    protected Object getRequestValue(Message request) throws ServiceResolutionException {
        try {
            if (request.isSoap()) {
                return request.getSoapKnob().getPayloadNamespaceUri();
            } else {
                return null;
            }
        } catch (SAXException se) {
            throw new ServiceResolutionException(se.getMessage(), se);
        } catch (MessageNotSoapException e) {
            throw new RuntimeException(e); // can't happen, we already checked
        } catch (IOException e) {
            throw new ServiceResolutionException(e.getMessage(), e);
        }
    }

    public int getSpeed() {
        return SLOW;
    }
}
