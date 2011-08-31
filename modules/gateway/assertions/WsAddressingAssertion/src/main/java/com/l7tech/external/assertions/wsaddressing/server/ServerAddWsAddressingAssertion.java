package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.wsaddressing.AddWsAddressingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SoapKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;

public class ServerAddWsAddressingAssertion extends AbstractMessageTargetableServerAssertion<AddWsAddressingAssertion> {

    public ServerAddWsAddressingAssertion(final AddWsAddressingAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        //validate required fields
        if(assertion.getWsaNamespaceUri() == null){
            throw new PolicyAssertionException(assertion, "WS-Addressing namespace is required.");
        }

        if(assertion.getAction() == null){
            throw new PolicyAssertionException(assertion, "Action message addressing property is required.");
        }
    }

    @SuppressWarnings({"UnusedAssignment", "ThrowableResultOfMethodCallIgnored"})
    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message message,
                                             final String messageDescription,
                                             final AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        try {
            if (!message.isSoap()) {
                logAndAudit(AssertionMessages.MESSAGE_NOT_SOAP, messageDescription, "Cannot add WS-Addressing headers");
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
        
        final Element header;
        final String soapAction;
        try {
            final SoapKnob soapKnob = message.getSoapKnob();
            soapAction = SoapUtil.stripQuotes(soapKnob.getSoapAction());//ok if soap action is null
            final Document writeDoc = message.getXmlKnob().getDocumentWritable();
            header = SoapUtil.getOrMakeHeader(writeDoc);
        } catch (Exception e) {
            String msg = "Cannot get XML document from target message: " + ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        try {
            final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

            String wsaNs = resolveProperty(assertion.getWsaNamespaceUri(), vars);

            if(!ValidationUtils.isValidUri(wsaNs)){
                logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_NAMESPACE, wsaNs, "Namespace is not a valid URI");
                return AssertionStatus.FAILED;
            } else if (!new URI(wsaNs).isAbsolute()) {
                logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_NAMESPACE, wsaNs, "Namespace is not an absolute URI");
                return AssertionStatus.FAILED;
            }

            String resolvedAction = resolveProperty(assertion.getAction(), vars);
            if(resolvedAction == null){
                logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_ACTION_SUPPLIED);
                return AssertionStatus.FAILED;
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
                        //If the WSDLReader was correctly configured, then the type of the Action property should
                        //be a String. If it is incorrectly configured e.g. the extension attribute has not been registered,
                        //then the type may be either a String or a QName. Either way, a String is the expected type,
                        //so toString is simply called on the value from the map. If a QName is returned, it will fail
                        //checking if it's a URI below.
                        final Object o = map.get(actionQName);
                        if(o != null){
                            wsdlAction = o.toString();
                        } else {
                            for(String namespaceUri: SoapConstants.WSA_WSDL_NAMESPACES){
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

                        //need to look up operation with the input and output names, if available, as the operation name
                        //is not unique in the WSDL.
                        final BindingOperation lookedUpOp = binding.getBindingOperation(operation.getName(), inputName, outputName);
                        if(lookedUpOp != null){
                            wsdlAction = SoapUtil.extractSoapAction(lookedUpOp, context.getService().getSoapVersion());
                        }
                    }
                }

                if(wsdlAction == null || wsdlAction.trim().isEmpty()){
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_WSDL_ACTION_FOUND);
                    return AssertionStatus.FAILED;
                }
                resolvedAction = wsdlAction;
            } else if (AddWsAddressingAssertion.ACTION_FROM_TARGET_MESSAGE.equals(resolvedAction)){
                if (soapAction == null || soapAction.trim().isEmpty()) {
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_NO_SOAP_ACTION);
                    return AssertionStatus.FAILED;
                }
                resolvedAction = soapAction;
            }

            if (!ValidationUtils.isValidUri(resolvedAction)) {
                logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_WARN, resolvedAction, SoapConstants.WSA_MSG_PROP_ACTION);
                return AssertionStatus.FAILED;
            }

            //set action context variable.
            context.setVariable(assertion.getVariablePrefix() + "." + AddWsAddressingAssertion.SUFFIX_ACTION, resolvedAction);
            int elementNumber = 0;
            addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_ACTION, resolvedAction, elementNumber++, false);

            final String resolvedMessageId = resolveProperty(assertion.getMessageId(), vars);

            final String msgIdToUse;
            if(AddWsAddressingAssertion.MESSAGE_ID_AUTOMATIC.equals(resolvedMessageId)){
                msgIdToUse = SoapUtil.generateUniqueUri("MessageId-", true);
            } else {
                msgIdToUse = resolvedMessageId;
            }

            if(msgIdToUse != null){
                if (!ValidationUtils.isValidUri(msgIdToUse)) {
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_WARN, msgIdToUse, SoapConstants.WSA_MSG_PROP_MESSAGE_ID);
                    return AssertionStatus.FAILED;
                }
                //set message id context variable
                context.setVariable(assertion.getVariablePrefix() +"." + AddWsAddressingAssertion.SUFFIX_MESSAGE_ID, msgIdToUse);
                addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_MESSAGE_ID, msgIdToUse, elementNumber++, false);
            }

            final String destination = resolveProperty(assertion.getDestination(), vars);
            if(destination != null){
                if (!ValidationUtils.isValidUri(destination)) {
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, destination, SoapConstants.WSA_MSG_PROP_DESTINATION);
                } else {
                    addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_DESTINATION, destination, elementNumber++, false);
                }
            }

            final String from = resolveProperty(assertion.getSourceEndpoint(), vars);
            if(from != null){
                if (!ValidationUtils.isValidUri(from)) {
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, from, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT);
                } else {
                    addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT, from, elementNumber++, true);
                }
            }

            final String replyTo = resolveProperty(assertion.getReplyEndpoint(), vars);
            if(replyTo != null){
                if (!ValidationUtils.isValidUri(replyTo)) {
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, replyTo, SoapConstants.WSA_MSG_PROP_REPLY_TO);
                } else {
                    addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_REPLY_TO, replyTo, elementNumber++, true);
                }
            }

            final String faultTo = resolveProperty(assertion.getFaultEndpoint(), vars);
            if(faultTo != null){
                if (!ValidationUtils.isValidUri(faultTo)) {
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, faultTo, SoapConstants.WSA_MSG_PROP_FAULT_TO);
                } else {
                    addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_FAULT_TO, faultTo, elementNumber++, true);
                }
            }

            final String relatesMsgId = resolveProperty(assertion.getRelatesToMessageId(), vars);
            if(relatesMsgId != null){
                if (!ValidationUtils.isValidUri(relatesMsgId)) {
                    logAndAudit(AssertionMessages.ADD_WS_ADDRESSING_INVALID_URI_VALUE_INFO, relatesMsgId, SoapConstants.WSA_MSG_PROP_RELATES_TO);
                } else {
                    final Element relatesToEl =
                            addElementToHeader(header, wsaNs, SoapConstants.WSA_MSG_PROP_RELATES_TO, relatesMsgId, elementNumber++, false);
                    final String prefix = DomUtils.getOrCreatePrefixForNamespace(relatesToEl, wsaNs, "wsa");
                    relatesToEl.setAttributeNS(wsaNs, prefix + ":" + SoapConstants.WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE,
                                                      SoapConstants.WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE);
                }
            }

        } catch (Exception e) {
            String msg = "Cannot add WS-Addressing element to target message: " + ExceptionUtils.getMessage(e);
            logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
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
     * @throws InvalidRuntimeValueException if the String value maybeAVariable cannot be processed.
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    private String getStringVariable(final Map<String, Object> vars, String maybeAVariable)
            throws InvalidRuntimeValueException{
        //explicitly checking as exception throw below should only happen for the case when a string resolves to nothing.
        if(maybeAVariable == null || maybeAVariable.trim().isEmpty()) throw new IllegalArgumentException("maybeAVariable must be non null and not empty");

        final String value;
        try {
            value =  ExpandVariables.process(maybeAVariable, vars, getAudit());
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

    private final String[] variablesUsed;
}
