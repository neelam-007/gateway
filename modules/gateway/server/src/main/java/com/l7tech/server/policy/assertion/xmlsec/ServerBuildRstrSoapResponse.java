package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.BuildRstrSoapResponse;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.util.RstSoapMessageProcessor;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class ServerBuildRstrSoapResponse extends ServerAddWssEncryption<BuildRstrSoapResponse> {
    private static final Logger logger = Logger.getLogger(ServerBuildRstrSoapResponse.class.getName());

    private static final String IS_SCT = "token.info.is.sct";
    private static final String TOKEN_XML = "token.info.token.xml.content";
    private static final String SCT_WSU_ID = "sct.info.wsu.id";
    private static final String SCT_IDENTIFIER = "sct.info.token.identifier";
    private static final String SAML_ASSERTION_ID = "saml.info.assertion.id";
    private static final String SAML_VALUE_TYPE = "saml.info.value.type";

    private final InboundSecureConversationContextManager scContextManager;
    private final Auditor auditor;
    private final String[] variablesUsed;

    public ServerBuildRstrSoapResponse( final BuildRstrSoapResponse assertion,
                                        final BeanFactory factory ) {
        super(assertion, assertion, assertion, assertion, logger);
        auditor = factory instanceof ApplicationContext?
                new Auditor(this, (ApplicationContext)factory, logger) :
                new LogOnlyAuditor(logger);
        variablesUsed = assertion.getVariablesUsed();
        scContextManager = factory.getBean("inboundSecureConversationContextManager", InboundSecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context,
                                             Message message,
                                             String messageDescription,
                                             AuthenticationContext authContext) throws IOException, PolicyAssertionException {

        // Get all related info from the target SOAP message.  RstSoapMessageProcessor checks the syntax and the semantics of the target SOAP message.
        final Map<String, String> rstParameters = RstSoapMessageProcessor.getRstParameters(message, assertion.isResponseForIssuance());
        if (rstParameters.containsKey(RstSoapMessageProcessor.ERROR)) {
            RstSoapMessageProcessor.generateSoapFaultResponse(
                context,
                rstParameters,
                getRstrResponseVariable(),
                RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_REQUEST,
                rstParameters.get(RstSoapMessageProcessor.ERROR)
            );
            
            auditor.logAndAudit(AssertionMessages.STS_INVALID_RST_REQUEST, rstParameters.get(RstSoapMessageProcessor.ERROR));
            return AssertionStatus.BAD_REQUEST;
        }

        // At this point, everything is fine since the validation is done.  It is ready to generate the RSTR response content depending on Binding type such as Issuance Binding or Cancel Binding.
        String rstrXml;
        final Map<String, String> tokenInfo = new HashMap<String, String>();
        if (assertion.isResponseForIssuance()) {
            final AssertionStatus status = getTokenInfo(context, tokenInfo, rstParameters);
            if (status != AssertionStatus.NONE) return status;
            rstrXml = generateRstrElement(context, rstParameters, tokenInfo);
        } else {
            rstrXml = generateRstrElement(context, rstParameters, null); // no need of token info
        }

        // Build a RSTR SOAP response message and set the context variable for rstrResponse
        final String rstrSoapResponse = buildRstrSoapResponse(rstParameters.get(RstSoapMessageProcessor.SOAP_ENVELOPE_NS), rstrXml);
        context.setVariable(assertion.getVariablePrefix() + "." + BuildRstrSoapResponse.VARIABLE_RSTR_RESPONSE, new Message(XmlUtil.stringAsDocument(rstrSoapResponse),0));// todo

        // Set the context variable for WS-Addressing Namespace (Note: WS-Addressing Namespace is optional to have.)
        final String wsaNS = RstSoapMessageProcessor.getWsaNamespace( rstParameters );
        context.setVariable(assertion.getVariablePrefix() + "." + BuildRstrSoapResponse.VARIABLE_WSA_NAMESPACE, (wsaNS == null)? "" : wsaNS);

        // Set the context variable for RSTR WS-Addressing Action (Optional)
        String rstrWsaAction = "";
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_WS_ADDRESSING_ACTION))) {
            String rstWsaAction = rstParameters.get(RstSoapMessageProcessor.WS_ADDRESSING_ACTION);

            if (assertion.isResponseForIssuance()) {
                // Get the type of the token issued
                boolean isSCT = Boolean.parseBoolean(tokenInfo.get(IS_SCT));
                if (isSCT) { // SCT
                    if (SoapConstants.WSC_RST_SCT_ACTION.equals(rstWsaAction)) {
                        rstrWsaAction = SoapConstants.WSC_RSTR_SCT_ACTION;
                    } else if (SoapConstants.WSC_RST_SCT_ACTION2.equals(rstWsaAction)) {
                        rstrWsaAction = SoapConstants.WSC_RSTR_SCT_ACTION2;
                    } else if (SoapConstants.WSC_RST_SCT_ACTION3.equals(rstWsaAction)) {
                        rstrWsaAction = SoapConstants.WSC_RSTR_SCT_ACTION3;
                    } else {
                        // Set the current latest WS-Addressing action in this case.
                        if (SoapConstants.WSC_RSTR_SCT_ACTION_LIST.size() > 0) {
                            rstrWsaAction = SoapConstants.WSC_RSTR_SCT_ACTION_LIST.get(SoapConstants.WSC_RSTR_SCT_ACTION_LIST.size() - 1);
                        }
                    }
                } else { // SAML Token
                    if (SoapConstants.WST_RST_ISSUE_ACTION.equals(rstWsaAction)) {
                        rstrWsaAction = SoapConstants.WST_RSTR_ISSUE_ACTION;
                    } else if (SoapConstants.WST_RST_ISSUE_ACTION2.equals(rstWsaAction)) {
                        rstrWsaAction = SoapConstants.WST_RSTR_ISSUE_ACTION2;
                    } else if (SoapConstants.WST_RST_ISSUE_ACTION3.equals(rstWsaAction)) {
                        rstrWsaAction = SoapConstants.WST_RSTR_ISSUE_ACTION3;
                    } else {
                        // Set the current latest WS-Addressing action in this case.
                        if (SoapConstants.WST_RSTR_ISSUE_ACTION_LIST.size() > 0) {
                            rstrWsaAction = SoapConstants.WST_RSTR_ISSUE_ACTION_LIST.get(SoapConstants.WST_RSTR_ISSUE_ACTION_LIST.size() - 1);
                        }
                    }
                }
            } else {
                if (SoapConstants.WSC_RST_CANCEL_ACTION.equals(rstWsaAction)) {
                    rstrWsaAction = SoapConstants.WSC_RSTR_CANCEL_ACTION;
                } else if (SoapConstants.WSC_RST_CANCEL_ACTION2.equals(rstWsaAction)) {
                    rstrWsaAction = SoapConstants.WSC_RSTR_CANCEL_ACTION2;
                } else {
                    // Set the current latest WS-Addressing action in this case.
                    if (SoapConstants.WSC_RST_CANCEL_ACTION_LIST.size() > 0) {
                        rstrWsaAction = SoapConstants.WSC_RST_CANCEL_ACTION_LIST.get(SoapConstants.WSC_RST_CANCEL_ACTION_LIST.size() - 1);
                    }
                }
            }
        }
        context.setVariable(assertion.getVariablePrefix() + "." + BuildRstrSoapResponse.VARIABLE_RSTR_WSA_ACTION, rstrWsaAction);

        // End up with success
        return AssertionStatus.NONE;
    }

    private String getRstrResponseVariable() {
        return assertion.getVariablePrefix() + "." + BuildRstrSoapResponse.VARIABLE_RSTR_RESPONSE;
    }

    private AssertionStatus getTokenInfo( final PolicyEnforcementContext context,
                                          final Map<String, String> tokenInfo,
                                          final Map<String,String> rstParameters ) {
        // Get the token issued
        String tokenVariable = assertion.getTokenIssued();

        // Get the XML content of the issued token
        String tokenXml = ExpandVariables.process(tokenVariable, context.getVariableMap(variablesUsed, auditor), auditor);
        tokenInfo.put(TOKEN_XML, tokenXml);

        Element root;
        try {
            Document tokenDoc = XmlUtil.stringToDocument(tokenXml);
            root = (Element) tokenDoc.getFirstChild();
        } catch (Throwable t) { // Such as SAXException, ClassCastException, etc.
            String errorMessage = "The security token used to generate a RSTR response is invalid or not well-formatted.";
            RstSoapMessageProcessor.generateSoapFaultResponse(
                context,
                rstParameters,
                getRstrResponseVariable(),
                RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_SECURITY_TOKEN,
                errorMessage
            );

            auditor.logAndAudit(AssertionMessages.STS_INVALID_SECURITY_TOKEN, errorMessage);
            return AssertionStatus.BAD_REQUEST;
        }

        String rootNS = root.getNamespaceURI();
        String rootElmtName = root.getLocalName();

        // The token is a SecurityContextToken.
        if ("SecurityContextToken".equals(rootElmtName)) {
            tokenInfo.put(IS_SCT, "true");

            // Set wsu:Id
            String sctWsuId;
            try {
                sctWsuId = DomUtils.getElementIdValue(root, SoapUtil.getDefaultIdAttributeConfig());
            } catch (InvalidDocumentFormatException e) {
                String errorMessage = "There are more than one ID attribute in the SecurityContextToken element.";
                RstSoapMessageProcessor.generateSoapFaultResponse(
                    context,
                    rstParameters,
                    getRstrResponseVariable(),
                    RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_SECURITY_TOKEN,
                    errorMessage
                );

                auditor.logAndAudit(AssertionMessages.STS_INVALID_SECURITY_TOKEN, errorMessage);
                return AssertionStatus.BAD_REQUEST;
            }
            tokenInfo.put(SCT_WSU_ID, sctWsuId);

            try {
                // Set Identifier
                tokenInfo.put(SCT_IDENTIFIER, DomUtils.findExactlyOneChildElementByName(root, rootNS, "Identifier").getTextContent());
            } catch (TooManyChildElementsException e) {
                String errorMessage = "There are more than one Identifier element in the SecurityContextToken element.";
                RstSoapMessageProcessor.generateSoapFaultResponse(
                    context,
                    rstParameters,
                    getRstrResponseVariable(),
                    RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_SECURITY_TOKEN,
                    errorMessage
                );

                auditor.logAndAudit(AssertionMessages.STS_INVALID_SECURITY_TOKEN, errorMessage);
                return AssertionStatus.BAD_REQUEST;
            } catch (MissingRequiredElementException e) {
                String errorMessage = "There is no Identifier element in the SecurityContextToken element.";
                RstSoapMessageProcessor.generateSoapFaultResponse(
                    context,
                    rstParameters,
                    getRstrResponseVariable(),
                    RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_SECURITY_TOKEN,
                    errorMessage
                );

                auditor.logAndAudit(AssertionMessages.STS_INVALID_SECURITY_TOKEN, errorMessage);
                return AssertionStatus.BAD_REQUEST;
            }
        }
        // The token is a SAML token.
        else if ("Assertion".equals(rootElmtName)) {
            tokenInfo.put(IS_SCT, "false");

            if (SoapConstants.SAML_NAMESPACE.equals(rootNS)) { // SAML 1.0 and 1.1
                // Set ID or AssertionID
                tokenInfo.put(SAML_ASSERTION_ID, root.getAttribute("AssertionID"));
                // Set ValueType
                tokenInfo.put(SAML_VALUE_TYPE, SoapConstants.VALUETYPE_SAML_ASSERTIONID2);
            } else if (SoapConstants.SAML_NAMESPACE2.equals(rootNS)) { // SAML 2.0
                // Set ID or AssertionID
                tokenInfo.put(SAML_ASSERTION_ID, root.getAttribute("ID"));
                // Set ValueType
                tokenInfo.put(SAML_VALUE_TYPE, SoapConstants.VALUETYPE_SAML_ASSERTIONID3);
            } else {
                String errorMessage = "The SAML namespace is invalid.";
                RstSoapMessageProcessor.generateSoapFaultResponse(
                    context,
                    rstParameters,
                    getRstrResponseVariable(),
                    RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_SECURITY_TOKEN,
                    errorMessage
                );

                auditor.logAndAudit(AssertionMessages.STS_INVALID_SECURITY_TOKEN, errorMessage);
                return AssertionStatus.BAD_REQUEST;
            }
        } else { // The token is not recognizable.
            String errorMessage = "The security token provided is neither a SecurityContextToken nor a SAML token.";
            RstSoapMessageProcessor.generateSoapFaultResponse(
                context,
                rstParameters,
                getRstrResponseVariable(),
                RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_SECURITY_TOKEN,
                errorMessage
            );

            auditor.logAndAudit(AssertionMessages.STS_INVALID_SECURITY_TOKEN, errorMessage);
            return AssertionStatus.BAD_REQUEST;
        }

        return AssertionStatus.NONE;
    }

    private String buildRstrSoapResponse(String soapEnvelopeNS, String rstrElement) {
        StringBuilder soapMessageBuilder = new StringBuilder("<soap:Envelope xmlns:soap=\"").append(soapEnvelopeNS).append("\">\n")
            .append("<soap:Body>\n")
            .append(rstrElement).append("\n")
            .append("</soap:Body>\n")
            .append("</soap:Envelope>");

        return soapMessageBuilder.toString();
    }

    /**
     * Generate a RequestSecurityTokenResponse element
     */
    private String generateRstrElement( final PolicyEnforcementContext context,
                                        final Map<String, String> parameters,
                                        final Map<String, String> tokenInfo ) throws PolicyAssertionException {
        final StringBuilder rstrBuilder = new StringBuilder();

        // Build RequestSecurityTokenResponse
        final String wsuNS = parameters.get(RstSoapMessageProcessor.WSU_NS) == null? SoapConstants.WSU_NAMESPACE : parameters.get(RstSoapMessageProcessor.WSU_NS);
        rstrBuilder.append("<wst:RequestSecurityTokenResponse ");

        // Check if the response message is for Token Cancel Binding.
        if (assertion.isResponseForIssuance()) {
            rstrBuilder
                .append("xmlns:wst=\"").append(parameters.get(RstSoapMessageProcessor.WST_NS)).append("\" ")
                .append("xmlns:wsu=\"").append(wsuNS).append("\">\n");
        } else {
            rstrBuilder
                .append("xmlns:wst=\"").append(parameters.get(RstSoapMessageProcessor.WST_NS)).append("\">\n");

            // Build RequestedTokenCancelled
            rstrBuilder
                .append("<wst:RequestedTokenCancelled/>\n")
                .append("</wst:RequestSecurityTokenResponse>");

            return rstrBuilder.toString();
        }

        // Get the token xml
        final String securityTokenXml = tokenInfo.get(TOKEN_XML);

        // Build TokenType
        final boolean hasTokeType = Boolean.parseBoolean(parameters.get(RstSoapMessageProcessor.HAS_TOKEN_TYPE));
        if (hasTokeType) {
            rstrBuilder
                .append("<wst:TokenType>")
                .append(parameters.get(RstSoapMessageProcessor.TOKEN_TYPE)).append("</wst:TokenType>\n");
        }

        // Build RequestedSecurityToken with a security token inside.
        rstrBuilder
            .append("<wst:RequestedSecurityToken>\n")
            .append(securityTokenXml).append("\n")
            .append("</wst:RequestedSecurityToken>\n");

        // Build AppliesTo
        if (assertion.isIncludeAppliesTo()) {
            final String address = assertion.getAddressOfEPR();
            if (address != null) {
                final String addressContent = TextUtils.escapeHtmlSpecialCharacters( // escape those special characters
                    ExpandVariables.process(address, context.getVariableMap(variablesUsed, auditor), auditor)
                );

                if (assertion.isIncludeAppliesTo() && addressContent != null && !addressContent.trim().isEmpty()) {
                    final String wsaNS = RstSoapMessageProcessor.getWsaNamespace( parameters );
                    final String wspNS = RstSoapMessageProcessor.getWspNamespace( parameters );
                    rstrBuilder
                        .append("<wsp:AppliesTo xmlns:wsp=\"").append(wspNS).append("\" xmlns:wsa=\"").append(wsaNS).append("\">\n")
                        .append("<wsa:EndpointReference>\n")
                        .append("<wsa:Address>").append(addressContent).append("</wsa:Address>\n")
                        .append("</wsa:EndpointReference>\n")
                        .append("</wsp:AppliesTo>\n");
                }
            }
        }

        // Get the type of the token issued
        final boolean isSCT = Boolean.parseBoolean(tokenInfo.get(IS_SCT));

        // Build RequestedAttachedReference including a SecurityTokenReference.
        String wsseNS = parameters.get(RstSoapMessageProcessor.WSSE_NS);
        if (wsseNS == null || wsseNS.trim().isEmpty()) {
            wsseNS = SoapConstants.SECURITY_NAMESPACE;
        }

        if (assertion.isIncludeAttachedRef()) {
            rstrBuilder
                .append("<wst:RequestedAttachedReference>\n")
                .append("<wsse:SecurityTokenReference xmlns:wsse=\"").append(wsseNS).append("\">\n");

            if (isSCT) { // If it is for SecurityContextToken, then include a Reference element.
                final String sctWsuId = tokenInfo.get(SCT_WSU_ID);
                rstrBuilder.append("<wsse:Reference URI=\"#").append(sctWsuId).append("\"");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(parameters.get(RstSoapMessageProcessor.TOKEN_TYPE)).append("\"");
                }
                rstrBuilder.append("/>\n");
            } else { // If it is a SAML token, then include a KeyIdentifier.
                final String valueType = tokenInfo.get(SAML_VALUE_TYPE);
                final String assertionId = tokenInfo.get(SAML_ASSERTION_ID);

                rstrBuilder.append("<wsse:KeyIdentifier");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(valueType).append("\"");
                }
                rstrBuilder.append(">").append(assertionId).append("</wsse:KeyIdentifier>\n");
            }
            rstrBuilder
                .append("</wsse:SecurityTokenReference>\n")
                .append("</wst:RequestedAttachedReference>\n");
        }

        // Build RequestedUnattachedReference including a SecurityTokenReference.
        if (assertion.isIncludeUnattachedRef()) {
            rstrBuilder
                .append("<wst:RequestedUnattachedReference>\n")
                .append("<wsse:SecurityTokenReference xmlns:wsse=\"").append(wsseNS).append("\">\n");

            if (isSCT) { // If it is for SecurityContextToken, then include a Reference element.
                final String sctIdentifier = tokenInfo.get(SCT_IDENTIFIER);

                rstrBuilder.append("<wsse:Reference URI=\"").append(sctIdentifier).append("\"");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(parameters.get(RstSoapMessageProcessor.TOKEN_TYPE)).append("\"");
                }
                rstrBuilder.append("/>\n");
            } else { // If it is a SAML token, then include a KeyIdentifier.
                final String valueType = tokenInfo.get(SAML_VALUE_TYPE);
                final String assertionId = tokenInfo.get(SAML_ASSERTION_ID);

                rstrBuilder.append("<wsse:KeyIdentifier");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(valueType).append("\"");
                }
                rstrBuilder.append(">").append(assertionId).append("</wsse:KeyIdentifier>\n");
            }
            rstrBuilder
                .append("</wsse:SecurityTokenReference>\n")
                .append("</wst:RequestedUnattachedReference>\n");
        }

        // Build shared secret (Note: it is only for SCT and not for SAML token.)
        final int sctKeySize;
        if (isSCT) {
            // Check if the session does exist and is not expired before using it.
            final String sessionId = tokenInfo.get(SCT_IDENTIFIER);
            final SecureConversationSession session = scContextManager.getSession(sessionId);
            final String message = "Session not found '"+sessionId+"'";
            if ( session == null ) {
                RstSoapMessageProcessor.generateSoapFaultResponse(
                    context,
                    parameters,
                    getRstrResponseVariable(),
                    RstSoapMessageProcessor.WST_FAULT_CODE_EXPIRED_DATA,
                    message
                );

                auditor.logAndAudit(AssertionMessages.STS_EXPIRED_SC_SESSION, message);
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);    
            }

            sctKeySize = session.getKeySize();

            // Build RequestedProofToken
            rstrBuilder.append("<wst:RequestedProofToken>\n");

            // The shared secret in RequestedProofToken could be an EncryptedKey element, a BinarySecret element, or ComputedKey element
            if ( session.hasEntropy() ) {
                String psha1AlgUri;
                String wstNS = parameters.get(RstSoapMessageProcessor.WST_NS);  // wstNS must not be null, since it has been checked.
                if (SoapConstants.WST_NAMESPACE.equals(wstNS)) { // for WS-Trust pre 1.2
                    psha1AlgUri = SoapConstants.P_SHA1_ALG_URI;
                } else if (SoapConstants.WST_NAMESPACE2.equals(wstNS)) { // for WS-Trust 1.2
                    psha1AlgUri = SoapConstants.P_SHA1_ALG_URI2;
                } else { // SoapConstants.WST_NAMESPACE3.equals(wstNS) or SoapConstants.WST_NAMESPACE4.equals(wstNS))  // for WS-Trust 1.3 and post 1.3 
                    psha1AlgUri = SoapConstants.P_SHA1_ALG_URI3;
                }
                rstrBuilder.append("<wst:ComputedKey>").append(psha1AlgUri).append("</wst:ComputedKey>\n");
            } else {
                final X509Certificate clientCert = getClientCertificate( context, parameters );
                String secretXml;
                try {
                    String keyEncAlg = Boolean.parseBoolean(parameters.get(RstSoapMessageProcessor.HAS_KEY_ENCRYPTION_ALGORITHM))?
                        parameters.get(RstSoapMessageProcessor.KEY_ENCRYPTION_ALGORITHM) : SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO;

                    secretXml = clientCert != null?
                        produceEncryptedKeyXml(session.getSharedSecret(), clientCert, wsseNS, keyEncAlg) :
                        produceBinarySecretXml(session.getSharedSecret(), parameters.get(RstSoapMessageProcessor.WST_NS));
                } catch (GeneralSecurityException e) {
                    RstSoapMessageProcessor.generateSoapFaultResponse(
                        context,
                        parameters,
                        getRstrResponseVariable(),
                        RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_REQUEST,
                        "Request invalid"
                    );

                    auditor.logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Response encryption failure"}, e);
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }

                rstrBuilder.append(secretXml).append("\n");
            }
            rstrBuilder.append("</wst:RequestedProofToken>\n");

            // Build Entropy
            if ( session.hasEntropy() ) {
                String wsuId = "uuid-" + UUID.randomUUID().toString();
                String secret = HexUtils.encodeBase64(session.getServerEntropy(), true);
                String typeNonce = parameters.get(RstSoapMessageProcessor.WST_NS) + "/Nonce";
                if ( SoapUtil.WST_NAMESPACE.equals( parameters.get(RstSoapMessageProcessor.WST_NS) ) ) {
                    typeNonce = SoapConstants.WST_BINARY_SECRET_NONCE_TYPE_URI;
                }
                rstrBuilder
                    .append("<wst:Entropy>\n")
                    .append("<wst:BinarySecret Type=\"").append(typeNonce).append("\" wsu:Id=\"").append(wsuId).append("\">")
                    .append(secret).append("</wst:BinarySecret>\n")
                    .append("</wst:Entropy>\n");
            }
        } else {
            sctKeySize = 0;
        }

        // Build Lifetime
        if (assertion.isIncludeLifetime()) {
            long createdTime = System.currentTimeMillis(); // Unit: milliseconds
            long expiryTime = createdTime + assertion.getLifetime(); // Unit: milliseconds
            Calendar exp = Calendar.getInstance();

            rstrBuilder.append("<wst:Lifetime>\n");

            exp.setTimeInMillis(createdTime);
            rstrBuilder.append("<wsu:Created>").append(ISO8601Date.format(exp.getTime())).append("</wsu:Created>\n");

            exp.setTimeInMillis(expiryTime);
            rstrBuilder.append("<wsu:Expires>").append(ISO8601Date.format(exp.getTime())).append("</wsu:Expires>\n");

            rstrBuilder.append("</wst:Lifetime>\n");
        }

        // Build KeySize (Note: this is only for SCT and not for SAML token.)
        if (isSCT && assertion.isIncludeKeySize()) {
            rstrBuilder.append("<wst:KeySize>").append(sctKeySize).append("</wst:KeySize>\n");
        }

        // Finish up
        rstrBuilder.append("</wst:RequestSecurityTokenResponse>");

        return rstrBuilder.toString();
    }

    private X509Certificate getClientCertificate( final PolicyEnforcementContext context,
                                                  final Map<String, String> parameters ) throws PolicyAssertionException {
        final AddWssEncryptionContext encryptionContext;
        try {
           encryptionContext = buildEncryptionContext( context );
        } catch ( AddWssEncryptionSupport.MultipleTokensException e ) {
            RstSoapMessageProcessor.generateSoapFaultResponse(
                context,
                parameters,
                getRstrResponseVariable(),
                RstSoapMessageProcessor.WST_FAULT_CODE_INVALID_REQUEST,
                "Request invalid"
            );

            auditor.logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Response encryption token not found (multiple tokens)"}, e);
            throw new AssertionStatusException( AssertionStatus.FALSIFIED);
        }

        return encryptionContext != null && encryptionContext.isCertificate() ? encryptionContext.getCertificate() : null;
    }

    @Override
    protected boolean isResponse() {
        // Assume we are always building a response message.
        return true;
    }

    @Override
    public Audit getAuditor() {
        return auditor;
    }

    // This method is modified from the method "produceBinarySecretXml" in TokenServiceImpl.
    private String produceBinarySecretXml(final byte[] sharedSecret, final String wstNS) {
        StringBuilder output = new StringBuilder();
        output.append("<wst:BinarySecret Type=\"").append(wstNS).append("/SymmetricKey" + "\">");
        output.append(HexUtils.encodeBase64(sharedSecret, true));
        output.append("</wst:BinarySecret>");
        return output.toString();
    }

    // This method is modified from the method "produceEncryptedKeyXml" in TokenServiceImpl.
    private String produceEncryptedKeyXml( final byte[] sharedSecret,
                                           final X509Certificate requestorCert,
                                           final String wsseNS,
                                           final String keyEncryptionAlgorithm ) throws GeneralSecurityException {
        final StringBuilder encryptedKeyXml = new StringBuilder();
        final boolean oaep = !SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO.equals(keyEncryptionAlgorithm);
        // Key info and all
        final String wsuId = "uuid-" + UUID.randomUUID().toString();
        encryptedKeyXml.append("<xenc:EncryptedKey wsu:Id=\"").append(wsuId).append("\" xmlns:xenc=\"").append(SoapConstants.XMLENC_NS)
            .append("\"><xenc:EncryptionMethod Algorithm=\"").append(keyEncryptionAlgorithm).append("\"");

        if ( oaep ) {
            encryptedKeyXml.append( "><DigestMethod xmlns=\"" ).append(SoapConstants.DIGSIG_URI).append("\" Algorithm=\"")
                    .append(SoapConstants.DIGSIG_URI).append("sha1").append("\"/></xenc:EncryptionMethod>");
        } else {
            encryptedKeyXml.append( "/>" );
        }

        // append ski if applicable
        final String recipSkiB64 = CertUtils.getSki(requestorCert);
        if (recipSkiB64 != null) {
            // add the ski
            String skiRef = "<wsse:SecurityTokenReference xmlns:wsse=\"" + wsseNS + "\">" +
                "<wsse:KeyIdentifier ValueType=\"" + SoapConstants.VALUETYPE_SKI + "\">" +
                recipSkiB64 +
                "</wsse:KeyIdentifier>" +
                "</wsse:SecurityTokenReference>";

            encryptedKeyXml.append("<KeyInfo xmlns=\"").append(SoapConstants.DIGSIG_URI).append("\">");
            encryptedKeyXml.append(skiRef);
            encryptedKeyXml.append("</KeyInfo>");
        } else {
            // add a full cert ?
        }
        encryptedKeyXml.append("<xenc:CipherData>" +
            "<xenc:CipherValue>");
        final String encryptedKeyValue = oaep ?
                HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaOaepMGF1SHA1(sharedSecret, requestorCert, requestorCert.getPublicKey(), new byte[0]), true) :
                HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(sharedSecret, requestorCert, requestorCert.getPublicKey()), true);

        encryptedKeyXml.append(encryptedKeyValue);
        encryptedKeyXml.append("</xenc:CipherValue>" +
            "</xenc:CipherData>" +
            "</xenc:EncryptedKey>");

        return encryptedKeyXml.toString();
    }
}