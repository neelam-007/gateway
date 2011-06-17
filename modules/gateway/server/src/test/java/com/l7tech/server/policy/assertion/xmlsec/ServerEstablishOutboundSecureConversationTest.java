package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.EstablishOutboundSecureConversation;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.server.secureconversation.StoredSecureConversationSessionManagerStub;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author ghuang
 */
public class ServerEstablishOutboundSecureConversationTest {
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private static final MockConfig mockConfig = new MockConfig(new Properties());
    private static final InboundSecureConversationContextManager inboundContextManager = new InboundSecureConversationContextManager(mockConfig,new StoredSecureConversationSessionManagerStub());
    private static final OutboundSecureConversationContextManager outboundContextManager = new OutboundSecureConversationContextManager(mockConfig,new StoredSecureConversationSessionManagerStub());

    static {
        beanFactory.addBean("inboundSecureConversationContextManager", inboundContextManager);
        beanFactory.addBean("outboundSecureConversationContextManager", outboundContextManager);
        beanFactory.addBean("serverConfig", new MockConfig(new Properties()));
    }

    private final String serviceUrl = "http://service_url";
    private final long now = System.currentTimeMillis();
    private final long creationTime = now - (long) (20 * 60 * 1000);   // 20 minutes before now;
    private final long expirationTime = now + (long) (20 * 60 * 1000); // 20 minute after now

    private EstablishOutboundSecureConversation establishmentAssertion;
    private ServerEstablishOutboundSecureConversation serverEstablishmentAssertion;
    private PolicyEnforcementContext context;
    private Message request;
    private User user;

    @Before
    public void setUp() throws SessionCreationException, SAXException {
        establishmentAssertion = new EstablishOutboundSecureConversation();
        serverEstablishmentAssertion = new ServerEstablishOutboundSecureConversation(establishmentAssertion, beanFactory);
        request = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,  new Message());

        user = user( 1L, "Alice");
        final LoginCredentials loginCredentials = LoginCredentials.makeLoginCredentials(new HttpBasicToken("Alice", "password".toCharArray()), HttpBasic.class);
        context.getAuthenticationContext(request).addCredentials(loginCredentials);
        context.getAuthenticationContext(request).addAuthenticationResult(new AuthenticationResult(user, loginCredentials.getSecurityTokens(), null, false));

        final String tokenStr =
            "<wsc:SecurityContextToken wsu:Id=\"1234567890\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:wsc=\"http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512\">\n" +
            "  <wsc:Identifier>urn:tokenid</wsc:Identifier>\n" +
            "  <Cookie>Value</Cookie>\n" +
            "</wsc:SecurityContextToken>";
        Element tokenEl = XmlUtil.parse(tokenStr).getDocumentElement();
        context.setVariable("rstrResponseProcessor.token", tokenEl);

        establishmentAssertion.setServiceUrl(serviceUrl);
        establishmentAssertion.setSecurityContextTokenVarName("rstrResponseProcessor.token");
        establishmentAssertion.setFullKey("");
        establishmentAssertion.setClientEntropy("BF9gTwzpFq81zxDC4GLOt3R4a6CGXzfTx2OR1fW9K68=");
        establishmentAssertion.setServerEntropy("TWbYuUCjczs1n4hQEWENV8HkGMcHOgQlJJircNa1Cd4");

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(creationTime);
        establishmentAssertion.setCreationTime(ISO8601Date.format(calendar.getTime()));

