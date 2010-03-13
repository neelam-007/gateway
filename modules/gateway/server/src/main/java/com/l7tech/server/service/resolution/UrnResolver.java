/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service.resolution;

import com.l7tech.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.Functions;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.server.util.AuditingOperationListener;
import org.xml.sax.SAXException;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author alex
 */
public class UrnResolver extends WsdlOperationServiceResolver<String> {
    public UrnResolver( final Auditor.AuditorFactory auditorFactory ) {
        super( auditorFactory );
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    protected Set<String> getTargetValues(Definition def, BindingOperation operation) {
        String bindingStyle = null;
        Collection<Binding> bindings = def.getBindings().values();
        for (Binding binding : bindings) {
            // TODO this idiocy is necessary because the BindingOperation does not hold a reference to its parent Binding
            if (binding.getBindingOperations().contains(operation)) {
                List<ExtensibilityElement> eels = binding.getExtensibilityElements();
                for (ExtensibilityElement eel : eels) {
                    if (eel instanceof SOAPBinding) {
                        SOAPBinding soapBinding = (SOAPBinding) eel;
                        bindingStyle = soapBinding.getStyle();
                    }
                }
            }
        }

        AuditingOperationListener listener = new AuditingOperationListener(auditor);
        Set<List<QName>> names = SoapUtil.getOperationPayloadQNames(operation, bindingStyle, listener);
        if (names == null || names.isEmpty()) {
            return new HashSet<String>( Arrays.asList( def.getTargetNamespace(), null ) );            
        }
        return new HashSet<String>( Functions.map( names, new Functions.Unary<String,List<QName>>(){
            @Override
            public String call( final List<QName> qNames ) {
                if ( qNames!=null && !qNames.isEmpty() ) {
                    return qNames.get( 0 ).getNamespaceURI();
                }
                return null;
            }
        } ) );
    }

    @Override
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
        } catch (NoSuchPartException e) {
            throw new ServiceResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public boolean usesMessageContent() {
        return true;
    }
}
