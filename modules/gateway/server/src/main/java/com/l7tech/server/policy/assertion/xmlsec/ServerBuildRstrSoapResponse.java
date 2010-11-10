package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.BuildRstrSoapResponse;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.X509BinarySecurityTokenImpl;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.NoSuchSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionExpiredException;
import com.l7tech.server.util.RstSoapMessageProcessor;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: ghuang
 */
public class ServerBuildRstrSoapResponse extends AbstractMessageTargetableServerAssertion<BuildRstrSoapResponse> {
    private static final Logger logger = Logger.getLogger(ServerBuildRstrSoapResponse.class.getName());
    private static final int DEFAULT_KEY_SIZE = 256;

    private static final String IS_SCT = "token.info.is.sct";
    private static final String TOKEN_XML = "token.info.token.xml.content";
    private static final String SCT_WSU_ID = "sct.info.wsu.id";
    private static final String SCT_IDENTIFIER = "sct.info.token.identifier";
    private static final String SAML_ASSERTION_ID = "saml.info.assertion.id";
    private static final String SAML_VALUE_TYPE = "saml.info.value.type";

    private final SecureConversationContextManager scContextManager;
    private final Auditor auditor;
    private final String[] variablesUsed;

    public ServerBuildRstrSoapResponse(BuildRstrSoapResponse assertion, ApplicationContext springContext) {
        super(assertion, assertion);
        auditor = new Auditor(this, springContext, logger);
        variablesUsed = assertion.getVariablesUsed();
        scContextManager = springContext.getBean("secureConversationContextManager", SecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context,
                                             Message message,
                                             String messageDescription,
                                             AuthenticationContext authContext) throws IOException, PolicyAssertionException {

        // Check if the RST SOAP inbound message is a well-formatted or not and also get all namespaces if if is well-formatted message.
        Map<String, String> rstParameters = RstSoapMessageProcessor.getRstParameters(message);
        if (rstParameters.containsKey(RstSoapMessageProcessor.ERROR)) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:invalid_soap_message", rstParameters.get(RstSoapMessageProcessor.ERROR));
            return AssertionStatus.BAD_REQUEST;
        }

        // Check the semantics of the RST SOAP inbound message
        // WS-Trust namespace is required
        String wstNS = rstParameters.get(RstSoapMessageProcessor.WST_NS);
        if (wstNS == null || wstNS.trim().isEmpty()) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:ws.trust_namespace.not.specified", "The namespace of WS-Trust is not specified in the RST message.");
            return AssertionStatus.BAD_REQUEST;
        }

