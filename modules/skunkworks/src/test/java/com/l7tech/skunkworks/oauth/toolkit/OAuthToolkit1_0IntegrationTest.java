package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.*;

/**
 * Integration tests for the OAuth Tool Kit. Each test requires the SSG to be running with an installed OTK that includes
 * the default OTK client.
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 * <p/>
 * At minimum you need to change the BASE_URL and SIGNATURE strings.
 * <p/>
 * Note: These tests will FAIL if they are executed more than once and the Protect Against Message Replay assertion is enabled in policy.
 * Either disable the assertion or shorten the expiry period.
 */
@Ignore
public class OAuthToolkit1_0IntegrationTest {
    //localhost
    private static final String BASE_URL = "localhost";
    private static final String SIGNATURE = "zQQtKbwhcAcDnwzI8gg6f2tBHUQ="; // method = POST
    private static final String SIGNATURE_GET = "y9ktczNolnPv+DlvlWjijIEcrfY="; // method = GET
    private static final String SIGNATURE_EMPTY_TOKEN = "pMitXg+kdUV34iFDDV6UnuhWkps=";
    private static final String SIGNATURE_OOB = "Ggy6504qjHwaGG/lBJrRGSBDd+0=";
    private static final String SIGNATURE_INVALID_TOKEN = "TnSzZi5/MQr9dTIBxrH9Jei0DhQ=";

    //aleeoauth.l7tech.com
//    private static final String BASE_URL = "aleeoauth.l7tech.com";
//    private static final String SIGNATURE = "pwvUKSVVqeU4nZsBfbm1Pr6lEpk=";
//    private static final String SIGNATURE_GET = ""; // method = GET
//    private static final String SIGNATURE_EMPTY_TOKEN = "zMX7NBnryxDm+x3MJTJx2KP/eQw=";

    private static final String CLIENT_REQUEST_TOKEN = "https://" + BASE_URL + ":8443/oauth/v1/client?state=request_token";
    private static final String REQUEST_TOKEN_ENDPOINT = "https://" + BASE_URL + ":8443/auth/oauth/v1/request";
    private static final String AUTHORIZE_ENDPOINT = "https://" + BASE_URL + ":8443/auth/oauth/v1/authorize";
    private static final String ACCESS_TOKEN_ENDPOINT = "https://" + BASE_URL + ":8443/auth/oauth/v1/token";
    private static final String CLIENT_DOWNLOAD_RESOURCE = "https://" + BASE_URL + ":8443/oauth/v1/client?state=download";
    private static final String OTK_CLIENT_CALLBACK = "https://" + BASE_URL + ":8443/oauth/v1/client?state=authorized";
    private static final String PROTECTED_RESOURCE_ENDPOINT = "https://" + BASE_URL + ":8443/protected/resource";
    private static String OTK_CLIENT_CALLBACK_ENCODED;

