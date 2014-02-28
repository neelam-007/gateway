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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import saml.support.xenc.EncryptedDataType;
import saml.v1.assertion.AuthenticationStatementType;
import saml.v1.assertion.SubjectStatementAbstractType;
import saml.v2.assertion.*;
import saml.v2.protocol.ExtensionsType;
import saml.v2.protocol.ResponseType;
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
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;

/**
 * Build a SAML Protocol Response for SAML Version 1.x or 2.0
 * 
 * @author darmstrong
 */
public class ServerSamlpResponseBuilderAssertion extends AbstractServerAssertion<SamlpResponseBuilderAssertion> {

    public ServerSamlpResponseBuilderAssertion(final SamlpResponseBuilderAssertion assertion,
                                               final ApplicationContext applicationContext ) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();

        switch (assertion.getVersion()) {
            case 2:
                v2SamlpFactory = new saml.v2.protocol.ObjectFactory();
                v1SamlpFactory = null;
                break;
            case 1:
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

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final ResponseContext responseContext;
        try {
            responseContext = validateAssertionInContext(context, assertion.getVersion());
        } catch (Exception e) {
            if (e instanceof VariableNameSyntaxException){
                //catch any exception: VariableNameSyntaxException and InvalidRuntimeValueException
                logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                        new String[]{"create", "Unknown variable: '" + ExceptionUtils.getMessage(e) + "'"},
                        ExceptionUtils.getDebugException(e));
            } else {
                //catch any exception: VariableNameSyntaxException and InvalidRuntimeValueException
                logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                        new String[]{"create", ExceptionUtils.getMessage(e)},
                        ExceptionUtils.getDebugException(e));
            }
            return AssertionStatus.SERVER_ERROR;
        }

        final Marshaller marshaller;

        final Document responseDoc = XmlUtil.createEmptyDocument();
        try {
            switch (assertion.getVersion()) {
                case 2:
                    marshaller = (assertion.isAddIssuer()) ? JaxbUtil.getMarshallerV2(Arrays.asList(JaxbUtil.SAML_2)) : JaxbUtil.getMarshallerV2(true);
                    break;
                case 1:
                    marshaller = JaxbUtil.getMarshallerV1(true);
                    break;
                default:
                    throw new RuntimeException("Unknown SAML Version found");//cannot happen due to constructor.
            }
            
            createResponse(responseContext, marshaller, responseDoc);
        } catch (JAXBException e) {
            logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                    new String[]{"create", ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (InvalidRuntimeValueException e) {
            logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                    new String[]{"create", ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        if (assertion.isSignResponse()) {
            try {
                logger.log(Level.FINEST, "Signing Response");
                signResponse(responseDoc);
            } catch (Exception e) {
                logAndAudit(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC,
                        new String[]{"sign", ExceptionUtils.getMessage(e)},
                        ExceptionUtils.getDebugException(e));
                return AssertionStatus.SERVER_ERROR;
            }
        }

        final Message message;
        try {
            message = context.getOrCreateTargetMessage(assertion, true);
        } catch (NoSuchVariableException e) {
            
            logAndAudit( AssertionMessages.VARIABLE_NOTSET,
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
            SignatureStructureException, XSignatureException, UnrecoverableKeyException {
        final Element responseElement = responseDoc.getDocumentElement();

        final String xsdIdAttribute;
        boolean lookForIssuer = false;
        switch (assertion.getVersion()) {
            case 2:
                xsdIdAttribute = "ID";
                lookForIssuer = assertion.isAddIssuer();
                break;
            case 1:
                xsdIdAttribute = "ResponseID";
                break;
            default:
                throw new RuntimeException("Unknown SAML Version found");//cannot happen due to constructor.
        }

        String idValue = responseElement.getAttribute(xsdIdAttribute);
        if(idValue == null || idValue.trim().isEmpty()){
            throw new InvalidRuntimeValueException("Cannot find xsd:ID attribute with name '" +
                    xsdIdAttribute+"' on element to sign '" + responseElement.getNodeName()+"'");
        }

        final Map<String, Element> elementsToSignWithIDs = new HashMap<>();
        elementsToSignWithIDs.put( idValue, responseElement );
        X509Certificate[] chain =
                assertion.isIncludeSignerCertChain()
                    ? signer.getCertificateChain()
                    : new X509Certificate[] { signer.getCertificate() };
        final Element signature = DsigUtil.createSignature( elementsToSignWithIDs,
                responseElement.getOwnerDocument(), chain, signer.getPrivate(), null, null, null, null, true, false );

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
    private ResponseContext validateAssertionInContext(final PolicyEnforcementContext context, final int version) throws InvalidRuntimeValueException{

        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        final ResponseContext responseContext = new ResponseContext(context.getRequestId().toString());

        final String samlStatusCode = ExpandVariables.process(assertion.getSamlStatusCode().trim(), vars, getAudit());

        if (version == 2) {
            if (!saml2xStatusSet.contains(samlStatusCode)) {
                throw new InvalidRuntimeValueException("Unknown SAML 2.0 status code value: '" + samlStatusCode + "'");
            }
        } else if (!saml1xStatusSet.contains(samlStatusCode)) {
            throw new InvalidRuntimeValueException("Unknown SAML 1.1 status code value: '" + samlStatusCode + "'");
        }

        responseContext.statusCode = samlStatusCode;

        final String customIssuer = assertion.getCustomIssuerValue();
        if (customIssuer != null && !customIssuer.trim().isEmpty()) {
            responseContext.customIssuer = getStringVariable(vars, customIssuer, true);
        }

        final String customIssuerFormat = assertion.getCustomIssuerFormat();
        if (customIssuerFormat != null && !customIssuerFormat.trim().isEmpty()) {
            if (!ValidationUtils.isValidUri(customIssuerFormat)) {
                throw new InvalidRuntimeValueException("Issuer Format attribute must be a URI");
            }
            //variable support not needed here yet
            responseContext.customIssuerFormat = customIssuerFormat;
        }

        final String customIssuerNameQualifier = assertion.getCustomIssuerNameQualifier();
        if (customIssuerNameQualifier != null && !customIssuerNameQualifier.trim().isEmpty()) {
            responseContext.customIssuerNameQualifier = getStringVariable(vars, customIssuerNameQualifier, true);
        }

        final String statusMessage = assertion.getStatusMessage();
        if (statusMessage != null && !statusMessage.trim().isEmpty()) {
            responseContext.statusMessage = getStringVariable(vars, statusMessage, true);
        }

        final String statusDetail = assertion.getStatusDetail();
        if (statusDetail != null && !statusDetail.trim().isEmpty()) {
            responseContext.statusDetail = getMessageOrElementVariables(vars, statusDetail, "StatusDetail");
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
            //noinspection ThrowableResultOfMethodCallIgnored
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
            responseContext.samlTokens = getMessageOrElementVariables(vars, respAssertions, "Assertion");
        }

        switch (assertion.getVersion()){
            case 2:
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
                    responseContext.extensions = getMessageOrElementVariables(vars, respExtensions, "Extensions");
                }

                final String encryptedAssertions = assertion.getEncryptedAssertions();
                if(encryptedAssertions != null && !encryptedAssertions.trim().isEmpty()){
                    responseContext.encryptedAssertions = getMessageOrElementVariables(vars, encryptedAssertions, "EncryptedAssertion");
                }
                
                break;
            case 1:

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
     * @param throwIfEmpty boolean if true throw if maybeAVariable resolves to the empty string.
     * @return resolved String value
     * @throws InvalidRuntimeValueException if invalid variable reference or if throwIfEmpty and an empty value found.
     */
    private String getStringVariable(@NotNull final Map<String, Object> vars, @NotNull String maybeAVariable, boolean throwIfEmpty)
            throws InvalidRuntimeValueException{
        //explicitly checking as exception throw below should only happen for the case when a string resolves to nothing.
        if(maybeAVariable.trim().isEmpty()) throw new IllegalArgumentException("maybeAVariable must not be empty");

        final String value;
        try {
            value =  ExpandVariables.process(maybeAVariable, vars, getAudit());
        } catch (Exception e) {
            //we want to catch any exception which the above call can generate. Any exception means the assertion fails.
            //noinspection ThrowableResultOfMethodCallIgnored
            throw new InvalidRuntimeValueException("Error getting value: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final boolean isEmpty = value.trim().isEmpty();
        if(isEmpty && throwIfEmpty) {
            throw new InvalidRuntimeValueException("Field value '" + maybeAVariable + "' resolved to nothing.");
        } else if (isEmpty) {
            logger.log(Level.INFO, "Field value '" + maybeAVariable + "' resolved to nothing.");
        }
        return value;
    }

    /**
     * Get the Message or Element variables from a string containing variable references only.
     *
     * @param vars Map of variables in the context
     * @param variablesOnly String which can only reference variables.
     * @param elemAttrName String name of the Element which is being processed. Used for logging / auditing.
     * @return Collection containing only Element or Message objects.
     * @throws InvalidRuntimeValueException If the parameter 'variablesOnly' contains anything which is not a variable
     *                                      reference or resolves to a value which is not a Message or an Element.
     */
    private Collection getMessageOrElementVariables(final Map<String, Object> vars,
                                                    final String variablesOnly,
                                                    final String elemAttrName)
            throws InvalidRuntimeValueException {
        if (!Syntax.validateStringOnlyReferencesVariables(variablesOnly)) {
            throw new InvalidRuntimeValueException("Value for field '" + variablesOnly + "' may only contain valid context variables references");
        }

        final List<Object> objects = ExpandVariables.processNoFormat(variablesOnly, vars, getAudit(), true);

        final Collection returnCol = new ArrayList();
        for (Object o : objects) {
            if (o instanceof Message || o instanceof Element) {
                returnCol.add(o);
            } else {
                if (o instanceof String) {
                    final String token = o.toString();
                    //it may just be the empty String
                    if (token.trim().isEmpty()) continue;

                    //try convert to a Message, then we support it
                    try {
                        Message message = new Message(XmlUtil.parse(token));
                        returnCol.add(message);
                    } catch (SAXException e) {
                        final String msg = "String value resolved from '" + variablesOnly + "' for element '" +
                                elemAttrName + "' cannot be resolved into a Message variable: " +
                                ExceptionUtils.getMessage(e);
                        throw new InvalidRuntimeValueException(msg, ExceptionUtils.getDebugException(e));
                    }
                } else if (o == null) {
                    //this happens when a built in prefix like gateway.invalid is referenced. Message processing treats these differently.
                    // or a variable from the PEC had a null value.
                    final String msg = "Variable referenced from variable string: '" + variablesOnly + "' for element '" +
                            elemAttrName + "' resolved to no value";
                    throw new InvalidRuntimeValueException(msg);
                } else {
                    final String msg = "Unexpected variable of type '" + o.getClass().getName() +
                            "' found in variable string: '" + variablesOnly + "' for element '" +
                            elemAttrName + "'";
                    throw new InvalidRuntimeValueException(msg);
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
        private String customIssuer; // V2 only
        private String customIssuerFormat;
        private String customIssuerNameQualifier;
        @NotNull
        private String statusCode; // already processed for variables
        private String statusMessage;
        private Collection statusDetail = new ArrayList();//Message, Element or String

        private String responseId;
        private XMLGregorianCalendar issueInstant;
        private String inResponseTo; //null when no value. Does not hold a value when resolved to null.
        private String destination;
        private String consent;
        private String recipient;

        private Collection samlTokens = new ArrayList();//Message or Element only
        private Collection encryptedAssertions = new ArrayList();//Message or Element only
        private Collection extensions = new ArrayList();//Message or Element only

    }

    private void createResponse(final ResponseContext responseContext, final Marshaller marshaller, final Document responseDoc)
            throws JAXBException, InvalidRuntimeValueException {

        switch (assertion.getVersion()) {
            case 2:
                final String caDn = signer.getCertificateChain()[0].getSubjectDN().getName();
                final Map caMap = CertUtils.dnToAttributeMap(caDn);
                final String caCn = (String)((List)caMap.get("CN")).get(0);

                createV2Response(responseContext, JaxbUtil.getUnmarshallerV2(), caCn, marshaller, responseDoc);
                return;
            case 1:
                createV1Response(responseContext, JaxbUtil.getUnmarshallerV1(), marshaller, responseDoc);
                return;
            default:
                throw new RuntimeException("Unknown SAML Version");//can't happen.
        }
    }

    private void createV1Response(final ResponseContext responseContext,
                                  final Unmarshaller um,
                                  final Marshaller marshaller,
                                  final Document responseDoc)
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
        if (statusMessage != null) {
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
            if (detail instanceof Element || detail instanceof String) {
                statusType.getStatusDetail().getAny().add(detail);
            } else if (detail instanceof Message) {
                Message message = (Message) detail;
                statusType.getStatusDetail().getAny().add(getDocumentElement(message));
            } else {
                logger.log(Level.WARNING, "Unexpected value of type '" + detail.getClass().getName() + "' found for status detail");
            }
        }

        response.setStatus(statusType);

        response.setResponseID(responseContext.responseId);
        response.setIssueInstant(responseContext.issueInstant);
        final String inResponseTo = responseContext.inResponseTo;
        if (inResponseTo != null) {
            response.setInResponseTo(inResponseTo);
        }
        final String recipient = responseContext.recipient;
        if (recipient != null) {
            response.setRecipient(recipient);
        }

        final boolean validateWebSsoRules = isValidateWebSsoRules();

        final Collection tokens = responseContext.samlTokens;
        final List<Element> resolvedAssertions = new ArrayList<Element>();
        for (Object token : tokens) {
            if (token instanceof Element) {
                resolvedAssertions.add((Element) token);
            } else if (token instanceof Message) {
                Message message = (Message) token;
                final Element documentElement = getDocumentElement(message);
                resolvedAssertions.add(documentElement);
            } else {
                logger.log(Level.WARNING, "Unexpected value of type '" + token.getClass().getName() + "' found for response assertions");
            }
        }

        if (validateWebSsoRules) {
            for (Element resolvedAssertion : resolvedAssertions) {
                final JAXBElement<saml.v1.assertion.AssertionType> typeJAXBElement = um.unmarshal(resolvedAssertion, saml.v1.assertion.AssertionType.class);
                final saml.v1.assertion.AssertionType value = typeJAXBElement.getValue();
                if (value.getMajorVersion() == null) {
                    throw new InvalidRuntimeValueException("SAML Assertion version must be SAML 1.x");
                }
                response.getAssertion().add(value);
            }
        }

        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1SamlpFactory.createResponse(response);
        if (validateWebSsoRules) {
            validateV1Response(typeJAXBElement.getValue());
            final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();
            // clean up - remove added assertions - we will add them manually
            responseType.getAssertion().clear();
        }

        marshaller.marshal(typeJAXBElement, responseDoc);
        final Element responseEl = responseDoc.getDocumentElement();

        for (Element resolvedAssertion : resolvedAssertions) {
            final Node importedAssertion = responseDoc.importNode(resolvedAssertion, true);
            responseEl.appendChild(importedAssertion);
        }
    }

    private void createV2Response(final ResponseContext responseContext,
                                  final Unmarshaller um,
                                  final String issuer,
                                  final Marshaller marshaller,
                                  final Document responseDoc)
            throws JAXBException, InvalidRuntimeValueException {

        //The order in which methods are set on ResponseType matter for the XML it will produce!! Add elements in
        //the order the schema requires.

        final saml.v2.protocol.ResponseType response = v2SamlpFactory.createResponseType();
        response.setVersion("2.0");

        //Issuer
        if (assertion.isAddIssuer()) {
            final NameIDType idType = v2SamlpAssnFactory.createNameIDType();
            final JAXBElement<NameIDType> nameIdElement = v2SamlpAssnFactory.createIssuer(idType);
            final NameIDType value = nameIdElement.getValue();
            value.setValue(responseContext.customIssuer == null ? issuer : responseContext.customIssuer);
            if (responseContext.customIssuerFormat != null) {
                value.setFormat(responseContext.customIssuerFormat);
            }
            if (responseContext.customIssuerNameQualifier != null) {
                value.setNameQualifier(responseContext.customIssuerNameQualifier);
            }
            response.setIssuer(value);
        }

        final saml.v2.protocol.StatusCodeType statusCodeType = v2SamlpFactory.createStatusCodeType();
        statusCodeType.setValue(responseContext.statusCode);

        final saml.v2.protocol.StatusType statusType = v2SamlpFactory.createStatusType();
        statusType.setStatusCode(statusCodeType);

        final String statusMessage = responseContext.statusMessage;
        if (statusMessage != null) {
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
            } else {
                logger.log(Level.WARNING, "Unexpected value of type '" + detail.getClass().getName() + "' found for status detail");
            }
        }

        response.setStatus(statusType);

        response.setID(responseContext.responseId);
        response.setIssueInstant(responseContext.issueInstant);
        final String inResponseTo = responseContext.inResponseTo;
        if (inResponseTo != null) {
            response.setInResponseTo(inResponseTo);
        }

        final String destination = responseContext.destination;
        if (destination != null) {
            response.setDestination(destination);
        }

        final String consent = responseContext.consent;
        if (consent != null) {
            response.setConsent(consent);
        }

        //Extensions must be added first to JAXB Object to satisfy schema requirements
        final Collection extensions = responseContext.extensions;
        if (!extensions.isEmpty()) {
            final ExtensionsType extensionType = v2SamlpFactory.createExtensionsType();
            response.setExtensions(extensionType);
        }

        for (Object extension : extensions) {
            if (extension instanceof Element) {
                response.getExtensions().getAny().add(extension);
            } else if (extension instanceof Message) {
                Message message = (Message) extension;
                response.getExtensions().getAny().add(getDocumentElement(message));
            } else {
                logger.log(Level.WARNING, "Unexpected value  type '" + extension.getClass().getName() + "' found for response extensions");
            }
        }

        final boolean validateWebSsoRules = isValidateWebSsoRules();

        final Collection tokens = responseContext.samlTokens;
        final List<Element> resolvedAssertions = new ArrayList<Element>();
        for (Object token : tokens) {
            if (token instanceof Element) {
                resolvedAssertions.add((Element) token);
            } else if (token instanceof Message) {
                Message message = (Message) token;
                final Element documentElement = getDocumentElement(message);
                resolvedAssertions.add(documentElement);
            } else {
                logger.log(Level.WARNING, "Unexpected value of type '" + token.getClass().getName() + "' found for response assertions");
            }
        }

        if (validateWebSsoRules) {
            for (Element resolvedAssertion : resolvedAssertions) {
                final JAXBElement<saml.v2.assertion.AssertionType> typeJAXBElement = um.unmarshal(resolvedAssertion, saml.v2.assertion.AssertionType.class);
                final AssertionType value = typeJAXBElement.getValue();
                if (value.getVersion() == null) {
                    throw new InvalidRuntimeValueException("SAML Assertion version must be SAML 2.0");
                }
                response.getAssertionOrEncryptedAssertion().add(value);
            }
        }

        final Collection encryptedAssertions = responseContext.encryptedAssertions;
        final List<Element> resolvedEncryptedAssertions = new ArrayList<Element>();
        for (Object token : encryptedAssertions) {
            if (token instanceof Element) {
                resolvedEncryptedAssertions.add((Element) token);
            } else if (token instanceof Message) {
                Message message = (Message) token;
                final Element documentElement = getDocumentElement(message);
                resolvedEncryptedAssertions.add(documentElement);
            } else {
                logger.log(Level.WARNING, "Unexpected value of type '" + token.getClass().getName() + "' found for response encrypted assertions");
            }
        }

        if (validateWebSsoRules) {
            for (Element encryptedAssertion : resolvedEncryptedAssertions) {
                final JAXBElement<EncryptedDataType> typeJAXBElement = um.unmarshal(encryptedAssertion, EncryptedDataType.class);
                final EncryptedDataType value = typeJAXBElement.getValue();
                final EncryptedElementType encryptedElementType = v2SamlpAssnFactory.createEncryptedElementType();
                encryptedElementType.setEncryptedData(value);
                response.getAssertionOrEncryptedAssertion().add(encryptedElementType);
            }
        }

        final JAXBElement<ResponseType> typeJAXBElement = v2SamlpFactory.createResponse(response);
        if (validateWebSsoRules) {
            validateV2Response(typeJAXBElement.getValue(), responseContext);
            //fix - bug 10855 - remove assertions now that validation has been done
            final ResponseType responseType = typeJAXBElement.getValue();
            // clean up - remove added assertions - we will add them manually
            responseType.getAssertionOrEncryptedAssertion().clear();
        }

        marshaller.marshal(typeJAXBElement, responseDoc);

        // add assertions
        final Element responseEl = responseDoc.getDocumentElement();

        for (Element resolvedAssertion : resolvedAssertions) {
            final Node importedAssertion = responseDoc.importNode(resolvedAssertion, true);
            responseEl.appendChild(importedAssertion);
        }

        // add encrypted assertions
        for (Element encryptedAssertion : resolvedEncryptedAssertions) {
            final Element samlEncryptedAssertion = responseDoc.createElementNS(SamlConstants.NS_SAML2, "EncryptedAssertion");
            final Node importedAssertion = responseDoc.importNode(encryptedAssertion, true);
            samlEncryptedAssertion.appendChild(importedAssertion);
            responseEl.appendChild(samlEncryptedAssertion);
        }
    }

    private boolean isValidateWebSsoRules() {
        return assertion.isValidateWebSsoRules() && validateSSOProfileDetails;
    }

    private void validateV1Response(final saml.v1.protocol.ResponseType responseType){

        final String recipient = responseType.getRecipient();
        if(recipient == null || recipient.trim().isEmpty() || !ValidationUtils.isValidUri(recipient)){
            logAndAudit( AssertionMessages.SAMLP_1_1_PROCREQ_PROFILE_VIOLATION,
                    "Response must include the Recipient attribute with a valid value");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        //Find an authentication statement
        final List<saml.v1.assertion.AssertionType> assertionTypes = responseType.getAssertion();

        boolean authenticationStatementFound = false;
        boolean ssoAssertionFound = false; //true when authenticationStatementFound is true, plus NotBefore and NotOnOrAfter are also found
        for (saml.v1.assertion.AssertionType assertionType : assertionTypes) {

            final List<saml.v1.assertion.StatementAbstractType> statements = assertionType.getStatementOrSubjectStatementOrAuthenticationStatement();
            for (saml.v1.assertion.StatementAbstractType statement : statements) {
                if(statement instanceof AuthenticationStatementType){
                    authenticationStatementFound = true;
                    final saml.v1.assertion.ConditionsType conditionsType = assertionType.getConditions();
                    if(conditionsType != null){
                        if(!ssoAssertionFound){
                            ssoAssertionFound = conditionsType.getNotBefore() != null &&
                                    conditionsType.getNotOnOrAfter() != null;
                        }
                    }
                }

                if(SubjectStatementAbstractType.class.isAssignableFrom(statement.getClass())){
                    SubjectStatementAbstractType subjectAbstractType = (SubjectStatementAbstractType) statement;
                    final saml.v1.assertion.SubjectType subjectType = subjectAbstractType.getSubject();
                    final List<JAXBElement<?>> content = subjectType.getContent();
                    boolean bearerMethodFound = false;
                    boolean subjectConfirmationFound = false;
                    for (JAXBElement<?> jaxbElement : content) {
                        final Object value = jaxbElement.getValue();
                        if(value instanceof saml.v1.assertion.SubjectConfirmationType){
                            subjectConfirmationFound = true;
                            saml.v1.assertion.SubjectConfirmationType subjectConfType = (saml.v1.assertion.SubjectConfirmationType) value;
                            final List<String> stringList = subjectConfType.getConfirmationMethod();
                            for (String method : stringList) {
                                if(SamlConstants.CONFIRMATION_BEARER.equals(method)){
                                    bearerMethodFound = true;
                                    break;
                                }
                            }
                        }
                    }

                    //SubjectConfirmation must be found
                    if(!subjectConfirmationFound){
                        logAndAudit( AssertionMessages.SAMLP_1_1_PROCREQ_PROFILE_VIOLATION,
                                "Each subject based statement must contain a saml:SubjectConfirmation element");
                        throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                    }

                    //this is a Subject based statement. We must have found a bearer method
                    if(!bearerMethodFound){
                        logAndAudit( AssertionMessages.SAMLP_1_1_PROCREQ_PROFILE_VIOLATION,
                                "Each subject based statement's saml:SubjectConfirmation must contain a ConfirmationMethod method of " + SamlConstants.CONFIRMATION_BEARER);
                        throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                    }
                }
            }
        }

        if(!authenticationStatementFound){
            logAndAudit( AssertionMessages.SAMLP_1_1_PROCREQ_PROFILE_VIOLATION,
                    "No SSO Assertion found. No authentication statement was found");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        if(!ssoAssertionFound){
            logAndAudit( AssertionMessages.SAMLP_1_1_PROCREQ_PROFILE_VIOLATION,
                    "No SSO Assertion found. No authentication statement found which contains a Conditions element with NotBefore and NotOnOrAfter");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }
    }

    private void validateV2Response(final ResponseType responseType, final ResponseContext responseContext){
        //validate the assertion bean
        final String respAssertions = assertion.getResponseAssertions();
        final String encryptedAssertions = assertion.getEncryptedAssertions();

        final boolean isSuccessResponse = SamlStatus.SAML2_SUCCESS.getValue().equals(responseContext.statusCode);
        final boolean assertionsNotSupplied = (respAssertions == null || respAssertions.trim().isEmpty()) &&
                (encryptedAssertions == null || encryptedAssertions.trim().isEmpty());

        if(isSuccessResponse && assertionsNotSupplied){
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                    "Assertion(s) and / or EncryptedAssertion(s) are not configured. One ore more assertions are required when Response represents Success");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        if(!isSuccessResponse && !assertionsNotSupplied) {
            //no assertions can be included
            logAndAudit(AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                    "If Response status does not represent Success then the Assertion(s) and EncryptedAssertion(s) fields cannot include any SAML Assertions");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        final List<Object> tokens = responseType.getAssertionOrEncryptedAssertion();
        if(tokens.isEmpty()){
            //all other rules apply only when assertions are present
            return;
        }

        final Set<String> subjectNames = new HashSet<String>();
        final Set<String> issuerNames = new HashSet<String>();

        boolean issuerIsRequired = assertion.isSignResponse();
        boolean bearerAssertionIsFound = false;//high level exception than bearerIsValidated
        boolean bearerIsValidated = false;
        boolean authnStatementFound = false;//also covers the 'one assertion must be included', as this must be found

        boolean includesEncrypted = false;//need to know if encrypted assertions are present. Cannot see inside them.
        for (Object token : tokens) {
            if(token instanceof AssertionType){
                AssertionType assertionType = (AssertionType) token;

                //Process Subject
                final SubjectType subjectType = assertionType.getSubject();
                final List<JAXBElement<?>> elementList = subjectType.getContent();
                for (JAXBElement<?> jaxbElement : elementList) {
                    final Object value = jaxbElement.getValue();
                    if(value instanceof NameIDType){
                        NameIDType name = (NameIDType) value;
                        subjectNames.add(name.getValue());

                    } else if (value instanceof SubjectConfirmationType){
                        //Ensure Bearer assertion with correct attributes0
                        SubjectConfirmationType confirmationType = (SubjectConfirmationType) value;
                        final String method = confirmationType.getMethod();
                        if (SamlConstants.CONFIRMATION_SAML2_BEARER.equals(method)){
                            bearerAssertionIsFound = true;
                            final SubjectConfirmationDataType scd = confirmationType.getSubjectConfirmationData();
                            if(scd != null && !bearerIsValidated){//if rule is satisfied, don't keep looking
                                final String recipient = scd.getRecipient();
                                final boolean recipientValid = recipient != null && !recipient.trim().isEmpty();
                                final boolean notOnOrAfterValid = scd.getNotOnOrAfter() != null;
                                final boolean noNotBefore = scd.getNotBefore() == null;
                                bearerIsValidated = recipientValid && notOnOrAfterValid && noNotBefore;
                            }

                            //each Bearer assertion must contain an AudienceRestriction
                            final ConditionsType type = assertionType.getConditions();
                            if(type == null){
                                logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION, "Bearer saml:Assertion does not contain any Conditions");
                                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                            }

                            boolean audienceFound = false;
                            final List<ConditionAbstractType> conditions = type.getConditionOrAudienceRestrictionOrOneTimeUse();
                            for (ConditionAbstractType condition : conditions) {
                                if(condition instanceof AudienceRestrictionType) {
                                    AudienceRestrictionType audience = (AudienceRestrictionType) condition;
                                    final List<String> audienceList = audience.getAudience();

                                    for (String audienceVal : audienceList) {
                                        final boolean isValid = audienceVal != null &&
                                                !audienceVal.trim().isEmpty() && ValidationUtils.isValidUri(audienceVal);
                                        if(isValid) audienceFound = true;
                                    }
                                }
                            }
                            if(!audienceFound){
                                logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                                        "Each bearer saml:Assertion must contain an AudienceRestriction with an Audience element");
                                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                            }
                        }
                    }
                }

                //Ensure Issuer and collect Issuer name
                final NameIDType issuer = assertionType.getIssuer();
                if(issuer == null){
                    logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION, "Assertion does not contain an Issuer");
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
                if ( issuer.getFormat() != null && !SamlConstants.NAMEIDENTIFIER_ENTITY.equals( issuer.getFormat() ) ) {
                    logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION, "Issuer format must be " + SamlConstants.NAMEIDENTIFIER_ENTITY );
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }

                issuerNames.add(issuer.getValue());

                //Look for Authentication statement
                final List<StatementAbstractType> statements = assertionType.getStatementOrAuthnStatementOrAuthzDecisionStatement();
                for (StatementAbstractType statement : statements) {
                    if(statement instanceof AuthnStatementType){
                        authnStatementFound = true;
                    }
                }

            } else if (token instanceof EncryptedElementType){
                issuerIsRequired = true;
                includesEncrypted = true;
            }

        }

        final NameIDType nameIDType = responseType.getIssuer();
        if (issuerIsRequired && nameIDType == null) {
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                    "Issuer is required if the samlp:Response is signed or if it contains an encrypted assertion");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        if(!includesEncrypted){
            //The following rules can only be validated for sure when no encrypted assertions are included.

            if(subjectNames.size() > 1){
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                        "All saml:Assertion saml:Subject's must represent the same principal");
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            if(issuerNames.size() > 1){
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                        "All saml:Assertion saml:Issuer's must represent the same Issuer");
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            //These 3 rules are in order of granularity - highest to lowest
            //A bearer assertion must be found
            if(!bearerAssertionIsFound){
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                        "No bearer saml:Assertion found");
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            if(!authnStatementFound){
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                        "No bearer saml:Assertion found which contains an authentication statement");
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            //A valid bearer subjectconfirmation assertion is required.
            if(!bearerIsValidated){
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION,
                        "No bearer saml:Assertion found which satisfies all profile rules: " +
                                "Requires SubjectConfirmationData with Recipient and NotOnOrAfter attributes, with no NotBefore attribute");
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }
        }
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


    private final String[] variablesUsed;
    private final saml.v2.protocol.ObjectFactory v2SamlpFactory;
    private final saml.v1.protocol.ObjectFactory v1SamlpFactory;
    private final saml.v2.assertion.ObjectFactory v2SamlpAssnFactory;
    private final SignerInfo signer;

    private final static Set<String> saml2xStatusSet = SamlStatus.getSaml2xStatusSet();
    private final static Set<String> saml1xStatusSet = SamlStatus.getSaml1xStatusSet();

    //Not static to support test cases
    private final boolean validateSSOProfileDetails = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile", true );

}
