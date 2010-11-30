package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.CancelSecurityContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.secureconversation.NoSuchSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SessionExpiredException;
import com.l7tech.server.util.RstSoapMessageProcessor;
import com.l7tech.util.SoapConstants;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class ServerCancelSecurityContext extends AbstractMessageTargetableServerAssertion<CancelSecurityContext> {
    private static final Logger logger = Logger.getLogger(ServerCancelSecurityContext.class.getName());

    private final SecureConversationContextManager secureConversationContextManager;
    private final Auditor auditor;

    public ServerCancelSecurityContext(CancelSecurityContext assertion, ApplicationContext springContext) {
        super(assertion, assertion);
        auditor = new Auditor(this, springContext, logger);
        secureConversationContextManager = springContext.getBean("secureConversationContextManager", SecureConversationContextManager.class);
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

        // From this point, start to check if the RST SOAP message is a well-formatted and its semantics are correct or not.

        Map<String, String> rstParameters = RstSoapMessageProcessor.getRstParameters(message);
        if (rstParameters.containsKey(RstSoapMessageProcessor.ERROR)) {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:invalid_soap_message", rstParameters.get(RstSoapMessageProcessor.ERROR));
            return AssertionStatus.BAD_REQUEST;
        }

        // Check WS-Addressing Action
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_WS_ADDRESSING_ACTION))) {
            String action = rstParameters.get(RstSoapMessageProcessor.WS_ADDRESSING_ACTION);
            if (action == null || action.trim().isEmpty()) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wsa_action_value_not_specified", "The value of WS-Addressing Action is not specified in the RST/Cancel message.");
                return AssertionStatus.BAD_REQUEST;
            } else if (! SoapConstants.WSC_RST_CANCEL_ACTION_LIST.contains(action)) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wsa_action_value_not_supported", "The value of WS-Addressing Action is not supported.");
                return AssertionStatus.BAD_REQUEST;
            }
        } else {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_wsa_action", "There is no WS-Addressing Action element in the RST/Cancel message.");
            return AssertionStatus.BAD_REQUEST;
        }

        String targetIdentifier;

        // Check RequestSecurityToken
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_REQUEST_SECURITY_TOKEN))) {
            //  Check RequestType (Note: RequestType is mandatory in WS-Trust.)
            if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_REQUEST_TYPE))) {

                String requestType = rstParameters.get(RstSoapMessageProcessor.REQUEST_TYPE);
                if (requestType == null || requestType.trim().isEmpty()) {
                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wst_cancel_requesttype_value_not_specified", "The value of WS-Trust Cancel RequestType is not specified.");
                    return AssertionStatus.BAD_REQUEST;
                } else if (! SoapConstants.WST_RST_CANCEL_REQUEST_TYPE_LIST.contains(requestType)) {
                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:wst_cancel_requesttype_value_not_supported", "The value of WS-Trust Cancel RequestType is not supported.");
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
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_request_type", "There is no RequestToken element in the RST/Cancel message.");
                return AssertionStatus.BAD_REQUEST;
            }

            // Check CancelTarget (Note: CancelTarget is mandatory in WS-Trust.)
            // If all validations passed, the target identifier will be obtained.
            if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_CANCEL_TARGET))) {
                // Check SecurityTokenReference
                if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_SECURITY_TOKEN_REFERENCE))) {
                    // Check Reference
                    if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_REFERENCE))) {
                        String valueType = rstParameters.get(RstSoapMessageProcessor.REFERENCE_ATTR_VALUE_TYPE);
                        if (valueType != null && !valueType.isEmpty() && !SoapConstants.WSC_RST_SCT_TOKEN_TYPE_LIST.contains(valueType)) {
                            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:canceled_token_type_not_supported", "The type of the canceled token is not supported.");
                            return AssertionStatus.BAD_REQUEST;
                        }

                        targetIdentifier = rstParameters.get(RstSoapMessageProcessor.REFERENCE_ATTR_URI);
                        if (targetIdentifier == null || targetIdentifier.trim().isEmpty()) {
                            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:uri_in_reference_not_specified", "The attribute URI of Reference is not specified in SecurityTokenReference.");
                            return AssertionStatus.BAD_REQUEST;
                        }
                    } else {
                        RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_reference", "There is no Reference element in the RST/Cancel message.");
                        return AssertionStatus.BAD_REQUEST;
                    }
                } else {
                    RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_str", "There is no SecurityTokenReference element in the RST/Cancel message.");
                    return AssertionStatus.BAD_REQUEST;
                }
            } else {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_cancel_target", "There is no CancelTarget element in the RST/Cancel message.");
                return AssertionStatus.BAD_REQUEST;
            }
        } else {
            RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:rst_message_missing_rst", "There is no RequestSecurityToken element in the RST/Cancel message.");
            return AssertionStatus.BAD_REQUEST;
        }

        // At this point, everything is fine and ready to cancel a SecurityContextToken.
        try {
            secureConversationContextManager.cancelSession(targetIdentifier);
        } catch (NoSuchSessionException e) {
            if (assertion.isFailIfNotExist()) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:no.such.session", e.getMessage());
                return AssertionStatus.BAD_REQUEST;
            } else {
                logger.warning(e.getMessage());
            }
        } catch (SessionExpiredException e) {
            if (assertion.isFailIfExpired()) {
                RstSoapMessageProcessor.setAndLogSoapFault(context, "l7:session_expired", e.getMessage());
                return AssertionStatus.BAD_REQUEST;
            } else {
                logger.warning(e.getMessage());
            }
        }

        return AssertionStatus.NONE;
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }
}