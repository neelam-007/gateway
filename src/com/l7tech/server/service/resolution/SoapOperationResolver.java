/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.SoapKnob;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.service.PublishedService;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
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

    protected List<List<QName>> doGetTargetValues(PublishedService service) throws ServiceResolutionException {
        try {
            List<List<QName>> operationQnameLists = new ArrayList<List<QName>>();
            if (!service.isSoap()) {
                logger.fine("Service is not SOAP");
                return EMPTY;
            }
            Wsdl wsdl = service.parsedWsdl();
            if (wsdl == null) {
                logger.info("Service is SOAP but has no WSDL");
                return EMPTY;
            }
            
            //noinspection unchecked
            Map<QName, Binding> bindings = wsdl.getDefinition().getBindings();
            if (bindings == null || bindings.isEmpty()) {
                logger.info("WSDL has no bindings");
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
                        logger.warning("Couldn't get style for BindingOperation " + bop.getName());
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
                                logger.warning(MessageFormat.format("Part {0} has both an element and a type", part.getName()));
                                continue nextBindingOperation;
                            } else if (tq != null) {
                                operationQNames.add(new QName(null, part.getName()));
                                logger.warning("Input message " + inputMessage.getQName() + " in document-style operation has a type, not an element");
                            } else if (eq != null) {
                                operationQNames.add(eq);
                            }
                        }
                    } else {
                        logger.warning("Unsupported style '" + operationStyle + "' for " + bop.getName());
                        continue nextBindingOperation;
                    }

                    if (operationQNames.isEmpty()) logger.warning("Unable to find QNames for BindingOperation " + bop.getName());

                    operationQnameLists.add(operationQNames);
                }

            }

            if (operationQnameLists.isEmpty()) {
                logger.log(Level.WARNING, "Unable to find any payload element QNames for service {0} (#{1})", new Object[] { service.getName(), service.getOid() });
                return EMPTY;
            }

            return operationQnameLists;
        } catch (WSDLException e) {
            logger.log(Level.WARNING, "Unable to parse WSDL for " + service.getName() + " service (#" + service.getOid() +")", e);
            return EMPTY;
        }
    }

    protected List<QName> getRequestValue(Message request) throws ServiceResolutionException {
        SoapKnob sk = (SoapKnob) request.getKnob(SoapKnob.class);
        try {
            return Arrays.asList(sk.getPayloadNames());
        } catch (IOException e) {
            throw new ServiceResolutionException("Unable to parse payload element QNames", e);
        } catch (SAXException e) {
            throw new ServiceResolutionException("Unable to parse payload element QNames", e);
        }
    }

    public int getSpeed() {
        return ServiceResolver.SLOW;
    }

    public Set<List<QName>> getDistinctParameters(PublishedService candidateService) throws ServiceResolutionException {
        throw new UnsupportedOperationException();
    }
}