    static {
        try {
            OTK_CLIENT_CALLBACK_ENCODED = URLEncoder.encode(OTK_CLIENT_CALLBACK, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static final String AUTH_HEADER = "OAuth realm=\"http://" + BASE_URL + "\",oauth_consumer_key=\"acf89db2-994e-427b-ac2c-88e6101f9433\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"1344979213\",oauth_nonce=\"ca6e55f3-3e1c-41d6-8584-41c7e2611d34\",oauth_callback=\"" + OTK_CLIENT_CALLBACK_ENCODED + "\",oauth_version=\"1.0\",oauth_signature=\"" + SIGNATURE + "\"";
    private static final String USER = "admin";
    private static final String PASSWORD = "password";
    private static final String OTK_CLIENT_CONSUMER_KEY = "acf89db2-994e-427b-ac2c-88e6101f9433";
    private GenericHttpClient client;

    private PasswordAuthentication passwordAuthentication;

    @Before
    public void setup() {
        client = new CommonsHttpClient();
        passwordAuthentication = new PasswordAuthentication(USER, PASSWORD.toCharArray());
    }

    /**
     * Request a request token using the OTK client. The client will process the oauth request token request for you.
     */
    @Test
    public void requestTokenClient() throws Exception {
        final String oauthToken = getRequestTokenFromClient();
        assertFalse(oauthToken.isEmpty());
    }

    /**
     * Request a request token via OTK endpoint. You are responsible for passing the correct oauth parameters.
     */
    @Test
    public void requestTokenEndpoint() throws Exception {
        final Map<String, String> responseParameters = getRequestTokenFromEndpoint();
        assertParamsFromRequestTokenEndpoint(responseParameters);
    }

    /**
     * OAuth params are in the Authorization header instead of request body.
     */
    @Test
    public void requestTokenEndpointAuthorizationHeader() throws Exception {
        System.out.println("Requesting request token from " + REQUEST_TOKEN_ENDPOINT + " using Authorization header");

        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(REQUEST_TOKEN_ENDPOINT));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        requestParams.setExtraHeaders(new HttpHeader[]{new GenericHttpHeader("Authorization", AUTH_HEADER)});
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final Map<String, String> responseParameters = extractParamsFromString(response.getAsString(false, Integer.MAX_VALUE));
        assertParamsFromRequestTokenEndpoint(responseParameters);
    }

    /**
     * All oauth parameters are in the URL.
     * <p/>
     * Note: this test will not pass on the tactical OTK.
     */
    @BugNumber(12974)
    @Test
    public void requestTokenEndpointUrlParameters() throws Exception {
        final StringBuilder stringBuilder = new StringBuilder(REQUEST_TOKEN_ENDPOINT);
        stringBuilder.append("?");
        final Map<String, String> oauthParameters = createDefaultRequestTokenParameters();
        oauthParameters.put("oauth_callback", OTK_CLIENT_CALLBACK_ENCODED);
        oauthParameters.put("oauth_signature", URLEncoder.encode(SIGNATURE_GET, "UTF-8"));
        for (final Map.Entry<String, String> entry : oauthParameters.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append("=");
            stringBuilder.append(entry.getValue());
            stringBuilder.append("&");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        final String url = stringBuilder.toString();
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(url));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final Map<String, String> responseParameters = extractParamsFromString(response.getAsString(false, Integer.MAX_VALUE));
        assertParamsFromRequestTokenEndpoint(responseParameters);
    }

    /**
     * Signature doesn't pass verification.
     */
    @Test
    public void requestTokenEndpointInvalidSignature() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_signature", "invalidSignature");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        assertEquals(500, response.getStatus());
    }

    /**
     * Consumer key isn't recognized by the OTK.
     */
    @Test
    @BugNumber(13103)
    public void requestTokenEndpointUnrecognizedConsumerKey() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_consumer_key", "unregisteredClient");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(401, response.getStatus());
        assertEquals("oauth_consumer_key invalid or expired", responseBody);
    }