        calendar.setTimeInMillis(expirationTime);
        establishmentAssertion.setExpirationTime(ISO8601Date.format(calendar.getTime()));
    }

    @Test
    public void testOutboundSecureConversationEstablishment() throws Exception {
        AssertionStatus status = serverEstablishmentAssertion.doCheckRequest(context, request, "", context.getAuthenticationContext(request));
        assertEquals( "Outbound Secure Conversation Established Successfully:", AssertionStatus.NONE, status);

        // Verify the context variable, outboundSC.session
        SecureConversationSession session = (SecureConversationSession) context.getVariable("outboundSC.session");
        assertNotNull( "Session Variable Found:", session);
        assertNotNull( "Session token element", session.getElement() );
        XmlUtil.findExactlyOneChildElementByName( session.getElement(), "Cookie" );

        // Look up the established session by using User and Service URL and then check if two sessions are matched.
        OutboundSecureConversationContextManager.OutboundSessionKey sessionKey = OutboundSecureConversationContextManager.newSessionKey(user, serviceUrl);
        SecureConversationSession lookupResult = outboundContextManager.getSession(sessionKey);
        assertEquals("Session Matched:", session, lookupResult);
    }

    @Test
    public void testMaxExpiryPeriodAndPreExpiryAge() throws IOException, PolicyAssertionException, NoSuchVariableException {
        // Test 1: Expired
        establishmentAssertion.setUseSystemDefaultSessionDuration(false);
        // Set 15 minutes as the max expiry time.  This implies that the session has expired, since the max lifetime is set to 15 minutes after the creation time and then the new expiration time will be 5 minutes earlier than "now".
        establishmentAssertion.setMaxLifetime( (long) (15 * 60 * 1000) );

        AssertionStatus status = serverEstablishmentAssertion.doCheckRequest(context, request, "", context.getAuthenticationContext(request));
        assertEquals( "Outbound Secure Conversation Session Expired:", AssertionStatus.FALSIFIED, status);

        // Test 2: Not Expired
        // 2.1: Set "Maximum Expiry Period" to zero, then the session is not expired.
        establishmentAssertion.setUseSystemDefaultSessionDuration(false);
        establishmentAssertion.setMaxLifetime( 0L );
        cancelSession();
        status = serverEstablishmentAssertion.doCheckRequest(context, request, "", context.getAuthenticationContext(request));
        assertEquals( "Outbound Secure Conversation Session Not Expired:", AssertionStatus.NONE, status);

        // 2.2: Set "Maximum Expiry Period" to the System Default Session Duration (2 hours), then the session will not be expired.
        establishmentAssertion.setUseSystemDefaultSessionDuration(true); // The system default of Max Expiry Period is 2 hours.
        cancelSession();
        status = serverEstablishmentAssertion.doCheckRequest(context, request, "", context.getAuthenticationContext(request));
        assertEquals( "Outbound Secure Conversation Session Not Expired:", AssertionStatus.NONE, status);

        // The following two tests to verify the new expiration time

        // Test 3: "Maximum Expiry Period" is greater than [(expirationTime - creationTime) - defaultPreExpiryAge)]
        establishmentAssertion.setUseSystemDefaultSessionDuration(true); // The system default of Max Expiry Period is 2 hours.
        cancelSession();
        serverEstablishmentAssertion.doCheckRequest(context, request, "", context.getAuthenticationContext(request));

        SecureConversationSession session = (SecureConversationSession) context.getVariable("outboundSC.session");
        final long defaultPreExpiryAge = (long) (60 * 1000); // The default value of the pre-expiry age is 1 minute.

        long expectedExpirationTime = expirationTime - defaultPreExpiryAge; // Since the system default max expiry period is 2 hours, which is greater than [(expirationTime - creationTime) - defaultPreExpiryAge)].
        assertEquals( "New Expiration Time:", expectedExpirationTime, session.getExpiration());

        // Test 4: "Maximum Expiry Period" is less than [(expirationTime - creationTime) - defaultPreExpiryAge)]
        establishmentAssertion.setUseSystemDefaultSessionDuration(false);
        final long maxExpiryPeriod = (long) (30 * 60 * 1000); // 30 minutes for Maximum Expiry Period
        establishmentAssertion.setMaxLifetime(maxExpiryPeriod);

        cancelSession();
        serverEstablishmentAssertion.doCheckRequest(context, request, "", context.getAuthenticationContext(request));
        session = (SecureConversationSession) context.getVariable("outboundSC.session");

        expectedExpirationTime = creationTime + maxExpiryPeriod; // Since the system default max expiry period is 30 minutes, which is less than  [(expirationTime - creationTime) - defaultPreExpiryAge)].
        assertEquals( "New Expiration Time:", expectedExpirationTime, session.getExpiration());
    }

    private User user(final long userId, final String login) {
        UserBean user = new UserBean(login);
        user.setUniqueIdentifier(Long.toString(userId));
        return user;
    }

    /**
     *  Cancel the previous session, otherwise a duplicate session exception thrown
     */
    private void cancelSession() {
        OutboundSecureConversationContextManager.OutboundSessionKey sessionKey = OutboundSecureConversationContextManager.newSessionKey(user, serviceUrl);
        outboundContextManager.cancelSession(sessionKey);
    }
}
