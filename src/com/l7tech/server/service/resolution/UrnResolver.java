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
import javax.wsdl.Part;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class UrnResolver extends WsdlOperationServiceResolver {

    protected String getTargetValue(Definition def, BindingOperation operation) {
        BindingInput input = operation.getBindingInput();
        String uri = null;
        if (input != null) {
            Iterator eels = input.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof SOAPBody) {
                    SOAPBody body = (SOAPBody)ee;
                    uri = body.getNamespaceURI();
                    if (uri != null) return uri;
                }
            }
        }

        if (uri == null) {
            List parts = operation.getOperation().getInput().getMessage().getOrderedParts(null);
            if (parts.size() > 0) {
                Part firstPart = (Part)parts.get(0);
                QName elementName = firstPart.getElementName();
                if (elementName != null) {
                    uri = elementName.getNamespaceURI();
                    if (uri != null) return uri;
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
