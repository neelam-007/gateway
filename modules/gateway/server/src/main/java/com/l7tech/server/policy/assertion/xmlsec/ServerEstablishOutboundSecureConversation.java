package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.EstablishOutboundSecureConversation;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.util.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class ServerEstablishOutboundSecureConversation extends AbstractMessageTargetableServerAssertion<EstablishOutboundSecureConversation> {
    private static final Logger logger = Logger.getLogger(ServerEstablishOutboundSecureConversation.class.getName());
    private static final String DEFAULT_SESSION_DURATION = "outbound.secureConversation.defaultSessionDuration";
    private static final String SESSION_PRE_EXPIRY_AGE = "outbound.secureConversationSession.preExpiryAge";
    private static final long MIN_SESSION_PRE_EXPIRY_AGE = 0;
    private static final long MAX_SESSION_PRE_EXPIRY_AGE = 2*60*60*1000; // 2 hours

    private final Auditor auditor;
    private final Config config;
    private final String[] variablesUsed;
    private final OutboundSecureConversationContextManager securityContextManager;

    public ServerEstablishOutboundSecureConversation(EstablishOutboundSecureConversation assertion, final BeanFactory factory) {
        super(assertion, assertion);

        auditor = factory instanceof ApplicationContext ?
            new Auditor(this, (ApplicationContext)factory, logger) :
            new LogOnlyAuditor(logger);
        config = validated(factory.getBean("serverConfig", Config.class));
        variablesUsed = assertion.getVariablesUsed();
        securityContextManager = factory.getBean("outboundSecureConversationContextManager", OutboundSecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        AuthenticationResult authenticationResult = authContext.getLastAuthenticationResult();
        if (authenticationResult == null) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "The target message does not contain any authentication information.");
            return AssertionStatus.AUTH_FAILED;
        }

        // 1. Get User  Login Credential
        LoginCredentials loginCredentials = null;
        for (LoginCredentials cred: authContext.getCredentials()) {
            if (authenticationResult.matchesSecurityToken(cred.getSecurityToken())) {
                loginCredentials = cred;
                break;
            }
        }
        if (loginCredentials == null) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "Credentials not found for the authenticated user.");
            return AssertionStatus.AUTH_FAILED;
        }

        // 2. Get Authenticated User
        final User user = authenticationResult.getUser();
        if (user == null) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "No authenticated user found in the target message.");
            return AssertionStatus.AUTH_FAILED;
        }

        // 3. Get Service URL (Note: no need to check if serviceUrl is null, since the GUI of the assertion dialog has validated Service URL not to be null.)
        final String serviceUrl = ExpandVariables.process(assertion.getServiceUrl(), context.getVariableMap(variablesUsed, auditor), auditor);

        // 4. Get the session identifier and the namespace of WS-Secure Conversation
        String sessionId;
        String wsscNamespace;
        String tokenVarName = ExpandVariables.process(assertion.getSecurityContextTokenVarName(), context.getVariableMap(variablesUsed, auditor), auditor);
        try {
            Element sctEl = (Element) context.getVariable(tokenVarName);
            wsscNamespace = getSecureConversationNamespace(sctEl);

            Element identifierEl;
            if (wsscNamespace == null) {
                identifierEl = DomUtils.findExactlyOneChildElement(sctEl);
                wsscNamespace = SoapConstants.WSSC_NAMESPACE2; // Set it using a default namespace
            } else {
                identifierEl = DomUtils.findExactlyOneChildElementByName(sctEl, wsscNamespace, SoapConstants.WSSC_ID_EL_NAME);
            }
            sessionId = DomUtils.getTextValue(identifierEl);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "The security context token variable (Name: \"" + tokenVarName + "\") does not exist.");
            return AssertionStatus.FALSIFIED;
        } catch (MissingRequiredElementException e) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "The security context token does not contain any <Identifier> element.");
            return AssertionStatus.FALSIFIED;
        } catch (TooManyChildElementsException e) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "The security context token has more than one <Identifier> element.");
            return AssertionStatus.FALSIFIED;
        }

        // 5. Get the creation time and the expiration time of the session and validate them.
        long creationTime;
        long expirationTime;
        final long now = System.currentTimeMillis();
        final String creationTimeStr = ExpandVariables.process(assertion.getCreationTime(), context.getVariableMap(variablesUsed, auditor), auditor);
        final String expirationTimeStr = ExpandVariables.process(assertion.getExpirationTime(), context.getVariableMap(variablesUsed, auditor), auditor);

        // 5.1 Get the creation time
        if (! isEmptyString(creationTimeStr)) {
            try {
                creationTime = ISO8601Date.parse(creationTimeStr).getTime();
            } catch (ParseException e) {
                auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "The session creation time is invalid.");
                return AssertionStatus.FALSIFIED;
            }
        } else {
            creationTime = now;
        }
        // 5.2 Get the expiration time
        if (! isEmptyString(expirationTimeStr)) {
            try {
                expirationTime = ISO8601Date.parse(expirationTimeStr).getTime();
            } catch (ParseException e) {
                auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "The session expiration time is invalid.");
                return AssertionStatus.FALSIFIED;
            }
        }  else {
            expirationTime = Long.MAX_VALUE;
        }
        // 5.3 Apply Pre-expiry age on the expiration time
        long preExpiryAge = config.getTimeUnitProperty(SESSION_PRE_EXPIRY_AGE, TimeUnit.MINUTES.toMillis(1)); // Default: 1 minute
        if (expirationTime != Long.MAX_VALUE) {
            expirationTime -= preExpiryAge;
            if (expirationTime < 0) {
                expirationTime = 0;
            }
        }
        // 5.4 Check the maximum expiry period against  "Maximum Expiry Period".
        long maxExpiryPeriod;
        if (assertion.isUseSystemDefaultSessionDuration()) {
            maxExpiryPeriod = config.getTimeUnitProperty(DEFAULT_SESSION_DURATION, TimeUnit.HOURS.toMillis(2)); // Default: 2 hrs
        } else {
            maxExpiryPeriod = assertion.getMaxLifetime();
        }
        if (maxExpiryPeriod > 0 && maxExpiryPeriod < (expirationTime - creationTime)) {
            expirationTime = creationTime + maxExpiryPeriod;
        }
        // 5.5 Validate the expiration time against the creation time and check if the session has expired
        if (expirationTime - creationTime < 0) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "Invalid Session Time: the session expiration time is before the session creation time.");
            return AssertionStatus.FALSIFIED;
        } else if (expirationTime < now) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, "The session (ID: " + sessionId + ") has expired.");
            return AssertionStatus.FALSIFIED;
        }

        // 6. Get the full key, client entropy, or server entropy
        final String fullKey = ExpandVariables.process(assertion.getFullKey(), context.getVariableMap(variablesUsed, auditor), auditor);
        final String clientEntropyStr = ExpandVariables.process(assertion.getClientEntropy(), context.getVariableMap(variablesUsed, auditor), auditor);
        final String serverEntropyStr = ExpandVariables.process(assertion.getServerEntropy(), context.getVariableMap(variablesUsed, auditor), auditor);

        final byte[] sharedSecret = isEmptyString(fullKey)? null : HexUtils.decodeBase64(fullKey);
        final byte[] clientEntropy = isEmptyString(clientEntropyStr)? null : HexUtils.decodeBase64(clientEntropyStr);
        final byte[] serverEntropy = isEmptyString(serverEntropyStr)? null : HexUtils.decodeBase64(serverEntropyStr);

        // 7. Create a new outbound secure conversation session
        SecureConversationSession session;
        try {
            session = securityContextManager.createContextForUser(
                user,
                serviceUrl,
                loginCredentials,
                wsscNamespace,
                sessionId,
                creationTime,
                expirationTime,
                sharedSecret,
                clientEntropy,
                serverEntropy
            );
        } catch (SessionCreationException e) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_ESTABLISHMENT_FAILURE, e.getMessage());
            return AssertionStatus.FALSIFIED;
        }

        // 8. Set the variable, outboundSC.session
        context.setVariable(EstablishOutboundSecureConversation.VARIABLE_SESSION, session);

        return AssertionStatus.NONE;
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    private boolean isEmptyString(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static Config validated(final Config config) {
        final ValidatedConfig vc = new ValidatedConfig(config, logger);

        vc.setMinimumValue(DEFAULT_SESSION_DURATION, EstablishOutboundSecureConversation.MIN_SESSION_DURATION);  // 1 min
        vc.setMaximumValue(DEFAULT_SESSION_DURATION, EstablishOutboundSecureConversation.MAX_SESSION_DURATION); // 24 hrs

        vc.setMinimumValue(SESSION_PRE_EXPIRY_AGE, MIN_SESSION_PRE_EXPIRY_AGE);  // 0
        vc.setMaximumValue(SESSION_PRE_EXPIRY_AGE, MAX_SESSION_PRE_EXPIRY_AGE); // 2 hrs

        return vc;
    }

    private String getSecureConversationNamespace(Element sctElement) {
        Collection<String> allNamespaces = DomUtils.findAllNamespaces(sctElement).values();

        for (String namespace: SoapConstants.WSSC_NAMESPACE_ARRAY) {
            if (allNamespaces.contains(namespace)) {
                return namespace;
            }
        }

        return null;
    }
}