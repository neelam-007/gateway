package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Map;

import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.getSSLSocketFactory;
import static org.junit.Assert.*;

/**
 * Integration tests for the OAuth Tool Kit (oauth version 2.0). Each test requires the SSG to be running with an installed OTK that includes
 * the default OTK client.
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

        final GenericHttpRequestParams tokenParams = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/auth/oauth/v2/token"));
        tokenParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, tokenParams);
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", authCode);
        request.addParameter("client_id", CONSUMER_KEY);
        request.addParameter("client_secret", CONSUMER_SECRET);

        final GenericHttpResponse response = request.getResponse();
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

        final GenericHttpRequestParams tokenParams = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/auth/oauth/v2/token"));
        tokenParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, tokenParams);
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", authCode);
        request.addParameter("client_id", CONSUMER_KEY);
        request.addParameter("client_secret", CONSUMER_SECRET);
        request.addParameter("redirect_uri", CALLBACK + "/mismatch");

        final GenericHttpResponse response = request.getResponse();
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
        final String callbacksWithWhitespace = "https://localhost:8443/oauth/v2/client/authcode, https://localhost:8443/oauth/v2/client/implicit";
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
