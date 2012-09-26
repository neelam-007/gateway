package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.TestTimeSource;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;

import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.*;
import static org.junit.Assert.*;

public class ServerGenerateOAuthSignatureBaseStringAssertionTest {
    private static final String REQUEST_URL = "http://photos.example.net/photos";
    private static final String CALLBACK = "http%3A%2F%2Fmycallback.com";
    private static final String CALLBACK_ENCODED = "http%253A%252F%252Fmycallback.com";
    private static final String CONSUMER_KEY = "dpf43f3p2l4k3l03";
    private static final String TIMESTAMP = "2000";
    private static final String NONCE = "nongeneratednonce";
    private static final String QUERY_STRING = "z=last&a=first&p=middle";
    private static final String FILE_QUERY_STRING = "?size=original&file=vacation.jpg";
    private static final String SIG_METHOD = "HMAC-SHA1";
    private static final String VERSION = "1.0";
    private static final String HTTP_METHOD = "GET";
    private static final String TOKEN = "nnch734d00sl2jdk";
    private static final String VERIFIER = "473f82d3";
    private static final String SERVER_NAME = "photos.example.net";
    private static final String CLIENT_NONCE = "stubgeneratednonce";
    private static final String CLIENT_TIMESTAMP = "1000";
    private static final String GET = "GET";
    private static final String POST = "POST";

    private GenerateOAuthSignatureBaseStringAssertion assertion;
    private ServerGenerateOAuthSignatureBaseStringAssertion serverAssertion;
    private PolicyEnforcementContext policyContext;
    private Message requestMessage;
    private MockHttpServletRequest request;
    private TestAudit testAudit;

