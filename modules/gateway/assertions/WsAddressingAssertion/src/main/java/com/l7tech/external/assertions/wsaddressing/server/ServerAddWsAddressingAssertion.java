/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.wsaddressing.AddWsAddressingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SoapKnob;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.xmlsec.ServerAddWssSignature;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.wsdl.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerAddWsAddressingAssertion extends ServerAddWssSignature<AddWsAddressingAssertion> {

    public ServerAddWsAddressingAssertion(final AddWsAddressingAssertion assertion,
                                          final ApplicationContext applicationContext ) throws PolicyAssertionException {
        super(assertion, assertion, assertion, applicationContext, logger, false);
        this.auditor = applicationContext != null ? new Auditor(this, applicationContext, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        //validate required fields
        if(assertion.getAction() == null){
            throw new PolicyAssertionException(assertion, "Action message addressing property is required.");
        }

    }

    @Override
    public boolean hasDecorationRequirements() {
        return assertion.isSignMessageProperties();
    }

    @Override
    protected int addDecorationRequirements(final PolicyEnforcementContext context,
                                            final AuthenticationContext authContext,
                                            final Document soapmsg,
                                            final DecorationRequirements wssReq,
                                            final Message targetMessage) throws PolicyAssertionException {
        final Element header;
        final String soapAction;
        try {
            final SoapKnob soapKnob = targetMessage.getSoapKnob();
            soapAction = soapKnob.getSoapAction();
            final Document writeDoc = targetMessage.getXmlKnob().getDocumentWritable();
            header = SoapUtil.getOrMakeHeader(writeDoc);
        } catch (Exception e) {
            String msg = "Cannot get XML document from target message: " + ExceptionUtils.getMessage(e);
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, ExceptionUtils.getDebugException(e));
            return -1;
        } 

        final List<Element> elementsToSign = new ArrayList<Element>();
        try {
            final Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);
            
            String wsaNs = resolveProperty(assertion.getWsaNamespaceUri(), vars);
            if (wsaNs == null) {
                wsaNs = AddWsAddressingAssertion.DEFAULT_NAMESPACE;
                logger.log(Level.INFO, "No namespace value found for WS-Addressing. Using default value of " + AddWsAddressingAssertion.DEFAULT_NAMESPACE);
            }

            if(!ValidationUtils.isValidUri(wsaNs)){
                logger.log(Level.INFO, "Invalid namespace URI found for WS-Addressing Namespace '"+wsaNs+"'. Using default value of " + AddWsAddressingAssertion.DEFAULT_NAMESPACE);
                wsaNs = AddWsAddressingAssertion.DEFAULT_NAMESPACE;
            }

            String resolvedAction = resolveProperty(assertion.getAction(), vars);
            if(resolvedAction == null){
                auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_ACTION_SUPPLIED);
                return -1;
            }

            if (AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_INPUT.equals(resolvedAction) ||
                    AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_OUTPUT.equals(resolvedAction)) {
                final Pair<Binding,Operation> pair = context.getBindingAndOperation();
                String wsdlAction = null;
                if(pair != null){
                    final Operation operation = pair.right; 
                    Map map = null;
                    if(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_OUTPUT.equals(resolvedAction)){
                        final Output output = operation.getOutput();
                        if(output != null){
                            map = output.getExtensionAttributes();
                        }
                    } else {
                        final Input input = operation.getInput();
                        if(input != null){
                            map = input.getExtensionAttributes();
                        }
                    }

                    if(map != null && !map.isEmpty()){
                        final QName actionQName = new QName(SoapConstants.WSA_WSDL_LATEST, SoapConstants.WSA_MSG_PROP_ACTION);
                        final Object o = map.get(actionQName);
                        if(o != null){
                            wsdlAction = o.toString();
                        } else {
                            for(String namespaceUri: AddWsAddressingAssertion.WSA_WSDL_NAMESPACES){
                                final QName qName = new QName(namespaceUri, SoapConstants.WSA_MSG_PROP_ACTION);
                                if(map.containsKey(qName)){
                                    wsdlAction = map.get(qName).toString();
                                    break;
                                }
                            }
                        }
                    }
                    
                    if(wsdlAction == null && AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_INPUT.equals(resolvedAction)){
                        //check binding for soapAction ONLY when dealing with the INPUT, there is no fallback for
                        //output, as the soap binding extension element only allows a soap action for a request to be specified.
                        final Binding binding = pair.left;
                        String inputName = null;
                        String outputName = null;
                        if(operation.getInput() != null){
                            final Input input = operation.getInput();
                            if(input != null){
                                inputName = input.getName(); //may be null, that is ok.
                            }
                        }

                        if(operation.getOutput() != null){
                            final Output output = operation.getOutput();
                            if(output != null){
                                outputName = output.getName(); //may be null, that is ok.
                            }
                        }

                        //need to look up operation with the input and ouput names, if available, as the operation name
                        //is not unique in the WSDL.
                        final BindingOperation lookedUpOp = binding.getBindingOperation(operation.getName(), inputName, outputName);
                        if(lookedUpOp != null){
                            wsdlAction = SoapUtil.extractSoapAction(lookedUpOp, context.getService().getSoapVersion());
                        }
                    }
                }

                if(wsdlAction == null || wsdlAction.trim().isEmpty()){
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_WSDL_ACTION_FOUND);
                    return -1;
                }
                resolvedAction = wsdlAction;
            } else if (AddWsAddressingAssertion.ACTION_FROM_TARGET_MESSAGE.equals(resolvedAction)){
                if (soapAction == null || soapAction.trim().isEmpty()) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_SOAP_ACTION);
                    return -1;
                }
                resolvedAction = soapAction;
            }

            if (!ValidationUtils.isValidUri(resolvedAction)) {
                auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_WARN, new String[]{resolvedAction, SoapConstants.WSA_MSG_PROP_ACTION});
                return -1;
            }

            //set action context variable.
            context.setVariable(assertion.getVariablePrefix() + "." + AddWsAddressingAssertion.SUFFIX_ACTION, resolvedAction);
            int elementNumber = 0;
            elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_ACTION, resolvedAction, elementNumber++, false));

            final String resolvedMessageId = resolveProperty(assertion.getMessageId(), vars);

            final String msgIdToUse;
            if(AddWsAddressingAssertion.MESSAGE_ID_AUTOMATIC.equals(resolvedMessageId)){
                msgIdToUse = SoapUtil.generateUniqueUri("MessageId-", true);
            } else {
                msgIdToUse = resolvedMessageId;
            }

            if(msgIdToUse != null){
                if (!ValidationUtils.isValidUri(msgIdToUse)) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_WARN, new String[]{msgIdToUse, SoapConstants.WSA_MSG_PROP_MESSAGE_ID});
                    return -1;
                }
                //set message id context variable
                context.setVariable(assertion.getVariablePrefix() +"." + AddWsAddressingAssertion.SUFFIX_MESSAGE_ID, msgIdToUse);
                elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_MESSAGE_ID, msgIdToUse, elementNumber++, false));
            }

            final String destination = resolveProperty(assertion.getDestination(), vars);
            if(destination != null){
                if (!ValidationUtils.isValidUri(destination)) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, new String[]{destination, SoapConstants.WSA_MSG_PROP_DESTINATION});
                } else {
                    elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_DESTINATION, destination, elementNumber++, false));
                }
            }

            final String from = resolveProperty(assertion.getSourceEndpoint(), vars);
            if(from != null){
                if (!ValidationUtils.isValidUri(from)) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, new String[]{from, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT});
                } else {
                    elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT, from, elementNumber++, true));
                }
            }

            final String replyTo = resolveProperty(assertion.getReplyEndpoint(), vars);
            if(replyTo != null){
                if (!ValidationUtils.isValidUri(replyTo)) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, new String[]{replyTo, SoapConstants.WSA_MSG_PROP_REPLY_TO});
                } else {
                    elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_REPLY_TO, replyTo, elementNumber++, true));
                }
            }

            final String faultTo = resolveProperty(assertion.getFaultEndpoint(), vars);
            if(faultTo != null){
                if (!ValidationUtils.isValidUri(faultTo)) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, new String[]{faultTo, SoapConstants.WSA_MSG_PROP_FAULT_TO});
                } else {
                    elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_FAULT_TO, faultTo, elementNumber++, true));
                }
            }

            final String relatesMsgId = resolveProperty(assertion.getRelatesToMessageId(), vars);
            if(relatesMsgId != null){
                if (!ValidationUtils.isValidUri(relatesMsgId)) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, new String[]{relatesMsgId, SoapConstants.WSA_MSG_PROP_RELATES_TO});
                } else {
                    final Element relatesToEl =
                            addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_RELATES_TO, relatesMsgId, elementNumber++, false);
                    final String prefix = DomUtils.getOrCreatePrefixForNamespace(relatesToEl, wsaNs, "wsa");
                    relatesToEl.setAttributeNS(wsaNs, prefix + ":" + SoapConstants.WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE,
                                                      SoapConstants.WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE);

                    elementsToSign.add(relatesToEl);
                }
            }

        } catch (Exception e) {
            String msg = "Cannot add WS-Addressing element to target message: " + ExceptionUtils.getMessage(e);
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, ExceptionUtils.getDebugException(e));
            return -1;
        }

        if(assertion.isSignMessageProperties()){
            wssReq.getElementsToSign().addAll(elementsToSign);
            return elementsToSign.size();
        }

        return 0;
    }

    private static class InvalidRuntimeValueException extends Exception{
        private InvalidRuntimeValueException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    /**
     * Convert a string variable into it's expanded form in the case it references any variables.
     *
     * @param vars Available context variables
     * @param maybeAVariable String. Must not be null and must not be the empty string
     * @return resolved String.
     * @throws InvalidRuntimeValueException
     */
    private String getStringVariable(final Map<String, Object> vars, String maybeAVariable)
            throws InvalidRuntimeValueException{
        //explicitly checking as exception throw below should only happen for the case when a string resolves to nothing.
        if(maybeAVariable == null || maybeAVariable.trim().isEmpty()) throw new IllegalArgumentException("maybeAVariable must be non null and not empty");

        final String value;
        try {
            value =  ExpandVariables.process(maybeAVariable, vars, auditor);
        } catch (Exception e) {
            //we want to catch any exception which the above call can generate. Any exception means the assertion fails.
            throw new InvalidRuntimeValueException("Error getting value: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final boolean isEmpty = value.trim().isEmpty();
        if (isEmpty) {
            logger.log(Level.INFO, "Value for field '" + maybeAVariable + "' resolved to nothing.");
        }
        return value;
    }

    private String resolveProperty(
            final String propertyValue,
            final Map<String, Object> vars) throws InvalidRuntimeValueException {
        if(propertyValue == null || propertyValue.trim().isEmpty()) return null;

        final String resolved = getStringVariable(vars, propertyValue);

        if(resolved == null || resolved.trim().isEmpty()) return null;

        return resolved;
    }

    private Element addElementToHeader(final Element soapHeaderEl,
                                       final String wsaNs,
                                       final String localName,
                                       final String propertyValue,
                                       final int elementNumber,
                                       final boolean isEndPointReference
    ) throws InvalidRuntimeValueException, InvalidDocumentFormatException {
        final Element existingHeaderEl = XmlUtil.findFirstChildElementByName(soapHeaderEl, wsaNs, localName);
        if ( existingHeaderEl != null ) {
            soapHeaderEl.removeChild( existingHeaderEl );
        }

        final Element newHeaderEl = XmlUtil.createAndAppendElementNS(soapHeaderEl, localName, wsaNs, "wsa");
        //add Id attribute
        SoapUtil.getOrCreateElementWsuId(newHeaderEl, elementNumber);

        if(isEndPointReference){
            final Element addressElement = XmlUtil.createAndAppendElementNS(newHeaderEl, "Address", wsaNs, "wsa");
            addressElement.setTextContent(propertyValue);
        } else {
            newHeaderEl.setTextContent(propertyValue);
        }

        return newHeaderEl;
    }

    // - PRIVATE
    private final Auditor auditor;
    private final String[] variablesUsed;
    private static final Logger logger = Logger.getLogger(ServerAddWsAddressingAssertion.class.getName());
}
