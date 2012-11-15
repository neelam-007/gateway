package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;

import java.net.PasswordAuthentication;

import static org.junit.Assert.*;
import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.*;

/**
 * Integration tests for the OAuth Tool Kit (oauth version 2.0) that uses Scribe.
 * <p/>
 * Each test requires the SSG to be running with an installed OTK (does not require the OTK Test Client).
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 * <p/>
 * Do NOT use the default OAuth2Client (because its callback will automatically obtain the access token).
 * <p/>
 * Preconditions: <br />
 * 1. publish a REST service on the gateway for callbacks (obtaining the authorization code) - return template response with body = ${request.http.parameter.code}<br />
 * 2. register a new client using callback url from step 1<br />
 * 3. set the CONSUMER_KEY<br />
 * 4. set the CONSUMER_SECRET<br />
 * 5. set the CALLBACK using callback url from step 1
 * <p/>
 * Note on SSL with Scribe: Scribe cannot handle self-signed certs.
 * You will most likely have to add the gateway SSL cert to your local cacerts file (use the keytool command).
 * If using localhost, you may have to change the gateway's default SSL cert to use 'localhost' instead of your computer name.
 */
@Ignore
public class OAuthToolkit2_0ScribeIntegrationTest {
    // ALEEOAUTH
//    private static final String GATEWAY = "aleeoauth.l7tech.com";
//    private static final String CONSUMER_KEY = "e5f0bfb6-a981-4ef4-b4b4-2716e7904b84";
//    private static final String CONSUMER_SECRET = "fbebc8b9-2165-4bd0-98c8-a74e03f0298e";

    //LOCALHOST
    private static final String GATEWAY = "localhost";
    private static final String CONSUMER_KEY = "182637fd-8b6b-4dca-9192-3d1e23d556b5";
    private static final String CONSUMER_SECRET = "de88c414-fb69-4107-aac0-d1fdf0986017";

    private static final String PROTECTED_RESOURCE_URI = "http://" + GATEWAY + ":8080/oauth/v2/protectedapi";
    private static final String CALLBACK = "https://" + GATEWAY + ":8443/oauth_callback";
    private static final Token EMPTY_TOKEN = null;
    private OAuthService service;
    private Layer720Api api;

    @Before
    public void setup() {
        api = new Layer720Api(GATEWAY);
        service = new ServiceBuilder()
                .provider(api)
                .apiKey(CONSUMER_KEY)
                .apiSecret(CONSUMER_SECRET)
                .callback(CALLBACK)
                .build();
    }

    @Test
    public void authCode() throws Exception {
        // Obtain the Authorization Code from callback plain text response body
        System.out.println("Fetching Authorization Code...");
        final String authCode = api.authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, new PasswordAuthentication("admin", "password".toCharArray()), null, "scope_test", "state_test");
        System.out.println("Received Authorization Code: " + authCode);

        // Trade the Request Token and Verifier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        final Token accessToken = service.getAccessToken(EMPTY_TOKEN, new Verifier(authCode));
        System.out.println("Received the Access Token: " + accessToken.getToken());

