package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.message.Message;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the WsAddressingAssertion.
 *
 * @see com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion
 */
public class ServerWsAddressingAssertion extends AbstractServerAssertion<WsAddressingAssertion> {
    private final String otherNamespaceUri = assertion.getEnableOtherNamespace();
    private final String[] acceptableNamespaces;
    {
        if (otherNamespaceUri != null && otherNamespaceUri.length() > 0) {
            this.acceptableNamespaces = new String[NS_ADDRESSING.length+1];
            System.arraycopy(NS_ADDRESSING, 0, this.acceptableNamespaces, 0, NS_ADDRESSING.length);
            this.acceptableNamespaces[NS_ADDRESSING.length] = otherNamespaceUri;
        } else {
            this.acceptableNamespaces = NS_ADDRESSING;
        }
    }

    //- PUBLIC

    /**
     *
     */
    public ServerWsAddressingAssertion(final WsAddressingAssertion assertion,
                                       final ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.auditor = new Auditor(this, context, logger);
    }

    /**
     * 
     */
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;
        final Message msg;
        try {
            msg = context.getTargetMessage(assertion);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.FAILED;
        }

        try {
            Map<QName, String> addressingProperties = new HashMap();
            if ( assertion.isRequireSignature() ) {
                populateAddressingFromSignedElements(getSignedElements(msg), addressingProperties);
            } else {
                populateAddressingFromMessage(getElementCursor(msg), addressingProperties);
            }

            if ( assertion.isEnableWsAddressing10() && addressingPresent(addressingProperties, SoapUtil.WSA_NAMESPACE_10) ) {
                if ( assertion.getVariablePrefix() != null) {
                    setVariables(addressingProperties, SoapUtil.WSA_NAMESPACE_10, context);
                }

                auditor.logAndAudit(AssertionMessages.WS_ADDRESSING_HEADERS_OK);
                status = AssertionStatus.NONE;
            } else if ( assertion.isEnableWsAddressing200408() && addressingPresent(addressingProperties, NS_WS_ADDRESSING_200408) ) {
                if ( assertion.getVariablePrefix() != null) {
                    setVariables(addressingProperties, NS_WS_ADDRESSING_200408, context);
                }

                auditor.logAndAudit(AssertionMessages.WS_ADDRESSING_HEADERS_OK);
                status = AssertionStatus.NONE;
            } else if (otherNamespaceUri != null && otherNamespaceUri.length() > 0 && addressingPresent(addressingProperties, otherNamespaceUri) ) {
                if ( assertion.getVariablePrefix() != null) {
                    setVariables(addressingProperties, otherNamespaceUri, context);
                }

                auditor.logAndAudit(AssertionMessages.WS_ADDRESSING_HEADERS_OK);
                status = AssertionStatus.NONE;
            } else {
                context.setRequestPolicyViolated();
                if ( assertion.isRequireSignature() ) {
                    auditor.logAndAudit(AssertionMessages.WS_ADDRESSING_NO_SIGNED_HEADERS);                    
                } else {
                    auditor.logAndAudit(AssertionMessages.WS_ADDRESSING_NO_HEADERS);
                }
            }
        } catch (AddressingProcessingException ape) {
            logger.log(Level.INFO, "WS-Addressing processing terminated due to ''{0}''", ape.getMessage());
            status = ape.getAssertionStatus();
        }

