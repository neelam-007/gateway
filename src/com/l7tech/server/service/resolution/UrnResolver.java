/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import org.xml.sax.SAXException;
import org.springframework.context.ApplicationContext;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * @author alex
 */
public class UrnResolver extends WsdlOperationServiceResolver<String> {
    public UrnResolver(ApplicationContext spring) {
        super(spring);
    }

    protected String getTargetValue(Definition def, BindingOperation operation) {
        return SoapUtil.findTargetNamespace(def, operation);
    }

    protected String getRequestValue(Message request) throws ServiceResolutionException {
        try {
            if (request.isSoap()) {
                QName[] names = request.getSoapKnob().getPayloadNames();
                if (names == null || names.length < 1)
                    return null;

                // TODO there might be a way to properly handle a request with multiple payload URIs
                String sawUri = null;
                for (QName q : names) {
                    String uri = q.getNamespaceURI();
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
