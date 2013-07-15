package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderCredentials;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.logging.Logger;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Test the SiteMinderAuthenticateAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSiteMinderAssertionTest {

    private static final Logger log = Logger.getLogger(ServerSiteMinderAssertionTest.class.getName());

    @Mock
    SiteMinderHighLevelAgent mockHla;

    ServerSiteMinderAuthenticateAssertion fixture;

    SiteMinderAuthenticateAssertion assertion;
    private Message responseMsg;
    private Message requestMsg;
    private PolicyEnforcementContext pec;
    private TestHttpRequestKnob httpRequestKnob;

    @Before
    public void setUp() throws Exception {
        assertion = new SiteMinderAuthenticateAssertion();
        //Setup Context
        requestMsg = new Message();
        httpRequestKnob = new TestHttpRequestKnob(new HttpCookie("SMSESSION", "abcdefgh", 1, "/", "domain"));
        requestMsg.attachHttpRequestKnob(httpRequestKnob);
        responseMsg = new Message();
        responseMsg.attachHttpResponseKnob(new TestResponse());
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
    }

    @Test
    public void shouldAuthenticateUserWhenCredentialsPresent() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setAgentID("<Default>");

        assertion.setLastCredential(true);
        assertion.setUseSMCookie(false);
        assertion.setCookieNameVariable("SMSESSION");
        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken("user","password".toCharArray()), assertion.getClass()));
        when(mockHla.processAuthenticationRequest(any(SiteMinderCredentials.class), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);
        fixture = new ServerSiteMinderAuthenticateAssertion(assertion, mockHla);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
    }

    @Test
    public void shouldValidateCookieWhenCredentialsPresent() throws Exception {
        assertion.setPrefix("siteminder");

        assertion.setAgentID("<Default>");

        assertion.setLastCredential(true);
        assertion.setUseSMCookie(true);
        assertion.setUseCustomCookieName(true);
        assertion.setCookieNameVariable("SMSESSION");

        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken("user","password".toCharArray()), assertion.getClass()));
        fixture = new ServerSiteMinderAuthenticateAssertion(assertion, mockHla);
        when(mockHla.processAuthenticationRequest(any(SiteMinderCredentials.class), anyString(), eq("abcdefgh"), any(SiteMinderContext.class))).thenReturn(1);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
    }

    private static class TestResponse extends AbstractHttpResponseKnob {

        @Override
        public void addCookie(HttpCookie cookie) {
        }

    }

    private  static class TestHttpRequestKnob extends HttpRequestKnobStub {
        private final HttpCookie[] cookies;

        TestHttpRequestKnob(HttpCookie smsession) {
            cookies = new HttpCookie[] {smsession};
        }

        TestHttpRequestKnob(){
            cookies = new HttpCookie[0];
        }

        @Override
        public HttpCookie[] getCookies() {
            return cookies;
        }

    }

}