    @Before
    public void setup() throws Exception {
        assertion = new GenerateOAuthSignatureBaseStringAssertion();
        assertion.setUseAuthorizationHeader(true);
        assertion.setUsageMode(UsageMode.CLIENT);
        assertion.setUseMessageTarget(true);
        assertion.setUseOAuthVersion(true);
        serverAssertion = new TestableServerAssertion(assertion);
        serverAssertion.setTimeSource(new TestTimeSource(Long.valueOf(CLIENT_TIMESTAMP) * 1000, 1L));
        request = new MockHttpServletRequest();
        requestMessage = new Message();
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, new Message());
        testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
    }

    @Test
    public void requestToken() throws Exception {
        setParamsForRequestToken(assertion);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void authorizedRequestToken() throws Exception {
        setParamsForAuthRequestToken(assertion);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, AUTHORIZED_REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(AUTHORIZED_REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAuthRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_CALLBACK, "oauth." + AUTH_HEADER);
    }

    @Test
    public void accessToken() throws Exception {
        setParamsForAccessToken(assertion);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, ACCESS_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(ACCESS_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAccessTokenVariables();
        assertContextVariablesDoNotExist("oauth." + CALLBACK, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void authorizationHeaderRequestToken() throws Exception {
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void authorizationHeaderRequestTokenPostNoContentType() throws Exception {
        setParamsForRequestToken(request, true, false);
        request.setMethod(POST);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(POST, REQUEST_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void authorizationHeaderRequestTokenUnrecognizedContentType() throws Exception {
        setParamsForRequestToken(request, true, false);
        request.setMethod(POST);
        request.setContentType("unrecognized");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(POST, REQUEST_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void authorizationHeaderAuthorizedRequestToken() throws Exception {
        setParamsForAuthRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, AUTHORIZED_REQUEST_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(AUTHORIZED_REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAuthRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
    }

    @Test
    public void authorizationHeaderAccessToken() throws Exception {
        setParamsForAccessToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, ACCESS_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(ACCESS_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAccessTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void messageTargetRequestToken() throws Exception {
        setParamsForRequestToken(request, false, true);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dnongeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D2000%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void messageTargetAuthorizedRequestToken() throws Exception {
        setParamsForAuthRequestToken(request, false, true);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3D" + NONCE + "%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D" + TIMESTAMP + "%26" +
                "oauth_token%3Dnnch734d00sl2jdk%26oauth_verifier%3D473f82d3%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(AUTHORIZED_REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAuthRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + AUTH_HEADER);
    }

    @Test
    public void messageTargetAccessToken() throws Exception {
        setParamsForAccessToken(request, false, true);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dnongeneratednonce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D2000%26" +
                "oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(ACCESS_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAccessTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void requestUrlMixedCase() throws Exception {
        assertion.setRequestUrl("htTp://pHOtos.exaMPle.net/photos");
        assertion.setHttpMethod(HTTP_METHOD);
        assertion.setOauthCallback(CALLBACK);
        assertion.setOauthConsumerKey(CONSUMER_KEY);
        assertion.setQueryString(QUERY_STRING);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void quotesStripped() throws Exception {
        assertion.setRequestUrl(REQUEST_URL);
        assertion.setHttpMethod(HTTP_METHOD);
        assertion.setOauthCallback(CALLBACK);
        assertion.setOauthConsumerKey(CONSUMER_KEY);
        // quotes in query string
        assertion.setQueryString("?z=\"last\"&a=\"first\"&p=\"middle\"");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void signatureStripped() throws Exception {
        final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                "oauth_consumer_key=\"" + CONSUMER_KEY + "\"," +
                "oauth_signature_method=\"HMAC-SHA1\"," +
                "oauth_timestamp=\"" + TIMESTAMP + "\"," +
                "oauth_nonce=\"" + NONCE + "\"," +
                "oauth_callback=\"" + CALLBACK + "\"," +
                "oauth_signature=\"asdf\"," +
                "oauth_version=\"1.0\"";
        request.addHeader("Authorization", authorizationHeader);
        request.setMethod(HTTP_METHOD);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos?z=last&a=first&p=middle");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void paramDetectedMoreThanOnce() throws Exception {
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        // this is the 2nd time that the consumer key has been set
        assertion.setQueryString("oauth_consumer_key=2ndconsumerkey");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_DUPLICATE_PARAMETER));
        assertEquals("Duplicate oauth parameter: oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void paramDetectedMoreThanOnceInRequestBody() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=" + SIG_METHOD + "&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_callback=" + CALLBACK + "duplicate&" +
                "oauth_version=" + VERSION;
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_DUPLICATE_PARAMETER));
        assertEquals("Duplicate oauth parameter: oauth_callback", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    @BugNumber(13108)
    public void paramDetectedMoreThanOnceButEqual() throws Exception {
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("oauth_consumer_key=" + CONSUMER_KEY);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_DUPLICATE_PARAMETER));
        assertEquals("Duplicate oauth parameter: oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void invalidOAuthVersion() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=" + SIG_METHOD + "&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=2.0";
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_PARAMETER));
        assertEquals("Invalid oauth_version: 2.0", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void invalidSignatureMethod() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=blah&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=" + VERSION;
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_PARAMETER));
        assertEquals("Invalid oauth_signature_method: blah", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void doNotUseAuthorizationHeader() throws Exception {
        setParamsForRequestToken(assertion);
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUseAuthorizationHeader(false);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + AUTH_HEADER);
    }

    @Test
    public void doNotUseMessageTarget() throws Exception {
        setParamsForRequestToken(assertion);
        setParamsForRequestToken(request, false, true);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUseMessageTarget(false);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void doNotIncludeOAuthVersion() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setUseOAuthVersion(false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1000%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERSION);
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullRequestUrl() throws Exception {
        assertion.setRequestUrl(null);
        new ServerGenerateOAuthSignatureBaseStringAssertion(assertion);
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullHttpMethod() throws Exception {
        assertion.setHttpMethod(null);
        new ServerGenerateOAuthSignatureBaseStringAssertion(assertion);
    }

    @Test(expected = PolicyAssertionException.class)
    public void emptyRequestUrl() throws Exception {
        assertion.setRequestUrl(null);
        new ServerGenerateOAuthSignatureBaseStringAssertion(assertion);
    }

    @Test(expected = PolicyAssertionException.class)
    public void emptyHttpMethod() throws Exception {
        assertion.setHttpMethod(null);
        new ServerGenerateOAuthSignatureBaseStringAssertion(assertion);
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullVariablePrefix() throws Exception {
        assertion.setVariablePrefix(null);
        new ServerGenerateOAuthSignatureBaseStringAssertion(assertion);
    }

    @Test(expected = PolicyAssertionException.class)
    public void emptyVariablePrefix() throws Exception {
        assertion.setVariablePrefix(null);
        new ServerGenerateOAuthSignatureBaseStringAssertion(assertion);
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullUsageMode() throws Exception {
        assertion.setUsageMode(null);
        new ServerGenerateOAuthSignatureBaseStringAssertion(assertion);
    }

    @Test
    public void invalidRequestUrl() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setRequestUrl("*@$   &@(");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_REQUEST_URL));
        assertEquals("Invalid request url: *@$   &@(", (String) policyContext.getVariable("oauth." + ERROR));
    }

    @Test
    public void requestUrlNoScheme() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setRequestUrl("photos.example.net/photos");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_REQUEST_URL));
        assertEquals("Invalid request url: photos.example.net/photos", (String) policyContext.getVariable("oauth." + ERROR));
    }

    @Test
    public void requestUrlResolvesEmpty() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setRequestUrl("${doesnotexist}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_REQUEST_URL));
        assertEquals("Invalid request url: ", (String) policyContext.getVariable("oauth." + ERROR));
    }

    @Test
    public void httpMethodResolvesEmpty() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setHttpMethod("${doesnotexist}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_HTTP_METHOD));
        assertEquals("Missing http method", (String) policyContext.getVariable("oauth." + ERROR));
    }

    @Test
    public void callbackVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthCallback("${callback}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("callback", CALLBACK);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
    }

    @Test
    public void consumerKeyVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthConsumerKey("${consumerKey}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("consumerKey", CONSUMER_KEY);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
    }

    @Test
    public void queryStringVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString("${queryString}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("queryString", QUERY_STRING);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
    }

    @Test
    public void tokenVariable() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthToken("${token}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("token", TOKEN);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, AUTHORIZED_REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(AUTHORIZED_REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAuthRequestTokenVariables();
    }

    @Test
    public void verifierVariable() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthVerifier("${verifier}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("verifier", VERIFIER);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, AUTHORIZED_REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(AUTHORIZED_REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAuthRequestTokenVariables();
    }

    @Test
    public void nullQueryString() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1000%26oauth_version%3D1.0";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
    }

    @Test
    public void nullAuthorizationHeader() throws Exception {
        setParamsForRequestToken(assertion);
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");
        assertion.setAuthorizationHeader(null);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + AUTH_HEADER);
    }

    @Test
    public void nullToken() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthToken(null);
        assertion.setOauthCallback("oob");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Doob%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1000%26" +
                "oauth_verifier%3D473f82d3%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN);
    }

    @Test
    public void nullVerifier() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthVerifier(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1000%26" +
                "oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(ACCESS_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERIFIER);
    }

    @Test
    @BugNumber(13092)
    public void nullCallback() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthCallback(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_callback", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void nonDefaultPrefix() throws Exception {
        assertion.setVariablePrefix("nondefault");
        setParamsForRequestToken(assertion);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("nondefault." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("nondefault." + REQUEST_TYPE));
        assertRequestTokenVariables("nondefault");
    }

    @Test
    public void percentEncodeReservedCharacters() throws Exception {
        // should not be encoded
        final String[] reservedChars = {"-", "_", ".", "~"};
        for (int i = 0; i < reservedChars.length; i++) {
            System.out.print(reservedChars[i] + " ");
            final String encoded = serverAssertion.percentEncode(reservedChars[i]);
            assertEquals("Should not encode reserved Character " + reservedChars[i], reservedChars[i], encoded);
        }
    }

    @Test
    public void percentEncodeWeirdCharacters() throws Exception {
        // should be encoded
        final String[] unReservedWeirdChars = {" ", "#", "<", "\"", ">", "%", "{", "}", "'", "}", "|", "?", ";", "=", "$", "&", "+", ",", "/", ":"};
        for (int i = 0; i < unReservedWeirdChars.length; i++) {
            System.out.print(unReservedWeirdChars[i] + " ");
            assertNotSame("Should encode reserved Character " + unReservedWeirdChars[i], unReservedWeirdChars[i], serverAssertion.percentEncode(unReservedWeirdChars[i]));
        }
    }

    @Test
    public void queryStringMissingQuestionMark() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString("z=last&a=first&p=middle");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void queryStringLeadingBackslash() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString("/z=last&a=first&p=middle");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void queryStringParamWithEmptyValue() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString("?z=last&a=&p=middle");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3D%26oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1000%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void requestTokenMissingConsumerKey() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthConsumerKey(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void requestTokenMissingSignatureMethod() throws Exception {
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setUseMessageTarget(true);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=" + VERSION;
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_signature_method", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void requestTokenMissingTimestamp() throws Exception {
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setUseMessageTarget(true);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=" + SIG_METHOD + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=" + VERSION;
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_timestamp", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void requestTokenMissingNonce() throws Exception {
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setUseMessageTarget(true);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=" + SIG_METHOD + "&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=" + VERSION;
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_nonce", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void authorizedRequestTokenMissingConsumerKey() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthConsumerKey(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void accessTokenMissingConsumerKey() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthConsumerKey(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void requestTokenEmptyConsumerKey() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthConsumerKey("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void requestTokenEmptySignatureMethod() throws Exception {
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setUseMessageTarget(true);
        request.setMethod(HTTP_METHOD);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + FILE_QUERY_STRING);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_token=" + TOKEN + "&" +
                "oauth_version=" + VERSION;
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_signature_method", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void authorizedRequestTokenEmptyConsumerKey() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthConsumerKey("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void accessTokenEmptyConsumerKey() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthConsumerKey("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_consumer_key", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void lowerCaseHttpMethod() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setHttpMethod("get");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void encodedQueryParam() throws Exception {
        setParamsForRequestToken(request, false, true);
        request.setQueryString("Query=Layer%207%20Oauth");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&Query%3DLayer%25207%2520Oauth%26oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dnongeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D2000%26oauth_version%3D1.0";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void clientTokenResolvesEmpty() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthToken("${doesnotexist}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void clientVerifierResolvesEmpty() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthVerifier("${doesnotexist}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, true), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    @BugNumber(13092)
    public void clientCallbackResolvesEmpty() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthCallback("${doesnotexist}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
        assertEquals("Missing oauth_callback", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    public void paramWithMultipleValues() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString(QUERY_STRING + ",a=secondvalue");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26a%3Dsecondvalue%26" +
                "oauth_callback%3D" + CALLBACK_ENCODED + "%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1000%26" +
                "oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    @BugNumber(13108)
    public void paramWithEqualMultipleValues() throws Exception {
        setParamsForRequestToken(assertion);
        //a=first specified twice which is okay as long as it's not an oauth parameter
        assertion.setQueryString(QUERY_STRING + ",a=first");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26a%3Dfirst%26" +
                "oauth_callback%3D" + CALLBACK_ENCODED + "%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1000%26" +
                "oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void paramWithEmptyValue() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString(QUERY_STRING + ",x=");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26" +
                "oauth_callback%3D" + CALLBACK_ENCODED + "%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1000%26" +
                "oauth_version%3D1.0%26p%3Dmiddle%26x%3D%26z%3Dlast";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @BugNumber(12963)
    @Test
    public void requestTokenEmptyOAuthTokenParameter() throws Exception {
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setUseMessageTarget(true);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=" + SIG_METHOD + "&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=" + VERSION + "&" +
                "oauth_token=";
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26" +
                "oauth_callback%3D" + CALLBACK_ENCODED + "%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dnongeneratednonce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D2000%26" +
                "oauth_token%3D%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertTrue(((String) policyContext.getVariable("oauth." + OAUTH_TOKEN)).isEmpty());
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @BugNumber(12982)
    @Test
    public void nonOAuthAuthorizationHeader() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=" + SIG_METHOD + "&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=" + VERSION;
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        request.addHeader("Authorization", "Basic QWxhZGluOnNlc2FtIG9wZW4=");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(POST, REQUEST_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + AUTH_HEADER, "oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void lowerCaseAuthorizationHeader() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        // lower case oauth
        final String authorizationHeader = "oauth realm=\"http://photos.example.net/\"," +
                "oauth_consumer_key=\"" + CONSUMER_KEY + "\"," +
                "oauth_signature_method=\"" + SIG_METHOD + "\"," +
                "oauth_timestamp=\"" + TIMESTAMP + "\"," +
                "oauth_nonce=\"" + NONCE + "\"," +
                "oauth_callback=\"" + CALLBACK + "\"," +
                "oauth_version=\"" + VERSION + "\"";
        request.addHeader("Authorization", authorizationHeader);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(buildExpectedString(GET, REQUEST_TOKEN, false), (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER);
    }

    @BugNumber(12868)
    @Test
    public void unrecognizedOAuthParamInQuery() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString("?oauth_unrecognized=shouldberejected");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_QUERY_PARAMETER));
        assertEquals("Query parameter oauth_unrecognized is not allowed", (String) policyContext.getVariable("oauth.error"));
    }

    @BugNumber(12868)
    @Test
    public void unrecognizedOAuthParamInQueryAllowed() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setAllowCustomOAuthQueryParams(true);
        assertion.setQueryString("?oauth_unrecognized=okay");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1000%26oauth_unrecognized%3Dokay%26oauth_version%3D1.0";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    @BugNumber(13108)
    public void unrecognizedOAuthParamInQueryAllowedMultiple() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setAllowCustomOAuthQueryParams(true);
        assertion.setQueryString("?oauth_unrecognized=okay&oauth_unrecognized=dokay");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1000%26oauth_unrecognized%3Ddokay%26oauth_unrecognized%3Dokay%26oauth_version%3D1.0";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    @BugNumber(13108)
    public void unrecognizedOAuthParamInQueryAllowedEqualMultiple() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setAllowCustomOAuthQueryParams(true);
        assertion.setQueryString("?oauth_unrecognized=okay&oauth_unrecognized=okay");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1000%26oauth_unrecognized%3Dokay%26oauth_unrecognized%3Dokay%26oauth_version%3D1.0";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @BugNumber(12868)
    @Test
    public void unrecognizedOAuthParamInBody() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                "oauth_signature_method=" + SIG_METHOD + "&" +
                "oauth_timestamp=" + TIMESTAMP + "&" +
                "oauth_nonce=" + NONCE + "&" +
                "oauth_callback=" + CALLBACK + "&" +
                "oauth_version=" + VERSION + "&" +
                "oauth_unrecognized=okay";
        request.setContent(body.getBytes());
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setMethod("POST");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dnongeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D2000%26oauth_unrecognized%3Dokay%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @BugNumber(12868)
    @Test
    public void unrecognizedOAuthParamInAuthHeader() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                "oauth_consumer_key=\"" + CONSUMER_KEY + "\"," +
                "oauth_signature_method=\"" + SIG_METHOD + "\"," +
                "oauth_timestamp=\"" + TIMESTAMP + "\"," +
                "oauth_nonce=\"" + NONCE + "\"," +
                "oauth_callback=\"" + CALLBACK + "\"," +
                "oauth_version=\"" + VERSION + "\"," +
                "oauth_unrecognized=\"okay\"";
        request.addHeader("Authorization", authorizationHeader);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3D" + CALLBACK_ENCODED + "%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dnongeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D2000%26oauth_unrecognized%3Dokay%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER);
    }

    @Test
    @BugNumber(13097)
    public void invalidCallback() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthCallback("123");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_PARAMETER));
        assertEquals("Invalid oauth_callback: 123", (String) policyContext.getVariable("oauth.error"));
    }

    @Test
    @BugNumber(13097)
    public void emptyCallback() throws Exception {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                "oauth_consumer_key=\"" + CONSUMER_KEY + "\"," +
                "oauth_signature_method=\"" + SIG_METHOD + "\"," +
                "oauth_timestamp=\"" + TIMESTAMP + "\"," +
                "oauth_nonce=\"" + NONCE + "\"," +
                "oauth_callback=\"\"," +
                "oauth_version=\"" + VERSION + "\"";
        request.addHeader("Authorization", authorizationHeader);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setUsageMode(UsageMode.SERVER);
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_PARAMETER));
        assertEquals("Invalid oauth_callback: ", (String) policyContext.getVariable("oauth.error"));
    }

    private void assertContextVariablesDoNotExist(final String... names) throws NoSuchVariableException {
        for (final String name : names) {
            try {
                policyContext.getVariable(name);
                fail("Expected NoSuchVariableException for " + name);
            } catch (final NoSuchVariableException e) {
                //pass
            }
        }
    }

    /**
     * Sets manual params for a request token.
     */
    private void setParamsForRequestToken(final GenerateOAuthSignatureBaseStringAssertion assertion) {
        assertion.setRequestUrl(REQUEST_URL);
        assertion.setHttpMethod(HTTP_METHOD);
        assertion.setOauthCallback(CALLBACK);
        assertion.setOauthConsumerKey(CONSUMER_KEY);
        assertion.setQueryString(QUERY_STRING);
    }

    /**
     * Sets auth header and/or request body params for a request token.
     */
    private void setParamsForRequestToken(final MockHttpServletRequest request, final boolean authHeader, final boolean requestBody) {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        request.setMethod(HTTP_METHOD);
        if (requestBody) {
            final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                    "oauth_signature_method=" + SIG_METHOD + "&" +
                    "oauth_timestamp=" + TIMESTAMP + "&" +
                    "oauth_nonce=" + NONCE + "&" +
                    "oauth_callback=" + CALLBACK + "&" +
                    "oauth_version=" + VERSION;
            request.setContent(body.getBytes());
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setMethod("POST");
        }
        if (authHeader) {
            final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                    "oauth_consumer_key=\"" + CONSUMER_KEY + "\"," +
                    "oauth_signature_method=\"" + SIG_METHOD + "\"," +
                    "oauth_timestamp=\"" + TIMESTAMP + "\"," +
                    "oauth_nonce=\"" + NONCE + "\"," +
                    "oauth_callback=\"" + CALLBACK + "\"," +
                    "oauth_version=\"" + VERSION + "\"";
            request.addHeader("Authorization", authorizationHeader);
        }
    }

    /**
     * Sets manual parameters for an authenticated request token.
     */
    private void setParamsForAuthRequestToken(final GenerateOAuthSignatureBaseStringAssertion assertion) {
        assertion.setRequestUrl(REQUEST_URL);
        assertion.setHttpMethod(HTTP_METHOD);
        assertion.setOauthConsumerKey(CONSUMER_KEY);
        assertion.setOauthToken(TOKEN);
        assertion.setOauthVerifier(VERIFIER);
        assertion.setQueryString(QUERY_STRING);
    }

    /**
     * Sets auth header and/or request body params for an authenticated request token.
     */
    private void setParamsForAuthRequestToken(final MockHttpServletRequest request, final boolean authHeader, final boolean requestBody) {
        request.setMethod(HTTP_METHOD);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos");
        request.setQueryString(QUERY_STRING);
        if (requestBody) {
            final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                    "oauth_signature_method=" + SIG_METHOD + "&" +
                    "oauth_timestamp=" + TIMESTAMP + "&" +
                    "oauth_nonce=" + NONCE + "&" +
                    "oauth_token=" + TOKEN + "&" +
                    "oauth_verifier=" + VERIFIER + "&" +
                    "oauth_version=" + VERSION;
            request.setContent(body.getBytes());
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setMethod("POST");
        }
        if (authHeader) {
            final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                    "oauth_consumer_key=\"" + CONSUMER_KEY + "\"," +
                    "oauth_signature_method=\"" + SIG_METHOD + "\"," +
                    "oauth_timestamp=\"" + TIMESTAMP + "\"," +
                    "oauth_nonce=\"" + NONCE + "\"," +
                    "oauth_token=\"" + TOKEN + "\"," +
                    "oauth_verifier=\"" + VERIFIER + "\"," +
                    "oauth_version=\"" + VERSION + "\"";
            request.addHeader("Authorization", authorizationHeader);
        }
    }

    /**
     * Sets manual params for an access token.
     */
    private void setParamsForAccessToken(final GenerateOAuthSignatureBaseStringAssertion assertion) {
        assertion.setRequestUrl(REQUEST_URL);
        assertion.setHttpMethod(HTTP_METHOD);
        assertion.setOauthConsumerKey(CONSUMER_KEY);
        assertion.setOauthToken(TOKEN);
        assertion.setQueryString(FILE_QUERY_STRING);
    }

    /**
     * Sets auth header and/or request body params for an access token.
     */
    private void setParamsForAccessToken(final MockHttpServletRequest request, final boolean authHeader, final boolean requestBody) {
        request.setMethod(HTTP_METHOD);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + FILE_QUERY_STRING);
        if (requestBody) {
            final String body = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                    "oauth_signature_method=" + SIG_METHOD + "&" +
                    "oauth_timestamp=" + TIMESTAMP + "&" +
                    "oauth_nonce=" + NONCE + "&" +
                    "oauth_token=" + TOKEN + "&" +
                    "oauth_version=" + VERSION;
            request.setContent(body.getBytes());
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setMethod("POST");
        }
        if (authHeader) {
            final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                    "oauth_consumer_key=\"" + CONSUMER_KEY + "\"," +
                    "oauth_signature_method=\"" + SIG_METHOD + "\"," +
                    "oauth_timestamp=\"" + TIMESTAMP + "\"," +
                    "oauth_nonce=\"" + NONCE + "\"," +
                    "oauth_token=\"" + TOKEN + "\"," +
                    "oauth_version=\"" + VERSION + "\"";
            request.addHeader("Authorization", authorizationHeader);
        }
    }

    private void assertRequestTokenVariables() throws NoSuchVariableException {
        assertRequestTokenVariables("oauth");
    }

    private void assertRequestTokenVariables(final String prefix) throws NoSuchVariableException {
        assertCommonVariables(prefix);
        assertEquals(CALLBACK, (String) policyContext.getVariable(prefix + "." + OAUTH_CALLBACK));
    }

    private void assertCommonVariables(final String prefix) throws NoSuchVariableException {
        if (assertion.getUsageMode().equals(UsageMode.CLIENT)) {
            assertEquals(CLIENT_NONCE, (String) policyContext.getVariable(prefix + "." + OAUTH_NONCE));
            assertEquals(CLIENT_TIMESTAMP, (String) policyContext.getVariable(prefix + "." + OAUTH_TIMESTAMP));
        } else {
            assertEquals(NONCE, (String) policyContext.getVariable(prefix + "." + OAUTH_NONCE));
            assertEquals(TIMESTAMP, (String) policyContext.getVariable(prefix + "." + OAUTH_TIMESTAMP));
        }
        assertEquals(CONSUMER_KEY, (String) policyContext.getVariable(prefix + "." + OAUTH_CONSUMER_KEY));
        assertEquals(SIG_METHOD, (String) policyContext.getVariable(prefix + "." + OAUTH_SIGNATURE_METHOD));
        assertEquals(VERSION, (String) policyContext.getVariable(prefix + "." + OAUTH_VERSION));
    }

    private void assertAuthRequestTokenVariables(final String prefix) throws NoSuchVariableException {
        assertAccessTokenVariables(prefix);
        assertEquals(VERIFIER, (String) policyContext.getVariable(prefix + "." + OAUTH_VERIFIER));
    }

    private void assertAuthRequestTokenVariables() throws NoSuchVariableException {
        assertAuthRequestTokenVariables("oauth");
    }

    private void assertAccessTokenVariables(final String prefix) throws NoSuchVariableException {
        assertCommonVariables(prefix);
        assertEquals(TOKEN, (String) policyContext.getVariable(prefix + "." + OAUTH_TOKEN));
    }

    private void assertAccessTokenVariables() throws NoSuchVariableException {
        assertAccessTokenVariables("oauth");
    }

    private String buildExpectedString(final String httpMethod, final String requestType, final boolean clientSide) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(httpMethod + "&");
        stringBuilder.append("http%3A%2F%2Fphotos.example.net%2Fphotos&");
        if (!ACCESS_TOKEN.equals(requestType)) {
            stringBuilder.append("a%3Dfirst%26");
        } else {
            stringBuilder.append("file%3Dvacation.jpg%26");
        }
        if (REQUEST_TOKEN.equals(requestType)) {
            stringBuilder.append("oauth_callback%3D" + CALLBACK_ENCODED + "%26");
        }
        stringBuilder.append("oauth_consumer_key%3D" + CONSUMER_KEY + "%26");
        if (clientSide) {
            stringBuilder.append("oauth_nonce%3D" + CLIENT_NONCE + "%26");
        } else {
            stringBuilder.append("oauth_nonce%3D" + NONCE + "%26");
        }
        stringBuilder.append("oauth_signature_method%3D" + SIG_METHOD + "%26");
        if (clientSide) {
            stringBuilder.append("oauth_timestamp%3D" + CLIENT_TIMESTAMP + "%26");
        } else {
            stringBuilder.append("oauth_timestamp%3D" + TIMESTAMP + "%26");
        }
        if (AUTHORIZED_REQUEST_TOKEN.equals(requestType)) {
            stringBuilder.append("oauth_token%3D" + TOKEN + "%26oauth_verifier%3D" + VERIFIER + "%26");
        }
        if (ACCESS_TOKEN.endsWith(requestType)) {
            stringBuilder.append("oauth_token%3D" + TOKEN + "%26");
        }
        stringBuilder.append("oauth_version%3D1.0%26");
        if (!ACCESS_TOKEN.equals(requestType)) {
            stringBuilder.append("p%3Dmiddle%26");
            stringBuilder.append("z%3Dlast");
        } else {
            stringBuilder.append("size%3Doriginal");
        }
        return stringBuilder.toString();
    }

    /**
     * Hardcodes the time and nonce returned for client side requests.
     */
    private class TestableServerAssertion extends ServerGenerateOAuthSignatureBaseStringAssertion {
        public TestableServerAssertion(@NotNull final GenerateOAuthSignatureBaseStringAssertion assertion) throws PolicyAssertionException {
            super(assertion);
        }

        @Override
        String generateNonce() {
            return CLIENT_NONCE;
        }
    }
}
