package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

import java.net.PasswordAuthentication;
import java.net.URL;

import static org.junit.Assert.*;

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
     * Authorize the token using admin user.
     */
    public Verifier authorize(final Token requestToken) throws Exception {
        final CommonsHttpClient client = new CommonsHttpClient();
        final PasswordAuthentication admin = new PasswordAuthentication("admin", "password".toCharArray());

        final String url = "https://" + gatewayHost + ":8443/auth/oauth/v1/authorize?state=authenticate&oauth_token=" + requestToken.getToken();
        final GenericHttpRequestParams authenticateParams = new GenericHttpRequestParams(new URL(url));
        authenticateParams.setSslSocketFactory(SSLUtil.getSSLSocketFactory());
        authenticateParams.setPasswordAuthentication(admin);
        final GenericHttpRequest authenticateRequest = client.createRequest(HttpMethod.GET, authenticateParams);
        final GenericHttpResponse authenticateResponse = authenticateRequest.getResponse();
        assertEquals(200, authenticateResponse.getStatus());

        final GenericHttpRequestParams grantedParams = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v1/authorize?oauth_token=" + requestToken.getToken() + "&state=granted"));
        grantedParams.setFollowRedirects(true);
        grantedParams.setSslSocketFactory(SSLUtil.getSSLSocketFactory());
        grantedParams.setPasswordAuthentication(admin);
        final GenericHttpRequest grantedRequest = client.createRequest(HttpMethod.GET, grantedParams);
        final GenericHttpResponse grantResponse = grantedRequest.getResponse();
        assertEquals(200, grantResponse.getStatus());
        final String asString = grantResponse.getAsString(false, Integer.MAX_VALUE);
        return new Verifier(getVerifierFromHtml(asString));
    }

    private String getVerifierFromHtml(final String html) {
        final Integer start = html.indexOf("<h2>verifier=");
        final Integer end = html.indexOf("</h2>") + 7;
        final String verifier = html.substring(start, end).replace("<h2>verifier=", "").replace("</h2>", "").replaceAll("\"", "").trim();
        System.out.println("Received verifier: " + verifier);
        return verifier;
    }
}


