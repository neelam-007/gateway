package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.*;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Server side implementation of the WsAddressingAssertion.
 *
 * @see com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion
 */
public class ServerWsAddressingAssertion extends AbstractServerAssertion<WsAddressingAssertion> {

    //- PUBLIC

    /**
     *
     */
    public ServerWsAddressingAssertion(final WsAddressingAssertion assertion,
                                       final ApplicationContext context) throws PolicyAssertionException {
        super( assertion );
        this.config = context.getBean("serverConfig", Config.class);
        this.securityTokenResolver = context.getBean("securityTokenResolver",SecurityTokenResolver.class);
        this.otherNamespaceUri = assertion.getEnableOtherNamespace();
        this.acceptableNamespaces = buildAcceptableNamespaces( this.otherNamespaceUri );
    }

    /**
     * 
     */
    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;
        final Message msg;
        final String messageDescription;
        try {
            messageDescription = assertion.getTargetName();
            msg = context.getTargetMessage(assertion);
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }

        try {
            final Map<QName, String> addressingProperties = new HashMap<QName,String>();
            final List<Element> addressingElements = isElementsVariableUsed() ? new ArrayList<Element>() : null;
            if ( assertion.isRequireSignature() ) {
                populateAddressingFromSignedElements(getSignedElements(context, msg, messageDescription), addressingProperties, addressingElements);
            } else {
                populateAddressingFromMessage(getElementCursor(msg), addressingProperties, addressingElements);
            }

            auditAddressing( addressingProperties, assertion.isRequireSignature() );

            final List<String> permittedNamespaces = new ArrayList<String>();
            if ( assertion.isEnableWsAddressing10() ) permittedNamespaces.add( SoapConstants.WSA_NAMESPACE_10 );
            if ( assertion.isEnableWsAddressing200408() ) permittedNamespaces.add( NS_WS_ADDRESSING_200408 );
            if ( otherNamespaceUri != null && otherNamespaceUri.length() > 0 ) permittedNamespaces.add( otherNamespaceUri );

            boolean foundAddressing = false;
            for ( final String namespace : permittedNamespaces ) {
                if ( addressingPresent(addressingProperties, namespace) ) {
                    if ( assertion.getVariablePrefix() != null) {
                        setVariables(addressingProperties, addressingElements, namespace, context);
                    }

                    logAndAudit(AssertionMessages.WS_ADDRESSING_HEADERS_OK);
                    status = AssertionStatus.NONE;
                    foundAddressing = true;
                    break;
                }
            }

            if ( !foundAddressing ) {
                if (assertion.getTarget() == TargetMessageType.REQUEST) context.setRequestPolicyViolated();
                if ( assertion.isRequireSignature() ) {
                    logAndAudit(AssertionMessages.WS_ADDRESSING_NO_SIGNED_HEADERS);
                } else {
                    logAndAudit(AssertionMessages.WS_ADDRESSING_NO_HEADERS);
                }
            }
        } catch (AddressingProcessingException ape) {
            logger.log(Level.INFO, "WS-Addressing processing terminated due to ''{0}''", ape.getMessage());
            status = ape.getAssertionStatus();
        }