        return status;
    }

    //- PROTECTED

    /**
     *
     */
    public ServerWsAddressingAssertion(final WsAddressingAssertion assertion,
                                       final Auditor auditor) throws PolicyAssertionException {
        super(assertion);
        this.auditor = auditor;
    }

    /**
     * Populate the given addressing properties using an element cursor
     */
    protected void populateAddressingFromMessage(final ElementCursor cursor,
                                                 final Map<QName, String> addressingProperties)
            throws AddressingProcessingException, IOException {
        try {
            moveToSoapHeader(cursor);
            final ElementCursor.Visitor visitor = new ElementCursor.Visitor(){
                public void visit(final ElementCursor ec) throws InvalidDocumentFormatException {
                    if ( ArrayUtils.contains(acceptableNamespaces, ec.getNamespaceUri()) ) {
                        QName name = new QName(ec.getNamespaceUri(), ec.getLocalName());

                        if ( !addressingProperties.containsKey(name) ) {
                            if ( WSA_FAULTTO.equals(ec.getLocalName()) || WSA_REPLYTO.equals(ec.getLocalName()) ) {
                                // process the address child element
                                ec.pushPosition();
                                if (ec.moveToFirstChildElement(WSA_ADDRESS, ec.getNamespaceUri()) ) {
                                    addressingProperties.put(name, ec.getTextValue());
                                }
                                ec.popPosition();
                            } else {
                                addressingProperties.put(name, ec.getTextValue());
                            }
                        }
                    }
                }
            };
            cursor.visitChildElements(visitor);
        } catch (InvalidDocumentFormatException idfe) {
            throw new CausedIOException(idfe);
        }
    }

    /**
     * Populate the given addressing properties using signed elements
     */
    protected void populateAddressingFromSignedElements(final SignedElement[] signedElements,
                                                        final Map<QName, String> addressingProperties)
            throws AddressingProcessingException, IOException {
        for (SignedElement signedElement : signedElements ) {
            Element element = signedElement.asElement();

            if ( ArrayUtils.contains(acceptableNamespaces, element.getNamespaceURI()) && isSoapHeader(element.getParentNode()) ) {
                QName name = new QName(element.getNamespaceURI(), element.getLocalName());

                if ( addressingProperties.containsKey(name) ) {
                    continue;
                }

                if ( WSA_FAULTTO.equals(element.getLocalName()) || WSA_REPLYTO.equals(element.getLocalName()) ) {
                    // process the address child element
                    Element address = XmlUtil.findFirstChildElementByName(element, element.getNamespaceURI(), WSA_ADDRESS);
                    if ( address != null ) {
                        addressingProperties.put(name, XmlUtil.getTextValue(address));
                    }
                } else {
                    addressingProperties.put(name, XmlUtil.getTextValue(element));
                }
            }
        }
    }

    /**
     * Set context variables from the given addressing properties
     */
    protected void setVariables(final Map<QName, String> addressingProperties,
                                final String namespace,
                                final Functions.BinaryVoid<String,String> setter) {

        for (Map.Entry<QName,String> entry : addressingProperties.entrySet() ) {
            if ( namespace.equals(entry.getKey().getNamespaceURI()) ) {
                String name = getName(entry.getKey().getLocalPart());
                if ( name != null)
                    setter.call(name, entry.getValue());
            }
        }

        setter.call(assertion.getVariablePrefix() + "." + WsAddressingAssertion.VAR_SUFFIX_NAMESPACE, namespace);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerWsAddressingAssertion.class.getName());

    private static final String NS_WS_ADDRESSING_200408 = SoapUtil.WSA_NAMESPACE2;
    private static final String[] NS_ADDRESSING = { SoapUtil.WSA_NAMESPACE_10, NS_WS_ADDRESSING_200408 };

    // elements
    private static final String WSA_ACTION = "Action";
    private static final String WSA_ADDRESS = "Address";
    private static final String WSA_FAULTTO = "FaultTo";
    private static final String WSA_FROM = "From";
    private static final String WSA_MESSAGEID = "MessageID";
    private static final String WSA_REPLYTO = "ReplyTo";
    private static final String WSA_TO = "To";

    private final Auditor auditor;

    /**
     * Get signed elements for request message of given context. 
     */
    private SignedElement[] getSignedElements(final Message msg)
            throws AddressingProcessingException, IOException {
        ProcessorResult wssResults;

        try {
            if (!msg.isSoap()) {
                auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP);
                throw new AddressingProcessingException("Request is not SOAP", AssertionStatus.NOT_APPLICABLE);
            }

            wssResults = msg.getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        if ( wssResults == null ) {
            auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NO_WSS);
            throw new AddressingProcessingException("Request does not contain any WSS security", AssertionStatus.FALSIFIED);
        }

        return wssResults.getElementsThatWereSigned();
    }

    /**
     * Get the ElementCursor for the request message
     */
    private ElementCursor getElementCursor(final Message msg) throws IOException {
        try {
            return msg.getXmlKnob().getElementCursor();
        } catch (SAXException se) {
             throw new CausedIOException(se);    
        }
    }

    /**
     * Move the given cursor to the soap:Header 
     */
    private void moveToSoapHeader(final ElementCursor cursor) throws AddressingProcessingException {
        cursor.moveToDocumentElement();

        String elementNamespace = cursor.getNamespaceUri();
        String localName = cursor.getLocalName();

        if ( SoapUtil.ENVELOPE_URIS.contains(elementNamespace) &&
             SoapUtil.ENVELOPE_EL_NAME.equals(localName) ) {

            cursor.moveToFirstChildElement();
            String childNamespace = cursor.getNamespaceUri();
            String childLocalName = cursor.getLocalName();

            if ( !elementNamespace.equals(childNamespace) ||
                 !SoapUtil.HEADER_EL_NAME.equals(childLocalName) ) {
                auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP);
                throw new AddressingProcessingException("Message is not SOAP (header not found)", AssertionStatus.FALSIFIED);
            }
        } else {
            auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP);
            throw new AddressingProcessingException("Message is not SOAP (envelope not found)", AssertionStatus.FALSIFIED);
        }
    }

    /**
     * Check if the given node is the soap:Header
     */
    private boolean isSoapHeader(final Node node) {
        boolean isHeader = false;

        if ( node != null && node.getNodeType()==Node.ELEMENT_NODE ) {
            Element element = (Element) node;

            // check element has a soap env namespace and name
            String elementNamespace = element.getNamespaceURI();
            if ( SoapUtil.ENVELOPE_URIS.contains(elementNamespace) &&
                 SoapUtil.HEADER_EL_NAME.equals(element.getLocalName()) ) {
                Node parent = element.getParentNode();

                if ( parent != null && parent.getNodeType()==Node.ELEMENT_NODE  ) {
                    Element parentElement = (Element) parent;

                    // check parent has same soap env namespace and correct name / location
                    if ( elementNamespace.equals(parentElement.getNamespaceURI()) &&
                         SoapUtil.ENVELOPE_EL_NAME.equals(parentElement.getLocalName()) &&
                         parentElement.getParentNode() != null &&
                         parentElement.getParentNode().getNodeType() == Node.DOCUMENT_NODE ) {
                        isHeader = true;    
                    }
                }
            }
        }

        return isHeader;
    }

    /**
     * Check if the given addressing properties contain any addressing headers in
     * the given namespace
     */
    private boolean addressingPresent(final Map<QName, String> addressingProperties,
                                      final String namespace) {
        boolean present = false;

        for (QName element : addressingProperties.keySet() ) {
            if ( namespace.equals(element.getNamespaceURI()) ) {
                present = true;
                break;
            }
        }

        return present;
    }

    /**
     * Set context variables from the given addressing properties 
     */
    private void setVariables(final Map<QName, String> addressingProperties,
                              final String namespace,
                              final PolicyEnforcementContext context) {
        setVariables(addressingProperties, namespace, new Functions.BinaryVoid<String,String>(){
            public void call(String name, String value) {
                context.setVariable(name, value);
            }
        });
    }

    /**
     * Get variable name for element name; 
     */
    private String getName(final String elementName) {
        String prefix = assertion.getVariablePrefix();
        String name = null;

        if ( WSA_ACTION.equals(elementName) ) {
            name = prefix + "." + WsAddressingAssertion.VAR_SUFFIX_ACTION;
        } else if ( WSA_TO.equals(elementName) ) {
            name = prefix + "." + WsAddressingAssertion.VAR_SUFFIX_TO;
        } else if ( WSA_MESSAGEID.equals(elementName) ) {
            name = prefix + "." + WsAddressingAssertion.VAR_SUFFIX_MESSAGEID;
        } else if ( WSA_FROM.equals(elementName) ) {
            name = prefix + "." + WsAddressingAssertion.VAR_SUFFIX_FROM;
        } else if ( WSA_FAULTTO.equals(elementName) ) {
            name = prefix + "." + WsAddressingAssertion.VAR_SUFFIX_FAULTTO;
        } else if ( WSA_REPLYTO.equals(elementName) ) {
            name = prefix + "." + WsAddressingAssertion.VAR_SUFFIX_REPLYTO;
        }
        
        return name;
    }

    /**
     * Exception used to end addressing processing with the given status
     */
    private static final class AddressingProcessingException extends Exception {
        private AssertionStatus assertionStatus;

        public AddressingProcessingException(String message, AssertionStatus assertionStatus) {
            super(message);
            this.assertionStatus = assertionStatus;
        }

        public AssertionStatus getAssertionStatus() {
            return assertionStatus;
        }
    }
}
