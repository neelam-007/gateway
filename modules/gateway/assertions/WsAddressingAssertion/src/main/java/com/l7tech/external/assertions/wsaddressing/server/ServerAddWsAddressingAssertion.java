/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.wsaddressing.AddWsAddressingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SoapInfo;
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
        final SoapInfo soapInfo;
        try {
            final SoapKnob soapKnob = targetMessage.getSoapKnob();
            soapInfo = soapKnob.getSoapInfo();
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
            if(wsaNs == null) {
                wsaNs = AddWsAddressingAssertion.DEFAULT_NAMESPACE;
                logger.log(Level.INFO, "No namespace value found for WS-Addressing. Using default value of " + AddWsAddressingAssertion.DEFAULT_NAMESPACE);
            }

            String resolvedAction = resolveProperty(assertion.getAction(), vars);
            if(resolvedAction == null){
                auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_ACTION_SUPPLIED);
                return -1;
            }

            final boolean hasSoapAction = soapInfo != null && soapInfo.getSoapAction() != null;
            if (resolvedAction.equals(AddWsAddressingAssertion.ACTION_AUTOMATIC)) {
                if (!hasSoapAction) {
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_SOAP_ACTION);
                    return -1;
                }
                resolvedAction = soapInfo.getSoapAction();
            } else if (hasSoapAction) {
                final String soapAction = soapInfo.getSoapAction();
                if(!soapAction.equals(resolvedAction)){
                    auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_SOAP_ACTION_MISMATCH, soapAction, resolvedAction);
                    return -1;
                }
            }

            int elementNumber = 0;
            elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_ACTION, resolvedAction, elementNumber++, false));

            final String messageId = resolveProperty(assertion.getMessageId(), vars);
            if(messageId != null){
                elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_MESSAGE_ID, messageId, elementNumber++, false));
            }

            final String destination = resolveProperty(assertion.getDestination(), vars);
            if(destination != null){
                elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_DESTINATION, destination, elementNumber++, false));
            }

            final String from = resolveProperty(assertion.getSourceEndpoint(), vars);
            if(from != null){
                elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT, from, elementNumber++, true));
            }

            final String replyTo = resolveProperty(assertion.getReplyEndpoint(), vars);
            if(replyTo != null){
                elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_REPLY_TO, replyTo, elementNumber++, true));
            }

            final String faultTo = resolveProperty(assertion.getFaultEndpoint(), vars);
            if(faultTo != null){
                elementsToSign.add(addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_FAULT_TO, faultTo, elementNumber++, true));
            }

            final String relatesMsgId = resolveProperty(assertion.getRelatesToMessageId(), vars);
            if(relatesMsgId != null){
                final Element relatesToEl =
                        addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_RELATES_TO, relatesMsgId, elementNumber++, false);
                final String prefix = DomUtils.getOrCreatePrefixForNamespace(relatesToEl, wsaNs, "wsa");
                relatesToEl.setAttributeNS(wsaNs, prefix + ":" + SoapConstants.WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE,
                                                  SoapConstants.WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE);

                elementsToSign.add(relatesToEl);
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
