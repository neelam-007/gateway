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

/**
 * Integration tests for the OAuth Tool Kit (oauth version 2.0). Each test requires the SSG to be running with an installed OTK that includes
 * the default OTK client.
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 */
@Ignore
public class OAuthToolkit2_0IntegrationTest {
    private static final String BASE_URL = "localhost";
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
}
