package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.CancelSecurityContext;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.secureconversation.NoSuchSessionException;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.util.RstSoapMessageProcessor;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
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

    public ServerCancelSecurityContext( final CancelSecurityContext assertion,
                                        final BeanFactory factory ) {
        super(assertion, assertion);
        auditor = factory instanceof ApplicationContext?
                new Auditor(this, (ApplicationContext)factory, logger) :
                new LogOnlyAuditor(logger);
        secureConversationContextManager = factory.getBean("secureConversationContextManager", SecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {

        // Get all related info from the target SOAP message.  RstSoapMessageProcessor checks the syntax and the semantics of the target SOAP message.
        final Map<String, String> rstParameters = RstSoapMessageProcessor.getRstParameters(message, false);
        if (rstParameters.containsKey(RstSoapMessageProcessor.ERROR)) {
            auditor.logAndAudit(AssertionMessages.STS_INVALID_RST_REQUEST, rstParameters.get(RstSoapMessageProcessor.ERROR));
            return AssertionStatus.BAD_REQUEST;
        }

        // At this point, everything is fine since the validation is done.  It is ready to cancel a SecurityContextToken.
        String targetIdentifier = rstParameters.get(RstSoapMessageProcessor.REFERENCE_ATTR_URI);
        try {
            switch ( assertion.getRequiredAuthorization() ) {
                case USER:
                    final SecureConversationSession session = secureConversationContextManager.getSession( targetIdentifier );
                    if ( session != null ) {
                        checkAuthenticated( authContext, session.getUsedBy() );
                    }
                    break;
                case TOKEN:
                    checkAuthenticationToken( authContext, targetIdentifier );
                    break;
            }

            secureConversationContextManager.cancelSession(targetIdentifier);
        } catch (NoSuchSessionException e) {
            if (assertion.isFailIfNotExist()) {
                auditor.logAndAudit(AssertionMessages.STS_EXPIRED_SC_SESSION, ExceptionUtils.getMessage(e));
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

    private void checkAuthenticated( final AuthenticationContext authContext, final User user ) {
        boolean found = false;

        for ( final AuthenticationResult authenticationResult : authContext.getAllAuthenticationResults() ) {
            final User authenticatedUser = authenticationResult.getUser();
            if ( authenticatedUser.getProviderId()==user.getProviderId() &&
                 authenticatedUser.getId().equals( user.getId() ) ) {
                found = true;
                break;
            }
        }

        if ( !found ) {
            auditor.logAndAudit(AssertionMessages.STS_AUTHORIZATION_FAILURE, "Not authorized.");
            throw new AssertionStatusException(AssertionStatus.BAD_REQUEST);
        }
    }

    private void checkAuthenticationToken( final AuthenticationContext authContext, final String targetIdentifier ) {
        boolean found = false;
        SecurityContextToken securityContextToken = null;

        outer:
        for ( final LoginCredentials credentials : authContext.getCredentials() ) {
            for ( final SecurityToken token : credentials.getSecurityTokens() ) {
                if ( token instanceof SecurityContextToken &&
                     targetIdentifier.equals(((SecurityContextToken)token).getContextIdentifier()) ) {
                    securityContextToken = (SecurityContextToken) token;
                    break outer;
                }
            }
        }

        if ( securityContextToken != null && securityContextToken.isPossessionProved() ) {
            for ( final AuthenticationResult authenticationResult : authContext.getAllAuthenticationResults() ) {
                if ( authenticationResult.matchesSecurityToken( securityContextToken ) ) {
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            auditor.logAndAudit(AssertionMessages.STS_AUTHORIZATION_FAILURE, "Not authorized.");
            throw new AssertionStatusException(AssertionStatus.BAD_REQUEST);
        }
    }
}