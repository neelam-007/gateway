package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.external.assertions.samlpassertion.server.v1.AuthorizationDecisionQueryGenerator;
import com.l7tech.external.assertions.samlpassertion.server.v2.AuthnRequestGenerator;
import com.l7tech.external.assertions.samlpassertion.server.v2.AuthzDecisionQueryGenerator;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.security.xml.XmlElementEncryptionResolvedConfig;
import com.l7tech.security.xml.XmlElementEncryptor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.security.XmlElementEncryptorConfigUtils;
import com.l7tech.util.*;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import saml.v1.protocol.RequestType;
import saml.v2.protocol.AttributeQueryType;
import saml.v2.protocol.AuthnRequestType;
import saml.v2.protocol.AuthzDecisionQueryType;

import javax.crypto.SecretKey;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

/**
 * User: vchan
 */
public class ServerSamlpRequestBuilderAssertion extends AbstractServerAssertion<SamlpRequestBuilderAssertion> {
    private static final String SOAP_1_1_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_1_2_NS = "http://www.w3.org/2003/05/soap-envelope";


    private final String[] variablesUsed;
    private final SignerInfo signerInfo;

    private static final class BuilderContext {
        private int soapVersion;
        private Map<String, Object> ctxVariables;
        private NameIdentifierResolver nameResolver;
        @Nullable
        private NameIdentifierResolver issuerNameResolver; // SAML Version 2.0 only
        private InetAddressResolver addressResolver;
        private MessageValueResolver<String> authnMethodResolver;
        private MessageValueResolver<X509Certificate> clientCertResolver;
        private EvidenceBlockResolver evidenceBlockResolver;
    }

    /**
     * Constructor.
     *
     * @param assertion the SAMLP request builder assertion
     * @param spring the Spring ApplicationContext
     * @throws ServerPolicyException  when missing or invalid assertion properties are encountered
     */
    public ServerSamlpRequestBuilderAssertion(final SamlpRequestBuilderAssertion assertion, final ApplicationContext spring)
         throws ServerPolicyException
    {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();

        // get the Server Cert and signer info -- Copied from SamlIssuer
        try {
            this.signerInfo = ServerAssertionUtils.getSignerInfo(spring, assertion);
        } catch (KeyStoreException e) {
            throw new ServerPolicyException(assertion, "Unable to access configured private key: " + ExceptionUtils.getMessage(e), e);
        }

        if (assertion.isEncryptNameIdentifier()) {
            final XmlElementEncryptionConfig xmlEncryptConfig = assertion.getXmlEncryptConfig();
            final String varName = xmlEncryptConfig.getRecipientCertContextVariableName();
            if (varName == null || varName.trim().isEmpty()) {
                // if no context variable reference, then we require a cert.
                if (xmlEncryptConfig.getRecipientCertificateBase64() == null) {
                    throw new ServerPolicyException(assertion, "Assertion is configured to encrypt the Name Identifier but cannot do so as no recipient certificate is configured.");
                }
            }
        }
    }

    /**
     * @see com.l7tech.server.policy.assertion.AbstractServerAssertion#checkRequest(com.l7tech.server.message.PolicyEnforcementContext)
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            // initialize stuff
            final BuilderContext bContext = new BuilderContext();
            setSoapVersion(bContext, context.getRequest());
            bContext.ctxVariables = context.getVariableMap(variablesUsed, getAudit());

            /*
             * 1) Determine where the target message is supposed to go
             */
            final Message msg;
            try {
                msg = context.getOrCreateTargetMessage(assertion, false);
            } catch (NoSuchVariableException e) {
                logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
                throw new SamlpAssertionException(e);
            } catch (VariableNotSettableException e) {
                logAndAudit(AssertionMessages.VARIABLE_NOTSET, e.getVariable());
                throw new SamlpAssertionException(e);
            }

