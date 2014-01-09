package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.kerberos.*;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.FileUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServerHttpNegotiateTest {

    private static final Logger log = Logger.getLogger(ServerHttpNegotiateTest.class.getName());
    public static final String CONNECTION_ID = "connectionID";
    private static File tmpDir;


    private ServerHttpNegotiate fixture;

    private HttpNegotiate assertion;

    private Message responseMsg;
    private Message requestMsg;
    private PolicyEnforcementContext context;
    private HttpRequestKnobStub httpRequestKnob;

    @BeforeClass
    public static void init() throws IOException, KerberosException {
        tmpDir = FileUtils.createTempDirectory("kerberos", null, null, false);
        KerberosTestSetup.init(tmpDir);
    }

    @Before
    public void setUp() throws Exception {
        //Setup Assertion
        assertion = new HttpNegotiate();

        //Setup Context
        requestMsg = new Message();
        httpRequestKnob = getHttpRequestKnob();
        requestMsg.attachHttpRequestKnob(httpRequestKnob);
        responseMsg = new Message();
        responseMsg.attachHttpResponseKnob(new Response());
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);

        fixture = new ServerHttpNegotiate(assertion);

    }

    @AfterClass
    public static void dispose() {
        FileUtils.deleteDir(tmpDir);
    }

    @Test
    public void testNoAuthentication() throws Exception {
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
        assertTrue(((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().size() > 0);
        assertEquals(HttpNegotiate.SCHEME, ((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().get(0).left.trim());
    }


    @Ignore("Require KDC Connection")
    @Test
    public void testAuthenticateMessage() throws Exception {
        assertTrue(sendServiceTicket() == AssertionStatus.NONE);
        assertEquals(KerberosConfigTest.REALM, context.getVariable(HttpNegotiate.KERBEROS_REALM));
    }

    @Test
    public void shouldIgnoreMessageContainingNtlmData() throws Exception {
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        String ntlmData = "TlRMTVNTUAABAAAAFbII4AYABgAgAAAADQANACYAAABMN1RFQ0hNWVdPUktTVEFUSU9O";
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, HttpNegotiate.SCHEME + " " + ntlmData);
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
        assertTrue(((Response) context.getResponse().getHttpResponseKnob()).getChallengesToSend().isEmpty());

    }

    @Test
    public void shouldIgnoreMessageContainingNTLMAuthHeader() throws Exception {
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        String ntlmData = "NTLM TlRMTVNTUAABAAAAFbII4AYABgAgAAAADQANACYAAABMN1RFQ0hNWVdPUktTVEFUSU9O";
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, ntlmData);
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
        assertTrue(((Response) context.getResponse().getHttpResponseKnob()).getChallengesToSend().isEmpty());

    }

    @Ignore("Require KDC Connection")
    @Test
    public void testAuthenticationWithCache() throws Exception {
        sendServiceTicket();
        //Remove the authentication header
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        //Clear the context
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.NONE);
    }

    @Test
    public void testInvalidHeader() throws Exception {
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "INVALID HEADER MESSAGE ");
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
        assertTrue(((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().size() > 0);
        assertEquals(HttpNegotiate.SCHEME, ((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().get(0).left.trim());

    }

    @Test
    public void testInvalidData() throws Exception {
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "Negotiate ");
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
        assertTrue(((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().size() > 0);
        assertEquals(HttpNegotiate.SCHEME, ((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().get(0).left.trim());

    }

    @Test
    public void testInvalidTicket() throws Exception {
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, HttpNegotiate.SCHEME + " " + "INVALID_TICKET_MESSAGE");
        httpRequestKnob.addHeader(header);
        assertTrue(fixture.checkRequest(context) == AssertionStatus.AUTH_REQUIRED);
        assertTrue(((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().size() > 0);
        assertEquals(HttpNegotiate.SCHEME, ((Response)context.getResponse().getHttpResponseKnob()).getChallengesToSend().get(0).left.trim());

    }

    private AssertionStatus sendServiceTicket() throws Exception {
        httpRequestKnob.removeHeader(HttpConstants.HEADER_AUTHORIZATION);
        KerberosClient client = new KerberosClient();
        KerberosServiceTicket kerberosTicket = client.getKerberosServiceTicket(KerberosConfigTest.SERVICE_PRINCIPAL_NAME, true);
        //set kerberos authorization info
        String ticket = HexUtils.encodeBase64(kerberosTicket.getGSSAPReqTicket().getSPNEGO(), true);
        HttpHeader header = new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, HttpNegotiate.SCHEME + " " + ticket);
        httpRequestKnob.addHeader(header);
        return fixture.checkRequest(context);
    }


    private static class Response extends AbstractHttpResponseKnob {

        public List<Pair<String,Integer>> getChallengesToSend() {
            Collections.reverse(challengesToSend);
            return challengesToSend;
        }
    }

    private HttpRequestKnobStub getHttpRequestKnob() {
        return new HttpRequestKnobStub() {

            private String connectionIdentifier = CONNECTION_ID;

            @Override
            public Object getConnectionIdentifier() {
                return connectionIdentifier;
            }

            @Override
            public URL getRequestURL() {
                try {
                    return new URL(KerberosConfigTest.SERVICE, KerberosConfigTest.HOST, "/test");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

}