        return status;
    }

    //- PACKAGE

    /**
     *
     */
    ServerWsAddressingAssertion(final WsAddressingAssertion assertion,
                                final Config config ) throws PolicyAssertionException {
        super(assertion);
        this.config = config;
        this.securityTokenResolver = null;
        this.otherNamespaceUri = assertion.getEnableOtherNamespace();
        this.acceptableNamespaces = buildAcceptableNamespaces( this.otherNamespaceUri );
    }

    //- PROTECTED

    /**
     * Populate the given addressing properties using an element cursor
     */
    protected void populateAddressingFromMessage(final ElementCursor cursor,
                                                 final Map<QName, String> addressingProperties,
                                                 final Collection<Element> elements )
            throws AddressingProcessingException {
        try {
            if ( moveToSoapHeader(cursor) ) {
                final ElementCursor.Visitor visitor = new ElementCursor.Visitor(){
                    @Override
                    public void visit(final ElementCursor ec) {
                        if ( ArrayUtils.contains(acceptableNamespaces, ec.getNamespaceUri()) ) {
                            QName name = new QName(ec.getNamespaceUri(), ec.getLocalName());

                            if ( !addressingProperties.containsKey(name) || WSA_RELATESTO.equals(ec.getLocalName()) ) {
                                if ( WSA_FAULTTO.equals(ec.getLocalName()) || WSA_REPLYTO.equals(ec.getLocalName()) || WSA_FROM.equals(ec.getLocalName()) ) {
                                    // process the address child element
                                    ec.pushPosition();
                                    if (ec.moveToFirstChildElement(WSA_ADDRESS, ec.getNamespaceUri()) ) {
                                        addressingProperties.put(name, ec.getTextValue());
                                    } else if (WSA_FROM.equals(ec.getLocalName())) {
                                        addressingProperties.put(name, ec.getTextValue());
                                    }
                                    ec.popPosition();
                                } else {
                                    addressingProperties.put(name, ec.getTextValue());
                                }

                                if ( elements != null ) {
                                    elements.add( ec.asDomElement() );
                                }
                            }
                        }
                    }
                };
                cursor.visitChildElements(visitor);
            }
        } catch (InvalidDocumentFormatException idfe) {
            // Not expected since our visitor does not throw this
            throw new AddressingProcessingException( "Unexpected error populating addressing: " + ExceptionUtils.getMessage(idfe), AssertionStatus.FAILED);
        }
    }

    /**
     * Populate the given addressing properties using signed elements
     */
    protected void populateAddressingFromSignedElements(final SignedElement[] signedElements,
                                                        final Map<QName, String> addressingProperties,
                                                        final Collection<Element> elements ) {
        for (SignedElement signedElement : signedElements ) {
            Element element = signedElement.asElement();

            if ( ArrayUtils.contains(acceptableNamespaces, element.getNamespaceURI()) && isSoapHeader(element.getParentNode()) ) {
                QName name = new QName(element.getNamespaceURI(), element.getLocalName());

                if ( addressingProperties.containsKey(name) && !WSA_RELATESTO.equals(element.getLocalName()) ) {
                    continue;
                }

                if ( WSA_FAULTTO.equals(element.getLocalName()) || WSA_REPLYTO.equals(element.getLocalName()) ) {
                    // process the address child element
                    Element address = DomUtils.findFirstChildElementByName(element, element.getNamespaceURI(), WSA_ADDRESS);
                    if ( address != null ) {
                        addressingProperties.put(name, DomUtils.getTextValue(address));
                    }
                } else {
                    addressingProperties.put(name, DomUtils.getTextValue(element));
                }

                if ( elements != null ) {
                    elements.add( element );
                }
            }
        }
    }

    /**
     * Set context variables from the given addressing properties
     */
    protected void setVariables(final Map<QName, String> addressingProperties,
                                final Collection<Element> elements,
                                final String namespace,
                                final Functions.BinaryVoid<String,Object> setter) {

        for (Map.Entry<QName,String> entry : addressingProperties.entrySet() ) {
            if ( namespace.equals(entry.getKey().getNamespaceURI()) ) {
                String name = getName(entry.getKey().getLocalPart());
                if ( name != null)
                    setter.call(name, entry.getValue());
            }
        }

        Element[] elementArray;
        if ( elements != null ) {
            elementArray = elements.toArray( new Element[elements.size()] );
        } else {
            elementArray = new Element[0];
        }

        setter.call(assertion.getVariablePrefix() + "." + WsAddressingAssertion.VAR_SUFFIX_NAMESPACE, namespace);
        setter.call(assertion.getVariablePrefix() + "." + WsAddressingAssertion.VAR_SUFFIX_ELEMENTS, elementArray);
    }

    //- PRIVATE

    private static final boolean requireCredentialSigningToken = SyspropUtil.getBoolean( "com.l7tech.server.policy.requireSigningTokenCredential", true );

    private static final String NS_WS_ADDRESSING_200408 = SoapConstants.WSA_NAMESPACE2;
    private static final String[] NS_ADDRESSING = { SoapConstants.WSA_NAMESPACE_10, NS_WS_ADDRESSING_200408 };

    // elements
    private static final String WSA_ACTION = "Action";
    private static final String WSA_ADDRESS = "Address";
    private static final String WSA_FAULTTO = "FaultTo";
    private static final String WSA_FROM = "From";
    private static final String WSA_MESSAGEID = "MessageID";
    private static final String WSA_RELATESTO = "RelatesTo";
    private static final String WSA_REPLYTO = "ReplyTo";
    private static final String WSA_TO = "To";

    private final Config config;

    private final String otherNamespaceUri;
    private final String[] acceptableNamespaces;
    private final SecurityTokenResolver securityTokenResolver;
    private Boolean elementsVariableUsed;

    private String[] buildAcceptableNamespaces( final String otherNamespaceUri ) {
        final String[] acceptableNamespaces;

        if ( otherNamespaceUri != null && otherNamespaceUri.length() > 0) {
            acceptableNamespaces = new String[NS_ADDRESSING.length+1];
            System.arraycopy(NS_ADDRESSING, 0, acceptableNamespaces, 0, NS_ADDRESSING.length);
            acceptableNamespaces[NS_ADDRESSING.length] = otherNamespaceUri;
        } else {
            acceptableNamespaces = NS_ADDRESSING;
        }

        return acceptableNamespaces;
    }

    /**
     * Get signed elements for message of given context.
     */
    private SignedElement[] getSignedElements(final PolicyEnforcementContext context,
                                              final Message msg,
                                              final String what)
            throws AddressingProcessingException, IOException {
        ProcessorResult wssResults;

        try {
            if (!msg.isSoap()) {
                logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP);
                throw new AddressingProcessingException("Message is not SOAP", AssertionStatus.NOT_APPLICABLE);
            }

            if (assertion.getTarget() == TargetMessageType.REQUEST && !config.getBooleanProperty(ServerConfig.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) )  {
                wssResults = msg.getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(msg, what, securityTokenResolver, getAudit());
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        } catch (AddressingProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new CausedIOException(e);
        }

        if ( wssResults == null ) {
            logAndAudit(MessageProcessingMessages.MESSAGE_NO_WSS);
            throw new AddressingProcessingException("Message does not contain any WSS security", AssertionStatus.FALSIFIED);
        }

        final Message relatedRequestMessage = msg.getRelated( MessageRole.REQUEST );
        return WSSecurityProcessorUtils.filterSignedElementsByIdentity(
                context.getAuthenticationContext(msg),
                wssResults,
                assertion.getIdentityTarget(),
                requireCredentialSigningToken,
                relatedRequestMessage,
                relatedRequestMessage==null ? null : context.getAuthenticationContext( relatedRequestMessage ),
                getAudit() );
    }

    /**
     * Get the ElementCursor for the message
     */
    private ElementCursor getElementCursor(final Message msg) throws IOException, AddressingProcessingException {
        try {
            if ( !msg.isSoap() ) {
                logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP);
                throw new AddressingProcessingException("Message is not SOAP", AssertionStatus.NOT_APPLICABLE);
            }

            if ( isElementsVariableUsed() ) { // then we need a DOM cursor
                return new DomElementCursor( msg.getXmlKnob().getDocumentReadOnly() );
            } else {
                return msg.getXmlKnob().getElementCursor();
            }
        } catch (SAXException se) {
             throw new CausedIOException(se);    
        }
    }

    /**
     * Is the elements variable used later in the policy?
     */
    private boolean isElementsVariableUsed() {
        boolean elementsVariableUsed = false;

        if ( this.elementsVariableUsed != null ) {
            elementsVariableUsed = this.elementsVariableUsed;
        } else {
            if ( assertion.getVariablePrefix() != null ) {
                final String elementsVariableName = assertion.getVariablePrefix() + "." + WsAddressingAssertion.VAR_SUFFIX_ELEMENTS;
                Set<String> vars = PolicyVariableUtils.getVariablesUsedBySuccessors(assertion);
                elementsVariableUsed = vars.contains( elementsVariableName );
            }
            this.elementsVariableUsed = elementsVariableUsed;                
        }

        return elementsVariableUsed;
    }

    /**
     * Move the given cursor to the soap:Header 
     */
    private boolean moveToSoapHeader(final ElementCursor cursor) throws AddressingProcessingException {
        boolean foundHeader = false;
        cursor.moveToDocumentElement();

        String elementNamespace = cursor.getNamespaceUri();
        String localName = cursor.getLocalName();

        if ( SoapConstants.ENVELOPE_URIS.contains(elementNamespace) &&
             SoapConstants.ENVELOPE_EL_NAME.equals(localName) ) {

            cursor.moveToFirstChildElement();
            String childNamespace = cursor.getNamespaceUri();
            String childLocalName = cursor.getLocalName();

            if ( elementNamespace.equals(childNamespace) &&
                 SoapUtil.HEADER_EL_NAME.equals(childLocalName) ) {
                foundHeader = true;
            } 
        } else {
            logAndAudit(MessageProcessingMessages.MESSAGE_NOT_SOAP);
            throw new AddressingProcessingException("Message is not SOAP (envelope not found)", AssertionStatus.FALSIFIED);
        }

        return foundHeader;
    }

    /**
     * Check if the given node is the soap:Header
     */
    private boolean isSoapHeader(final Node node) {
        boolean isHeader = false;

        if ( node != null && (int) node.getNodeType() == (int) Node.ELEMENT_NODE ) {
            Element element = (Element) node;

            // check element has a soap env namespace and name
            String elementNamespace = element.getNamespaceURI();
            if ( SoapConstants.ENVELOPE_URIS.contains(elementNamespace) &&
                 SoapConstants.HEADER_EL_NAME.equals(element.getLocalName()) ) {
                Node parent = element.getParentNode();

                if ( parent != null && (int) parent.getNodeType() == (int) Node.ELEMENT_NODE ) {
                    Element parentElement = (Element) parent;

                    // check parent has same soap env namespace and correct name / location
                    if ( elementNamespace.equals(parentElement.getNamespaceURI()) &&
                         SoapConstants.ENVELOPE_EL_NAME.equals(parentElement.getLocalName()) &&
                         parentElement.getParentNode() != null &&
                            (int) parentElement.getParentNode().getNodeType() == (int) Node.DOCUMENT_NODE ) {
                        isHeader = true;    
                    }
                }
            }
        }

        return isHeader;
    }

    private void auditAddressing( final Map<QName, String> addressingProperties,
                                  final boolean signed ) {
        if ( addressingProperties.isEmpty() ) {
            logAndAudit( signed ?
                    AssertionMessages.WS_ADDRESSING_HEADERS_SIGNED_NONE :
                    AssertionMessages.WS_ADDRESSING_HEADERS_NONE );
        } else {
            final Set<String> namespaces = Functions.reduce( addressingProperties.keySet(), new TreeSet<String>(), new Functions.Binary<TreeSet<String>, TreeSet<String>, QName>() {
                @Override
                public TreeSet<String> call( final TreeSet<String> namespaces, final QName qName ) {
                    if ( qName.getNamespaceURI() != null ) namespaces.add( qName.getNamespaceURI() );
                    return namespaces;
                }
            } );
            CollectionUtils.foreach( namespaces, false, new Functions.UnaryVoid<String>(){
                @Override
                public void call( final String namespace ) {
                    logAndAudit( signed ?
                            AssertionMessages.WS_ADDRESSING_FOUND_SIGNED_HEADERS :
                            AssertionMessages.WS_ADDRESSING_FOUND_HEADERS,
                            namespace );
                }
            } );
        }
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
                              final Collection<Element> elements,
                              final String namespace,
                              final PolicyEnforcementContext context ) {
        setVariables(addressingProperties, elements, namespace, new Functions.BinaryVoid<String,Object>(){
            @Override
            public void call(String name, Object value) {
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

        private AddressingProcessingException(String message, AssertionStatus assertionStatus) {
            super(message);
            this.assertionStatus = assertionStatus;
        }

        public AssertionStatus getAssertionStatus() {
            return assertionStatus;
        }
    }
}
