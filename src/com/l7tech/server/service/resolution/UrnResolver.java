/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.common.xml.MessageNotSoapException;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author alex
 */
public class UrnResolver extends WsdlOperationServiceResolver {

    protected String getTargetValue(Definition def, BindingOperation bindingOperation) {
        BindingInput bindingInput = bindingOperation.getBindingInput();
        if (bindingInput != null) {
            Iterator eels = bindingInput.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof SOAPBody) {
                    SOAPBody body = (SOAPBody)ee;
                    String uri = body.getNamespaceURI();
                    if (uri != null) return uri;
                }
            }
        }

        Input input = bindingOperation.getOperation().getInput();
        if (input != null) {
            List parts = input.getMessage().getOrderedParts(null);
            if (parts.size() > 0) {
                Part firstPart = (Part)parts.get(0);
                QName elementName = firstPart.getElementName();
                if (elementName != null) {
                    String uri = elementName.getNamespaceURI();
                    if (uri != null) return uri;
                }
            }
        }

        return def.getTargetNamespace();
    }

    protected Object getRequestValue(Message request) throws ServiceResolutionException {
        try {
            if (request.isSoap()) {
                String[] uris = request.getSoapKnob().getPayloadNamespaceUris();
                if (uris == null || uris.length < 1)
                    return null;

                // TODO there might be a way to properly handle a request with multiple payload URIs
                String sawUri = null;
                for (int i = 0; i < uris.length; i++) {
                    String uri = uris[i];
                    if (sawUri == null)
                        sawUri = uri;
                    else
                        if (!sawUri.equals(uri))
                            throw new ServiceResolutionException("Request uses more than one payload namespace URI");
                }
                return sawUri;
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
