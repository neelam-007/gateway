package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.CreateSecurityContextToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.server.util.RstSoapMessageProcessor;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class ServerCreateSecurityContextToken extends AbstractMessageTargetableServerAssertion<CreateSecurityContextToken> {
    private static final Logger logger = Logger.getLogger(ServerCreateSecurityContextToken.class.getName());

    private final SecureConversationContextManager scContextManager;
    private final Auditor auditor;

    public ServerCreateSecurityContextToken( final CreateSecurityContextToken assertion,
                                             final BeanFactory factory ) {
        super(assertion, assertion);
        auditor = factory instanceof ApplicationContext?
                new Auditor(this, (ApplicationContext)factory, logger) :
                new LogOnlyAuditor(logger);
        scContextManager = factory.getBean("secureConversationContextManager", SecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context,
                                             Message message,
                                             String messageDescription,
                                             AuthenticationContext authContext) throws IOException, PolicyAssertionException {

        // Get all related info from the target SOAP message.  RstSoapMessageProcessor checks the syntax and the semantics of the target SOAP message.
        final Map<String, String> rstParameters = RstSoapMessageProcessor.getRstParameters(message, true);
        if (rstParameters.containsKey(RstSoapMessageProcessor.ERROR)) {
            auditor.logAndAudit(AssertionMessages.STS_INVALID_RST_REQUEST, rstParameters.get(RstSoapMessageProcessor.ERROR));
            return AssertionStatus.BAD_REQUEST;
        }

        // Check if the credentials are provided and proven in the message (request, response, or context variable).
        AuthenticationResult authenticationResult = authContext.getLastAuthenticationResult();
        if (authenticationResult == null) {
            auditor.logAndAudit(AssertionMessages.STS_AUTHENTICATION_FAILURE, "The target message does not contain any authentication information.");
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
            auditor.logAndAudit(AssertionMessages.STS_AUTHENTICATION_FAILURE, "Credentials not found for the authenticated user.");
            return AssertionStatus.AUTH_FAILED;
        }

        // At this point, everything is fine since the validation is done.  It is ready to create a SecurityContextToken.
        String wscNS = rstParameters.get(RstSoapMessageProcessor.WSC_NS);
        if (wscNS == null || wscNS.trim().isEmpty()) {
            // Get the namespace of WS-Trust and then retrieve the namespace of WS-Secure Conversation according to the namespace of WS-Trust.
            String wstNS = rstParameters.get(RstSoapMessageProcessor.WST_NS);

            // Note: no need to check if the WS-Trust namespace is null, since it has been checked in RstSoapMessageProcessor.
            wscNS = deriveWscNamespace(wstNS);
        }

        String wsuNS = rstParameters.get(RstSoapMessageProcessor.WSU_NS);
        if (wsuNS == null || wsuNS.trim().isEmpty()) {
            wsuNS = SoapConstants.WSU_NAMESPACE;
        }

        // Check if there exists Entropy in the RST message
        byte[] clientEntropy = null;
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_ENTROPY))) {
            // todo: check if binary secret is only one element in entropy
            if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_BINARY_SECRET))) {
                String type = rstParameters.get(RstSoapMessageProcessor.BINARY_SECRET_ATTR_TYPE);
                if (type != null && type.endsWith("Nonce")) {
                    String nonce = rstParameters.get(RstSoapMessageProcessor.BINARY_SECRET);
                    clientEntropy = HexUtils.decodeBase64(nonce);
                }
            }
        }

        // Check if there exists KeySize in the RST message
        int keySize = assertion.getKeySize();
        if (Boolean.parseBoolean(rstParameters.get(RstSoapMessageProcessor.HAS_KEY_SIZE))) {
            final String keySizeText =rstParameters.get(RstSoapMessageProcessor.KEY_SIZE);

            // KeySize has been validated already in RstSoapMessageProcessor
            try {
                int requestKeySize = Integer.parseInt(keySizeText); // Unit: bits
                if ( requestKeySize > keySize ) {
                    keySize = requestKeySize;   
                }
            } catch ( NumberFormatException nfe ) {
                logger.info( "Ignoring invalid key size '"+keySizeText+"'." );
            }
        }

        // Create a new sc session and cache it
        final SecureConversationSession newSession;
        try {
            newSession = scContextManager.createContextForUser(
                authenticationResult.getUser(),
                loginCredentials,
                rstParameters.get(RstSoapMessageProcessor.WSC_NS),
                getSessionDuration(),
                clientEntropy,
                keySize
            );
        } catch ( SessionCreationException e ) {
            //noinspection ThrowableResultOfMethodCallIgnored
            auditor.logAndAudit( AssertionMessages.STS_TOKEN_ISSUE_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FALSIFIED;
        }

        final String tokenIssued = buildSCT(wscNS, wsuNS, newSession.getIdentifier());

        // Create a context variable, issuedSCT
        final String variableFullName = assertion.getVariablePrefix() + "." + CreateSecurityContextToken.VARIABLE_ISSUED_SCT;
        context.setVariable(variableFullName, tokenIssued);

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

    private String buildSCT( final String wscNS,
                             final String wsuNS,
                             final String identifier ) {
        StringBuilder sb = new StringBuilder()
            .append("<wsc:SecurityContextToken ")
            .append("wsu:Id=\"").append( SoapUtil.generateUniqueId( "SecurityContextToken", 1 )).append("\" ")
            .append("xmlns:wsc=\"").append(wscNS).append("\" ")
            .append("xmlns:wsu=\"").append(wsuNS).append("\">\n")
            .append("\t<wsc:Identifier>").append(identifier).append("</wsc:Identifier>\n")
            .append("</wsc:SecurityContextToken>");

        return sb.toString();
    }

    private long getSessionDuration() {
        if (assertion.isUseSystemDefaultSessionDuration()) {
            return scContextManager.getDefaultSessionDuration();
        } else {
            return assertion.getLifetime();
        }
    }
}