package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

import java.net.PasswordAuthentication;
import java.net.URL;

import static org.junit.Assert.*;
import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.*;

/**
 * OAuth version 1.0a.
 */
public class Layer710aApi extends DefaultApi10a {
    private static String AUTHORIZE_URL = "https://%s:8443/auth/oauth/v1/authorize?oauth_token=%s";
    private String gatewayHost;

    public Layer710aApi(String GatewayHost) {
        gatewayHost = GatewayHost;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return String.format("https://%s:8443/auth/oauth/v1/token", gatewayHost);
    }

    /**
     * This auth url is for manual authorization.
     */
    @Override
    public String getAuthorizationUrl(final Token token) {
        return String.format(AUTHORIZE_URL, gatewayHost, token.getToken());
    }

    @Override
    public String getRequestTokenEndpoint() {
        return String.format("https://%s:8443/auth/oauth/v1/request", gatewayHost);
    }

    /**
     * Authorize the token.
     *
     * @param oob true if callback is oob false otherwise
     * @return verification code if oob=true or an access code if oob= false
     */
    public String authorizeAndRetrieve(final String requestToken, boolean oob, final PasswordAuthentication passwordAuthentication) throws Exception {
        final GenericHttpResponse grantResponse = authorize(requestToken, passwordAuthentication);
        assertEquals(200, grantResponse.getStatus());
        final String asString = grantResponse.getAsString(false, Integer.MAX_VALUE);
        if (oob) {
            final String verifier = getVerifierFromHtml(asString);
            System.out.println("Received verifier: " + verifier);
            return verifier;
        } else {
            // access token is in a hidden field
            final String accessToken = getTokenFromHtmlForm(asString);
            System.out.println("Received access token: " + accessToken);
            return accessToken;
        }
    }

    /**
     * Authorize the token.
     *
     * @return GenericHttpResponse the auth response.
     */
    public GenericHttpResponse authorize(final String requestToken, final PasswordAuthentication passwordAuthentication) throws Exception {
        return authorize(requestToken, passwordAuthentication, null);
    }

    public GenericHttpResponse authorize(final String requestToken, final String cookie) throws Exception {
        return authorize(requestToken, null, cookie);
    }

    private GenericHttpResponse authorize(final String requestToken, final PasswordAuthentication passwordAuthentication, final String cookie) throws Exception {
        final HttpComponentsClient client = new HttpComponentsClient();

        final String url = "https://" + gatewayHost + ":8443/auth/oauth/v1/authorize?state=authorized&oauth_token=" + requestToken;
        final GenericHttpRequestParams authenticateParams = new GenericHttpRequestParams(new URL(url));
        authenticateParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest authenticateRequest = client.createRequest(HttpMethod.GET, authenticateParams);
        final GenericHttpResponse authenticateResponse = authenticateRequest.getResponse();
        final String authenticateResponseBody = authenticateResponse.getAsString(false, Integer.MAX_VALUE);
        assertEquals(200, authenticateResponse.getStatus());
        final String sessionId = getSessionIdFromHtmlForm(authenticateResponseBody);
        System.out.println("Received sessionId: " + sessionId);

        final GenericHttpRequestParams grantParams = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v1/authorize?sessionID=" + sessionId + "&action=Grant"));
        grantParams.setFollowRedirects(true);
        grantParams.setSslSocketFactory(getSSLSocketFactory());
        if (passwordAuthentication != null) {
            grantParams.setPasswordAuthentication(passwordAuthentication);
        }
        if (cookie != null) {
            grantParams.addExtraHeader(new GenericHttpHeader("Cookie", "l7otk1a=" + cookie));
        }
        final GenericHttpRequest grantedRequest = client.createRequest(HttpMethod.GET, grantParams);
        return grantedRequest.getResponse();
    }

    private String getVerifierFromHtml(final String html) {
        final Integer start = html.indexOf("<h2>verifier=");
        final Integer end = html.indexOf("</h2>") + 7;
        final String verifier = html.substring(start, end).replace("<h2>verifier=", "").replace("</h2>", "").replaceAll("\"", "").trim();
        System.out.println("Received verifier: " + verifier);
        return verifier;
    }
}