            /*
             * 2) Build SAMLP payload
             * 
             */
            setResolvers(context, bContext);
            final JAXBElement<?> samlpRequest = buildRequest(bContext);
            if (samlpRequest == null) {
                throw new SamlpAssertionException("Unable to create SAMLP request");
            }

            /*
             * 3) Set the SAMLP request msg into the target message
             */
            setRequestToTarget(bContext, msg, samlpRequest);

        } catch (SamlpAssertionException samlEx) {
            logAndAudit(AssertionMessages.SAMLP_REQUEST_BUILDER_FAILED_TO_BUILD,
                    new String[]{ExceptionUtils.getMessage(samlEx)},
                    ExceptionUtils.getDebugException(samlEx));
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }


    private void setSoapVersion( final BuilderContext bContext, final Message request ) throws SamlpAssertionException {

        final int DEFAULT_SOAP_VERSION = 1;
        try {
            Integer ver = assertion.getSoapVersion();
            if (assertion.getSoapVersion() == 0) {

                if (request.isSoap()) {
                    // parse SOAP request
                    Map nsMap = XmlUtil.getNamespaceMap(request.getXmlKnob().getDocumentReadOnly().getDocumentElement());
                    if (nsMap != null) {
                        if (nsMap.containsValue(SOAP_1_1_NS)) {
                            ver = 1;
                        } else if (nsMap.containsValue(SOAP_1_2_NS)) {
                            ver = 2;
                        } else {
                            logger.warning("Can not determine request SOAP version, using default SOAP version");
                            ver = DEFAULT_SOAP_VERSION;
                        }
                    }
                } else {
                    // for non-SOAP requests, use the default
                    logger.info("Request is non-SOAP, using default SOAP version");
                    bContext.soapVersion = DEFAULT_SOAP_VERSION;
                }
            }

            bContext.soapVersion = ver;

        } catch (IOException ioex) {
            throw new SamlpAssertionException("Error checking request message for SOAP version", ioex);
        } catch (SAXException saex) {
            throw new SamlpAssertionException("Error checking request message", saex);
        }
    }

    /**
     * Creates the NameResolvers used by the SAMLP message generators.
     *
     * @param context the current PolicyEnforcementContext
     * @param bContext BuilderContext with runtime values
     * @throws SamlpAssertionException when unable to obtain the NameIdentifier value(s)
     */
    private void setResolvers(final PolicyEnforcementContext context, final BuilderContext bContext ) throws SamlpAssertionException {
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final String[] nameOverrides = getNameIdentifier(authContext, bContext);

        bContext.nameResolver = new NameIdentifierResolver<SamlpRequestBuilderAssertion>(assertion) {
                @Override
                protected void parse() {
                    this.nameValue = nameOverrides[0];
                    this.nameFormat = nameOverrides[1];
                }
            };

        // SAML 1.1 does not contain an Issuer field in protocol requests
        bContext.issuerNameResolver = (!assertion.includeIssuer()) ? null :
                new NameIdentifierResolver<SamlpRequestBuilderAssertion>(assertion) {
                    @Override
                    protected void parse() throws SamlpAssertionException {
                        final String customIssuerValue = assertion.getCustomIssuerValue();

                        this.nameValue = (customIssuerValue == null) ?
                                signerInfo.getCertificateChain()[0].getSubjectDN().getName() :
                                ExpandVariables.process(customIssuerValue, bContext.ctxVariables, getAudit());

                        this.nameFormat = (assertion.getCustomIssuerFormat() == null) ?
                                null :
                                ExpandVariables.process(assertion.getCustomIssuerFormat(), bContext.ctxVariables, getAudit());
                        if (this.nameFormat != null) {
                            final String invalidError = ValidationUtils.isValidUriString(this.nameFormat);
                            if (invalidError != null) {
                                throw new SamlpAssertionException("Invalid Issuer Format attribute value, not a URI: " + invalidError);
                            }
                        }

                        this.nameQualifier = (assertion.getCustomIssuerNameQualifier() == null) ?
                                null :
                                ExpandVariables.process(assertion.getCustomIssuerNameQualifier(), bContext.ctxVariables, getAudit());
                    }
                };

        if (authContext.getLastCredentials() != null) {
            bContext.clientCertResolver = new MessageValueResolver<X509Certificate>(assertion) {
                @Override
                protected void parse() {
                    this.value = authContext.getLastCredentials().getClientCert();
                }
            };
        } else if (SamlpRequestBuilderAssertion.HOK_URIS.contains(assertion.getSubjectConfirmationMethodUri())) {
            // when subj confirmation is set to holder-of-key, log warning
            // -- but continue since SubjConfirmationData is not mandatory per SAML spec,
            //    expect the backend SAMLP service to fail if the info is required
//            logAndAudit(AssertionMessages.SAMLP_BUILDER_HOK_MISSING_CERT);
            //todo this needs to be audited
            logger.log(Level.WARNING, "Certificate-based credential source not found for Holder-of-Key subject confirmation");
        }

        // InetAddress only needed for AuthenticationStatements
        if (assertion.getAuthorizationStatement() != null || assertion.getAuthenticationStatement() != null) {
            if (context.getRequest().getTcpKnob() != null) {
                try {
                    String clientAddress = context.getRequest().getTcpKnob().getRemoteAddress();
                    final InetAddress hostAddr = InetAddress.getByName(clientAddress);

                    bContext.addressResolver = new InetAddressResolver(assertion) {
                        @Override
                        protected void parse() {
                            this.address = hostAddr;
                        }
                    };
                } catch (UnknownHostException badHost) {
                    logger.warning("Unable to obtain InetAddress for SubjectLocality");
                    bContext.addressResolver = null;
                }

            }
            // Authentication method
            bContext.authnMethodResolver = new MessageValueResolver<String>(assertion) {

                @Override
                protected void parse() {
                    if (authContext.getLastCredentials() != null) {
                        this.value = getAuthMethod(authContext.getLastCredentials().getCredentialSourceAssertion());
                    } else {
                        this.value = getAuthMethod(null);
                    }
                }
            };

            // Evidence
            bContext.evidenceBlockResolver = getEvidenceResolver(context);
        }
    }


    /**
     * Build the SAMLP request payload based on the assertion configuration.
     *
     * @param bContext the BuilderContext to use
     * @return the SAMLP request message payload
     * @throws SamlpAssertionException if
     */
    @Nullable
    private JAXBElement<?> buildRequest( final BuilderContext bContext ) throws SamlpAssertionException {

        final JAXBElement<?> request;
        if (assertion.getAuthenticationStatement() != null) {
            // Authentication
            request = buildAuthenticationRequest(bContext);

        } else if (assertion.getAuthorizationStatement() != null) {
            // Authorization Decision
            request = buildAuthorizationRequest(bContext);

        } else if (assertion.getAttributeStatement() != null) {
            // Attribute Query
            request = buildAttributeQueryRequest(bContext);
        } else {
            throw new IllegalStateException("Unknown statement type configured."); // Coding error.
        }

        return request;
    }
    
    private JAXBElement<?> buildAuthenticationRequest( final BuilderContext bContext )
        throws SamlpAssertionException
    {
        if (assertion.getVersion() == 2) {
            /* SAML 2.0 generator */
            AuthnRequestGenerator gen = new AuthnRequestGenerator(bContext.ctxVariables, getAudit(), bContext.issuerNameResolver);
            gen.setNameResolver(bContext.nameResolver);
            gen.setAddressResolver(bContext.addressResolver);
            gen.setAuthnMethodResolver(bContext.authnMethodResolver);
            gen.setClientCertResolver(bContext.clientCertResolver);
            
            AuthnRequestType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        } else if (assertion.getVersion() == 1) {
            /* SAML 1.1 generator - not supported*/
            throw new SamlpAssertionException("Authentication request not supported in SAMLP 1.1");
        }
        return null;
    }

    private JAXBElement<?> buildAuthorizationRequest( final BuilderContext bContext )
        throws SamlpAssertionException
    {
        if (assertion.getVersion() == 2) {
            /* SAML 2.0 generator */
            AuthzDecisionQueryGenerator gen = new AuthzDecisionQueryGenerator(bContext.ctxVariables, getAudit(), bContext.issuerNameResolver);
            gen.setNameResolver(bContext.nameResolver);
            gen.setAddressResolver(bContext.addressResolver);
            gen.setAuthnMethodResolver(bContext.authnMethodResolver);
            gen.setClientCertResolver(bContext.clientCertResolver);
            gen.setEvidenceBlockResolver(bContext.evidenceBlockResolver);
            
            AuthzDecisionQueryType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        } else if (assertion.getVersion() == 1) {
            /* SAML 1.1 generator */
            AuthorizationDecisionQueryGenerator gen = new AuthorizationDecisionQueryGenerator(bContext.ctxVariables, getAudit());
            gen.setNameResolver(bContext.nameResolver);
            gen.setAddressResolver(bContext.addressResolver);
            gen.setAuthnMethodResolver(bContext.authnMethodResolver);
            gen.setClientCertResolver(bContext.clientCertResolver);
            gen.setEvidenceBlockResolver(bContext.evidenceBlockResolver);

            RequestType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        }
        return null;
    }

    private JAXBElement<?> buildAttributeQueryRequest( final BuilderContext bContext )
        throws SamlpAssertionException
    {
        if (assertion.getVersion() == 2) {
            /* SAML 2.0 generator */
            com.l7tech.external.assertions.samlpassertion.server.v2.AttributeQueryGenerator gen =
                    new com.l7tech.external.assertions.samlpassertion.server.v2.AttributeQueryGenerator(
                            bContext.ctxVariables, getAudit(), bContext.issuerNameResolver);

            gen.setNameResolver(bContext.nameResolver);
            gen.setClientCertResolver(bContext.clientCertResolver);
            AttributeQueryType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        } else if (assertion.getVersion() == 1) {
            /* SAML 1.1 generator */
            com.l7tech.external.assertions.samlpassertion.server.v1.AttributeQueryGenerator gen =
                    new com.l7tech.external.assertions.samlpassertion.server.v1.AttributeQueryGenerator(bContext.ctxVariables, getAudit());
            gen.setNameResolver(bContext.nameResolver);
            gen.setClientCertResolver(bContext.clientCertResolver);
            RequestType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);
        }
        return null;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    private void setRequestToTarget( final BuilderContext bContext,
                                     final Message target,
                                     final JAXBElement<?> request)
        throws SamlpAssertionException
    {
        final Document samlpDoc = XmlUtil.createEmptyDocument();
        marshal(request, samlpDoc);

        if (assertion.getNameIdentifierType() != NameIdentifierInclusionType.NONE &&
                assertion.isEncryptNameIdentifier() &&
                assertion.getVersion() != null &&
                assertion.getVersion() == 2) {

            encryptNameID(bContext.ctxVariables, samlpDoc);
        }

        if (assertion.isSignAssertion()) {
            signRequest(samlpDoc);
        }

        wrapDocumentInSoap(samlpDoc, bContext);
        target.initialize(samlpDoc);
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    private void encryptNameID(final Map<String, Object> ctxVariables, final Document samlpDoc) throws SamlpAssertionException {
        // find the NameID to encrypt.
        final Element nameIdElement = DomUtils.findFirstDescendantElement(samlpDoc.getDocumentElement(), SamlConstants.NS_SAML2, "NameID");
        if (nameIdElement == null) {
            throw new SamlpAssertionException("Could not find NameID to encrypt");
        }

        final XmlElementEncryptionResolvedConfig encryptionResolvedConfig;
        final XmlElementEncryptionConfig xmlEncryptConfig = assertion.getXmlEncryptConfig();

        try {
            encryptionResolvedConfig = XmlElementEncryptorConfigUtils.getXmlElementEncryptorConfig(xmlEncryptConfig, ctxVariables, getAudit());
        } catch (Exception e) {
            throw new SamlpAssertionException("Unable to process encryption configuration: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final XmlElementEncryptor encryptor;
        try {
            encryptor = new XmlElementEncryptor(encryptionResolvedConfig);
        } catch (Exception e) {
            throw new SamlpAssertionException("Unable to create encryption engine with encryption configuration: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final Pair<Element,SecretKey> encryptedKey;
        try {
            encryptedKey = encryptor.createEncryptedKey(samlpDoc, xmlEncryptConfig.isUseOaep(), null);
        } catch (GeneralSecurityException e) {
            throw new SamlpAssertionException("Unable to create encrypted key for NameID encryption: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final Element encryptedElement;
        try {
            encryptedElement = encryptor.encryptAndReplaceElement(nameIdElement, encryptedKey);
        } catch (Exception e) {
            throw new SamlpAssertionException("Unable to encrypt NameID: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        // need to place the encrypted data into the correct location - as NameID now contains an EncryptedData element - fix
        //todo - update this if the XmlElementEncryptor is updated to support encrypting an element and placing it somewhere else
        final Node parentOfEncryptedNameId = encryptedElement.getParentNode();
        parentOfEncryptedNameId.removeChild(encryptedElement);
        final String samlVersionNS = SamlConstants.NS_SAML2;
        final String samlVersionPrefix = SamlConstants.NS_SAML2_PREFIX;
        final Element encryptedID = samlpDoc.createElementNS(samlVersionNS, samlVersionPrefix + ":EncryptedID");
        encryptedID.appendChild(encryptedElement);
        parentOfEncryptedNameId.appendChild(encryptedID);
    }


    private void marshal(final JAXBElement<?> request, final Object marshalTo)
        throws SamlpAssertionException
    {
        try {
            final Marshaller m;
            if (assertion.getVersion() == 1)
                m = JaxbUtil.getMarshallerV1();
            else
                m = JaxbUtil.getMarshallerV2();

            if (marshalTo instanceof Document){
                m.marshal(request, (Document) marshalTo);
            }

        } catch (JAXBException jxbEx) {
            logger.log(Level.INFO, "Failed to marshal JAXB message: {0}", jxbEx);
            throw new SamlpAssertionException("Failed to marshal JAXB target message", jxbEx);
        }
    }

    private void wrapDocumentInSoap(final Document docToWrap, final BuilderContext bContext) throws SamlpAssertionException {

        final Node docToWrapContents = docToWrap.removeChild(docToWrap.getDocumentElement());

        final String soapEnv =
                "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"{0}\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        String soapMessageAsString;
        final SoapVersion soapVersion;
        if (bContext.soapVersion == 2) {
            /* SOAP 1.2 */
            soapMessageAsString = MessageFormat.format(soapEnv, SOAP_1_2_NS);
            soapVersion = SoapVersion.SOAP_1_2;
        } else {
            /* SOAP 1.1 */
            soapMessageAsString = MessageFormat.format(soapEnv, SOAP_1_1_NS);
            soapVersion = SoapVersion.SOAP_1_1;
        }

        final Document soapDoc = XmlUtil.stringAsDocument(soapMessageAsString);
        final Node importedNode = docToWrap.importNode(soapDoc.getDocumentElement(), true);
        docToWrap.appendChild(importedNode);
        try {
            final Element bodyElement = DomUtils.findExactlyOneChildElementByName(docToWrap.getDocumentElement(), soapVersion.getNamespaceUri(), "Body");
            bodyElement.appendChild(docToWrapContents);
        } catch (TooManyChildElementsException e) {
            throw new SamlpAssertionException(ExceptionUtils.getMessage(e));
        } catch (MissingRequiredElementException e) {
            throw new SamlpAssertionException(ExceptionUtils.getMessage(e));
        }
    }

    private void signRequest(final Document request) throws SamlpAssertionException {
        try {
            RequestSigner.signSamlpRequest(
                    assertion.getVersion(),
                    request,
                    signerInfo.getPrivate(),
                    signerInfo.getCertificateChain(),
                    assertion.getSignatureKeyInfoType()
            );
        } catch (SignatureException sigEx) {
            throw new SamlpAssertionException("Error while signing SAMLP request: " + ExceptionUtils.getMessage(sigEx), sigEx);
        } catch (UnrecoverableKeyException e) {
            throw new SamlpAssertionException("Error while signing SAMLP request: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private String getCustomNameFormatValue(final BuilderContext bContext) {
        String customFormatValue = null;
        final String customFormat = assertion.getCustomNameIdentifierFormat();
        if (customFormat != null) {
            final String customFormatResolved = ExpandVariables.process(customFormat, bContext.ctxVariables, getAudit());
            final String errorString = ValidationUtils.isValidUriString(customFormatResolved);
            if (errorString != null) {
                logAndAudit(AssertionMessages.SAMLP_REQUEST_BUILDER_INVALID_URI, "custom name identifier format", customFormatResolved, errorString);
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
            } else {
                customFormatValue = customFormatResolved;
            }
        }

        return customFormatValue;
    }

    /**
     * Taken from <code>ServerSamlTokenIssuerAssertion</code>.  Extracts the NameIdentifier based on the selected
     * assertion property.
     *
     * @param ctx the PolicyEnforcementContext
     * @param bContext context with variables and resolvers
     * @return String array of 2 elements: [0] is the nameValue; and [1] is the nameFormat
     * @throws SamlpAssertionException if the "FROM_USER" property is chosen and no user Auth is found in the ctx
     */
    private String[] getNameIdentifier( final AuthenticationContext ctx, final BuilderContext bContext )
        throws SamlpAssertionException
    {
        String nameValue;
        final String nameFormat;
        final String formatValue = (assertion.getNameIdentifierFormat() == null) ? getCustomNameFormatValue(bContext) : assertion.getNameIdentifierFormat();

        switch(assertion.getNameIdentifierType()) {
            case FROM_CREDS:
                LoginCredentials credentials = ctx.getLastCredentials();
                if (credentials != null) {
                    final X509Certificate clientCert = credentials.getClientCert();
                    final String nameFormatValueToUse;
                    if (clientCert != null) {
                        nameFormatValueToUse = (formatValue == null ? SamlConstants.NAMEIDENTIFIER_X509_SUBJECT : formatValue);
                        nameValue = clientCert.getSubjectDN().getName();
                    } else {
                        nameFormatValueToUse = (formatValue == null ? SamlConstants.NAMEIDENTIFIER_UNSPECIFIED : formatValue);
                        nameValue = credentials.getLogin();
                    }
                    nameFormat = nameFormatValueToUse;
                } else {
                    throw new SamlpAssertionException("Missing credentials to populate NameIdentifier");
                }
                break;
            case FROM_USER:
                User u = ctx.getLastAuthenticatedUser();
                if (u == null) {
                    logAndAudit(AssertionMessages.SAML_ISSUER_AUTH_REQUIRED);
                    throw new SamlpAssertionException("Missing authenticated user to populate NameIdentifier");
                }

                if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(formatValue)) {
                    nameFormat = formatValue;
                    nameValue = u.getEmail();
                } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(formatValue)) {
                    X509Certificate foundCert = null;
                    for (AuthenticationResult result : ctx.getAllAuthenticationResults()) {
                        X509Certificate cert = result.getAuthenticatedCert();
                        if (cert != null) {
                            foundCert = cert;
                            break;
                        }
                    }
                    nameFormat = formatValue;
                    nameValue = foundCert == null ? u.getSubjectDn() : foundCert.getSubjectDN().getName();
                } else {
                    nameFormat = (formatValue == null ? SamlConstants.NAMEIDENTIFIER_UNSPECIFIED : formatValue);
                    nameValue = u.getLogin();
                }

                if (nameValue == null) nameValue = u.getName();

                break;
            case SPECIFIED:
                String val = assertion.getNameIdentifierValue();
                if (val == null) {
                    logAndAudit(AssertionMessages.SAML_ISSUER_MISSING_NIVAL);
                    nameValue = null;
                } else {
                    nameValue = ExpandVariables.process(val, bContext.ctxVariables, getAudit());
                }
                //todo [Donal] - this looks like a possible bug as it may be null and should likely default to unspecified
                nameFormat = formatValue;
                break;
            case NONE:
            default:
                nameValue = null;
                nameFormat = null;
                break;
        }

        return new String[] {nameValue, nameFormat};
    }


    private EvidenceBlockResolver getEvidenceResolver(final PolicyEnforcementContext context)
        throws SamlpAssertionException
    {
        if (Integer.valueOf(SamlpRequestConstants.SAMLP_AUTHZ_EVIDENCE_FROM_VAR).equals(assertion.getEvidence())) {
            try {
                final String var = assertion.getEvidenceVariable();
                if (var == null) {
                    throw new SamlpAssertionException("Missing evidence context variable in assertion properties");
                }

                Object obj = context.getVariable(var);

                // two possible types of variables
                final Document doc;
                if (obj != null && obj instanceof Message && ((Message) obj).isXml()) {
                    // build resolver with document
                    doc = ((Message) obj).getXmlKnob().getDocumentReadOnly();
                } else if (obj != null && obj instanceof String) {
                    // build resolver w/ XML string
                    ByteArrayInputStream bais = new ByteArrayInputStream(obj.toString().getBytes());
                    doc = XmlUtil.parse(bais);
                } else {
                    throw new SamlpAssertionException("Unsupported SAML Evidence format in variable: " + var);
                }

                // create the resolver
                return new EvidenceBlockResolver(assertion) {
                    @Override
                    protected void parse() {
                        this.value = doc;
                    }
                };

            } catch (IOException ioex) {
                throw new SamlpAssertionException("Unsupported SAML Evidence message context variable format (non-XML)", ioex);
            } catch (SAXException saex) {
                throw new SamlpAssertionException("Unable to extract XML document from Evidence variable", saex);
            } catch (NoSuchVariableException noVar) {
                throw new SamlpAssertionException(noVar);
            }
        }
        return null;
    }


    /**
     * Figures out the SAML Authentication method based on the login credentials.
     *
     * @param credentialSourceClass
     * @return
     */
    private String getAuthMethod( @Nullable final Class credentialSourceClass ) {

        final int ver = assertion.getVersion();

        String authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_UNSPECIFIED : SamlConstants.UNSPECIFIED_AUTHENTICATION);
        if (credentialSourceClass == null) {
            //todo this needs to be audited
            logger.log(Level.WARNING, "Credential source not found, using default value (unspecified)");
            return authMethod;
        }

        if (SslAssertion.class.isAssignableFrom(credentialSourceClass)) {
            authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_TLS_CERT : SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION );

        } else if (RequireWssX509Cert.class.isAssignableFrom(credentialSourceClass)) {
            authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_XMLDSIG : SamlConstants.XML_DSIG_AUTHENTICATION);

        } else if (HttpCredentialSourceAssertion.class.isAssignableFrom(credentialSourceClass) ||
                   WssBasic.class.isAssignableFrom(credentialSourceClass)) {

            authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_PASSWORD : SamlConstants.PASSWORD_AUTHENTICATION);
        }

        return authMethod;
    }
}
