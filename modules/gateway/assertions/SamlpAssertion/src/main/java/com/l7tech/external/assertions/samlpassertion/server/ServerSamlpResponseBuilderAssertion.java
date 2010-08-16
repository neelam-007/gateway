package com.l7tech.external.assertions.samlpassertion.server;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import saml.v2.assertion.AssertionType;
import saml.v2.assertion.NameIDType;
import saml.v2.protocol.ExtensionsType;
import saml.v2.protocol.StatusDetailType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Build a SAML Protocol Response for SAML Version 1.x or 2.0
 * 
 * @author darmstrong
 */
public class ServerSamlpResponseBuilderAssertion extends AbstractServerAssertion<SamlpResponseBuilderAssertion> {

    public ServerSamlpResponseBuilderAssertion(final SamlpResponseBuilderAssertion assertion,
                                               final ApplicationContext applicationContext ) throws PolicyAssertionException {
        super(assertion);

        this.auditor = applicationContext != null ? new Auditor(this, applicationContext, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();

        //validate the assertion bean
        final boolean isSuccessResponse = assertion.getSamlStatus() == SamlStatus.SAML2_SUCCESS ||
                assertion.getSamlStatus() == SamlStatus.SAML_SUCCESS;
        final String respAssertions = assertion.getResponseAssertions();
        final boolean assertionsSupplied = respAssertions == null || respAssertions.trim().isEmpty();
        if(isSuccessResponse && assertionsSupplied){
            throw new PolicyAssertionException(assertion,
                    "Assertion(s) field is not configured. One ore more are required when Response represents Success.");
        }

        if(!isSuccessResponse && !assertionsSupplied){
            //no assertions can be included
                throw new PolicyAssertionException(assertion,
                        "If Response status does not represent Success then the Assertion(s) field cannot include any SAML Assertions.");
        }

        switch (assertion.getSamlVersion()) {
            case SAML2:
                v2SamlpFactory = new saml.v2.protocol.ObjectFactory();
                v1SamlpFactory = null;
                break;
            case SAML1_1:
                v1SamlpFactory = new saml.v1.protocol.ObjectFactory();
                v2SamlpFactory = null;
                break;
            default:
                throw new PolicyAssertionException(assertion, "Unknown SAML Version found");
        }
        v2SamlpAssnFactory = new saml.v2.assertion.ObjectFactory();

        try {
            signer = ServerAssertionUtils.getSignerInfo(applicationContext, assertion);
        } catch (KeyStoreException e) {
            throw new PolicyAssertionException(assertion,
                    "Cannot create SignerInfo: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final ResponseContext responseContext;
        try {
            responseContext = validateAssertionInContext(context);
        } catch (Exception e) {
            if (e instanceof VariableNameSyntaxException){
                //catch any exception: VariableNameSyntaxException and InvalidRuntimeValueException
                auditor.logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                        new String[]{"create", "Unknown variable: '" + ExceptionUtils.getMessage(e) + "'"},
                        ExceptionUtils.getDebugException(e));
            } else {
                //catch any exception: VariableNameSyntaxException and InvalidRuntimeValueException
                auditor.logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                        new String[]{"create", ExceptionUtils.getMessage(e)},
                        ExceptionUtils.getDebugException(e));
            }
            return AssertionStatus.SERVER_ERROR;
        }

        final Marshaller marshaller;

        final Document responseDoc = XmlUtil.createEmptyDocument();
        try {
            switch (assertion.getSamlVersion()) {
                case SAML2:
                    marshaller = JaxbUtil.getMarshallerV2(responseContext.requestId);
                    break;
                case SAML1_1:
                    marshaller = JaxbUtil.getMarshallerV1(responseContext.requestId);
                    break;
                default:
                    throw new RuntimeException("Unknown SAML Version found");//cannot happen due to constructor.
            }
            
            final JAXBElement<?> response = createResponse(responseContext);
            marshaller.marshal(response, responseDoc);

        } catch (JAXBException e) {
            auditor.logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                    new String[]{"create", ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (InvalidRuntimeValueException e) {
            auditor.logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                    new String[]{"create", ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } finally {
            JaxbUtil.releaseJaxbResources(responseContext.requestId);
        }

        if (assertion.isSignResponse()) {
            try {
                logger.log(Level.FINEST, "Signing Response");
                signResponse(responseDoc);
            } catch (Exception e) {
                auditor.logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                        new String[]{"sign", ExceptionUtils.getMessage(e)},
                        ExceptionUtils.getDebugException(e));
                return AssertionStatus.SERVER_ERROR;
            }
        }

        final Message message;
        try {
            message = context.getOrCreateTargetMessage(assertion, true);
        } catch (NoSuchVariableException e) {
            
            auditor.logAndAudit( AssertionMessages.VARIABLE_NOTSET,
                    "Error creating output message " + assertion.getTargetName(), ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
        message.initialize(responseDoc);
        if(logger.isLoggable(Level.FINEST)){
            logger.log(Level.FINEST, "SAMLP Response: " + XmlUtil.nodeToString(responseDoc));
        }

        return AssertionStatus.NONE;
    }

    // - PRIVATE

    /**
     * Sign the response. If the version is 2.0, then the Signature must come after the Issuer element if present,
     * and before any other element.
     * 
     * @param responseDoc Document (samlp:Response) to sign and add a ds:Signature element to.
     * @throws InvalidRuntimeValueException if the xsd:Id attribute (ID or ResponseID) is not found in responseDoc
     * @throws KeyStoreException if any problem with key
     * @throws SignatureException if any problem signing
     * @throws SignatureStructureException if any problem signing
     * @throws XSignatureException if any problem signing
     */
    private void signResponse(final Document responseDoc)
            throws InvalidRuntimeValueException, KeyStoreException, SignatureException,
            SignatureStructureException, XSignatureException {
        final Element responseElement = responseDoc.getDocumentElement();

        final String xsdIdAttribute;
        boolean lookForIssuer = false;
        switch (assertion.getSamlVersion()) {
            case SAML2:
                xsdIdAttribute = "ID";
                lookForIssuer = assertion.isAddIssuer();
                break;
            case SAML1_1:
                xsdIdAttribute = "ResponseID";
                break;
            default:
                throw new RuntimeException("Unknown SAML Version found");//cannot happen due to constructor.
        }

        String id = responseElement.getAttribute(xsdIdAttribute);
        if(id == null || id.trim().isEmpty()){
            throw new InvalidRuntimeValueException("Cannot find xsd:ID attribute with name '" +
                    xsdIdAttribute+"' on element to sign '" + responseElement.getNodeName()+"'");
        }

        final Element signature = DsigUtil.createEnvelopedSignature(
                responseElement, xsdIdAttribute, signer.getCertificate(), signer.getPrivate(), null, null, null);

        //Signature must be added AFTER the Issuer element, if it's present, for SAML 2.0
        final Node insertBeforeNode;
        final Node maybeIssuer = responseElement.getFirstChild();
        if (lookForIssuer &&
                maybeIssuer.getNamespaceURI().equals(SamlConstants.NS_SAML2) &&
                maybeIssuer.getLocalName().equals("Issuer")) {
            insertBeforeNode = maybeIssuer.getNextSibling();
            if (insertBeforeNode == null)
                throw new IllegalStateException("Element after issuer not found."); //Cannot happen as we always add the Status element.
        } else {
            insertBeforeNode = maybeIssuer;
        }

        responseElement.insertBefore(signature, insertBeforeNode);
    }

    /**
     * Given the assertion bean and the policy enforcement context, extract all values from the bean, process for
     * variables and create a ResponseContext which has all values ready to be used in creating a SAMLP Response.
     * @param context PolicyEnforcementContext
     * @return ResponseContext, never null. All required values for the SAML version are set. Non null values should be added to SAMLP Response.
     * @throws InvalidRuntimeValueException if any bean property contains an invalid value or resolves to an invalid value.
     */
    private ResponseContext validateAssertionInContext(final PolicyEnforcementContext context) throws InvalidRuntimeValueException{

        final Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);

        final ResponseContext responseContext = new ResponseContext(context.getRequestId().toString());

        final SamlStatus samlStatus = assertion.getSamlStatus();
        responseContext.statusCode = samlStatus.getValue();

        final String statusMessage = assertion.getStatusMessage();
        if (statusMessage != null && !statusMessage.trim().isEmpty()) {
            responseContext.statusMessage = getStringVariable(vars, statusMessage, true);
        }

        final String statusDetail = assertion.getStatusDetail();
        if (statusDetail != null && !statusDetail.trim().isEmpty()) {
            responseContext.statusDetail = getMessageOrElementVariables(vars, statusDetail);
        }

        final String responseId = assertion.getResponseId();
        if (responseId == null || responseId.trim().isEmpty()) {
            //generate a new one. The only restriction is that no ':' are allowed (It's an xsd:NCName)
            responseContext.responseId = "ResponseId_" + HexUtils.generateRandomHexId(16);
        } else {
            final String testResponseId = getStringVariable(vars, responseId, true);
            if(!DomUtils.isValidXmlNcName(testResponseId)){
                throw new InvalidRuntimeValueException("Invalid runtime value for ResponseId. It is not a valid xsd:NCName. (No colon and may not start with a digit.)");
            }

            responseContext.responseId = testResponseId;
        }

        final String issueInstant = assertion.getIssueInstant();
        try {
            if (issueInstant == null || issueInstant.trim().isEmpty()) {
                final GregorianCalendar gregCal = new GregorianCalendar();
                gregCal.setTimeZone(TimeZone.getTimeZone("UTC"));
                responseContext.issueInstant = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregCal);
            } else {
                responseContext.issueInstant = DatatypeFactory.newInstance().newXMLGregorianCalendar(getStringVariable(vars, issueInstant, true));
            }
        } catch (DatatypeConfigurationException e) {
            throw new InvalidRuntimeValueException("Cannot create IssueInstant: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final String inResponseTo = assertion.getInResponseTo();
        if(inResponseTo != null && !inResponseTo.trim().isEmpty()){
            final String testResponseTo = getStringVariable(vars, inResponseTo, false);
            if (testResponseTo.contains(":")) {
                throw new InvalidRuntimeValueException("Invalid runtime value for InResponseTo. Cannot contain colon characters");
            }

            if(!testResponseTo.trim().isEmpty()){
                responseContext.inResponseTo = testResponseTo;
            }
        }

        final String respAssertions = assertion.getResponseAssertions();
        if(respAssertions != null && !respAssertions.trim().isEmpty()){
            responseContext.samlTokens = getMessageOrElementVariables(vars, respAssertions);
        }

        switch (assertion.getSamlVersion()){
            case SAML2:
                final String destination = assertion.getDestination();
                if(destination != null && !destination.trim().isEmpty()){
                    final String destValidated = getStringVariable(vars, destination, false);
                    if(!destValidated.trim().isEmpty()){
                        responseContext.destination = destValidated;
                    }
                }

                final String consent = assertion.getConsent();
                if(consent != null && !consent.trim().isEmpty()){
                    final String consentValidated = getStringVariable(vars, consent, false);
                    if(!consentValidated.trim().isEmpty()){
                        responseContext.consent = consentValidated;
                    }
                }

                final String respExtensions = assertion.getResponseExtensions();
                if(respExtensions != null && !respExtensions.trim().isEmpty()){
                    responseContext.extensions = getMessageOrElementVariables(vars, respExtensions);
                }
               break;
            case SAML1_1:

                final String recipient = assertion.getRecipient();
                if(recipient != null && !recipient.trim().isEmpty()){
                    final String recipientValidated = getStringVariable(vars, recipient, false);
                    if (!recipientValidated.trim().isEmpty()) {
                        responseContext.recipient = recipientValidated;
                    }
                }
                break;
        }
        
        return responseContext;
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

    /**
     * Get the Message or Element variables from a string containing variable references only.
     * @param vars
     * @param variablesOnly
     * @return
     * @throws InvalidRuntimeValueException If the parameter 'variablesOnly' contains anything which is not a variable
     * reference or resolves to a value which is not a Message or an Element.
     */
    private Collection getMessageOrElementVariables(final Map<String, Object> vars, String variablesOnly)
            throws InvalidRuntimeValueException {
        if (!Syntax.validateStringOnlyReferencesVariables(variablesOnly)){
            throw new InvalidRuntimeValueException("Value for field '" +variablesOnly+"' may only reference variables");
        }

        final List<Object> objects = ExpandVariables.processNoFormat(variablesOnly, vars, auditor, true);

        final Collection returnCol = new ArrayList();
        for (Object o : objects) {
            if (o instanceof Message || o instanceof Element){
                returnCol.add(o);
            }else {
                if( o instanceof String){
                    final String token = o.toString();
                    //it may just be the empty String
                    if(token.trim().isEmpty()) continue;

                    //try convert to a Message, then we support it
                    try {
                        Message message = new Message(XmlUtil.parse(token));
                        returnCol.add(message);
                    } catch (SAXException e) {
                        throw new InvalidRuntimeValueException("String value resolved from '"+variablesOnly+"' cannot be resolved into a Message variable: " +
                                ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                } else {
                    throw new InvalidRuntimeValueException("Unexpected variable of type '" + o.getClass().getName()
                            + "' found in variable string: '" + variablesOnly + "'.");
                }
            }
        }

        return returnCol;
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
     * Holds values which have been validated and are ready to be added to a <samlp:Response> version 1 or 2.
     */
    private static class ResponseContext{

        private ResponseContext(String requestId) {
            this.requestId = requestId;
        }

        private final String requestId;//from SSG
        private String statusCode;
        private String statusMessage;
        private Collection statusDetail = new ArrayList();//Message, Element or String

        private String responseId;
        private XMLGregorianCalendar issueInstant;
        private String inResponseTo; //null when no value. Does not hold a value when resolved to null.
        private String destination;
        private String consent;
        private String recipient;

        private Collection samlTokens = new ArrayList();//Message or Element only
        private Collection extensions = new ArrayList();//Message or Element only

    }

    private JAXBElement<?> createResponse(final ResponseContext responseContext)
            throws JAXBException, InvalidRuntimeValueException {

        final String extraLockId = HexUtils.generateRandomHexId(16) + responseContext.requestId;
        switch (assertion.getSamlVersion()) {
            case SAML2:
                try {
                    final String caDn = signer.getCertificateChain()[0].getSubjectDN().getName();
                    final Map caMap = CertUtils.dnToAttributeMap(caDn);
                    final String caCn = (String)((List)caMap.get("CN")).get(0);
                    
                    final Unmarshaller um = JaxbUtil.getUnmarshallerV2(extraLockId);
                    return createV2Response(responseContext, um, caCn);
                } finally {
                    JaxbUtil.releaseJaxbResources(extraLockId);
                }
            case SAML1_1:
                try {
                    final Unmarshaller um = JaxbUtil.getUnmarshallerV1(extraLockId);
                    return createV1Response(responseContext, um);
                } finally {
                    JaxbUtil.releaseJaxbResources(extraLockId);
                }
            default:
                throw new RuntimeException("Unknown SAML Version");//can't happen.
        }
    }

    private JAXBElement<saml.v1.protocol.ResponseType> createV1Response(final ResponseContext responseContext,
                                                                        final Unmarshaller um)
            throws JAXBException, InvalidRuntimeValueException {

        final saml.v1.protocol.ResponseType response = v1SamlpFactory.createResponseType();
        final BigInteger version = new BigInteger("1");
        response.setMajorVersion(version);
        response.setMinorVersion(version);

        final saml.v1.protocol.StatusCodeType statusCodeType = v1SamlpFactory.createStatusCodeType();
        statusCodeType.setValue(new QName(SamlConstants.NS_SAMLP, responseContext.statusCode));

        final saml.v1.protocol.StatusType statusType = v1SamlpFactory.createStatusType();
        statusType.setStatusCode(statusCodeType);

        final String statusMessage = responseContext.statusMessage;
        if(statusMessage != null){
            statusType.setStatusMessage(statusMessage);
        }

        final Collection statusDetailColl = responseContext.statusDetail;
        if (!statusDetailColl.isEmpty()) {
            //add the status detail if required.
            final saml.v1.protocol.StatusDetailType type = v1SamlpFactory.createStatusDetailType();
            final JAXBElement<saml.v1.protocol.StatusDetailType> statusDetailType = v1SamlpFactory.createStatusDetail(type);
            statusType.setStatusDetail(statusDetailType.getValue());
        }

        for (Object detail : statusDetailColl) {
            if(detail instanceof Element || detail instanceof String){
                statusType.getStatusDetail().getAny().add(detail);
            } else if (detail instanceof Message){
                Message message = (Message) detail;
                statusType.getStatusDetail().getAny().add(getDocumentElement(message));
            } else{
                logger.log(Level.WARNING, "Unexpected value of type '" + detail.getClass().getName() +"' found for status detail");
            }
        }

        response.setStatus(statusType);

        response.setResponseID(responseContext.responseId);
        response.setIssueInstant(responseContext.issueInstant);
        final String inResponseTo = responseContext.inResponseTo;
        if(inResponseTo != null){
            response.setInResponseTo(inResponseTo);
        }
        final String recipient = responseContext.recipient;
        if(recipient != null){
            response.setRecipient(recipient);
        }

        final Collection tokens = responseContext.samlTokens;
        for (Object token : tokens) {
            final JAXBElement<saml.v1.assertion.AssertionType> typeJAXBElement;
            if(token instanceof Element){
                typeJAXBElement = um.unmarshal((Element) token, saml.v1.assertion.AssertionType.class);
            } else if(token instanceof Message){
                Message message = (Message) token;
                typeJAXBElement = um.unmarshal(getDocumentElement(message), saml.v1.assertion.AssertionType.class);
            } else {
                logger.log(Level.WARNING, "Unexpected value of type '" + token.getClass().getName() +"' found for response assertions");
                continue;
            }

            final saml.v1.assertion.AssertionType value = typeJAXBElement.getValue();
            if(value.getMajorVersion() == null){
                throw new InvalidRuntimeValueException("SAML Assertion version must be SAML 1.x");
            }
            response.getAssertion().add(value);
        }
        return v1SamlpFactory.createResponse(response);
    }

    private JAXBElement<saml.v2.protocol.ResponseType> createV2Response(final ResponseContext responseContext,
                                                                        final Unmarshaller um,
                                                                        final String issuer)
            throws JAXBException, InvalidRuntimeValueException {

        //The order in which methods are set on ResponseType matter for the XML it will produce!! Add elements in
        //the order the schema requires.

        final saml.v2.protocol.ResponseType response = v2SamlpFactory.createResponseType();
        response.setVersion("2.0");

        //Issuer
        if(assertion.isAddIssuer()){
            final NameIDType idType = v2SamlpAssnFactory.createNameIDType();
            final JAXBElement<NameIDType> nameIdElement = v2SamlpAssnFactory.createIssuer(idType);
            final NameIDType value = nameIdElement.getValue();
            value.setValue(issuer);
            response.setIssuer(value);
        }
        
        final saml.v2.protocol.StatusCodeType statusCodeType = v2SamlpFactory.createStatusCodeType();
        statusCodeType.setValue(responseContext.statusCode);

        final saml.v2.protocol.StatusType statusType = v2SamlpFactory.createStatusType();
        statusType.setStatusCode(statusCodeType);

        final String statusMessage = responseContext.statusMessage;
        if(statusMessage != null){
            statusType.setStatusMessage(statusMessage);
        }

        final Collection statusDetailColl = responseContext.statusDetail;
        if (!statusDetailColl.isEmpty()) {
            //add the status detail if required.
            final StatusDetailType type = v2SamlpFactory.createStatusDetailType();
            final JAXBElement<StatusDetailType> statusDetail = v2SamlpFactory.createStatusDetail(type);
            statusType.setStatusDetail(statusDetail.getValue());
        }

        for (Object detail : statusDetailColl) {
            if (detail instanceof Element) {
                statusType.getStatusDetail().getAny().add(detail);
            } else if (detail instanceof Message) {
                Message message = (Message) detail;
                statusType.getStatusDetail().getAny().add(getDocumentElement(message));
            } else{
                logger.log(Level.WARNING, "Unexpected value of type '" + detail.getClass().getName() +"' found for status detail");
            }
        }

        response.setStatus(statusType);

        response.setID(responseContext.responseId);
        response.setIssueInstant(responseContext.issueInstant);
        final String inResponseTo = responseContext.inResponseTo;
        if(inResponseTo != null){
            response.setInResponseTo(inResponseTo);
        }

        final String destination = responseContext.destination;
        if(destination != null) {
            response.setDestination(destination);
        }

        final String consent = responseContext.consent;
        if(consent != null) {
            response.setConsent(consent);
        }

        //Extensions must be added first to JAXB Object to satisfy schema requirements
        final Collection extensions = responseContext.extensions;
        if (!extensions.isEmpty()) {
            final ExtensionsType extensionType = v2SamlpFactory.createExtensionsType();
            response.setExtensions(extensionType);
        }

        for (Object extension : extensions) {
            if(extension instanceof Element){
                response.getExtensions().getAny().add(extension);
            } else if (extension instanceof Message){
                Message message = (Message) extension;
                response.getExtensions().getAny().add(getDocumentElement(message));
            } else {
                logger.log(Level.WARNING, "Unexpected value  type '" + extension.getClass().getName() +"' found for response extensions");
            }
        }
        
        final Collection tokens = responseContext.samlTokens;
        for (Object token : tokens) {
            final JAXBElement<saml.v2.assertion.AssertionType> typeJAXBElement;
            if(token instanceof Element){
                typeJAXBElement = um.unmarshal((Element) token, saml.v2.assertion.AssertionType.class);
            } else if(token instanceof Message){
                Message message = (Message) token;
                typeJAXBElement = um.unmarshal(getDocumentElement(message), saml.v2.assertion.AssertionType.class);
            } else {
                logger.log(Level.WARNING, "Unexpected value of type '" + token.getClass().getName() +"' found for response assertions");
                continue;
            }

            final AssertionType value = typeJAXBElement.getValue();
            if(value.getVersion() == null){
                throw new InvalidRuntimeValueException("SAML Assertion version must be SAML 2.0");
            }
            response.getAssertionOrEncryptedAssertion().add(value);
        }

        return v2SamlpFactory.createResponse(response);
    }

    private Element getDocumentElement(Message message) throws InvalidRuntimeValueException {
        final Document documentReadOnly;
        try {
            documentReadOnly = message.getXmlKnob().getDocumentReadOnly();
        } catch (SAXException e) {
            throw new InvalidRuntimeValueException(ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (IOException e) {
            throw new InvalidRuntimeValueException(ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return documentReadOnly.getDocumentElement();
    }

    private final Auditor auditor;
    private final String[] variablesUsed;
    private final saml.v2.protocol.ObjectFactory v2SamlpFactory;
    private final saml.v1.protocol.ObjectFactory v1SamlpFactory;
    private final saml.v2.assertion.ObjectFactory v2SamlpAssnFactory;
    private final SignerInfo signer;

    private static final Logger logger = Logger.getLogger(ServerSamlpResponseBuilderAssertion.class.getName());
}
