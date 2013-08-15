package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.CancelSecurityContext;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.util.RstSoapMessageProcessor;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author ghuang
 */
public class ServerCancelSecurityContext extends AbstractMessageTargetableServerAssertion<CancelSecurityContext> {

    private final InboundSecureConversationContextManager inboundSecureConversationContextManager;
    private final OutboundSecureConversationContextManager outboundSecureConversationContextManager;
    private final String[] variablesUsed;

    public ServerCancelSecurityContext( final CancelSecurityContext assertion,
                                        final BeanFactory factory ) {
        super(assertion);
        inboundSecureConversationContextManager = factory.getBean("inboundSecureConversationContextManager", InboundSecureConversationContextManager.class);
        outboundSecureConversationContextManager = factory.getBean("outboundSecureConversationContextManager", OutboundSecureConversationContextManager.class);
        variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authenticationContext ) throws IOException, PolicyAssertionException {
        if ( assertion.isCancelInbound() ) {
            return doCancelInbound( message, authenticationContext );
        } else {
            return doCancelOutbound( context, authenticationContext );
        }
    }

    private AssertionStatus doCancelInbound( final Message message,
                                             final AuthenticationContext authenticationContext ) {
        // Get all related info from the target SOAP message.  RstSoapMessageProcessor checks the syntax and the semantics of the target SOAP message.
        final Map<String, String> rstParameters = RstSoapMessageProcessor.getRstParameters(message, false);
        if (rstParameters.containsKey(RstSoapMessageProcessor.ERROR)) {
            logAndAudit( AssertionMessages.STS_INVALID_RST_REQUEST, rstParameters.get(RstSoapMessageProcessor.ERROR));
            return AssertionStatus.BAD_REQUEST;
        }

        // At this point, everything is fine since the validation is done.  It is ready to cancel a SecurityContextToken.
        final String targetIdentifier = rstParameters.get(RstSoapMessageProcessor.REFERENCE_ATTR_URI);
        switch ( assertion.getRequiredAuthorization() ) {
            case USER:
                final SecureConversationSession session = inboundSecureConversationContextManager.getSession( targetIdentifier );
                if ( session != null ) {
                    checkAuthenticated( authenticationContext, session.getUsedBy() );
                }
                break;
            case TOKEN:
                checkAuthenticationToken( authenticationContext, targetIdentifier );
                break;
        }

        if ( !inboundSecureConversationContextManager.cancelSession(targetIdentifier) &&
             assertion.isFailIfNotExist() ) {
            logAndAudit(AssertionMessages.STS_EXPIRED_SC_SESSION, "Session not found '"+targetIdentifier+"'");
            return AssertionStatus.BAD_REQUEST;
        }

        return AssertionStatus.NONE;
    }

    private AssertionStatus doCancelOutbound( final PolicyEnforcementContext context,
                                              final AuthenticationContext authenticationContext ) {
        final Map<String,Object> vars = context.getVariableMap( variablesUsed, getAudit() );
        final User user = authenticationContext.getLastAuthenticatedUser();
        if ( user == null ) {
            logAndAudit(AssertionMessages.STS_AUTHENTICATION_FAILURE, "The target message does not contain an authenticated user.");
            return AssertionStatus.FALSIFIED;
        }

        final String serviceUrl = ExpandVariables.process( assertion.getOutboundServiceUrl()==null ? "" : assertion.getOutboundServiceUrl(), vars, getAudit() );

        if ( !outboundSecureConversationContextManager.cancelSession( OutboundSecureConversationContextManager.newSessionKey( user, serviceUrl ) ) &&
             assertion.isFailIfNotExist() ) {
            logAndAudit(AssertionMessages.STS_EXPIRED_SC_SESSION, "Session not found for user '"+user.getLogin()+"', service URL '"+serviceUrl+"'");
            return AssertionStatus.FALSIFIED;
        }
        
        return AssertionStatus.NONE;
    }

    private void checkAuthenticated( final AuthenticationContext authenticationContext,
                                     final User user ) {
        boolean found = false;

        for ( final AuthenticationResult authenticationResult : authenticationContext.getAllAuthenticationResults() ) {
            final User authenticatedUser = authenticationResult.getUser();
            if ( authenticatedUser.getProviderId().equals(user.getProviderId()) &&
                 authenticatedUser.getId().equals( user.getId() ) ) {
                found = true;
                break;
            }
        }

        if ( !found ) {
            logAndAudit(AssertionMessages.STS_AUTHORIZATION_FAILURE, "User not permitted to cancel token");
            throw new AssertionStatusException(AssertionStatus.BAD_REQUEST);
        }
    }

    private void checkAuthenticationToken( final AuthenticationContext authenticationContext,
                                           final String targetIdentifier ) {
        boolean found = false;
        SecurityContextToken securityContextToken = null;

        outer:
        for ( final LoginCredentials credentials : authenticationContext.getCredentials() ) {
            for ( final SecurityToken token : credentials.getSecurityTokens() ) {
                if ( token instanceof SecurityContextToken &&
                     targetIdentifier.equals(((SecurityContextToken)token).getContextIdentifier()) ) {
                    securityContextToken = (SecurityContextToken) token;
                    break outer;
                }
            }
        }

        if ( securityContextToken != null && securityContextToken.isPossessionProved() ) {
            for ( final AuthenticationResult authenticationResult : authenticationContext.getAllAuthenticationResults() ) {
                if ( authenticationResult.matchesSecurityToken( securityContextToken ) ) {
                    found = true;
                    break;
                }
            }
        }

        if ( !found ) {
            logAndAudit(AssertionMessages.STS_AUTHORIZATION_FAILURE, "Required token not present");
            throw new AssertionStatusException(AssertionStatus.BAD_REQUEST);
        }
    }
}
