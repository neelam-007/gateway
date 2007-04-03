/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.SoapKnob;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.service.PublishedService;
import org.xml.sax.SAXException;
import org.springframework.context.ApplicationContext;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

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

        nextBinding: for (QName qName : bindings.keySet()) {
            Binding binding = bindings.get(qName);
            String bindingStyle = null;

            //noinspection unchecked
            List<ExtensibilityElement> bindingEels = binding.getExtensibilityElements();
            SOAPBinding sb = null;
            for (ExtensibilityElement eel : bindingEels) {
                if (eel instanceof SOAPBinding) {
                    sb = (SOAPBinding) eel;
                    bindingStyle = sb.getStyle();
                }
            }
            if (sb == null) continue nextBinding;

            //noinspection unchecked
            List<BindingOperation> bops = binding.getBindingOperations();
            nextBindingOperation: for (BindingOperation bop : bops) {
                String operationStyle;

                //noinspection unchecked
                List<ExtensibilityElement> bopEels = bop.getExtensibilityElements();
                SOAPOperation soapOperation = null;
                for (ExtensibilityElement eel : bopEels) {
                    if (eel instanceof SOAPOperation) {
                        soapOperation = (SOAPOperation) eel;
                    }
                }

                OperationType ot = bop.getOperation().getStyle();
                if (ot != null && ot != OperationType.REQUEST_RESPONSE && ot != OperationType.ONE_WAY)
                    continue nextBindingOperation;

                operationStyle = soapOperation == null ? null : soapOperation.getStyle();

                BindingInput input = bop.getBindingInput();
                //noinspection unchecked
                List<ExtensibilityElement> eels = input.getExtensibilityElements();
                String use = null;
                String namespace = null;
                for (ExtensibilityElement eel : eels) {
                    if (eel instanceof SOAPBody) {
                        SOAPBody body = (SOAPBody)eel;
                        use = body.getUse();
                        namespace = body.getNamespaceURI();
                    } else if (eel instanceof MIMEMultipartRelated) {
                        MIMEMultipartRelated mime = (MIMEMultipartRelated) eel;
                        //noinspection unchecked
                        List<MIMEPart> parts = mime.getMIMEParts();
                        MIMEPart part1 = parts.get(0);
                        //noinspection unchecked
                        List<ExtensibilityElement> partEels = part1.getExtensibilityElements();
                        for (ExtensibilityElement partEel : partEels) {
                            if (partEel instanceof SOAPBody) {
                                SOAPBody body = (SOAPBody) partEel;
                                use = body.getUse();
                                namespace = body.getNamespaceURI();
                            }
                        }
                    }
                }

                if (use == null) continue nextBindingOperation;

                List<QName> operationQNames = new ArrayList<QName>();

                if (operationStyle == null) operationStyle = bindingStyle;
                if (operationStyle == null) {
                    auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_WSDL_NO_STYLE, bop.getName());
                    continue nextBindingOperation;
                }
                if ("rpc".equalsIgnoreCase(operationStyle.trim())) {
                    operationQNames.add(new QName(namespace, bop.getName()));
                } else if ("document".equalsIgnoreCase(operationStyle.trim())) {
                    javax.wsdl.Message inputMessage = bop.getOperation().getInput().getMessage();
                    if (inputMessage == null) continue;
                    //noinspection unchecked
                    List<Part> parts = inputMessage.getOrderedParts(null);
                    //noinspection unchecked
                    for (Part part : parts) {
                        QName tq = part.getTypeName();
                        QName eq = part.getElementName();
                        if (tq != null && eq != null) {
                            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_WSDL_PART_TYPE, part.getName());
                            continue nextBindingOperation;
                        } else if (tq != null) {
                            operationQNames.add(new QName(null, part.getName()));
                            auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_WSDL_PART_TYPE, inputMessage.getQName().getLocalPart());
                        } else if (eq != null) {
                            operationQNames.add(eq);
                        }
                    }
                } else {
                    auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_BAD_STYLE, operationStyle, bop.getName());
                    continue nextBindingOperation;
                }

                if (operationQNames.isEmpty()) {
                    auditor.logAndAudit(MessageProcessingMessages.SR_SOAPOPERATION_NO_QNAMES_FOR_OP, bop.getName());
                }

                operationQnameLists.add(operationQNames);
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

    public int getSpeed() {
        return ServiceResolver.SLOW;
    }

    public Set<List<QName>> getDistinctParameters(PublishedService candidateService) throws ServiceResolutionException {
        throw new UnsupportedOperationException();
    }

    public boolean isSoap() {
        return true;
    }
}
