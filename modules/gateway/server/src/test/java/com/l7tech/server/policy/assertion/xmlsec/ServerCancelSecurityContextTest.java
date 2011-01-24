package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.CancelSecurityContext;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SignedPart;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.util.Functions;
import com.l7tech.util.MockConfig;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Element;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 *
 */
public class ServerCancelSecurityContextTest {

    private static final MockConfig mockConfig = new MockConfig(new Properties());
    private static final InboundSecureConversationContextManager inboundContextManager = new InboundSecureConversationContextManager( mockConfig );
    private static final OutboundSecureConversationContextManager outboundContextManager = new OutboundSecureConversationContextManager(mockConfig, inboundContextManager );
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

    static {
        beanFactory.addBean( "inboundSecureConversationContextManager", inboundContextManager );
        beanFactory.addBean( "outboundSecureConversationContextManager", outboundContextManager );
    }

    @Test
    public void testCancelContext() throws Exception {
        doCancelContext( AssertionStatus.NONE, true, CancelSecurityContext.AuthorizationType.NONE, null );
    }

    @Test
    public void testCancelContextNotExist() throws Exception {
        doCancelContext( AssertionStatus.NONE, true, CancelSecurityContext.AuthorizationType.NONE, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setFailIfNotExist( true );
            }
        } );

        doCancelContext( AssertionStatus.BAD_REQUEST, false, CancelSecurityContext.AuthorizationType.NONE, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setFailIfNotExist( true );
            }
        } );
    }

    @Test
    public void testCancelContextUser() throws Exception {
        // test cancellation failure
        doCancelContext( AssertionStatus.BAD_REQUEST, true, CancelSecurityContext.AuthorizationType.NONE, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setRequiredAuthorization( CancelSecurityContext.AuthorizationType.USER );
            }
        } );

        // test cancellation success (same user)
        doCancelContext( AssertionStatus.NONE, true, CancelSecurityContext.AuthorizationType.USER, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setRequiredAuthorization( CancelSecurityContext.AuthorizationType.USER );
            }
        } );

        // test cancellation failure (same token)
        doCancelContext( AssertionStatus.NONE, true, CancelSecurityContext.AuthorizationType.TOKEN, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setRequiredAuthorization( CancelSecurityContext.AuthorizationType.USER );
            }
        } );
    }

    @Test
    public void testCancelContextToken() throws Exception {
        // test cancellation failure
        doCancelContext( AssertionStatus.BAD_REQUEST, true, CancelSecurityContext.AuthorizationType.NONE, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setRequiredAuthorization( CancelSecurityContext.AuthorizationType.TOKEN );
            }
        } );

        // test cancellation failure (same user)
        doCancelContext( AssertionStatus.BAD_REQUEST, true, CancelSecurityContext.AuthorizationType.USER, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setRequiredAuthorization( CancelSecurityContext.AuthorizationType.TOKEN );
            }
        } );

        // test cancellation success (same token)
        doCancelContext( AssertionStatus.NONE, true, CancelSecurityContext.AuthorizationType.TOKEN, new Functions.UnaryVoid<CancelSecurityContext>(){
            @Override
            public void call( final CancelSecurityContext cancelSecurityContext ) {
                cancelSecurityContext.setRequiredAuthorization( CancelSecurityContext.AuthorizationType.TOKEN );
            }
        } );
    }

    private void doCancelContext( final AssertionStatus expectedStatus,
                                  final boolean createToken,
                                  final CancelSecurityContext.AuthorizationType authType,
                                  final Functions.UnaryVoid<CancelSecurityContext> configCallback ) throws Exception {
        final CancelSecurityContext cancelSecurityContext = new CancelSecurityContext();
        cancelSecurityContext.setFailIfNotExist( true );
        cancelSecurityContext.setRequiredAuthorization( CancelSecurityContext.AuthorizationType.NONE );

        if ( configCallback != null ) configCallback.call( cancelSecurityContext );

        final ServerCancelSecurityContext serverCancelSecurityContext = new ServerCancelSecurityContext( cancelSecurityContext, beanFactory );

        final SecureConversationSession session = createToken ? generateContextToken() : null;

        final String contextId = createToken ?
                 session.getIdentifier():
                "invalid-context-id";

        final Message request = new Message( XmlUtil.parse(
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <s:Header xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\">\n" +
                "        <wsa:MessageID>message1</wsa:MessageID>\n" +
                "        <wsa:Action>http://schemas.xmlsoap.org/ws/2005/02/trust/RST/SCT/Cancel</wsa:Action>\n" +
                "    </s:Header>\n" +
                "    <s:Body>\n" +
                "        <wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n" +
                "            <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Cancel</wst:RequestType>\n" +
                "            <wst:CancelTarget>\n" +
                "                <wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
                "                    <wsse:Reference URI=\""+contextId+"\"/>\n" +
                "                </wsse:SecurityTokenReference>\n" +
                "            </wst:CancelTarget>\n" +
                "        </wst:RequestSecurityToken>\n" +
                "    </s:Body>\n" +
                "</s:Envelope>" ),0);
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        switch ( authType ) {
            case USER:
                {
                    final LoginCredentials loginCredentials = LoginCredentials.makeLoginCredentials( new HttpBasicToken("Alice", "password".toCharArray()), HttpBasic.class );
                    context.getAuthenticationContext( request ).addCredentials( loginCredentials );
                    context.getAuthenticationContext( request ).addAuthenticationResult( new AuthenticationResult( user(1, "Alice"), loginCredentials.getSecurityTokens(), null, false ) );
                }
                break;
            case TOKEN:
                {
                    final LoginCredentials loginCredentials = LoginCredentials.makeLoginCredentials(session.getCredentials().getSecurityToken(), false, SecureConversation.class, new SecurityContextTokenImpl(session));
                    context.getAuthenticationContext( request ).addCredentials( loginCredentials );
                    context.getAuthenticationContext( request ).addAuthenticationResult( new AuthenticationResult( user(1, "Alice"), loginCredentials.getSecurityTokens(), null, false ) );
                    break;
                }
        }

        AssertionStatus status;
        try {
            status = serverCancelSecurityContext.checkRequest( context );
        } catch ( AssertionStatusException ase ) {
            status = ase.getAssertionStatus();    
        }
        assertEquals( "AssertionStatus", expectedStatus, status );
    }

    private SecureConversationSession generateContextToken() throws SessionCreationException {
        return inboundContextManager.createContextForUser(
                user(1, "Alice"),
                LoginCredentials.makeLoginCredentials( new HttpBasicToken("Alice", "password".toCharArray()), HttpBasic.class ),
                "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512" );
    }

    private User user( final long userId,
                       final String userLogin ) {
        UserBean user = new UserBean( userLogin );
        user.setUniqueIdentifier( Long.toString(userId) );
        return user;
    }

    private static class SecurityContextTokenImpl implements SecurityContextToken {
        private SecureConversationSession session;

        private SecurityContextTokenImpl( final SecureConversationSession session ) {
            this.session = session;
        }

        @Override
        public String getContextIdentifier() {
            return session.getIdentifier();
        }

        @Override
        public SecurityContext getSecurityContext() {
            return session;
        }

        @Override
        public boolean isPossessionProved() {
            return true;
        }

        @Override
        public SignedElement[] getSignedElements() {
            return new SignedElement[0];
        }

        @Override
        public void addSignedElement( final SignedElement signedElement ) {
        }

        @Override
        public SignedPart[] getSignedParts() {
            return new SignedPart[0];
        }

        @Override
        public void addSignedPart( final SignedPart signedPart ) {
        }

        @Override
        public void onPossessionProved() {
        }

        @Override
        public String getElementId() {
            return null;
        }

        @Override
        public Element asElement() {
            return null;
        }

        @Override
        public SecurityTokenType getType() {
            return SecurityTokenType.WSSC_CONTEXT;
        }
    }
}