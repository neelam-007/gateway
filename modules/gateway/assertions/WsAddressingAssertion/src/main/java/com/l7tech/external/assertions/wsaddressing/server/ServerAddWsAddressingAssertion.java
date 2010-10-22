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
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerAddWsAddressingAssertion extends AbstractServerAssertion<AddWsAddressingAssertion>{

    public ServerAddWsAddressingAssertion(final AddWsAddressingAssertion assertion,
                                          final ApplicationContext applicationContext ) throws PolicyAssertionException {
        super(assertion);
        this.auditor = applicationContext != null ? new Auditor(this, applicationContext, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        try {
            signer = (assertion.isSignMessageProperties()) ?
                    ServerAssertionUtils.getSignerInfo(applicationContext, assertion) : null;
        } catch (KeyStoreException e) {
            throw new PolicyAssertionException(assertion,
                    "Cannot create SignerInfo: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }

        //validate required fields
        if(assertion.getAction() == null){
            throw new PolicyAssertionException(assertion, "Action message addressing property is required.");
        }

    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message msg;
        final String messageDescription;
        try {
            messageDescription = assertion.getTargetName();
            msg = context.getTargetMessage(assertion);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        }

        final SoapInfo soapInfo;
        final Document writableDoc;
        try {
            if(!msg.isSoap()){
                auditor.logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_TARGET_NOT_SOAP);
                return AssertionStatus.SERVER_ERROR;
            }
            final SoapKnob soapKnob = msg.getSoapKnob();
            soapInfo = soapKnob.getSoapInfo();
            writableDoc = msg.getXmlKnob().getDocumentWritable();
        } catch (SAXException e) {
            //todo log and audit and see if this is right thing to do
            throw new CausedIOException(e);
        } catch (MessageNotSoapException e) {
            //todo log and audit and see if this is right thing to do
            throw new CausedIOException(e);
        }

        final Element header;
        try {
            header = SoapUtil.getHeaderElement(writableDoc);
        } catch (InvalidDocumentFormatException e) {
            e.printStackTrace();
            //todo log and audit and see if this is right thing to do
            throw new CausedIOException(e);
        }

        String wsaNs = assertion.getWsaNamespaceUri();
        if (wsaNs == null || wsaNs.trim().isEmpty()) wsaNs = SoapUtil.WSA_NAMESPACE;

        final Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);

        String action = assertion.getAction();

        if(soapInfo != null){
            final String soapAction = soapInfo.getSoapAction();
            if(soapAction != null){
                if(action.equals(AddWsAddressingAssertion.ACTION_AUTOMATIC)){
                    action = soapAction;
                } else if(!soapAction.equals(action)){
                    //todo log and audit
                    return AssertionStatus.SERVER_ERROR;
                }
            }
        }

        try {
            int elementNumber = 0;
            addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_ACTION, action, vars, elementNumber++, false);

            final String messageId = assertion.getMessageId();
            if(messageId != null && !messageId.trim().isEmpty()){
                addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_MESSAGE_ID, messageId, vars, elementNumber++, false);
            }

            final String destination = assertion.getDestination();
            if(destination != null && !destination.trim().isEmpty()){
                addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_DESTINATION, destination, vars, elementNumber++, false);
            }

            final String from = assertion.getSourceEndpoint();
            if(from != null && !from.trim().isEmpty()){
                addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT, from, vars, elementNumber++, true);
            }

            final String replyTo = assertion.getReplyEndpoint();
            if(replyTo != null && !replyTo.trim().isEmpty()){
                addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_REPLY_TO, replyTo, vars, elementNumber++, true);
            }

            final String faultTo = assertion.getFaultEndpoint();
            if(faultTo != null && !faultTo.trim().isEmpty()){
                addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_FAULT_TO, faultTo, vars, elementNumber++, true);
            }

            final String relatesMsgId = assertion.getRelatesToMessageId();
            if(relatesMsgId != null && !relatesMsgId.trim().isEmpty()){
                final Element relatesToEl =
                        addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_RELATES_TO, relatesMsgId, vars, elementNumber++, false);
                final String prefix = DomUtils.getOrCreatePrefixForNamespace(relatesToEl, wsaNs, "wsa");
                relatesToEl.setAttributeNS(wsaNs, prefix + ":" + SoapConstants.WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE,
                                                  SoapConstants.WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE);

            }

        } catch (InvalidRuntimeValueException e) {
            //todo log and audit and see if this is right thing to do
            e.printStackTrace();
            return AssertionStatus.SERVER_ERROR;
        } catch (InvalidDocumentFormatException e) {
            //todo log and audit and see if this is right thing to do
            e.printStackTrace();
            return AssertionStatus.SERVER_ERROR;
        }

        return AssertionStatus.NONE;
    }

    private static class InvalidRuntimeValueException extends Exception{
        private InvalidRuntimeValueException(String message, Throwable cause) {
            super(message, cause);
        }

        private InvalidRuntimeValueException(String message) {
            super(message);
        }
    }


    /**
     * Convert a string variable into it's expanded form in the case it references any variables.
     *
     * @param vars Available context variables
     * @param maybeAVariable String. Must not be null and must not be the empty string
     * @param strict boolean true if the resolved value cannot be the empty string
     * @return
     * @throws InvalidRuntimeValueException
     */
    private String getStringVariable(final Map<String, Object> vars, String maybeAVariable, boolean strict)
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
        if(isEmpty && strict) {
            throw new InvalidRuntimeValueException("Value for field '" + maybeAVariable + "'resolved to nothing.");
        } else if (isEmpty) {
            logger.log(Level.INFO, "Value for field '" + maybeAVariable + "' resolved to nothing.");
        }
        return value;
    }
    
    private Element addElementToHeader(final Element soapHeaderEl,
                                       final String wsaNs,
                                       final String localName,
                                       final String propertyValue,
                                       final Map<String, Object> vars,
                                       final int elementNumber, boolean isEndPointReference) throws InvalidRuntimeValueException, InvalidDocumentFormatException {
        final Element existingHeaderEl = XmlUtil.findFirstChildElementByName(soapHeaderEl, wsaNs, localName);
        if ( existingHeaderEl != null ) {
            soapHeaderEl.removeChild( existingHeaderEl );
        }

        final Element newHeaderEl = XmlUtil.createAndAppendElementNS(soapHeaderEl, localName, wsaNs, "wsa");
        //add Id attribute
        SoapUtil.getOrCreateElementWsuId(newHeaderEl, elementNumber);

        if(isEndPointReference){
            final Element addressElement = XmlUtil.createAndAppendElementNS(newHeaderEl, "Address", wsaNs, "wsa");
            final String value = getStringVariable(vars, propertyValue, false);
            addressElement.setTextContent(value);
        } else {
            final String value = getStringVariable(vars, propertyValue, false);
            newHeaderEl.setTextContent(value);
        }


        return newHeaderEl;
    }

    // - PRIVATE
    private final Auditor auditor;
    private final SignerInfo signer;
    private final String[] variablesUsed;
    
    private static final Logger logger = Logger.getLogger(ServerAddWsAddressingAssertion.class.getName());
}
