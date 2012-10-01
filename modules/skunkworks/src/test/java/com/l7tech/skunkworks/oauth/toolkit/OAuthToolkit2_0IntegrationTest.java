package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

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
    private static final String CALLBACK = "https://" + BASE_URL + ":8443/oauth_callback";
    private GenericHttpClient client;

    @Before
    public void setup(){
        client = new CommonsHttpClient();
    }

    @BugNumber(12946)
    @Test
    public void authCodeGrantDenied() throws Exception{
        final GenericHttpRequestParams requestParams =
                new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/v2/client/authcode?state=state_test&error=access_denied"));
        requestParams.setSslSocketFactory(SSLUtil.getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("The access was denied"));
    }

    @BugNumber(12946)
    @Test
    public void implicitGrantDenied() throws Exception{
        final GenericHttpRequestParams requestParams =
                new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/v2/client/implicit?state=state_test&error=access_denied"));
        requestParams.setSslSocketFactory(SSLUtil.getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("Access was denied"));
    }

    @Test
    @BugNumber(13155)
    public void authorizeWithInvalidCookie() throws Exception {
        final GenericHttpResponse response = new Layer720Api(BASE_URL).authorize(CONSUMER_KEY, CALLBACK, null, "invalid");

        assertEquals(401, response.getStatus());
        final String body = new String(IOUtils.slurpStream(response.getInputStream()));
        assertFalse(body.contains("verifier"));
        assertTrue(body.contains("Authentication failed"));
        assertEquals("l7otk2a=", response.getHeaders().getFirstValue("Set-Cookie"));
    }
}