        getProtectedResource(accessToken);
    }

    @Test
    @BugNumber(13456)
    public void authCodeScopeWithWhiteSpace() throws Exception {
        // Obtain the Authorization Code from callback plain text response body
        System.out.println("Fetching Authorization Code...");
        final String authCode = api.authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, new PasswordAuthentication("admin", "password".toCharArray()), null, "scope+test", "state_test");
        System.out.println("Received Authorization Code: " + authCode);

        // Trade the Request Token and Verifier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        final Token accessToken = service.getAccessToken(EMPTY_TOKEN, new Verifier(authCode));
        System.out.println("Received the Access Token: " + accessToken.getToken());

        getProtectedResource(accessToken);
    }

    @Test
    @BugNumber(13456)
    public void authCodeNoScope() throws Exception {
        // Obtain the Authorization Code from callback plain text response body
        System.out.println("Fetching Authorization Code...");
        final String authCode = api.authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, new PasswordAuthentication("admin", "password".toCharArray()), null, null, "state_test");
        System.out.println("Received Authorization Code: " + authCode);

        // Trade the Request Token and Verifier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        final Token accessToken = service.getAccessToken(EMPTY_TOKEN, new Verifier(authCode));
        System.out.println("Received the Access Token: " + accessToken.getToken());

        getProtectedResource(accessToken);
    }

    @Test
    @BugNumber(13456)
    public void authCodeStateWithWhiteSpace() throws Exception {
        // Obtain the Authorization Code from callback plain text response body
        System.out.println("Fetching Authorization Code...");
        final String authCode = api.authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, new PasswordAuthentication("admin", "password".toCharArray()), null, "scope_test", "state+test");
        System.out.println("Received Authorization Code: " + authCode);

        // Trade the Request Token and Verifier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        final Token accessToken = service.getAccessToken(EMPTY_TOKEN, new Verifier(authCode));
        System.out.println("Received the Access Token: " + accessToken.getToken());

        getProtectedResource(accessToken);
    }

    @Test
    @BugNumber(13456)
    public void authCodeNoState() throws Exception {
        // Obtain the Authorization Code from callback plain text response body
        System.out.println("Fetching Authorization Code...");
        final String authCode = api.authorizeAndRetrieve(CONSUMER_KEY, CALLBACK, new PasswordAuthentication("admin", "password".toCharArray()), null, "scope_test", null);
        System.out.println("Received Authorization Code: " + authCode);

        // Trade the Request Token and Verifier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        final Token accessToken = service.getAccessToken(EMPTY_TOKEN, new Verifier(authCode));
        System.out.println("Received the Access Token: " + accessToken.getToken());

        getProtectedResource(accessToken);
    }

    @Test
    public void implicit() throws Exception {
        // Obtain an Access Token from location header fragment
        System.out.println("Fetching Access Token ...");
        final GenericHttpResponse accessTokenResponse = api.authorize("token", CONSUMER_KEY, CALLBACK, new PasswordAuthentication("admin", "password".toCharArray()), null, false, "Grant", "scope_test", "state_test");
        final String locationHeader = accessTokenResponse.getHeaders().getFirstValue("Location");
        final String accessToken = locationHeader.substring(locationHeader.indexOf("#access_token=") + 14, locationHeader.indexOf("&"));
        System.out.println("Received Access Token: " + accessToken);

        getProtectedResource(new Token(accessToken, ""));
    }

    @Test
    public void clientCredentials() throws Exception {
        // Obtain an Access Token from JSON response body
        System.out.println("Fetching Access Token ...");
        final GenericHttpResponse accessTokenResponse = api.authorizeWithClientCredentials(new PasswordAuthentication(CONSUMER_KEY, CONSUMER_SECRET.toCharArray()));
        final String accessToken = getAccessTokenFromJsonResponse(accessTokenResponse);
        System.out.println("Received Access Token: " + accessToken);

        getProtectedResource(new Token(accessToken, ""));
    }

    @Test
    public void resourceOwnerCredentials() throws Exception {
        // Obtain an Access Token from JSON response body
        System.out.println("Fetching Access Token ...");
        final GenericHttpResponse accessTokenResponse = api.authorizeWithResourceOwnerCredentials(new PasswordAuthentication(CONSUMER_KEY, CONSUMER_SECRET.toCharArray()), "admin", "password");
        final String accessToken = getAccessTokenFromJsonResponse(accessTokenResponse);
        System.out.println("Received Access Token: " + accessToken);

        getProtectedResource(new Token(accessToken, ""));
    }

    @Test
    public void saml() throws Exception {
        // Obtain an Access Token from JSON response body
        System.out.println("Fetching Access Token ...");
        final GenericHttpResponse accessTokenResponse = api.authorizeWithSAMLToken(new PasswordAuthentication(CONSUMER_KEY, CONSUMER_SECRET.toCharArray()), new PasswordAuthentication("admin", "password".toCharArray()));
        final String accessToken = getAccessTokenFromJsonResponse(accessTokenResponse);
        System.out.println("Received Access Token: " + accessToken);

        getProtectedResource(new Token(accessToken, ""));
    }

    private void getProtectedResource(final Token accessToken) {
        // Ask for a protected resource
        System.out.println("Accessing a protected resource...");
        final OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URI);
        service.signRequest(accessToken, request);
        final Response response = request.send();
        assertEquals(200, response.getCode());
        System.out.println("Obtained protected resource: ");
        System.out.println(response.getBody());
    }
}
