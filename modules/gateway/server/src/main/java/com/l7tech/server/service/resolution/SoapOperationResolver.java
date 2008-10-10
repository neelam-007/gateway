/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SoapKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.util.AuditingOperationListener;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Attempts to resolve services using the QNames of the payload elements.
 * @author alex
 */
public class SoapOperationResolver extends NameValueServiceResolver<List<QName>> {
    private static final Logger logger = Logger.getLogger(SoapOperationResolver.class.getName());
    private static final List<List<QName>> EMPTY = Collections.emptyList();

    public SoapOperationResolver(ApplicationContext spring) {
        super(spring);
    }

    protected List<List<QName>> doGetTargetValues(PublishedService service) throws ServiceResolutionException {
        try {
            if (!service.isSoap()) {
                auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_NOT_SOAP);
                return EMPTY;
            }
            Wsdl wsdl = service.parsedWsdl();
            if (wsdl == null) {
                auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_NO_WSDL);
                return EMPTY;
            }

            List<List<QName>> operationQnameLists = getAllOperationQNames(wsdl);

            if (operationQnameLists.isEmpty()) {
                auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_NO_QNAMES_AT_ALL, service.getName(), Long.toString(service.getOid()));
                return EMPTY;
            } else {
                return operationQnameLists;
            }
        } catch (WSDLException e) {
            logger.log(Level.WARNING, MessageFormat.format("Unable to parse WSDL for {0} service (#{1})", service.getName(), service.getOid()), e);
            return EMPTY;
        }
    }

    private List<List<QName>> getAllOperationQNames(Wsdl wsdl) {
        List<List<QName>> operationQnameLists = new ArrayList<List<QName>>();
        //noinspection unchecked
        Map<QName, Binding> bindings = wsdl.getDefinition().getBindings();
        if (bindings == null || bindings.isEmpty()) {
            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_WSDL_NO_BINDINGS);
            return EMPTY;
        }

        for (QName qName : bindings.keySet()) {
            Binding binding = bindings.get(qName);
            String bindingStyle = null;

            //noinspection unchecked
            List<ExtensibilityElement> bindingEels = binding.getExtensibilityElements();
            SOAPBinding sb = null;
            SOAP12Binding sb12 = null;
            for (ExtensibilityElement eel : bindingEels) {
                if (eel instanceof SOAPBinding) {
                    sb = (SOAPBinding) eel;
                    bindingStyle = sb.getStyle();
                } else if (eel instanceof SOAP12Binding) {
                    sb12 = (SOAP12Binding) eel;
                    bindingStyle = sb12.getStyle();
                }
            }
            if (sb == null && sb12 == null) {
                continue;
            }

            //noinspection unchecked
            List<BindingOperation> bops = binding.getBindingOperations();
            AuditingOperationListener listener = new AuditingOperationListener(auditor);
            for (BindingOperation bop : bops) {
                List<QName> operationQnames = SoapUtil.getOperationPayloadQNames(bop, bindingStyle, listener);
                if (operationQnames != null) operationQnameLists.add(operationQnames);
            }
        }

        return operationQnameLists;
    }

    protected List<QName> getRequestValue(Message request) throws ServiceResolutionException {
        SoapKnob sk = (SoapKnob) request.getKnob(SoapKnob.class);
        try {
            QName[] names = sk.getPayloadNames();
            for (QName name : names) {
                auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_FOUND_QNAME, name.toString());
            }
            return Arrays.asList(names);
        } catch (IOException e) {
            throw new ServiceResolutionException("Unable to parse payload element QNames", e);
        } catch (SAXException e) {
            throw new ServiceResolutionException("Unable to parse payload element QNames", e);
        } catch (NoSuchPartException e) {
            throw new ServiceResolutionException("Unable to parse payload element QNames", e);
        }
    }

    public boolean usesMessageContent() {
        return true;
    }

    public Set<List<QName>> getDistinctParameters(PublishedService candidateService) throws ServiceResolutionException {
        throw new UnsupportedOperationException();
    }

    public boolean isSoap() {
        return true;
    }
}
