package com.l7tech.external.assertions.ntlm.server;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.external.assertions.ntlm.NtlmAuthenticationAssertion;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.ntlm.NtlmTestConstants;
import com.l7tech.ntlm.adapter.LocalAuthenticationAdapter;
import com.l7tech.ntlm.netlogon.NetLogon;
import com.l7tech.ntlm.protocol.AuthenticationProvider;
import com.l7tech.ntlm.protocol.NtlmAuthenticationServer;
import com.l7tech.ntlm.protocol.NtlmClient;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.server.identity.TestIdentityProviderConfigManager;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.HexUtils;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerNtlmAuthenticationAssertionTest {

    private static final Logger log = Logger.getLogger(ServerNtlmAuthenticationAssertionTest.class.getName());
    public static final String CONNECTION_ID = "connectionID";

    @Autowired
    private ApplicationContext applicationContext;

    private ServerNtlmAuthenticationAssertion fixture;

    private NtlmAuthenticationAssertion assertion;

    private Message responseMsg;
    private Message requestMsg;
    private PolicyEnforcementContext context;
    private HttpRequestKnobStub httpRequestKnob;

    @Before
    public void setUp() throws Exception {
        //Setup Assertion
        assertion = new NtlmAuthenticationAssertion();
        assertion.setVariablePrefix("protocol");
        assertion.setMaxConnectionIdleTime(0);
        assertion.setMaxConnectionDuration(0);
        //Setup Context
        requestMsg = new Message();
        httpRequestKnob = getHttpRequestKnob();
        requestMsg.attachHttpRequestKnob(httpRequestKnob);
        responseMsg = new Message();
        responseMsg.attachHttpResponseKnob(new Response());
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);

        //Setup LdapIdentityProvider
        LdapIdentityProviderConfig ldapIdentityProviderConfig = new LdapIdentityProviderConfig();
        ldapIdentityProviderConfig.setLdapUrl(new String[]{"TEST"});
        ldapIdentityProviderConfig.setOid(assertion.getLdapProviderOid());

        TestIdentityProviderConfigManager identityProviderConfigManager = applicationContext.getBean("identityProviderConfigManager", TestIdentityProviderConfigManager.class);
        identityProviderConfigManager.update(ldapIdentityProviderConfig);

        fixture = new TestServerNtlmAuthenticationAssertion(assertion, applicationContext);
    }


    @Test
    public void testNoAuthentication() throws Exception {
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
    }

    @Test
    public void testNegotiateMessage() throws Exception {
        assertTrue(sendNegotiateMsg() == AssertionStatus.AUTH_REQUIRED);
    }


    @Test
    public void testAuthenticateMessage() throws Exception {
        sendNegotiateMsg();
        assertTrue(sendAuthenticateMsg() == AssertionStatus.NONE);
        assertNotNull(context.getVariable(assertion.getVariablePrefix() + "." + NtlmAuthenticationAssertion.USER_LOGIN_NAME));
    }

    @Test
    public void testAuthenticationWithCache() throws Exception {
        sendNegotiateMsg();
        sendAuthenticateMsg();
        //Remove the authentication header
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        //Clear the context
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.NONE);
    }

    @Test
    public void testAuthenticationWithoutCache() throws Exception {
        sendNegotiateMsg();
        sendAuthenticateMsg();
        //Remove the authentication header
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
        //no cache
        assertion.setMaxConnectionDuration(-1);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
    }


    @Test
    public void testInvalidUser() throws Exception {
        sendNegotiateMsg();
        Response response = (Response) context.getResponse().getHttpResponseKnob();
        String type3Msg = HexUtils.encodeBase64(NtlmClient.generateType3Msg(NtlmTestConstants.USER + "INVALID", NtlmTestConstants.PASSWORD, NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION, response.getChallengesToSend().substring(5)).toByteArray(), true);
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM " + type3Msg);
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_FAILED);
    }

    @Test
    public void testInvalidProvider() throws Exception {
        //set invalid provider
        assertion.setLdapProviderOid(9999999);
        sendNegotiateMsg();
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_FAILED);
    }

    @Test
    public void testInvalidHeader() throws Exception {
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "INVALID HEADER MESSAGE ");
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
    }

    @Test
    public void testInvalidNegotiateMsg() throws Exception {
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM " + "INVALID_NEGOTIATE_MESSAGE");
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_FAILED);
    }

    //@Ignore("Need to connect to to NetLogon.")
    @Test
    public void testInvalidChallenge() throws Exception {
        sendNegotiateMsg();
        String type3Msg = HexUtils.encodeBase64(NtlmClient.generateType3Msg(NtlmTestConstants.USER, NtlmTestConstants.PASSWORD, NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION,
                HexUtils.encodeBase64(NtlmClient.generateType2Msg(NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION).toByteArray(), true)).toByteArray(), true);
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM " + type3Msg);
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_FAILED);
    }

    private AssertionStatus sendNegotiateMsg() throws Exception {
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        String type1Msg = HexUtils.encodeBase64(NtlmClient.generateType1Msg(NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION).toByteArray(), true);
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM " + type1Msg);
        httpRequestKnob.addHeader(header);
        return fixture.checkRequest(context);
    }

    private AssertionStatus sendAuthenticateMsg() throws Exception {
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        Response response = (Response) context.getResponse().getHttpResponseKnob();
        String type3Msg = HexUtils.encodeBase64(NtlmClient.generateType3Msg(NtlmTestConstants.USER, NtlmTestConstants.PASSWORD, NtlmTestConstants.DOMAIN, NtlmTestConstants.WORKSTATION, response.getChallengesToSend().substring(5)).toByteArray(), true);
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM " + type3Msg);
        httpRequestKnob.addHeader(header);
        return fixture.checkRequest(context);
    }

    private static class Response extends AbstractHttpResponseKnob {

        @Override
        public void addCookie(HttpCookie cookie) {
        }

        public String getChallengesToSend() {
            Collections.reverse(challengesToSend);
            return challengesToSend.get(0);
        }
    }

    private HttpRequestKnobStub getHttpRequestKnob() {
        return new HttpRequestKnobStub() {

            private String connectionIdentifier = CONNECTION_ID;

            @Override
            public Object getConnectionIdentifier() {
                return connectionIdentifier;
            }
        };
    }


    private static class TestServerNtlmAuthenticationAssertion extends ServerNtlmAuthenticationAssertion {

        public TestServerNtlmAuthenticationAssertion(final NtlmAuthenticationAssertion assertion, ApplicationContext ctx) throws PolicyAssertionException {
            super(assertion, ctx);
            try {
                Pair<Object,AuthenticationProvider> newPair = new Pair<Object, AuthenticationProvider>(CONNECTION_ID, createAuthenticationProvider());
                ntlmAuthenticationProviderThreadLocal.set(newPair);
            } catch (CredentialFinderException e) {
                throw new PolicyAssertionException(assertion, e);
            }
        }

        protected AuthenticationProvider createAuthenticationProvider() throws CredentialFinderException {
            AuthenticationProvider authenticationProvider;
            try {
/*                //Setup LdapIdentityProvider
                LdapIdentityProviderConfig ldapIdentityProviderConfig = new LdapIdentityProviderConfig();
                ldapIdentityProviderConfig.setLdapUrl(new String[]{"TEST"});
                ldapIdentityProviderConfig.setOid(assertion.getLdapProviderOid());

                TestIdentityProviderConfigManager identityProviderConfigManager = applicationContext.getBean("identityProviderConfigManager", TestIdentityProviderConfigManager.class);
                identityProviderConfigManager.update(ldapIdentityProviderConfig);*/

                HashMap props = new HashMap();
                props.put("domain.netbios.name", NtlmTestConstants.DOMAIN);
                props.put("domain.dns.name", "l7tech.com");
                props.put("localhost.dns.name", "linux-12vk");
                props.put("localhost.netbios.name", "LINUX12-VK");
                props.put("my.username", NtlmTestConstants.USER);
                props.put("my.password", NtlmTestConstants.PASSWORD);

                authenticationProvider = new NtlmAuthenticationServer(props, new LocalAuthenticationAdapter(props));
            } catch (Exception e) {
                final String errorMsg = "Unable to create NtlmAuthenticationServer instance";
                String[] messages = {errorMsg};
                //logAndAudit(AssertionMessages.NTLM_AUTHENTICATION_FAILED, messages , e);
                throw new CredentialFinderException(errorMsg, e, AssertionStatus.FAILED);
            }
            return authenticationProvider;
        }
    }
}