    /**
     * Currently OTK only supports HMAC-SHA1.
     */
    @Test
    public void requestTokenEndpointSignatureMethodNotHMACSHA1() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_signature_method", "RSA-SHA1");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth_signature_method: RSA-SHA1", responseBody);
    }

    /**
     * Check required parameters.
     */
    @Test
    public void requestTokenEndpointMissingParameters() throws Exception {
        assertRequestEndpointMissingParameter("oauth_consumer_key");
        assertRequestEndpointMissingParameter("oauth_signature_method");
        assertRequestEndpointMissingParameter("oauth_timestamp");
        assertRequestEndpointMissingParameter("oauth_nonce");
        assertRequestEndpointMissingParameter("oauth_signature");
    }

    @Test
    public void requestTokenEndpointInvalidVersion() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_version", "2.0");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth_version: 2.0", responseBody);
    }

    @Test
    public void requestTokenEndpointDuplicateParameter() throws Exception {
        final GenericHttpRequest request = createRequestTokenEndpointRequest(createDefaultRequestTokenParameters());
        request.addParameter("oauth_callback", "duplicateCallback");
        final GenericHttpResponse response = request.getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Duplicate oauth parameter: oauth_callback", responseBody);
    }

    @Test
    public void requestTokenEndpointNotGetOrPost() throws Exception {
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(REQUEST_TOKEN_ENDPOINT));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        requestParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.PUT, requestParams);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(400, response.getStatus());
        assertEquals("PUT not permitted", new String(IOUtils.slurpStream(response.getInputStream())));
    }

    /**
     * Obtain an access token via OTK client.
     * <p/>
     * Using the client so that we can use the default callback url to verify the access token.
     */
    @Test
    public void accessTokenClient() throws Exception {
        final String accessToken = getAccessTokenFromClient();

        assertFalse(accessToken.isEmpty());
    }

    /**
     * Download a protected resource using the client.
     * <p/>
     * Using the client so that we can use the default callback url to obtain the access token.
     */
    @Test
    public void downloadResourceClient() throws Exception {
        System.out.println("Downloading a protected resource from: " + CLIENT_DOWNLOAD_RESOURCE);
        final String accessToken = getAccessTokenFromClient();

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(CLIENT_DOWNLOAD_RESOURCE +
                "&oauth_consumer_key=" + OTK_CLIENT_CONSUMER_KEY + "&oauth_token=" + accessToken + "&tempuser=system"));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);

        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        assertTrue(response.getAsString(false, Integer.MAX_VALUE).contains("You made it, here is the resource"));
        System.out.println("Successfully downloaded a protected resource");
    }

    /**
     * Hit access token endpoint with missing parameters.
     */
    @Test
    public void accessTokenEndpointMissingParameters() throws Exception {
        assertTokenEndpointMissingParameter("oauth_signature");
        assertTokenEndpointMissingParameter("oauth_nonce");
        assertTokenEndpointMissingParameter("oauth_signature_method");
        assertTokenEndpointMissingParameter("oauth_consumer_key");
        assertTokenEndpointMissingParameter("oauth_timestamp");
    }

    /**
     * Hit access token endpoint with non GET/POST method.
     */
    @Test
    public void accessTokenEndpointNotGetOrPost() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(ACCESS_TOKEN_ENDPOINT));
        params.setSslSocketFactory(getSSLSocketFactory());
        params.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest request = client.createRequest(HttpMethod.PUT, params);
        final GenericHttpResponse response = request.getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("PUT not permitted", responseBody);
    }

    /**
     * Hit access token endpoint with oauth_version not equal to 1.0.
     */
    @Test
    public void accessTokenEndpointInvalidVersion() throws Exception {
        final Map<String, String> parameters = createDefaultAccessTokenParameters();
        parameters.put("oauth_version", "2.0");
        final GenericHttpResponse response = createAccessTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth_version: 2.0", responseBody);
    }

    /**
     * Hit access token endpoint with a duplicate oauth parameter.
     */
    @Test
    public void accessTokenEndpointDuplicateParameter() throws Exception {
        final GenericHttpRequest request = createAccessTokenEndpointRequest(createDefaultAccessTokenParameters());
        request.addParameter("oauth_token", "testTokenDuplicate");
        final GenericHttpResponse response = request.getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Duplicate oauth parameter: oauth_token", responseBody);
    }

    @BugNumber(12963)
    @Test
    public void requestTokenEndpointEmptyOAuthTokenParameter() throws Exception {
        // signature generated using OTK client consumer secret
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_signature", SIGNATURE_EMPTY_TOKEN);
        parameters.put("oauth_token", "");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        assertEquals(200, response.getStatus());

        final String responseAsString = response.getAsString(false, Integer.MAX_VALUE);
        final Map<String, String> responseParameters = extractParamsFromString(responseAsString);
        assertParamsFromRequestTokenEndpoint(responseParameters);
    }

    @BugNumber(12868)
    @Test
    public void unrecognizedOAuthQueryParameter() throws Exception {
        final StringBuilder stringBuilder = new StringBuilder(REQUEST_TOKEN_ENDPOINT);
        stringBuilder.append("?");
        final Map<String, String> oauthParameters = createDefaultRequestTokenParameters();
        oauthParameters.put("oauth_callback", OTK_CLIENT_CALLBACK_ENCODED);
        oauthParameters.put("oauth_signature", URLEncoder.encode(SIGNATURE_GET, "UTF-8"));
        oauthParameters.put("oauth_unrecognized", "shouldberejected");
        for (final Map.Entry<String, String> entry : oauthParameters.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append("=");
            stringBuilder.append(entry.getValue());
            stringBuilder.append("&");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        final String url = stringBuilder.toString();
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(url));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();

        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Query parameter oauth_unrecognized is not allowed", responseBody);
    }

    @Test
    @BugNumber(13097)
    public void requestTokenEndpointInvalidCallback() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_callback", "123");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth_callback: 123", responseBody);
    }

    @BugNumber(13092)
    @Test
    public void requestTokenEndpointNoCallback() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.remove("oauth_callback");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth parameters", responseBody);
    }

    @BugNumber(13092)
    @Test
    public void requestTokenEndpointEmptyTokenNoCallback() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.remove("oauth_callback");
        parameters.put("oauth_token", "");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth parameters", responseBody);
    }

    @Test
    @BugNumber(13103)
    public void accessTokenEndpointUnrecognizedConsumerKey() throws Exception {
        final Map<String, String> parameters = createDefaultAccessTokenParameters();
        parameters.put("oauth_consumer_key", "unregistered");
        final GenericHttpResponse response = createAccessTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(401, response.getStatus());
        assertEquals("oauth_consumer_key invalid or expired", responseBody);
    }

    @Test
    @BugNumber(13103)
    public void protectedResourceEndpointUnrecognizedConsumerKey() throws Exception {
        final Map<String, String> parameters = createDefaultProtectedResourceParameters();
        parameters.put("oauth_consumer_key", "unregistered");
        final GenericHttpResponse response = createProtectedResourceEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(401, response.getStatus());
        assertEquals("oauth_consumer_key invalid or expired", responseBody);
    }

    @Test
    @BugNumber(13103)
    public void requestTokenEndpointInvalidTimestamp() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_timestamp", "abc");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth_timestamp: abc", responseBody);
    }

    @Test
    @BugNumber(13117)
    public void tokenEndpointInvalidToken() throws Exception {
        final Map<String, String> parameters = createDefaultAccessTokenParameters();
        parameters.put("oauth_token", "invalid");
        parameters.put("oauth_signature", "invalidTokenSignature");
        final GenericHttpResponse response = createAccessTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(401, response.getStatus());
        assertEquals("oauth_token is invalid or expired", responseBody);
    }

    @Test
    @BugNumber(13117)
    public void authorizeEndpointInvalidToken() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT + "?oauth_token=10056aed-2b18-43bd-ac48-3e8b43560031"));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(401, response.getStatus());
        assertEquals("oauth_token is invalid or expired", responseBody);
    }

    @Test
    @BugNumber(13117)
    public void protectedResourceEndpointInvalidToken() throws Exception {
        final Map<String, String> parameters = createDefaultProtectedResourceParameters();
        parameters.put("oauth_token", "invalid");
        parameters.put("oauth_signature", SIGNATURE_INVALID_TOKEN);
        final GenericHttpResponse response = createProtectedResourceEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        //assertEquals(400, response.getStatus());
        assertEquals("oauth_token is invalid or expired", responseBody);
    }

    @Test
    @BugNumber(13124)
    public void tokenEndpointNoVerifier() throws Exception {
        final Map<String, String> parameters = createDefaultAccessTokenParameters();
        parameters.remove("oauth_verifier");
        final GenericHttpResponse response = createAccessTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth parameters", responseBody);
    }

    @Test
    @BugNumber(13124)
    public void requestTokenEndpointWithToken() throws Exception {
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.put("oauth_token", "shouldnotbehere");
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth parameters", responseBody);
    }

    @Test
    @BugNumber(13124)
    public void tokenEndpointVerifierWithoutToken() throws Exception {
        final Map<String, String> parameters = createDefaultAccessTokenParameters();
        parameters.remove("oauth_token");
        final GenericHttpResponse response = createAccessTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth parameters", responseBody);
    }

    @Test
    @BugNumber(13339)
    public void tokenEndpointVerifierWithEmptyToken() throws Exception {
        final Map<String, String> parameters = createDefaultAccessTokenParameters();
        parameters.put("oauth_token", "");
        final GenericHttpResponse response = createAccessTokenEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth parameters", responseBody);
    }

    @Test
    @BugNumber(13124)
    public void protectedResourceEndpointWithVerifier() throws Exception {
        final Map<String, String> parameters = createDefaultProtectedResourceParameters();
        parameters.put("oauth_verifier", "shouldnotbehere");
        final GenericHttpResponse response = createProtectedResourceEndpointRequest(parameters).getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Invalid oauth parameters", responseBody);
    }

    @Test
    public void authorizeEndpoint() throws Exception {
        final String requestToken = getRequestTokenFromEndpoint().get("oauth_token");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT + "?oauth_token=" + requestToken));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("sessionID"));
        assertTrue(responseBody.contains("username"));
        assertTrue(responseBody.contains("password"));
    }

    @Test
    @BugNumber(13180)
    public void authorizeEndpointNoToken() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Missing oauth_token parameter"));
    }

    @Test
    @BugNumber(13112)
    public void authorizeEndpointDuplicateToken() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT + "?oauth_token=one&oauth_token=two"));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals("Duplicate oauth_token parameter", responseBody);
    }

    @Test
    @BugNumber(13155)
    public void authorizeWithInvalidCookie() throws Exception {
        // first get a valid request token
        final Map<String, String> params = createDefaultRequestTokenParameters();
        params.put("oauth_callback", "oob");
        params.put("oauth_signature", SIGNATURE_OOB);
        final GenericHttpResponse requestTokenResponse = createRequestTokenEndpointRequest(params).getResponse();
        assertEquals(200, requestTokenResponse.getStatus());
        final String requestToken = extractParamsFromString(requestTokenResponse.getAsString(false, Integer.MAX_VALUE)).get("oauth_token");

        // try to authorize the token using an invalid cookie
        final GenericHttpResponse authorizeResponse = new Layer710aApi(BASE_URL).authorize(requestToken, "invalid");
        final String authorizeResponseBody = new String(IOUtils.slurpStream(authorizeResponse.getInputStream()));
        System.out.println(authorizeResponseBody);
        assertEquals(401, authorizeResponse.getStatus());
        assertFalse(authorizeResponseBody.contains("verifier"));
        assertTrue(authorizeResponseBody.contains("Session Expired"));
        assertEquals("l7otk1a=", authorizeResponse.getHeaders().getFirstValue("Set-Cookie"));
    }

    @Test
    @BugNumber(12868)
    public void authorizeEndpointUnrecognizedOAuthQueryParameter() throws Exception {
        final String requestToken = getRequestTokenFromEndpoint().get("oauth_token");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT + "?oauth_token=" + requestToken + "&oauth_unrecognized=invalid"));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals("Invalid oauth parameter(s)", responseBody);
    }

    @Test
    public void authorizeEndpointExtraNonOAuthParameter() throws Exception {
        final String requestToken = getRequestTokenFromEndpoint().get("oauth_token");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT + "?oauth_token=" + requestToken + "&test=okay"));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("sessionID"));
        assertTrue(responseBody.contains("username"));
        assertTrue(responseBody.contains("password"));
    }

    @Test
    public void authorizeEndpointExtraOAuthParameter() throws Exception {
        final String requestToken = getRequestTokenFromEndpoint().get("oauth_token");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT + "?oauth_token=" + requestToken + "&oauth_version=1.0"));
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("sessionID"));
        assertTrue(responseBody.contains("username"));
        assertTrue(responseBody.contains("password"));
    }

    @Test
    @BugNumber(12868)
    public void tokenEndpointUnrecognizedOAuthQueryParameter() throws Exception {
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(ACCESS_TOKEN_ENDPOINT + "?oauth_unrecognized=invalid"));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        requestParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, requestParams);
        for (final Map.Entry<String, String> entry : createDefaultAccessTokenParameters().entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        final GenericHttpResponse response = request.getResponse();
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Query parameter oauth_unrecognized is not allowed", responseBody);
    }

    private GenericHttpRequest createAccessTokenEndpointRequest(final Map<String, String> parameters) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(ACCESS_TOKEN_ENDPOINT));
        params.setSslSocketFactory(getSSLSocketFactory());
        params.setPasswordAuthentication(passwordAuthentication);
        params.setExtraHeaders(new HttpHeader[]{new GenericHttpHeader("Content-Type", "application/x-www-form-urlencoded")});
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        return request;
    }

    private String getAccessTokenFromClient() throws Exception {
        final String requestToken = getRequestTokenFromClient();
        final String accessToken = new Layer710aApi(BASE_URL).authorizeAndRetrieve(requestToken, false, passwordAuthentication);
        assertFalse(accessToken.equalsIgnoreCase(requestToken));
        return accessToken;
    }

    private String getRequestTokenFromClient() throws Exception {
        System.out.println("Requesting request token from " + CLIENT_REQUEST_TOKEN);
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(CLIENT_REQUEST_TOKEN));
        requestParams.setFollowRedirects(false);
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);
        final GenericHttpResponse response = request.getResponse();
        // redirect
        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        final String token = StringUtils.substringBetween(locationHeader, "oauth_token=", "&");
        System.out.println("Received oauth_token: " + token);
        return token;
    }

    private Map<String, String> getRequestTokenFromEndpoint() throws Exception {
        // signature generated using OTK client consumer secret
        final GenericHttpResponse response = createRequestTokenEndpointRequest(createDefaultRequestTokenParameters()).getResponse();
        assertEquals(200, response.getStatus());

        // expected response format: oauth_token=49f0589b-7b8c-4b3d-a416-e9b0fc6bf401&oauth_token_secret=2d288801-7e33-49ca-8f3c-0c39df56664c&oauth_callback_confirmed=true
        final String responseAsString = response.getAsString(false, Integer.MAX_VALUE);
        return extractParamsFromString(responseAsString);
    }

    private Map<String, String> extractParamsFromString(final String responseAsString) {
        final String[] pairs = responseAsString.split("&");
        final Map<String, String> responseParameters = new HashMap<String, String>(pairs.length);
        for (int i = 0; i < pairs.length; i++) {
            final String pair = pairs[i];
            final String[] keyValue = pair.split("=");
            responseParameters.put(keyValue[0], keyValue[1]);
        }
        return responseParameters;
    }

    private GenericHttpRequest createRequestTokenEndpointRequest(final Map<String, String> parameters) throws Exception {
        System.out.println("Requesting request token from " + REQUEST_TOKEN_ENDPOINT);
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(REQUEST_TOKEN_ENDPOINT));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        requestParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, requestParams);
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        return request;
    }

    private GenericHttpRequest createProtectedResourceEndpointRequest(final Map<String, String> parameters) throws Exception {
        System.out.println("Requesting protected resource from " + PROTECTED_RESOURCE_ENDPOINT);
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(PROTECTED_RESOURCE_ENDPOINT));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        requestParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, requestParams);
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        return request;
    }

    private Map<String, String> createDefaultRequestTokenParameters() {
        final Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("oauth_consumer_key", OTK_CLIENT_CONSUMER_KEY);
        parameterMap.put("oauth_signature_method", "HMAC-SHA1");
        parameterMap.put("oauth_timestamp", "1344979213");
        parameterMap.put("oauth_nonce", "ca6e55f3-3e1c-41d6-8584-41c7e2611d34");
        parameterMap.put("oauth_callback", OTK_CLIENT_CALLBACK);
        parameterMap.put("oauth_version", "1.0");
        parameterMap.put("oauth_signature", SIGNATURE);
        return parameterMap;
    }

    private Map<String, String> createDefaultAccessTokenParameters() {
        final Map<String, String> oauthParameters = new HashMap<String, String>();
        oauthParameters.put("oauth_signature", "testSignature");
        oauthParameters.put("oauth_nonce", "testNonce");
        oauthParameters.put("oauth_signature_method", "HMAC-SHA1");
        oauthParameters.put("oauth_consumer_key", OTK_CLIENT_CONSUMER_KEY);
        oauthParameters.put("oauth_timestamp", "1000000000");
        oauthParameters.put("oauth_token", "testToken");
        oauthParameters.put("oauth_verifier", "testVerifier");
        oauthParameters.put("oauth_version", "1.0");
        return oauthParameters;
    }

    private Map<String, String> createDefaultProtectedResourceParameters() {
        final Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("oauth_consumer_key", OTK_CLIENT_CONSUMER_KEY);
        parameterMap.put("oauth_signature_method", "HMAC-SHA1");
        parameterMap.put("oauth_timestamp", "1344979213");
        parameterMap.put("oauth_nonce", "ca6e55f3-3e1c-41d6-8584-41c7e2611d34");
        parameterMap.put("oauth_version", "1.0");
        parameterMap.put("oauth_signature", "test");
        parameterMap.put("oauth_token", "test");
        return parameterMap;
    }

    private void assertTokenEndpointMissingParameter(final String missingParameter) throws Exception {
        System.out.println("testing missing parameter: " + missingParameter);
        final Map<String, String> oauthParameters = createDefaultAccessTokenParameters();
        oauthParameters.remove(missingParameter);
        final GenericHttpResponse response = createAccessTokenEndpointRequest(oauthParameters).getResponse();
        assertMissingParameter(missingParameter, response);
    }

    private void assertRequestEndpointMissingParameter(final String missingParameter) throws Exception {
        System.out.println("testing missing parameter: " + missingParameter);
        final Map<String, String> parameters = createDefaultRequestTokenParameters();
        parameters.remove(missingParameter);
        final GenericHttpResponse response = createRequestTokenEndpointRequest(parameters).getResponse();
        assertMissingParameter(missingParameter, response);
    }

    private void assertMissingParameter(final String parameter, final GenericHttpResponse response) throws Exception {
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertEquals("Missing " + parameter, responseBody);
    }

    private void assertParamsFromRequestTokenEndpoint(final Map<String, String> responseParameters) {
        final String oauthToken = responseParameters.get("oauth_token");
        final String oauthTokenSecret = responseParameters.get("oauth_token_secret");
        final String callbackConfirmed = responseParameters.get("oauth_callback_confirmed").trim();
        assertFalse(oauthToken.isEmpty());
        assertFalse(oauthTokenSecret.isEmpty());
        assertTrue(callbackConfirmed.equalsIgnoreCase("true") || callbackConfirmed.equalsIgnoreCase("false"));
        System.out.println("Received oauth_token: " + oauthToken);
        System.out.println("Received oauth_token_secret: " + oauthTokenSecret);
        System.out.println("Callback confirmed: " + callbackConfirmed);
    }
}
