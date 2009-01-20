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
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.saml.SamlConstants;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import saml.v1.protocol.RequestType;
import saml.v2.protocol.AttributeQueryType;
import saml.v2.protocol.AuthnRequestType;
import saml.v2.protocol.AuthzDecisionQueryType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: vchan
 */
public class ServerSamlpRequestBuilderAssertion extends AbstractServerAssertion<SamlpRequestBuilderAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSamlpRequestBuilderAssertion.class.getName());

    private static final String SOAP_1_1_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_1_2_NS = "http://www.w3.org/2003/05/soap-envelope";

    private final Auditor auditor;
    private final String[] variablesUsed;
    private Map<String, Object> ctxVariables;

    private final SignerInfo signerInfo;
    private int soapVersion;
    private NameIdentifierResolver nameResolver;
    private NameIdentifierResolver issuerNameResolver;
    private InetAddressResolver addressResolver;
    private MessageValueResolver<String> authnMethodResolver;
    private MessageValueResolver<X509Certificate> clientCertResolver;
    private EvidenceBlockResolver evidenceBlockResolver;

    /**
     * Constructor.
     *
     * @param assertion the SAMLP request builder assertion
     * @param spring the Spring ApplicationContext
     * @throws ServerPolicyException  when missing or invalid assertion properties are encountered
     */
    public ServerSamlpRequestBuilderAssertion(SamlpRequestBuilderAssertion assertion, ApplicationContext spring)
         throws ServerPolicyException
    {
        super(assertion);
        this.auditor = new Auditor(this, spring, logger);
        this.variablesUsed = assertion.getVariablesUsed();

        // get the Server Cert and signer info -- Copied from SamlIssuer
        X509Certificate serverCert = (X509Certificate) spring.getBean("sslKeystoreCertificate");
        if (serverCert == null) throw new IllegalStateException("Unable to locate server certificate");
        X509Certificate[] serverCertChain = new X509Certificate[]{serverCert};
        this.signerInfo = new SignerInfo((PrivateKey) spring.getBean("sslKeystorePrivateKey"), serverCertChain);
    }

    /**
     * @see com.l7tech.server.policy.assertion.AbstractServerAssertion#checkRequest(com.l7tech.server.message.PolicyEnforcementContext)
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            // initialize stuff
            setSoapVersion(context.getRequest());
            ctxVariables = context.getVariableMap(variablesUsed, auditor);

            /*
             * 1) Determine where the target message is supposed to go
             */
            final Message msg;
            try {
                msg = context.getTargetMessage(assertion);
            } catch (NoSuchVariableException e) {
                auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
                throw new SamlpAssertionException(e);
            }

            /*
             * 2) Build SAMLP payload
             * 
             */
            setResolvers(context);
            JAXBElement<?> samlpRequest = buildRequest(ctxVariables);
            if (samlpRequest == null) {
//                auditor.logAndAudit(AssertionMessages.SAMLP_BUILDER_FAILED);
                logger.log(Level.WARNING, "Failed to create SAMLP request.");
                return AssertionStatus.FAILED;
            }

            /*
             * 3) Set the SAMLP request msg into the target message
             */
            setRequestToTarget(context, msg, samlpRequest);

        } catch (SamlpAssertionException samlEx) {
//            auditor.logAndAudit(AssertionMessages.SAMLP_BUILDER_ERROR, new String[] { ExceptionUtils.getMessage(samlEx) });
            logger.log(Level.WARNING, "SAMLP builder failed: " + ExceptionUtils.getMessage(samlEx), ExceptionUtils.getDebugException(samlEx));
            return AssertionStatus.FAILED;
        }

