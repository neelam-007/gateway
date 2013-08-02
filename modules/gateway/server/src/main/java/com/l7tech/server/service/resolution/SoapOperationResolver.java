package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.message.Message;
import com.l7tech.message.SoapKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.util.AuditingOperationListener;
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

/**
 * Attempts to resolve services using the QNames of the payload elements.
 * @author alex
 */
public class SoapOperationResolver extends NameValueServiceResolver<List<QName>> {
    private static final List<List<QName>> EMPTY = Collections.emptyList();
    private final ServiceDocumentManager serviceDocumentManager;

    public SoapOperationResolver( final AuditFactory auditorFactory,
                                  final ServiceDocumentManager serviceDocumentManager ) {
        super( auditorFactory );
        this.serviceDocumentManager = serviceDocumentManager;
    }

    @Override
    protected List<List<QName>> buildTargetValues( final PublishedService service ) throws ServiceResolutionException {
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

            List<List<QName>> operationQnameLists = getAllOperationQNames(wsdl, service.getGoid());

            if (operationQnameLists.isEmpty()) {
                auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_NO_QNAMES_AT_ALL, service.getName(), Goid.toString(service.getGoid()));
                return EMPTY;
            } else {
                return operationQnameLists;
            }
        } catch (WSDLException e) {
            logger.log(Level.WARNING, MessageFormat.format("Unable to parse WSDL for {0} service (#{1})", service.getName(), service.getGoid()), e);
            return EMPTY;
        }
    }

    @SuppressWarnings({ "unchecked" })
    private List<List<QName>> getAllOperationQNames( final Wsdl wsdl, final Goid serviceGoid ) {
        List<List<QName>> operationQnameLists = new ArrayList<List<QName>>();
        Collection<Binding> bindings = wsdl.getBindings();
        if (bindings == null || bindings.isEmpty()) {
            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_WSDL_NO_BINDINGS);
            return EMPTY;
        }

        final AuditingOperationListener listener = new AuditingOperationListener(auditor);
        final WsdlSchemaTypeResolver typeResolver = new WsdlSchemaTypeResolver(wsdl){
            private Collection<ServiceDocument> serviceDocuments;

            @Override
            public String resolveSchema( final String schemaUri ) {
                String schemaXml = null;

                for ( final ServiceDocument serviceDocument : getServiceDocuments() ) {
                    if ( schemaUri.equals(serviceDocument.getUri()) &&
                         "text/xml".equalsIgnoreCase(serviceDocument.getContentType())) {
                        schemaXml = serviceDocument.getContents();
                    }
                }

                return schemaXml;
            }

            private Collection<ServiceDocument> getServiceDocuments() {
                if ( serviceDocuments == null ) {
                    try {
                        serviceDocuments = serviceDocumentManager.findByServiceIdAndType( serviceGoid, "WSDL-IMPORT" );
                    } catch ( FindException fe ) {
                        serviceDocuments = Collections.emptyList();
                    }
                }
                return serviceDocuments;
            }
        };
        for ( final Binding binding : bindings ) {
            String bindingStyle = null;

            final List<ExtensibilityElement> bindingEels = binding.getExtensibilityElements();
            SOAPBinding sb = null;
            SOAP12Binding sb12 = null;
            for ( final ExtensibilityElement eel : bindingEels ) {
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

            final List<BindingOperation> bops = binding.getBindingOperations();
            for ( final BindingOperation bop : bops ) {
                final Set<List<QName>> operationQnames = SoapUtil.getOperationPayloadQNames(bop, bindingStyle, listener, typeResolver);
                if (operationQnames != null) {
                    operationQnameLists.addAll(operationQnames);
                }
            }
        }

        return operationQnameLists;
    }

    @Override
    protected List<QName> getRequestValue(Message request) throws ServiceResolutionException {
        SoapKnob sk = request.getKnob(SoapKnob.class);
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

    @Override
    public boolean isApplicableToMessage(Message msg) throws ServiceResolutionException {
        try {
            return msg.isSoap();
        } catch (Exception e) {
            throw new ServiceResolutionException("Unable to determine whether message is SOAP", e);
        }
    }

    @Override
    public boolean usesMessageContent() {
        return true;
    }
}
