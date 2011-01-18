package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.EstablishOutboundSecureConversation;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Element;

import java.util.Calendar;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author ghuang
 */
public class ServerEstablishOutboundSecureConversationTest {
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private static final OutboundSecureConversationContextManager outboundContextManager = new OutboundSecureConversationContextManager( new MockConfig( new Properties() ) );

    static {
        beanFactory.addBean("outboundSecureConversationContextManager", outboundContextManager);
        beanFactory.addBean("serverConfig", new MockConfig(new Properties()));
    }

    private EstablishOutboundSecureConversation establishmentAssertion;
    private ServerEstablishOutboundSecureConversation serverEstablishmentAssertion;
    private PolicyEnforcementContext context;
    private Message request;

    @Before
    public void setUp() throws SessionCreationException {
        establishmentAssertion = new EstablishOutboundSecureConversation();
        serverEstablishmentAssertion = new ServerEstablishOutboundSecureConversation(establishmentAssertion, beanFactory);
        request = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,  new Message());
    }

    @Test
    public void testAuthentication() throws Exception {
        final User user = user(1, "Alice");
        final LoginCredentials loginCredentials = LoginCredentials.makeLoginCredentials(new HttpBasicToken("Alice", "password".toCharArray()), HttpBasic.class);
        context.getAuthenticationContext(request).addCredentials(loginCredentials);
        context.getAuthenticationContext(request).addAuthenticationResult(new AuthenticationResult(user, loginCredentials.getSecurityTokens(), null, false));

        final String tokenStr =
            "<wsc:SecurityContextToken xmlns:wsc=\"http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512\">\n" +
            "  <wsc:Identifier>urn:tokenid</wsc:Identifier>\n" +
            "</wsc:SecurityContextToken>";
        Element tokenEl = XmlUtil.parse(tokenStr).getDocumentElement();
        context.setVariable("rstrResponseProcessor.token", tokenEl);

        final String serviceUrl = "http://service_url";
        establishmentAssertion.setServiceUrl(serviceUrl);

        establishmentAssertion.setSecurityContextTokenVarName("rstrResponseProcessor.token");

        establishmentAssertion.setFullKey("");
        establishmentAssertion.setClientEntropy("BF9gTwzpFq81zxDC4GLOt3R4a6CGXzfTx2OR1fW9K68=");
        establishmentAssertion.setServerEntropy("TWbYuUCjczs1n4hQEWENV8HkGMcHOgQlJJircNa1Cd4");

        final long now = System.currentTimeMillis();
        final long creationTime = now - 60*60*1000; // 1 hour before now;
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(creationTime);
        establishmentAssertion.setCreationTime(ISO8601Date.format(calendar.getTime()));

        final long expirationTime = now + 60*60*1000; // 1 hour after now
        calendar.setTimeInMillis(expirationTime);
        establishmentAssertion.setExpirationTime(ISO8601Date.format(calendar.getTime()));

        AssertionStatus status = serverEstablishmentAssertion.doCheckRequest(context, request, "", context.getAuthenticationContext(request));
        assertEquals( "Outbound Secure Conversation Established Successfully:", AssertionStatus.NONE, status);

        // Verify the context variable, outboundSC.session
        SecureConversationSession session = (SecureConversationSession) context.getVariable("outboundSC.session");
        assertNotNull( "Session Variable Found:", session);

        // Look up the established session by using User and Service URL and then check if two sessions are matched.
        OutboundSecureConversationContextManager.OutboundSessionKey sessionKey = new OutboundSecureConversationContextManager.OutboundSessionKey(user, serviceUrl);
        SecureConversationSession lookupResult = outboundContextManager.getSession(sessionKey);
        assertEquals("Session Matched:", session, lookupResult);
    }

    private User user(final long userId, final String login) {
        UserBean user = new UserBean(login);
        user.setUniqueIdentifier(Long.toString(userId));
        return user;
    }
}