        // WS-Addressing namespace is required
        String wsaNS = rstParameters.get(RstSoapMessageProcessor.WSA_NS);
        if (wsaNS == null || wsaNS.trim().isEmpty()) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:ws.addressing_namespace.not.specified", "The namespace of WS-Addressing is not specified in the RST message.");
            return AssertionStatus.BAD_REQUEST;
        } else {
            // Set the context variable for WS-Addressing namespace
            context.setVariable(assertion.getVariablePrefix() + "." + BuildRstrSoapResponse.VARIABLE_WSA_NAMESPACE, wsaNS);
        }

        // WS-Addressing Action is required.
        String rstWsaAction;
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_WS_ADDRESSING_ACTION))) {
            rstWsaAction = rstParameters.get(RstSoapMessageProcessor.WS_ADDRESSING_ACTION);
            if (rstWsaAction == null || rstWsaAction.trim().isEmpty()) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:ws.addressing.action.not.specified", "The value of WS-Addressing Action is not specified in the RST message.");
                return AssertionStatus.BAD_REQUEST;
            }
        } else {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:no.ws.addressing.action", "There is no WS-Addressing Action in the RST message.");
            return AssertionStatus.BAD_REQUEST;
        }

        // Get the RSTR content depending on Binding type such as Issuance Binding or Cancel Binding.
        String rstrXml;
        Map<String, String> tokenInfo = new HashMap<String, String>(6);
        try {
            if (assertion.isResponseForIssuance()) {
                AssertionStatus status = getTokenInfo(context, tokenInfo);

                if (status != AssertionStatus.NONE) return status;
                rstrXml = generateRstrElement(context, message, rstParameters, tokenInfo);
            } else {
                rstrXml = generateRstrElement(context, message, rstParameters, null); // no need of token info
            }
        } catch (NoSuchSessionException e) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:session.does.not.exist", e.getMessage());
            return AssertionStatus.BAD_REQUEST;
        } catch (SessionExpiredException e) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:session.expired", e.getMessage());
            return AssertionStatus.BAD_REQUEST;
        }

        // Build a RSTR SOAP response message
        String rstrSoapResponse = buildRstrSoapResponse(rstParameters, rstrXml);

        // Set the context variable for rstrResponse
        context.setVariable(assertion.getVariablePrefix() + "." + BuildRstrSoapResponse.VARIABLE_RSTR_RESPONSE, new Message(XmlUtil.stringAsDocument(rstrSoapResponse)));

        // Set the context variable for RSTR WS-Addressing Action
        String rstrWsaAction;
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
                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:ws.addressing_action.value.not.supported", "The value of WS-Addressing Action for SCT is not supported.");
                    return AssertionStatus.BAD_REQUEST;
                }
            } else {     // SAML Token
                if (SoapConstants.WST_RST_ISSUE_ACTION.equals(rstWsaAction)) {
                    rstrWsaAction = SoapConstants.WST_RSTR_ISSUE_ACTION;
                } else if (SoapConstants.WST_RST_ISSUE_ACTION2.equals(rstWsaAction)) {
                    rstrWsaAction = SoapConstants.WST_RSTR_ISSUE_ACTION2;
                } else if (SoapConstants.WST_RST_ISSUE_ACTION3.equals(rstWsaAction)) {
                    rstrWsaAction = SoapConstants.WST_RSTR_ISSUE_ACTION3;
                } else {
                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:ws.addressing_action.value.not.supported", "The value of WS-Addressing Action for Issue is not supported.");
                    return AssertionStatus.BAD_REQUEST;
                }
            }
        } else {
            if (SoapConstants.WSC_RST_CANCEL_ACTION.equals(rstWsaAction)) {
                rstrWsaAction = SoapConstants.WSC_RSTR_CANCEL_ACTION;
            } else if (SoapConstants.WSC_RST_CANCEL_ACTION2.equals(rstWsaAction)) {
                rstrWsaAction = SoapConstants.WSC_RSTR_CANCEL_ACTION2;
            } else {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:ws.addressing_action.value.not.supported", "The value of WS-Addressing Action for Cancel is not supported.");
                return AssertionStatus.BAD_REQUEST;
            }
        }
        context.setVariable(assertion.getVariablePrefix() + "." + BuildRstrSoapResponse.VARIABLE_RSTR_WSA_ACTION, rstrWsaAction);


        // End up with success
        return AssertionStatus.NONE;
    }

    private AssertionStatus getTokenInfo(PolicyEnforcementContext context, Map<String, String> tokenInfo) {
        // Get the token issued
        String tokenVariable = assertion.getTokenIssued();

        // Get the XML content of the issued token
        String tokenXml = ExpandVariables.process(tokenVariable, context.getVariableMap(variablesUsed, auditor), auditor);
        tokenInfo.put(TOKEN_XML, tokenXml);
        
        Document tokenDoc;
        try {
            tokenDoc = XmlUtil.stringToDocument(tokenXml);
        } catch (SAXException e) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:invalid_document", "The content of the issued token has invalid document format.");
            return AssertionStatus.BAD_REQUEST;
        }

        Element root = (Element) tokenDoc.getFirstChild();
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
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:bad_token", "There are more than one attribute recognized as an ID attribute in the SecurityContextToken element.");
                return AssertionStatus.BAD_TOKEN;
            }
            tokenInfo.put(SCT_WSU_ID, sctWsuId);

            try {
                // Set Identifier
                tokenInfo.put(SCT_IDENTIFIER, DomUtils.findExactlyOneChildElementByName(root, rootNS, "Identifier").getTextContent());
            } catch (TooManyChildElementsException e) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:bad_token", "There are more than one Identifier element in the SecurityContextToken element.");
                return AssertionStatus.BAD_TOKEN;
            } catch (MissingRequiredElementException e) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:bad_token", "There is no Identifier element in the SecurityContextToken element.");
                return AssertionStatus.BAD_TOKEN;
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
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:bad_token", "The SAML namespace is invalid.");
                return AssertionStatus.BAD_TOKEN;
            }
        }
        // The token is not recognizable.
        else {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:invalid_token_type", "The token issued is neither a SecurityContextToken nor a SAML token.");
            return AssertionStatus.BAD_TOKEN;
        }

        return AssertionStatus.NONE;
    }

    private String buildRstrSoapResponse(Map<String, String> parameters, String rstrElement) {
        StringBuilder soapMessageBuilder = new StringBuilder("<soap:Envelope xmlns:soap=\"").append(parameters.get(RstSoapMessageProcessor.SOAP_ENVELOPE_NS)).append("\">\n")
            .append("<soap:Body>\n")
            .append(rstrElement).append("\n")
            .append("</soap:Body>\n")
            .append("</soap:Envelope>");

        return soapMessageBuilder.toString();
    }

    /**
     * Generate a RequestSecurityTokenResponse element
     * @param context
     * @param targetMessage
     * @param parameters
     * @param tokenInfo
     * @return
     */
    private String generateRstrElement(PolicyEnforcementContext context,
                                       Message targetMessage,
                                       Map<String, String> parameters,
                                       Map<String, String> tokenInfo) throws NoSuchSessionException, SessionExpiredException {
        StringBuilder rstrBuilder = new StringBuilder();

        // Build RequestSecurityTokenResponse
        String wsuNS = parameters.get(RstSoapMessageProcessor.WSU_NS) == null? SoapConstants.WSU_NAMESPACE : parameters.get(RstSoapMessageProcessor.WSU_NS);
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
        String securityTokenXml = tokenInfo.get(TOKEN_XML);

        // Build TokenType
        boolean hasTokeType = Boolean.parseBoolean(parameters.get(RstSoapMessageProcessor.HAS_TOKEN_TYPE));
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
            String address = assertion.getAddressOfEPR();
            if (address != null) {
                String addressContent = ExpandVariables.process(address, context.getVariableMap(variablesUsed, auditor), auditor);

                if (assertion.isIncludeAppliesTo() && addressContent != null && !addressContent.trim().isEmpty()) {
                    String wsaNS = parameters.get(RstSoapMessageProcessor.WSA_NS);  // The WS-Addressing namespace must not be null.  It has been checked in doCheckRequest.
                    String wspNS = parameters.get(RstSoapMessageProcessor.WSP_NS);
                    if (wspNS == null || wspNS.trim().isEmpty()) {
                        wspNS = SoapConstants.WSP_NAMESPACE2;
                    }

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
        boolean isSCT = Boolean.parseBoolean(tokenInfo.get(IS_SCT));

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
                String sctWsuId = tokenInfo.get(SCT_WSU_ID);
                rstrBuilder.append("<wsse:Reference URI=\"#").append(sctWsuId).append("\"");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(parameters.get(RstSoapMessageProcessor.TOKEN_TYPE)).append("\"");
                }
                rstrBuilder.append("/>\n");
            } else { // If it is a SAML token, then include a KeyIdentifier.
                String valueType = tokenInfo.get(SAML_VALUE_TYPE);
                String assertionId = tokenInfo.get(SAML_ASSERTION_ID);

                rstrBuilder.append("<wsse:KeyIdentifier");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(valueType);
                }
                rstrBuilder.append("\">").append(assertionId).append("</wsse:KeyIdentifier>\n");
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
                String sctIdentifier = tokenInfo.get(SCT_IDENTIFIER);

                rstrBuilder.append("<wsse:Reference URI=\"").append(sctIdentifier).append("\"");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(parameters.get(RstSoapMessageProcessor.TOKEN_TYPE)).append("\"");
                }
                rstrBuilder.append("/>\n");
            } else { // If it is a SAML token, then include a KeyIdentifier.
                String valueType = tokenInfo.get(SAML_VALUE_TYPE);
                String assertionId = tokenInfo.get(SAML_ASSERTION_ID);

                rstrBuilder.append("<wsse:KeyIdentifier");
                if (hasTokeType) {
                    rstrBuilder.append(" ValueType=\"").append(valueType);
                }
                rstrBuilder.append("\">").append(assertionId).append("</wsse:KeyIdentifier>\n");
            }
            rstrBuilder
                .append("</wsse:SecurityTokenReference>\n")
                .append("</wst:RequestedUnattachedReference>\n");
        }

        // Build shared secret (Note: it is only for SCT and not for SAML token.)
        String sessionId;
        SecureConversationSession session = null;

        if (isSCT) {
            // Check if the session does exist and is not expired before using it.
            sessionId = tokenInfo.get(SCT_IDENTIFIER);
            if (scContextManager.isExpiredSession(sessionId)) {
                throw new SessionExpiredException("The session (identifier = " + sessionId + ") is expired.");
            }
            
            //  The session validation is done, then get the session
            session = scContextManager.getSession(sessionId);

            // Build RequestedProofToken
            rstrBuilder.append("<wst:RequestedProofToken>\n");

            // The shared secret in RequestedProofToken could be an EncryptedKey element, a BinarySecret element, or ComputedKey element
            String bsAttrType = parameters.get(RstSoapMessageProcessor.BINARY_SECRET_ATTR_TYPE);
            if (Boolean.parseBoolean(parameters.get(RstSoapMessageProcessor.HAS_ENTROPY)) &&
                Boolean.parseBoolean(parameters.get(RstSoapMessageProcessor.HAS_BINARY_SECRET)) &&
                bsAttrType != null && bsAttrType.endsWith("Nonce")) {
                rstrBuilder.append("<wst:ComputedKey>").append(SoapConstants.P_SHA1_ALG_URI).append("</wst:ComputedKey>\n");
            } else {
                X509Certificate clientCert = getClientCert(targetMessage);
                String secretXml;
                try {
                    secretXml = clientCert != null?
                        produceEncryptedKeyXml(session.getSharedSecret(), clientCert) :
                        produceBinarySecretXml(session.getSharedSecret(), parameters.get(RstSoapMessageProcessor.WST_NS));
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException("Cannot produce an EncryptedKey for shared secret in a RSTR element.", e);
                }

                rstrBuilder.append(secretXml).append("\n");
            }
            rstrBuilder.append("</wst:RequestedProofToken>\n");

            // Build Entropy
            if (Boolean.parseBoolean(parameters.get(RstSoapMessageProcessor.HAS_ENTROPY))) {
                String wsuId = "uuid:" + UUID.randomUUID().toString();
                String secret = HexUtils.encodeBase64(session.getSharedSecret(), true);
                rstrBuilder
                    .append("<wst:Entropy>\n")
                    .append("<wst:BinarySecret Type=\"").append(bsAttrType).append("\" wsu:Id=\"").append(wsuId).append("\">")
                    .append(secret).append("</wst:BinarySecret>\n")
                    .append("</wst:Entropy>\n");
            }
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
            int clientKeySize = session.getKeySize();
            int serverKeySize = assertion.getKeySize();  // If it is 0, it means automatically using the client key size.
            int biggerKeySize = Math.max(clientKeySize, serverKeySize);

            if (biggerKeySize == 0) {
                biggerKeySize = DEFAULT_KEY_SIZE;
            }

            session.setKeySize(biggerKeySize);

            rstrBuilder.append("<wst:KeySize>").append(biggerKeySize).append("</wst:KeySize>\n");
        }

        // Finish up
        rstrBuilder.append("</wst:RequestSecurityTokenResponse>");

        return rstrBuilder.toString();
    }

    private X509Certificate getClientCert(Message targetMessage) {
        ProcessorResult wssOutput = targetMessage.getSecurityKnob().getProcessorResult();
        if (wssOutput == null) return null;
        
        SecurityToken[] tokens = wssOutput.getXmlSecurityTokens();
        X509Certificate clientCert = null;

        for (SecurityToken token : tokens) {
            if (token instanceof X509BinarySecurityTokenImpl) {
                X509BinarySecurityTokenImpl x509token = (X509BinarySecurityTokenImpl) token;
                if (x509token.isPossessionProved()) {
                    if (clientCert != null) {
                        String msg = "Request included more than one X509 security token whose key ownership was proven";
                        logger.log(Level.WARNING, msg);
                        throw new RuntimeException(msg);
                    }
                    clientCert = x509token.getCertificate();
                }
            }
        }

        return clientCert;
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    private String produceBinarySecretXml( final byte[] sharedSecret,
                                           final String trustns ) {
        StringBuilder output = new StringBuilder();
        output.append("<wst:BinarySecret Type=\"").append(trustns).append("/SymmetricKey" + "\">");
        output.append(HexUtils.encodeBase64(sharedSecret, true));
        output.append("</wst:BinarySecret>");
        return output.toString();
    }

    private String produceEncryptedKeyXml( final byte[] sharedSecret, final X509Certificate requestorCert ) throws GeneralSecurityException {
        StringBuilder encryptedKeyXml = new StringBuilder();
        // Key info and all
        encryptedKeyXml.append("<xenc:EncryptedKey wsu:Id=\"newProof\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">" +
            "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\" />");

        // append ski if applicable
        String recipSkiB64 = CertUtils.getSki(requestorCert);
        if (recipSkiB64 != null) {
            // add the ski
            String skiRef = "<wsse:SecurityTokenReference>" +
                "<wsse:KeyIdentifier ValueType=\"" + SoapConstants.VALUETYPE_SKI + "\">" +
                recipSkiB64 +
                "</wsse:KeyIdentifier>" +
                "</wsse:SecurityTokenReference>";

            encryptedKeyXml.append("<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");
            encryptedKeyXml.append(skiRef);
            encryptedKeyXml.append("</KeyInfo>");
        } else {
            // add a full cert ?
        }
        encryptedKeyXml.append("<xenc:CipherData>" +
            "<xenc:CipherValue>");
        String encryptedKeyValue = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(sharedSecret, requestorCert, requestorCert.getPublicKey()),
            true);
        encryptedKeyXml.append(encryptedKeyValue);
        encryptedKeyXml.append("</xenc:CipherValue>" +
            "</xenc:CipherData>" +
            "</xenc:EncryptedKey>");
        return encryptedKeyXml.toString();
    }
}