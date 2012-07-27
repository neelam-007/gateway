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
import com.l7tech.util.TestTimeSource;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;

import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.*;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_CALLBACK;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_CONSUMER_KEY;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_NONCE;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_SIGNATURE_METHOD;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_TIMESTAMP;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_TOKEN;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_VERIFIER;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.OAUTH_VERSION;
import static com.l7tech.external.assertions.generateoauthsignaturebasestring.server.ServerGenerateOAuthSignatureBaseStringAssertion.*;
import static org.junit.Assert.*;

public class ServerGenerateOAuthSignatureBaseStringAssertionTest {
    private static final String REQUEST_TOKEN_EXPECTED = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
            "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
            "HMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";
    private static final String AUTHORIZED_REQUEST_TOKEN_EXPECTED = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
            "oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26" +
            "oauth_token%3Dnnch734d00sl2jdk%26oauth_verifier%3D473f82d3%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";
    private static final String ACCESS_TOKEN_EXPECTED = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
            "oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26" +
            "oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal";
    private static final String REQUEST_URL = "http://photos.example.net/photos";
    private static final String CALLBACK = "http://mycallback.com";
    private static final String CONSUMER_KEY = "dpf43f3p2l4k3l03";
    private static final String TIMESTAMP = "1191242096";
    private static final String NONCE = "kllo9940pd9333jh";
    private static final String QUERY_STRING = "?z=last&a=first&p=middle";
    private static final String FILE_QUERY_STRING = "?size=original&file=vacation.jpg";
    private static final String SIG_METHOD = "HMAC-SHA1";
    private static final String VERSION = "1.0";
    private static final String HTTP_METHOD = "GET";
    private static final String TOKEN = "nnch734d00sl2jdk";
    private static final String VERIFIER = "473f82d3";
    private static final String SERVER_NAME = "photos.example.net";

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
        assertion.setUseManualParameters(true);
        assertion.setUseMessageTarget(true);
        assertion.setUseOAuthVersion(true);
        serverAssertion = new TestableServerAssertion(assertion);
        serverAssertion.setTimeSource(new TestTimeSource(1000000L, 1L));
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
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(AUTHORIZED_REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(ACCESS_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(ACCESS_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAccessTokenVariables();
        assertContextVariablesDoNotExist("oauth." + CALLBACK, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void authorizationHeaderRequestToken() throws Exception {
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void authorizationHeaderAuthorizedRequestToken() throws Exception {
        setParamsForAuthRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(AUTHORIZED_REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(AUTHORIZED_REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAuthRequestTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
    }

    @Test
    public void authorizationHeaderAccessToken() throws Exception {
        setParamsForAccessToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(ACCESS_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(ACCESS_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAccessTokenVariables();
        assertEquals(request.getHeader("Authorization"), policyContext.getVariable("oauth." + AUTH_HEADER));
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void messageTargetRequestToken() throws Exception {
        setParamsForRequestToken(request, false, true);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

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
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26" +
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
        assertion.setQueryString("${request.url}");

        final String expected = "POST&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26" +
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
        assertion.setOauthTimestamp(TIMESTAMP);
        assertion.setOauthNonce(NONCE);
        assertion.setQueryString(QUERY_STRING);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void quotesStripped() throws Exception {
        assertion.setRequestUrl(REQUEST_URL);
        assertion.setHttpMethod(HTTP_METHOD);
        assertion.setOauthCallback(CALLBACK);
        assertion.setOauthConsumerKey(CONSUMER_KEY);
        assertion.setOauthTimestamp(TIMESTAMP);
        assertion.setOauthNonce(NONCE);
        // quotes in query string
        assertion.setQueryString("?z=\"last\"&a=\"first\"&p=\"middle\"");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void signatureStripped() throws Exception {
        final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                "oauth_consumer_key=\"dpf43f3p2l4k3l03\"," +
                "oauth_signature_method=\"HMAC-SHA1\"," +
                "oauth_timestamp=\"1191242096\"," +
                "oauth_nonce=\"kllo9940pd9333jh\"," +
                "oauth_callback=\"http://mycallback.com\"," +
                "oauth_signature=\"asdf\"," +
                "oauth_version=\"1.0\"";
        request.addHeader("Authorization", authorizationHeader);
        request.setMethod(HTTP_METHOD);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos?z=last&a=first&p=middle");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void autoTimestamp() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthTimestamp(AUTO);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1000%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void autoNonce() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthNonce(AUTO);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dstubgeneratednonce%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void paramDetectedMoreThanOnce() throws Exception {
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");
        // this is the 2nd time that the consumer key has been set
        assertion.setOauthConsumerKey("2ndconsumerkey");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_DUPLICATE_PARAMETER));
    }

    @Test
    public void paramDetectedMoreThanOnceButEqual() throws Exception {
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        assertion.setQueryString("${request.url}");
        // this is the 2nd time that the consumer key has been set
        assertion.setOauthConsumerKey(CONSUMER_KEY);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void doNotUseManualParameters() throws Exception {
        assertion.setOauthCallback("http://donotusemanualparams.com");
        assertion.setOauthConsumerKey("donotusemanualparams");
        assertion.setOauthTimestamp("1234");
        assertion.setOauthNonce("donotusemanualparams");
        assertion.setOauthVersion("donotusemanualparams");
        assertion.setUseManualParameters(false);

        assertion.setHttpMethod(HTTP_METHOD);
        assertion.setRequestUrl(REQUEST_URL);
        assertion.setQueryString(QUERY_STRING);
        setParamsForRequestToken(request, true, false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
    }

    @Test
    public void doNotUseMessageTargetHeaderOrManualParamsOrAuthHeader() throws Exception {
        assertion.setUseMessageTarget(false);
        assertion.setUseManualParameters(false);
        assertion.setUseAuthorizationHeader(false);
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos?z=last&a=first&p=middle");
        request.setMethod("POST");

        // manual params
        assertion.setOauthCallback("http://manualparams.com");
        assertion.setOauthConsumerKey("manualparams");
        assertion.setOauthTimestamp("manualparams");
        assertion.setOauthNonce("manualparams");
        assertion.setOauthVersion("manualparams");

        // header
        final String authorizationHeader = "OAuth realm=\"http://photos.example.net/\"," +
                "oauth_consumer_key=\"authheader\"," +
                "oauth_signature_method=\"authheader-SHA1\"," +
                "oauth_timestamp=\"authheader\"," +
                "oauth_nonce=\"authheader\"," +
                "oauth_callback=\"http://authheader.com\"," +
                "oauth_version=\"authheader\"";
        request.addHeader("Authorization", authorizationHeader);

        // message target
        final String body = "oauth_consumer_key=requestparams&" +
                "oauth_signature_method=requestparams-SHA1&" +
                "oauth_timestamp=requestparams&" +
                "oauth_nonce=requestparams&" +
                "oauth_callback=http://requestparams.com&" +
                "oauth_version=requestparams";
        request.setContent(body.getBytes());

        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        assertion.setHttpMethod("POST");
        assertion.setRequestUrl(REQUEST_URL);
        assertion.setQueryString(QUERY_STRING);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void doNotIncludeOAuthVersion() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setUseOAuthVersion(false);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26p%3Dmiddle%26z%3Dlast";

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

    @Test
    public void invalidRequestUrl() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setRequestUrl("*@$   &@(");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
    }

    @Test
    public void requestUrlNoScheme() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setRequestUrl("photos.example.net/photos");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_REQUEST_URL));
    }

    @Test
    public void requestUrlResolvesEmpty() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setRequestUrl("${doesnotexist}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_INVALID_REQUEST_URL));
    }

    @Test
    public void httpMethodResolvesEmpty() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setHttpMethod("${doesnotexist}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_HTTP_METHOD));
    }

    @Test
    public void callbackVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthCallback("${callback}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("callback", CALLBACK);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
    }

    @Test
    public void timestampVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthTimestamp("${timestamp}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("timestamp", TIMESTAMP);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
    }

    @Test
    public void nonceVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthNonce("${nonce}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("nonce", NONCE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
    }

    @Test
    public void versionVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthVersion("${version}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("version", "2.0");

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_version%3D2.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertEquals("2.0", (String) policyContext.getVariable("oauth." + OAUTH_VERSION));
    }

    @Test
    public void queryStringVariable() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString("${queryString}");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));
        policyContext.setVariable("queryString", QUERY_STRING);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(AUTHORIZED_REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(AUTHORIZED_REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(AUTHORIZED_REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertAuthRequestTokenVariables();
    }

    @Test
    public void nullQueryString() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_version%3D1.0";

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
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + AUTH_HEADER);
    }

    @Test
    public void nullVersion() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthVersion(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERSION);
    }

    @Test
    public void nullToken() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthToken(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26" +
                "oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26" +
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
                "oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26" +
                "oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(ACCESS_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + OAUTH_VERIFIER);
    }

    @Test
    public void nullCallback() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthCallback(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3Dfirst%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(expected, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertContextVariablesDoNotExist("oauth." + OAUTH_CALLBACK);
    }

    @Test
    public void nonDefaultPrefix() throws Exception {
        assertion.setVariablePrefix("nondefault");
        setParamsForRequestToken(assertion);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("nondefault." + SIG_BASE_STRING));
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
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
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
        assertEquals(REQUEST_TOKEN_EXPECTED, (String) policyContext.getVariable("oauth." + SIG_BASE_STRING));
        assertEquals(REQUEST_TOKEN, (String) policyContext.getVariable("oauth." + REQUEST_TYPE));
        assertRequestTokenVariables();
        assertContextVariablesDoNotExist("oauth." + OAUTH_TOKEN, "oauth." + OAUTH_VERIFIER, "oauth." + AUTH_HEADER);
    }

    @Test
    public void queryStringParamWithEmptyValue() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setQueryString("?z=last&a=&p=middle");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final String expected = "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&a%3D%26oauth_callback%3Dhttp%3A%2F%2Fmycallback.com%26" +
                "oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3D" +
                "HMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_version%3D1.0%26p%3Dmiddle%26z%3Dlast";

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
    }

    @Test
    public void requestTokenMissingSignatureMethod() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthSignatureMethod(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void requestTokenMissingTimestamp() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthTimestamp(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void requestTokenMissingNonce() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthNonce(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenMissingConsumerKey() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthConsumerKey(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenMissingSignatureMethod() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthSignatureMethod(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenMissingTimestamp() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthTimestamp(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenMissingNonce() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthNonce(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenMissingConsumerKey() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthConsumerKey(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenMissingSignatureMethod() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthSignatureMethod(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenMissingTimestamp() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthTimestamp(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenMissingNonce() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthNonce(null);
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void requestTokenEmptyConsumerKey() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthConsumerKey("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void requestTokenEmptySignatureMethod() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthSignatureMethod("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void requestTokenEmptyTimestamp() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthTimestamp("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void requestTokenEmptyNonce() throws Exception {
        setParamsForRequestToken(assertion);
        assertion.setOauthNonce("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenEmptyConsumerKey() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthConsumerKey("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenEmptySignatureMethod() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthSignatureMethod("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenEmptyTimestamp() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthTimestamp("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void authorizedRequestTokenEmptyNonce() throws Exception {
        setParamsForAuthRequestToken(assertion);
        assertion.setOauthNonce("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenEmptyConsumerKey() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthConsumerKey("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenEmptySignatureMethod() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthSignatureMethod("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenEmptyTimestamp() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthTimestamp("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
    }

    @Test
    public void accessTokenEmptyNonce() throws Exception {
        setParamsForAccessToken(assertion);
        assertion.setOauthNonce("");
        requestMessage.attachHttpRequestKnob(new HttpServletRequestKnob(request));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.OAUTH_MISSING_PARAMETER));
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
        assertion.setOauthTimestamp(TIMESTAMP);
        assertion.setOauthNonce(NONCE);
        assertion.setQueryString(QUERY_STRING);
    }

    /**
     * Sets auth header and/or request body params for a request token.
     */
    private void setParamsForRequestToken(final MockHttpServletRequest request, final boolean authHeader, final boolean requestBody) {
        request.setServerName(SERVER_NAME);
        request.setRequestURI("/photos" + QUERY_STRING);
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
        assertion.setOauthTimestamp(TIMESTAMP);
        assertion.setOauthNonce(NONCE);
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
        request.setRequestURI("/photos" + QUERY_STRING);
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
        assertion.setOauthTimestamp(TIMESTAMP);
        assertion.setOauthNonce(NONCE);
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
        assertEquals(CONSUMER_KEY, (String) policyContext.getVariable(prefix + "." + OAUTH_CONSUMER_KEY));
        assertEquals(NONCE, (String) policyContext.getVariable(prefix + "." + OAUTH_NONCE));
        assertEquals(SIG_METHOD, (String) policyContext.getVariable(prefix + "." + OAUTH_SIGNATURE_METHOD));
        assertEquals(TIMESTAMP, (String) policyContext.getVariable(prefix + "." + OAUTH_TIMESTAMP));
        assertEquals(VERSION, (String) policyContext.getVariable(prefix + "." + OAUTH_VERSION));
        assertEquals(CALLBACK, (String) policyContext.getVariable(prefix + "." + OAUTH_CALLBACK));
    }

    private void assertAuthRequestTokenVariables() throws NoSuchVariableException {
        assertAccessTokenVariables();
        assertEquals(VERIFIER, (String) policyContext.getVariable("oauth." + OAUTH_VERIFIER));
    }

    private void assertAccessTokenVariables() throws NoSuchVariableException {
        assertEquals(CONSUMER_KEY, (String) policyContext.getVariable("oauth." + OAUTH_CONSUMER_KEY));
        assertEquals(NONCE, (String) policyContext.getVariable("oauth." + OAUTH_NONCE));
        assertEquals(SIG_METHOD, (String) policyContext.getVariable("oauth." + OAUTH_SIGNATURE_METHOD));
        assertEquals(TIMESTAMP, (String) policyContext.getVariable("oauth." + OAUTH_TIMESTAMP));
        assertEquals(VERSION, (String) policyContext.getVariable("oauth." + OAUTH_VERSION));
        assertEquals(TOKEN, (String) policyContext.getVariable("oauth." + OAUTH_TOKEN));
    }

    /**
     * Hardcodes the time and nonce returned.
     */
    private class TestableServerAssertion extends ServerGenerateOAuthSignatureBaseStringAssertion {
        public TestableServerAssertion(@NotNull final GenerateOAuthSignatureBaseStringAssertion assertion) throws PolicyAssertionException {
            super(assertion);
        }

        @Override
        String generateNonce() {
            return "stubgeneratednonce";
        }
    }
}
