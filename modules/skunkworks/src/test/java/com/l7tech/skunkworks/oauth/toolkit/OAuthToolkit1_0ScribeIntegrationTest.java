package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;

import java.net.PasswordAuthentication;

import static org.junit.Assert.*;

/**
 * Integration tests for the OAuth Tool Kit (oauth version 1.0) that uses Scribe.
 * <p/>
 * Each test requires the SSG to be running with an installed OTK (does not require the OTK Test Client).
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 * <p/>
 * Note on SSL with Scribe: Scribe cannot handle self-signed certs.
 * You will most likely have to add the gateway SSL cert to your local cacerts file (use the keytool command).
 * If using localhost, you may have to change the gateway's default SSL cert to use 'localhost' instead of your computer name.
 */
@Ignore
public class OAuthToolkit1_0ScribeIntegrationTest {
    // LOCALHOST
    private static final String GATEWAY = "localhost";
    private static final String CONSUMER_KEY = "acf89db2-994e-427b-ac2c-88e6101f9433";
    private static final String CONSUMER_SECRET = "74d5e0db-cd8b-4d8e-a989-95a0746c3343";

    // ALEEOAUTH.L7TECH.COM
//    private static final String GATEWAY = "aleeoauth.l7tech.com";
//    private static final String CONSUMER_KEY = "a13a32e0-448e-4e76-853f-72eca2ffb4fa";
//    private static final String CONSUMER_SECRET = "4d3e5843-174a-4bb8-ac43-1537d7adca01";

    private static final String PROTECTED_RESOURCE_URI = "/protected/resource";
    private OAuthService service;
    private Layer710aApi api;

    @Before
    public void setup() {
        api = new Layer710aApi(GATEWAY);
        service = new ServiceBuilder()
                .provider(api)
                .apiKey(CONSUMER_KEY)
                .apiSecret(CONSUMER_SECRET)
                .build();
    }

    @Test
    public void happyPath() throws Exception {
        // Obtain the Request Token
        System.out.println("Fetching the Request Token...");
        final Token requestToken = service.getRequestToken();
        System.out.println("Received Request Token: " + requestToken);

        final String verifier = api.authorizeAndRetrieve(requestToken.getToken(), true, new PasswordAuthentication("admin", "password".toCharArray()));

        // Trade the Request Token and Verifier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        final Token accessToken = service.getAccessToken(requestToken, new Verifier(verifier));
        System.out.println("Received the Access Token: " + accessToken);

        // Ask for a protected resource
        System.out.println("Accessing a protected resource...");
        final OAuthRequest request = new OAuthRequest(Verb.POST, "https://" + GATEWAY + ":8443" + PROTECTED_RESOURCE_URI);
        request.addQuerystringParameter("Query", "Layer 7 Oauth");
        service.signRequest(accessToken, request);
        final Response response = request.send();
        assertEquals(200, response.getCode());
        System.out.println("Obtained protected resource: ");
        System.out.println(response.getBody());
    }

    @Test
    public void unauthorized() throws Exception {
        // Obtain the Request Token
        System.out.println("Fetching the Request Token...");
        final Token requestToken = service.getRequestToken();
        System.out.println("Received Request Token: " + requestToken);

        final GenericHttpResponse response = api.authorize(requestToken.getToken(), new PasswordAuthentication("unauthorized", "unauthorized".toCharArray()));

        assertEquals(401, response.getStatus());
        assertTrue(new String(IOUtils.slurpStream(response.getInputStream())).contains("Authentication failed"));
    }
}