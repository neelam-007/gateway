package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.getSSLSocketFactory;
import static org.junit.Assert.*;

/**
 * Integration tests for the OAuth Tool Kit (oauth version 2.0). Each test requires the SSG to be running with an installed OTK that includes
 * the default OTK client.
 * <p/>
 * Requires wsman to be published on the gateway (Gateway Management Service internal service).
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 */
@Ignore
public class OAuthToolkit2_0IntegrationTest extends OAuthToolkitSupport {
    //LOCALHOST
    private static final String BASE_URL = "localhost";
    private static final String CONSUMER_KEY = "182637fd-8b6b-4dca-9192-3d1e23d556b5";
    private static final String CONSUMER_SECRET = "de88c414-fb69-4107-aac0-d1fdf0986017";
    // this callback must be the registered callback on the client
    private static final String CALLBACK = "https://" + BASE_URL + ":8443/oauth_callback";
    private static final PasswordAuthentication PASSWORD_AUTHENTICATION = new PasswordAuthentication("admin", "password".toCharArray());
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String HMAC_SHA_1 = "HmacSHA1";
    private static final String PROTECTED_API = "/oauth/v2/protectedapi";
    private GenericHttpClient client;

    @Before
    public void setup() {
        client = new CommonsHttpClient();
    }

    @BugNumber(12946)
    @Test
    public void authCodeGrantDeniedClient() throws Exception {
        final GenericHttpRequestParams requestParams =
                new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/v2/client/authcode?state=state_test&error=access_denied"));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Access was denied"));
    }

    @BugNumber(12946)
    @Test
    public void implicitGrantDeniedClient() throws Exception {
        final GenericHttpRequestParams requestParams =
                new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/v2/client/implicit?state=state_test&error=access_denied"));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Access was denied"));
    }

    @Test
    @BugNumber(13155)
    public void authorizeWithInvalidCookie() throws Exception {
        final GenericHttpResponse response = new Layer720Api(BASE_URL).authorize("code", CONSUMER_KEY, CALLBACK, null, "invalid", true, "Grant");

        assertEquals(401, response.getStatus());
        final String body = new String(IOUtils.slurpStream(response.getInputStream()));
        assertFalse(body.contains("verifier"));
        assertTrue(body.contains("Authentication failed"));
        assertEquals("l7otk2a=", response.getHeaders().getFirstValue("Set-Cookie"));
    }

    /**
     * Redirect_uri is not required if the client has ONE registered callback.
     * <p/>
     * In order for this test to pass, the consumer key must be linked to a client with one registered callback.
     */
    @Test
    @BugNumber(13254)
    public void getAccessTokenNoRedirectUri() throws Exception {
        final String authCode = new Layer720Api(BASE_URL).authorizeAndRetrieve(CONSUMER_KEY, null, PASSWORD_AUTHENTICATION, null);

        final Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type", "authorization_code");
        params.put("code", authCode);
        params.put("client_id", CONSUMER_KEY);
        params.put("client_secret", CONSUMER_SECRET);

        final GenericHttpResponse response = getAccessToken(params);
        assertEquals(200, response.getStatus());
        final String accessToken = OAuthToolkitTestUtility.getAccessTokenFromJsonResponse(response);
        assertFalse(accessToken.isEmpty());
    }

    /**
     * Redirect_uri in token request must match redirect_uri in auth request.
     */
    @Test
    @BugNumber(13254)
    public void getAccessTokenMismatchRedirectUri() throws Exception {
        final String authCode = new Layer720Api(BASE_URL).authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, PASSWORD_AUTHENTICATION, null);

        final Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type", "authorization_code");
        params.put("code", authCode);
        params.put("client_id", CONSUMER_KEY);
        params.put("client_secret", CONSUMER_SECRET);
        params.put("redirect_uri", CALLBACK + "/mismatch");

        final GenericHttpResponse response = getAccessToken(params);
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("invalid_request"));
    }

    /**
     * If redirect_uri is provided, it must match the registered callback.
     */
    @Test
    @BugNumber(13254)
    public void authorizeRedirectUriMismatchRegisteredCallback() throws Exception {
        final String url = buildAuthorizeUrl(CONSUMER_KEY, "code", CALLBACK + "/mismatch", "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(true);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Mismatching redirect uri"));
    }

    @Test
    @BugNumber(13256)
    public void authorizeNoClientId() throws Exception {
        final String url = buildAuthorizeUrl(null, "code", CALLBACK, "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(false);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        assertEquals(CALLBACK + "?error=invalid_request&state=state_test", locationHeader);
    }

    @Test
    @BugNumber(13256)
    public void authorizeNoClientIdOrState() throws Exception {
        final String url = buildAuthorizeUrl(null, "code", CALLBACK, null);
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(false);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        assertEquals(CALLBACK + "?error=invalid_request", locationHeader);
    }

    /**
     * Cannot redirect back to client therefore return 400.
     */
    @Test
    @BugNumber(13256)
    public void authorizeNoClientIdOrRedirectUri() throws Exception {
        final String url = buildAuthorizeUrl(null, "code", null, "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(false);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Invalid request. Please try again."));
    }

    @Test
    @BugNumber(13256)
    public void authorizeNoResponseType() throws Exception {
        final String url = buildAuthorizeUrl(CONSUMER_KEY, null, CALLBACK, "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(false);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        assertEquals(CALLBACK + "?error=invalid_request&state=state_test", locationHeader);
    }

    @Test
    @BugNumber(13256)
    public void authorizeInvalidResponseType() throws Exception {
        final String url = buildAuthorizeUrl(CONSUMER_KEY, "invalid", CALLBACK, "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(false);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        assertEquals(CALLBACK + "?error=invalid_request&state=state_test", locationHeader);
    }

    @Test
    @BugNumber(13256)
    public void authorizeNoResponseTypeUseRegisteredCallback() throws Exception {
        final String url = buildAuthorizeUrl(CONSUMER_KEY, null, null, "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(false);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        assertEquals(CALLBACK + "?error=invalid_request&state=state_test", locationHeader);
    }

    @Test
    @BugNumber(13256)
    public void authorizeInvalidRedirectUri() throws Exception {
        final String url = buildAuthorizeUrl(CONSUMER_KEY, "code", "invalidurl", "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(true);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Mismatching redirect uri"));
    }

    @Test
    public void authorizeDeniedAuthCode() throws Exception {
        final GenericHttpResponse response = new Layer720Api(BASE_URL).authorize("code", CONSUMER_KEY, CALLBACK,
                PASSWORD_AUTHENTICATION, null, false, "Deny");
        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        assertEquals(CALLBACK + "?error=access_denied&state=state_test", locationHeader);
    }

    @Test
    public void authorizeDeniedImplicit() throws Exception {
        final GenericHttpResponse response = new Layer720Api(BASE_URL).authorize("token", CONSUMER_KEY, CALLBACK,
                PASSWORD_AUTHENTICATION, null, false, "Deny");
        assertEquals(302, response.getStatus());
        final String locationHeader = response.getHeaders().getFirstValue("Location");
        assertEquals(CALLBACK + "?error=access_denied&state=state_test", locationHeader);
    }

    @Test
    @BugNumber(13269)
    public void whitespaceInRegisteredCallbacks() throws Exception {
        // first register a client key with two callbacks that have whitespace between them
        final Map<String, String> clientParams = buildClientParams();
        final String clientIdentity = clientParams.get(CLIENT_IDENT);
        store("client", clientParams);
        final String callbacksWithWhitespace = "https://" + BASE_URL + ":8443/oauth/v2/client/authcode, https://" + BASE_URL + ":8443/oauth/v2/client/implicit";
        final Map<String, String> keyParams = buildKeyParams(callbacksWithWhitespace, OOB, ALL);
        keyParams.put(EXPIRATION, "0");
        final String clientKey = keyParams.get(CLIENT_KEY);
        keyParams.put(CLIENT_IDENT, clientIdentity);
        store("client", keyParams);
        final ClientKey key = getKey(clientKey);
        assertEquals(callbacksWithWhitespace, key.getCallback());

        // try to authorize without specifying a redirect_uri
        final String url = buildAuthorizeUrl(key.getClientKey(), "code", null, "state_test");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setFollowRedirects(false);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        // delete the client and key
        delete(CLIENT_IDENT, clientIdentity, "client");

        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals(400, response.getStatus());
        assertTrue(responseBody.contains("Mismatching redirect uri"));
    }

    @Test
    @BugNumber(13265)
    public void authorizeMultipleSessionId() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL +
                ":8443/auth/oauth/v2/authorize?action=Grant&sessionID=123&sessionID=456"));
        params.setPasswordAuthentication(passwordAuthentication);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Invalid request."));
    }

    @Test
    @BugNumber(13265)
    public void authorizeMultipleAction() throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL +
                ":8443/auth/oauth/v2/authorize?action=Grant&action=Deny&sessionID=123"));
        params.setPasswordAuthentication(passwordAuthentication);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Invalid request."));
    }

    /**
     * This test requires wsman and the current token type to be BEARER.
     */
    @Test
    @BugNumber(13305)
    public void macToken() throws Exception {
        setOAuth2TokenType("BEARER", "MAC");

        // get mac token
        final Layer720Api api = new Layer720Api(BASE_URL);
        final String authCode = api.authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, passwordAuthentication, null);

        final Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type", "authorization_code");
        params.put("code", authCode);
        params.put("client_id", CONSUMER_KEY);
        params.put("client_secret", CONSUMER_SECRET);
        params.put("redirect_uri", CALLBACK);
        final GenericHttpResponse response = getAccessToken(params);

        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        final Map<String, String> pairs = new ObjectMapper().readValue(responseBody, HashMap.class);
        final String accessToken = pairs.get("access_token");
        final String key = pairs.get("mac_key");
        final String nonce = UUID.randomUUID().toString();
        final long timestamp = new Date().getTime();

        // build normalized string
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(timestamp).append(NEW_LINE);
        stringBuilder.append(nonce).append(NEW_LINE);
        stringBuilder.append("GET").append(NEW_LINE);
        stringBuilder.append(URLEncoder.encode(PROTECTED_API, "UTF-8")).append(NEW_LINE);
        stringBuilder.append(BASE_URL).append(NEW_LINE);
        stringBuilder.append("8443").append(NEW_LINE);
        stringBuilder.append(NEW_LINE);
        final String normalizedString = stringBuilder.toString();

        // generate mac
        final Mac messageAuthenticationCode = Mac.getInstance(HMAC_SHA_1);
        messageAuthenticationCode.init(new SecretKeySpec(key.getBytes(Charsets.UTF8), HMAC_SHA_1));
        final byte[] digest = messageAuthenticationCode.doFinal(normalizedString.getBytes(Charsets.UTF8));
        final String mac = HexUtils.encodeBase64(digest, true);

        // call protected api
        final GenericHttpRequestParams resourceParams = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/v2/protectedapi"));
        resourceParams.setSslSocketFactory(getSSLSocketFactory());
        resourceParams.addExtraHeader(new GenericHttpHeader("Authorization", "MAC id=\"" + accessToken + "\", ts=\"" + timestamp + "\",  nonce=\"" + nonce + "\", mac=\"" + mac + "\""));
        final GenericHttpRequest resourceRequest = client.createRequest(HttpMethod.GET, resourceParams);
        final GenericHttpResponse resourceResponse = resourceRequest.getResponse();
        assertEquals(200, resourceResponse.getStatus());
        final String resourceResponseBody = new String(IOUtils.slurpStream(resourceResponse.getInputStream()));
        assertTrue(resourceResponseBody.contains("accessed_at"));

        setOAuth2TokenType("MAC", "BEARER");
    }

    /**
     * This test requires wsman and the current token type to be BEARER.
     * <p/>
     * OTK configured to have MAC token type, but receives a BEARER auth header.
     */
    @Test
    @BugNumber(13411)
    public void macTokenWithBearerAuthHeader() throws Exception {
        setOAuth2TokenType("BEARER", "MAC");

        // get mac token
        final Layer720Api api = new Layer720Api(BASE_URL);
        final String authCode = api.authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, passwordAuthentication, null);

        final Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type", "authorization_code");
        params.put("code", authCode);
        params.put("client_id", CONSUMER_KEY);
        params.put("client_secret", CONSUMER_SECRET);
        params.put("redirect_uri", CALLBACK);
        final GenericHttpResponse response = getAccessToken(params);

        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        final Map<String, String> pairs = new ObjectMapper().readValue(responseBody, HashMap.class);
        final String accessToken = pairs.get("access_token");

        // call protected api using bearer auth
        final GenericHttpRequestParams resourceParams = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/v2/protectedapi"));
        resourceParams.setSslSocketFactory(getSSLSocketFactory());
        resourceParams.addExtraHeader(new GenericHttpHeader("Authorization", "Bearer " + accessToken));
        final GenericHttpRequest resourceRequest = client.createRequest(HttpMethod.GET, resourceParams);
        final GenericHttpResponse resourceResponse = resourceRequest.getResponse();
        assertEquals(401, resourceResponse.getStatus());
        final String resourceResponseBody = new String(IOUtils.slurpStream(resourceResponse.getInputStream()));
        assertTrue(resourceResponseBody.contains("invalid_request"));

        setOAuth2TokenType("MAC", "BEARER");
    }

    private GenericHttpResponse getAccessToken(final Map<String, String> params) throws Exception {
        final GenericHttpRequestParams tokenParams = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/auth/oauth/v2/token"));
        tokenParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, tokenParams);
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        return request.getResponse();
    }

    /**
     * Sets token type context variable using wsman.
     */
    private void setOAuth2TokenType(final String currentTokenType, final String newTokenType) throws Exception {
        final String getBody = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "\txmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "        xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "\t<s:Header>\n" +
                "\t\t<a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "\t\t<a:To>http://" + BASE_URL + ":8080/wsman</a:To> \n" +
                "\t\t<a:ReplyTo> \n" +
                "\t\t\t<a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "\t\t</a:ReplyTo> \n" +
                "\t\t<a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "\t\t<w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies</w:ResourceURI> \n" +
                "\t\t<w:SelectorSet>\n" +
                "\t\t\t<w:Selector Name=\"name\">OAuth 2.0 Context Variables</w:Selector> \n" +
                "\t\t</w:SelectorSet>\n" +
                "\t\t<w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "\t</s:Header>\n" +
                "\t<s:Body/> \n" +
                "</s:Envelope>";
        final GenericHttpResponse getResponse = callWsman(getBody);
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));

        final String encodedCurrentTokenType = new String(Base64.encodeBase64(currentTokenType.getBytes()));
        final String encodedNewTokenType = new String(Base64.encodeBase64(newTokenType.getBytes()));
        if (!getResponseBody.contains("Base64Expression stringValue=\"" + encodedNewTokenType + "\"")) {
            assertTrue(getResponseBody.contains(encodedCurrentTokenType));
            final String getResponseSoapBody = getResponseBody.substring(getResponseBody.indexOf("<env:Body>") + 10, getResponseBody.indexOf("</env:Body>"));
            // replace old token type with new token type
            final String updatedSoapBody = getResponseSoapBody.replace("Base64Expression stringValue=\"" + encodedCurrentTokenType + "\"", "Base64Expression stringValue=\"" + encodedNewTokenType + "\"");

            final String putBody = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                    "\t<s:Header>\n" +
                    "\t\t<wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action>\n" +
                    "\t\t<wsa:To s:mustUnderstand=\"true\">http://" + BASE_URL + ":8080/wsman</wsa:To>\n" +
                    "\t\t<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI>\n" +
                    "\t\t<wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID>\n" +
                    "\t\t<wsa:ReplyTo>\n" +
                    "\t\t\t<wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
                    "\t\t</wsa:ReplyTo>\n" +
                    "\t\t<wsman:SelectorSet>\n" +
                    "\t\t\t<wsman:Selector Name=\"name\">OAuth 2.0 Context Variables</wsman:Selector>\n" +
                    "\t\t</wsman:SelectorSet>\n" +
                    "\t</s:Header>\n" +
                    "\t<s:Body>" + updatedSoapBody +
                    "</s:Body>\n" +
                    "</s:Envelope>";

            callWsman(putBody);
        } else {
            // token type is already the newTokenType
        }
    }

    private GenericHttpResponse callWsman(final String requestBody) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/wsman"));
        params.setPasswordAuthentication(passwordAuthentication);
        params.setSslSocketFactory(getSSLSocketFactory());
        params.addExtraHeader(new GenericHttpHeader("Content-Type", "application/soap+xml;charset=UTF-8"));
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        request.setInputStream(new ByteArrayInputStream(requestBody.getBytes()));
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        return response;
    }

    private String buildAuthorizeUrl(final String clientId, final String responseType, final String callback, final String state) {
        final StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://").append(BASE_URL);
        urlBuilder.append(":8443/auth/oauth/v2/authorize?");
        if (clientId != null) {
            urlBuilder.append("client_id=").append(clientId);
        }
        if (responseType != null) {
            urlBuilder.append("&response_type=").append(responseType);
        }
        urlBuilder.append("&scope=scope_test");
        if (state != null) {
            urlBuilder.append("&state=state_test");
        }
        if (callback != null) {
            urlBuilder.append("&redirect_uri=").append(callback);
        }
        return urlBuilder.toString();
    }
}
