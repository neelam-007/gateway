package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.LookupOutboundSecureConversationSession;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * @author ghuang
 */
public class ServerLookupOutboundSecureConversationSessionTest {
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private static final OutboundSecureConversationContextManager outboundContextManager = new OutboundSecureConversationContextManager( new MockConfig( new Properties() ) );
    private static final String FAKE_SERVICE_URL = "fake_service_url";
    private static final String FAKE_SESSION_ID = "fake_session_identifier";

    static {
        beanFactory.addBean( "outboundSecureConversationContextManager", outboundContextManager );
    }
    private LookupOutboundSecureConversationSession lookupAssertion;
    private ServerLookupOutboundSecureConversationSession serverLookupAssertion;
    private PolicyEnforcementContext context;
    private Message request;

    @Before
    public void setUp() throws SessionCreationException {
        lookupAssertion = new LookupOutboundSecureConversationSession();
        serverLookupAssertion = new ServerLookupOutboundSecureConversationSession(lookupAssertion, beanFactory);
        request = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,  new Message());
    }

    @Test
    public void testLookupFound() throws Exception {
        // Add a new session into the cache
        User user1 = user(1, "Alice");
        addNewSession(user1, FAKE_SERVICE_URL, FAKE_SESSION_ID);

        // To attempt to match the session, configure the lookup assertion
        lookupAssertion.setServiceUrl(FAKE_SERVICE_URL);

        final AssertionStatus status = serverLookupAssertion.doCheckRequest(context, request, "", authenticationContext(user1));

        assertEquals("Found Session:", AssertionStatus.NONE, status);
    }

    @Test
    public void testLookupNotFound() throws Exception {
        // Add a new session into the cache
        User user2 = user(2, "Bob");
        addNewSession(user2, FAKE_SERVICE_URL, FAKE_SESSION_ID);

        // Case 1: User Not Matched (but still set Service URL matched)
        lookupAssertion.setServiceUrl(FAKE_SERVICE_URL);
        User user3 = user(3, "John");
        AssertionStatus status = serverLookupAssertion.doCheckRequest(context, request, "", authenticationContext(user3));

        assertEquals("Session Not Found: authenticated user mismatched", AssertionStatus.FALSIFIED, status);

        // Case 2: Service URL Not Matched
        lookupAssertion.setServiceUrl("other_service_url");
        status = serverLookupAssertion.doCheckRequest(context, request, "", authenticationContext(user2));

        assertEquals("Session Not Found: service URL mismatched", AssertionStatus.FALSIFIED, status);
    }

    private AuthenticationContext authenticationContext(User user) {
        AuthenticationResult authResult = new AuthenticationResult(user, (SecurityToken) null);
        AuthenticationContext authContext = new AuthenticationContext();
        authContext.addAuthenticationResult(authResult);
        return authContext;
    }

    private SecureConversationSession addNewSession(User user, String serviceUrl, String sessionId) throws SessionCreationException {
        long creationTime = System.currentTimeMillis();

        return outboundContextManager.createContextForUser(
            user,
            serviceUrl,
            LoginCredentials.makeLoginCredentials( new HttpBasicToken(user.getLogin(), "password".toCharArray()), HttpBasic.class ),
            "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512",
            sessionId,
            creationTime,
            creationTime + 2*60*1000,
            generateNewSecret(64),
            null,
            null,
            0
        );
    }

    private User user(final long userId, final String login) {
        UserBean user = new UserBean(login);
        user.setUniqueIdentifier(Long.toString(userId));
        return user;
    }

    private byte[] generateNewSecret(int length) {
        final byte[] output = new byte[length];
        Random random = new SecureRandom();
        random.nextBytes(output);
        return output;
    }
}