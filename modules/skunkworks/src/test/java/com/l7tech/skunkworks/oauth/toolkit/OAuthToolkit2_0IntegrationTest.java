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

import static com.l7tech.skunkworks.oauth.toolkit.OAuthToollkitTestUtility.getSSLSocketFactory;
import static org.junit.Assert.*;

/**
 * Integration tests for the OAuth Tool Kit (oauth version 2.0). Each test requires the SSG to be running with an installed OTK that includes
 * the default OTK client.
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 */
@Ignore
public class OAuthToolkit2_0IntegrationTest {
    //LOCALHOST
    private static final String BASE_URL = "localhost";
    private static final String CONSUMER_KEY = "182637fd-8b6b-4dca-9192-3d1e23d556b5";
    private static final String CONSUMER_SECRET = "de88c414-fb69-4107-aac0-d1fdf0986017";
    // this callback must be the registered callback on the client
    private static final String CALLBACK = "https://" + BASE_URL + ":8443/oauth_callback";
    private GenericHttpClient client;

    @Before
    public void setup() {
        client = new CommonsHttpClient();
    }

    @BugNumber(12946)
    @Test
    public void authCodeGrantDenied() throws Exception {
        final GenericHttpRequestParams requestParams =
                new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/v2/client/authcode?state=state_test&error=access_denied"));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("The access was denied"));
    }

    @BugNumber(12946)
    @Test
    public void implicitGrantDenied() throws Exception {
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
        final GenericHttpResponse response = new Layer720Api(BASE_URL).authorize("code", CONSUMER_KEY, CALLBACK, null, "invalid", true);

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
        final String authCode = new Layer720Api(BASE_URL).authorizeAndRetrieve(CONSUMER_KEY, null, new PasswordAuthentication("admin", "password".toCharArray()), null);

        final GenericHttpRequestParams tokenParams = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/auth/oauth/v2/token"));
        tokenParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, tokenParams);
        request.addParameter("grant_type", "authorization_code");
        request.addParameter("code", authCode);
        request.addParameter("client_id", CONSUMER_KEY);
        request.addParameter("client_secret", CONSUMER_SECRET);

        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        final String accessToken = OAuthToollkitTestUtility.getAccessTokenFromJsonResponse(response);
        assertFalse(accessToken.isEmpty());
    }

    /**
     * Redirect_uri in token request must match redirect_uri in auth request.
     */
    @Test
    @BugNumber(13254)
    public void getAccessTokenMismatchRedirectUri() throws Exception {
        final String authCode = new Layer720Api(BASE_URL).authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, new PasswordAuthentication("admin", "password".toCharArray()), null);

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
        final StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://").append(BASE_URL);
        urlBuilder.append(":8443/auth/oauth/v2/authorize?client_id=").append(CONSUMER_KEY);
        urlBuilder.append("&response_type=code");
        urlBuilder.append("&scope=scope_test&state=state_test");
        urlBuilder.append("&redirect_uri=").append(CALLBACK + "/mismatch");
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(urlBuilder.toString()));
        params.setFollowRedirects(true);
        params.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, params);
        final GenericHttpResponse response = request.getResponse();

        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Mismatching redirect uri"));
    }
}