//        auditor.logAndAudit(AssertionMessages.SAMLP_BUILDER_COMPLETE, getRequestType());
        return AssertionStatus.NONE;
    }


    private void setSoapVersion(Message request) throws SamlpAssertionException {

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
                    this.soapVersion = DEFAULT_SOAP_VERSION;
                }
            }

            this.soapVersion = ver;

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
     * @throws SamlpAssertionException when unable to obtain the NameIdentifier value(s)
     */
    private void setResolvers(final PolicyEnforcementContext context) throws SamlpAssertionException {

        final String[] nameOverrides = getNameIdentifier(context);

        this.nameResolver = new NameIdentifierResolver(assertion) {
                protected void parse() {
                    this.nameValue = nameOverrides[0];
                    this.nameFormat = nameOverrides[1];
                }
            };

        this.issuerNameResolver = new NameIdentifierResolver(assertion) {
                protected void parse() {
                    if (signerInfo != null)
                        this.nameValue = signerInfo.getCertificateChain()[0].getSubjectDN().getName();
                }
            };

        if (context.getLastCredentials() != null) {
            this.clientCertResolver = new MessageValueResolver<X509Certificate>(assertion) {
                protected void parse() {
                    this.value = context.getLastCredentials().getClientCert();
                }
            };
        } else if (SamlpRequestBuilderAssertion.HOK_URIS.contains(assertion.getSubjectConfirmationMethodUri())) {
            // when subj confirmation is set to holder-of-key, log warning
            // -- but continue since SubjConfirmationData is not mandatory per SAML spec,
            //    expect the backend SAMLP service to fail if the info is required
//            auditor.logAndAudit(AssertionMessages.SAMLP_BUILDER_HOK_MISSING_CERT);
            logger.log(Level.WARNING, "Certificate-based credential source not found for Holder-of-Key subject confirmation");
        }

        // InetAddress only needed for AuthenticationStatements
        if (assertion.getAuthorizationStatement() != null || assertion.getAuthenticationStatement() != null) {
            if (context.getRequest().getTcpKnob() != null) {
                try {
                    String clientAddress = context.getRequest().getTcpKnob().getRemoteAddress();
                    final InetAddress hostAddr = InetAddress.getByName(clientAddress);

                    this.addressResolver = new InetAddressResolver(assertion) {
                        protected void parse() {
                            this.address = hostAddr;
                        }
                    };
                } catch (UnknownHostException badHost) {
                    logger.warning("Unable to obtain InetAddress for SubjectLocality");
                    this.addressResolver = null;
                }

            }
            // Authentication method
            this.authnMethodResolver = new MessageValueResolver<String>(assertion) {

                protected void parse() {
                    if (context.getLastCredentials() != null) {
                        this.value = getAuthMethod(context.getLastCredentials().getCredentialSourceAssertion());
                    } else {
                        this.value = getAuthMethod(null);
                    }
                }
            };

            // Evidence
            this.evidenceBlockResolver = getEvidenceResolver(context);
        }
    }


    /**
     * Build the SAMLP request payload based on the assertion configuration.
     *
     * @param vars the Map of available context variables
     * @return the SAMLP request message payload
     */
    private JAXBElement<?> buildRequest(Map<String, Object> vars) {

        JAXBElement<?> request = null;
        try {
            if (assertion.getAuthenticationStatement() != null) {
                // Authentication
                request = buildAuthenticationRequest(vars);

            } else if (assertion.getAuthorizationStatement() != null) {
                // Authorization Decision
                request = buildAuthorizationRequest(vars);

            } else if (assertion.getAttributeStatement() != null) {
                // Attribute Query
                request = buildAttributeQueryRequest(vars);
            }

        } catch (SamlpAssertionException saex) {
            logger.warning("Failed to build Request message: " + ExceptionUtils.getMessage(saex));
            request = null;
        }
        return request;
    }
    
    private JAXBElement<?> buildAuthenticationRequest(Map<String, Object> ctxVariables)
        throws SamlpAssertionException
    {
        if (assertion.getSamlVersion() == 2) {
            /* SAML 2.0 generator */
            AuthnRequestGenerator gen = new AuthnRequestGenerator(ctxVariables, auditor);
            gen.setNameResolver(nameResolver);
            gen.setIssuerNameResolver(issuerNameResolver);
            gen.setAddressResolver(addressResolver);
            gen.setAuthnMethodResolver(authnMethodResolver);
            gen.setClientCertResolver(clientCertResolver);
            
            AuthnRequestType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        } else if (assertion.getSamlVersion() == 1) {
            /* SAML 1.1 generator - not supported*/
            throw new SamlpAssertionException("Authentication request not supported in SAMLP 1.1");
        }
        return null;
    }

    private JAXBElement<?> buildAuthorizationRequest(Map<String, Object> ctxVariables)
        throws SamlpAssertionException
    {
        if (assertion.getSamlVersion() == 2) {
            /* SAML 2.0 generator */
            AuthzDecisionQueryGenerator gen = new AuthzDecisionQueryGenerator(ctxVariables, auditor);
            gen.setNameResolver(nameResolver);
            gen.setIssuerNameResolver(issuerNameResolver);
            gen.setAddressResolver(addressResolver);
            gen.setAuthnMethodResolver(authnMethodResolver);
            gen.setClientCertResolver(clientCertResolver);
            gen.setEvidenceBlockResolver(evidenceBlockResolver);
            
            AuthzDecisionQueryType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        } else if (assertion.getSamlVersion() == 1) {
            /* SAML 1.1 generator */
            AuthorizationDecisionQueryGenerator gen = new AuthorizationDecisionQueryGenerator(ctxVariables, auditor);
            gen.setNameResolver(nameResolver);
            gen.setIssuerNameResolver(issuerNameResolver);
            gen.setAddressResolver(addressResolver);
            gen.setAuthnMethodResolver(authnMethodResolver);
            gen.setClientCertResolver(clientCertResolver);
            gen.setEvidenceBlockResolver(evidenceBlockResolver);

            RequestType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        }
        return null;
    }

    private JAXBElement<?> buildAttributeQueryRequest(Map<String, Object> ctxVariables)
        throws SamlpAssertionException
    {
        if (assertion.getSamlVersion() == 2) {
            /* SAML 2.0 generator */
            com.l7tech.external.assertions.samlpassertion.server.v2.AttributeQueryGenerator gen =
                    new com.l7tech.external.assertions.samlpassertion.server.v2.AttributeQueryGenerator(ctxVariables, auditor);
            gen.setNameResolver(nameResolver);
            gen.setIssuerNameResolver(issuerNameResolver);
            gen.setClientCertResolver(clientCertResolver);
            AttributeQueryType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);

        } else if (assertion.getSamlVersion() == 1) {
            /* SAML 1.1 generator */
            com.l7tech.external.assertions.samlpassertion.server.v1.AttributeQueryGenerator gen =
                    new com.l7tech.external.assertions.samlpassertion.server.v1.AttributeQueryGenerator(ctxVariables, auditor);
            gen.setNameResolver(nameResolver);
            gen.setIssuerNameResolver(issuerNameResolver);
            gen.setClientCertResolver(clientCertResolver);
            RequestType jaxbMessage = gen.create(assertion);

            if (jaxbMessage != null)
                return gen.createJAXBElement(jaxbMessage);
        }
        return null;
    }


    private void setRequestToTarget(PolicyEnforcementContext context, final Message target, JAXBElement<?> request)
        throws SamlpAssertionException
    {
        final String msgXmlString;
        if (assertion.isSignRequest()) {

            // marshal into Document and sign the request
            Document samlpDoc = XmlUtil.createEmptyDocument();
            marshal(context, request, samlpDoc);
            msgXmlString = signAndSerialize(samlpDoc);

        } else {
            // marshal int XML string
            StringWriter msgWriter = new StringWriter();
            marshal(context, request, msgWriter);

            // create SOAP message
            msgXmlString = msgWriter.getBuffer().toString();
        }

        // log complete message for debug
        if (logger.isLoggable(Level.FINER)) { // true ||
            logger.finer(new StringBuffer("samlp_req:").append(msgXmlString).toString());
        }

        // set the target message with the full SOAP request message
        target.initialize( soapify(msgXmlString) );
    }

    private void marshal(PolicyEnforcementContext context, JAXBElement<?> request, Object marshalTo)
        throws SamlpAssertionException
    {
        final String key = context.getRequestId().toString();
        try {
            Marshaller m;
            if (assertion.getSamlVersion() == 1)
                m = JaxbUtil.getMarshallerV1(key);
            else
                m = JaxbUtil.getMarshallerV2(key);

            if (marshalTo instanceof Document)
                m.marshal(request, (Document) marshalTo);
            else if (marshalTo instanceof StringWriter)
                m.marshal(request, (StringWriter) marshalTo);

            /*
             * shouldn't get here
             */
        } catch (JAXBException jxbEx) {
            logger.log(Level.INFO, "Failed to marshal JAXB message: {0}", jxbEx);
            throw new SamlpAssertionException("Failed to marshal JAXB target message", jxbEx);
        } finally {
            JaxbUtil.releaseJaxbResources(key);
        }
    }

    private Document soapify(String requestPayload) throws SamlpAssertionException {

        final String soapEnv =
                "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"{0}\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "     {1}" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        String finalMessage;
        if (this.soapVersion == 2) {
            /* SOAP 1.2 */
            finalMessage = MessageFormat.format(soapEnv, SOAP_1_2_NS, requestPayload);
        } else {
            /* SOAP 1.1 */
            finalMessage = MessageFormat.format(soapEnv, SOAP_1_1_NS, requestPayload);
        }
        return XmlUtil.stringAsDocument(finalMessage);
    }


    private String signAndSerialize(Document request) throws SamlpAssertionException {
        try {
            RequestSigner.signSamlpRequest(
                    assertion.getSamlVersion(),
                    request,
                    signerInfo.getPrivate(),
                    signerInfo.getCertificateChain(),
                    assertion.getSignatureKeyInfoType()
            );

            // serialize into XML string
            return new String(XmlUtil.toByteArray(request));

        } catch (SignatureException sigEx) {
            throw new SamlpAssertionException("Error while signing SAMLP request", sigEx);
        } catch (IOException ioex) {
            throw new SamlpAssertionException("Error while serializing signed SAMLP request", ioex);
        }
    }

    /**
     * Taken from <code>ServerSamlIssuerAssertion</code>.  Extracts the NameIdentifier based on the selected
     * assertion property.
     *
     * @param ctx the PolicyEnforcementContext
     * @return String array of 2 elements: [0] is the nameValue; and [1] is the nameFormat
     * @throws SamlpAssertionException if the "FROM_USER" property is chosen and no user Auth is found in the ctx
     */
    private String[] getNameIdentifier(PolicyEnforcementContext ctx)
        throws SamlpAssertionException
    {
        String nameValue;
        final String nameFormat;
        String formatValue = assertion.getNameIdentifierFormat();

        switch(assertion.getNameIdentifierType()) {
            case FROM_CREDS:
                LoginCredentials credentials = ctx.getLastCredentials();
                if (credentials != null) {
                    final X509Certificate clientCert = credentials.getClientCert();
                    if (clientCert != null) {
                        formatValue = (formatValue == null ? SamlConstants.NAMEIDENTIFIER_X509_SUBJECT : formatValue);
                        nameValue = clientCert.getSubjectDN().getName();
                    } else {
                        formatValue = (formatValue == null ? SamlConstants.NAMEIDENTIFIER_UNSPECIFIED : formatValue);
                        nameValue = credentials.getLogin();
                    }
                    nameFormat = formatValue;
                } else {
                    // TODO: add audit msg
                    throw new SamlpAssertionException("Missing credentials to populate NameIdentifier");
                }
                break;
            case FROM_USER:
                User u = ctx.getLastAuthenticatedUser();
                if (u == null) {
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_AUTH_REQUIRED);
//                    auditor.logAndAudit(AssertionMessages.SAMLP_BUILDER_AUTH_REQUIRED);
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
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_MISSING_NIVAL);
//                    auditor.logAndAudit(AssertionMessages.SAMLP_BUILDER_MISSING_NIVAL);
                    nameValue = null;
                } else {
                    nameValue = ExpandVariables.process(val, ctxVariables, auditor);
                }
                nameFormat = assertion.getNameIdentifierFormat();
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
                    // logger.log(Level.WARNING, "Missing evidence context variable in assertion properties");
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
                    protected void parse() {
                        this.value = doc;
                        this.key = context.getRequestId().toString();
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
    private String getAuthMethod(Class credentialSourceClass) {

        final int ver = assertion.getSamlVersion();

        String authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_UNSPECIFIED : SamlConstants.UNSPECIFIED_AUTHENTICATION);
        if (credentialSourceClass == null) {
            logger.log(Level.WARNING, "Credential source not found, using default value (unspecified)");
            return authMethod;
        }

        if (SslAssertion.class.isAssignableFrom(credentialSourceClass)) {
            authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_TLS_CERT : SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION );

        } else if (RequestWssX509Cert.class.isAssignableFrom(credentialSourceClass)) {
            authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_XMLDSIG : SamlConstants.XML_DSIG_AUTHENTICATION);

        } else if (HttpCredentialSourceAssertion.class.isAssignableFrom(credentialSourceClass) ||
                   WssBasic.class.isAssignableFrom(credentialSourceClass)) {

            authMethod = (ver == 2? SamlConstants.AUTHENTICATION_SAML2_PASSWORD : SamlConstants.PASSWORD_AUTHENTICATION);
        }

        return authMethod;
    }

    private String getRequestType() {
        if (assertion.getAuthorizationStatement() != null)
            return "Authorization";
        else if (assertion.getAttributeStatement() != null)
            return "Attribute";
        return "Authentication";
    }
}
