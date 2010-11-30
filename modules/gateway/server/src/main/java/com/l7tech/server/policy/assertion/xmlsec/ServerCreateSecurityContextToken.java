package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.CreateSecurityContextToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.secureconversation.DuplicateSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.util.RstSoapMessageProcessor;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SoapConstants;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class ServerCreateSecurityContextToken extends AbstractMessageTargetableServerAssertion<CreateSecurityContextToken> {
    private static final Logger logger = Logger.getLogger(ServerCreateSecurityContextToken.class.getName());

    private final SecureConversationContextManager scContextManager;
    private final Auditor auditor;

    public ServerCreateSecurityContextToken(CreateSecurityContextToken assertion, ApplicationContext springContext) {
        super(assertion, assertion);
        auditor = new Auditor(this, springContext, logger);
        scContextManager = springContext.getBean("secureConversationContextManager", SecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context,
                                             Message message,
                                             String messageDescription,
                                             AuthenticationContext authContext) throws IOException, PolicyAssertionException {

        // Check if the credentials are provided and proven in the message (request, response, or context variable).
        AuthenticationResult authenticationResult = authContext.getLastAuthenticationResult();
        if (authenticationResult == null) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:no_authentication_result", "The target message does not provide an authentication info.");
            return AssertionStatus.AUTH_FAILED;
        }

        LoginCredentials loginCredentials = null;
        for (LoginCredentials cred: authContext.getCredentials()) {
            if (authenticationResult.matchesSecurityToken(cred.getSecurityToken())) {
                loginCredentials = cred;
                break;
            }
        }

        if (loginCredentials == null) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:no_matched_credentials", "Credentials not found for the authenticated user.");
            return AssertionStatus.AUTH_FAILED;
        }

        // Check if the RST SOAP message is a well-formatted or not
        Map<String, String> rstParameters = RstSoapMessageProcessor.getRstParameters(message);
        if (rstParameters.containsKey(RstSoapMessageProcessor.ERROR)) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:invalid_soap_message", rstParameters.get(RstSoapMessageProcessor.ERROR));
            return AssertionStatus.BAD_REQUEST;
        }

        // Check if the message semantics are correct or not.
        // First check WS-Addressing Action
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_WS_ADDRESSING_ACTION))) {
            String action = rstParameters.get(RstSoapMessageProcessor.WS_ADDRESSING_ACTION);
            if (action == null || action.trim().isEmpty()) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wsa_action_value_not_specified", "The value of WS-Addressing Action is not specified in the RST/SCT request message.");
                return AssertionStatus.BAD_REQUEST;
            } else if (! SoapConstants.WSC_RST_SCT_ACTION_LIST.contains(action)) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wsa_action_value_not_supported", "The value of WS-Addressing Action is not supported.");
                return AssertionStatus.BAD_REQUEST;
            }
        } else {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_wsa_action", "There is no WS-Addressing Action element in the RST/SCT request message.");
            return AssertionStatus.BAD_REQUEST;
        }

        // Check RequestSecurityToken
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_REQUEST_SECURITY_TOKEN))) {

            //  Check RequestType (Note: RequestType is mandatory in WS-Trust.)
            if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_REQUEST_TYPE))) {

                String requestType = rstParameters.get(RstSoapMessageProcessor.REQUEST_TYPE);
                if (requestType == null || requestType.trim().isEmpty()) {
                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wst_issue_requesttype_value_not_specified", "The value of WS-Trust Issue RequestType is not specified.");
                    return AssertionStatus.BAD_REQUEST;
                } else if (! SoapConstants.WST_RST_ISSUE_REQUEST_TYPE_LIST.contains(requestType)) {
                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wst_issue_requesttype_value_not_supported", "The value of WS-Trust Issue RequestType is not supported.");
                    return AssertionStatus.BAD_REQUEST;
                }

                // Check TokenType (Note: TokenType is optional in WS-Trust)
                String tokenType = rstParameters.get(RstSoapMessageProcessor.TOKEN_TYPE);
                if (tokenType != null && !tokenType.trim().isEmpty() &&
                    !SoapConstants.WSC_RST_SCT_TOKEN_TYPE_LIST.contains(tokenType)) {

                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wst_tokentype_value_not_supported", "The value of WS-Trust TokenType is not supported.");
                    return AssertionStatus.BAD_REQUEST;
                }
            } else {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_request_type", "There is no RequestToken element in the RST/SCT request message.");
                return AssertionStatus.BAD_REQUEST;
            }
        } else {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_rst", "There is no RequestSecurityToken element in the RST/SCT request message.");
            return AssertionStatus.BAD_REQUEST;
        }

        // At this point, everything is fine and ready to create a SecurityContextToken.
        String wsuId = "uuid:" + UUID.randomUUID().toString();
        String identifier = "urn:uuid:" + UUID.randomUUID().toString();

        String wscNS = rstParameters.get(RstSoapMessageProcessor.WSC_NS);
        if (wscNS == null || wscNS.trim().isEmpty()) {
            // Get the namespace of WS-Trust first then retrieve the namespace of WS-Secure Conversation according to the namespace of WS-Trust.
            String wstNS = rstParameters.get(RstSoapMessageProcessor.WST_NS);

            // Check if the namespace is specified in the RST/SCT request message.  If not specified, fail this assertion.
            if (wstNS == null || wstNS.trim().isEmpty()) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:missing_ws-trust_namespace", "The namespace of WS-Trust is not specified in the RST/SCT request message.");
                return AssertionStatus.BAD_REQUEST;
            }

            wscNS = deriveWscNamespace(wstNS);
        }

        String wsuNS = rstParameters.get(RstSoapMessageProcessor.WSU_NS);
        if (wsuNS == null || wsuNS.trim().isEmpty()) {
            wsuNS = SoapConstants.WSU_NAMESPACE;
        }

        String tokenIssued = buildSCT(wscNS, wsuNS, wsuId, identifier);

        // Create a context variable, issuedSCT
        String variableFullName = assertion.getVariablePrefix() + "." + CreateSecurityContextToken.VARIABLE_ISSUED_SCT;
        context.setVariable(variableFullName, tokenIssued);

        // Create a new sc session and cache it
        SecureConversationSession newSession;
        try {
            newSession = scContextManager.createContextForUser(
                identifier,
                authenticationResult.getUser(),
                loginCredentials,
                rstParameters.get(RstSoapMessageProcessor.WSC_NS),
                assertion.getLifetime()
            );
        } catch (DuplicateSessionException e) {
            throw new RuntimeException(e);
        }

        // Check if there exists Entropy in the RST message
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_ENTROPY))) {
            // todo: check if binary secret is only one element in entropy
            if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_BINARY_SECRET))) {
                String type = rstParameters.get(RstSoapMessageProcessor.BINARY_SECRET_ATTR_TYPE);
                if (type != null && type.endsWith("Nonce")) {
                    String nonce = rstParameters.get(RstSoapMessageProcessor.BINARY_SECRET);
                    newSession.setClientEntropy(HexUtils.decodeBase64(nonce));
                }
            }
        }

        // Check if there exists Entropy in the RST message

        String keySize =rstParameters.get(RstSoapMessageProcessor.KEY_SIZE);
        if (keySize != null && !keySize.trim().isEmpty()) {
            try {
                int size = Integer.parseInt(keySize); // Unit: bits
                newSession.setKeySize(size);
            } catch (NumberFormatException e) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:invalid_key_size", "The key size is not a integer in the RST/SCT request message.");
                return AssertionStatus.BAD_REQUEST;
            }
        }

        return AssertionStatus.NONE;
    }

    /**
     * Derive the namespace of WS-Secure Conversation from a given namespace of WS-Trust.
     * @param wstNamespace: the namespace of WS-Trust
     * @return a corresponding namespace of WS-Secure Conversation.
     */
    private String deriveWscNamespace(String wstNamespace) {
        if (wstNamespace == null) throw new IllegalArgumentException("WS-Trust Namespace must be required.");

        String wscNamespace;
        if (SoapConstants.WST_NAMESPACE1.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE;
        } else if (SoapConstants.WST_NAMESPACE2.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE2;
        } else if (SoapConstants.WST_NAMESPACE3.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE3;
        } else if (SoapConstants.WST_NAMESPACE4.equals(wstNamespace)) {
            wscNamespace = SoapConstants.WSSC_NAMESPACE3;
        } else {
            throw new IllegalArgumentException("Invalid WS-Trust namespace, " + wstNamespace);
        }

        return wscNamespace;
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    private String buildSCT(String wscNS, String wsuNS, String wsuId, String identifier) {
        StringBuilder sb = new StringBuilder()
            .append("<wsc:SecurityContextToken ")
            .append("wsu:Id=\"").append(wsuId).append("\" ")
            .append("xmlns:wsc=\"").append(wscNS).append("\" ")
            .append("xmlns:wsu=\"").append(wsuNS).append("\">\n")
            .append("\t<wsc:Identifier>").append(identifier).append("</wsc:Identifier>\n")
            .append("</wsc:SecurityContextToken>");

        return sb.toString();
    }
}