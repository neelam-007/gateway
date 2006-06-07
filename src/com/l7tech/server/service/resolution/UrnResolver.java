/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import org.xml.sax.SAXException;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import java.io.IOException;

/**
 * @author alex
 */
public class UrnResolver extends WsdlOperationServiceResolver {

    protected String getTargetValue(Definition def, BindingOperation operation) {
        return SoapUtil.findTargetNamespace(def, operation);
    }

    protected Object getRequestValue(Message request) throws ServiceResolutionException {
        try {
            if (request.isSoap()) {
                String[] uris = request.getSoapKnob().getPayloadNamespaceUris();
                if (uris == null || uris.length < 1)
                    return null;

                // TODO there might be a way to properly handle a request with multiple payload URIs
                String sawUri = null;
                for (String uri : uris) {
                    if (sawUri == null)
                        sawUri = uri;
                    else if (!sawUri.equals(uri))
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
