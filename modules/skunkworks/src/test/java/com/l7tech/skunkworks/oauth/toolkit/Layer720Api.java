package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.utils.OAuthEncoder;

import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.getSSLSocketFactory;
import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.getSessionIdFromHtmlForm;
import static org.junit.Assert.*;

/**
 * OAuth version 2.0.
 */
public class Layer720Api extends DefaultApi20 {
    private static final String AUTHORIZE_URL = "https://%s:8443/auth/oauth/v2/authorize?response_type=code";

    private String gatewayHost;

    public Layer720Api(final String gatewayHost) {
        this.gatewayHost = gatewayHost;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return String.format("https://%s:8443/auth/oauth/v2/token?grant_type=authorization_code", gatewayHost);
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    @Override
    public String getAuthorizationUrl(final OAuthConfig config) {
        StringBuilder authUrl = new StringBuilder();
        authUrl.append(String.format(AUTHORIZE_URL, gatewayHost));

        // Append scope if present
        if (config.hasScope()) {
            authUrl.append("&scope=").append(OAuthEncoder.encode(config.getScope()));
        }

        // add redirect URI if callback isn't equal to 'oob'
        if (!config.getCallback().equalsIgnoreCase("oob")) {
            authUrl.append("&redirect_uri=").append(OAuthEncoder.encode(config.getCallback()));
        }

        authUrl.append("&client_id=").append(OAuthEncoder.encode(config.getApiKey()));
        return authUrl.toString();
    }

    public String authorizeAndRetrieve(final String consumerKey, final String callback, final PasswordAuthentication passwordAuthentication,
                                       final String cookie, final String scope, final String state) throws Exception {
        final GenericHttpResponse authResponse = authorize("code", consumerKey, callback, passwordAuthentication, cookie, true, "Grant", scope, state);
        assertEquals(200, authResponse.getStatus());
        final String authCode = new String(IOUtils.slurpStream(authResponse.getInputStream()));
        System.out.println("Received auth code: " + authCode);
        assertFalse(authCode.isEmpty());
        return authCode;
    }

    public GenericHttpResponse authorize(final String responseType, final String consumerKey, final String callback,
                                         final PasswordAuthentication passwordAuthentication, final String cookie,
                                         final boolean followRedirects, final String action, final String scope, final String state) throws Exception {
        final HttpComponentsClient client = new HttpComponentsClient();
        final StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://").append(gatewayHost);
        urlBuilder.append(":8443/auth/oauth/v2/authorize?client_id=").append(consumerKey);
        urlBuilder.append("&response_type=").append(responseType);
        if (scope != null) {
            urlBuilder.append("&scope=" + scope);
        }
        if (state != null) {
            urlBuilder.append("&state=" + state);
        }
        if (callback != null) {
            urlBuilder.append("&redirect_uri=").append(callback);
        }
        final GenericHttpRequestParams sessionIdParams = new GenericHttpRequestParams(new URL(urlBuilder.toString()));
        sessionIdParams.setFollowRedirects(true);
        sessionIdParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest sessionIdRequest = client.createRequest(HttpMethod.GET, sessionIdParams);
        final GenericHttpResponse sessionIdResponse = sessionIdRequest.getResponse();
        assertEquals(200, sessionIdResponse.getStatus());
        final String html = sessionIdResponse.getAsString(false, Integer.MAX_VALUE);
        final String sessionId = getSessionIdFromHtmlForm(html);
        assertFalse(sessionId.isEmpty());

        final GenericHttpRequestParams authParams = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v2/authorize?action=" + action + "&sessionID=" + sessionId));
        if (passwordAuthentication != null) {
            authParams.setPasswordAuthentication(passwordAuthentication);
        }
        if (cookie != null) {
            authParams.addExtraHeader(new GenericHttpHeader("Cookie", "l7otk2a=" + cookie));
        }
        authParams.setFollowRedirects(followRedirects);
        authParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest authRequest = client.createRequest(HttpMethod.GET, authParams);
        return authRequest.getResponse();
    }

    public GenericHttpResponse authorizeWithClientCredentials(final PasswordAuthentication clientCredentials) throws Exception {
        final HttpComponentsClient client = new HttpComponentsClient();
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v2/token"));
        params.setSslSocketFactory(getSSLSocketFactory());
        params.setPasswordAuthentication(clientCredentials);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);

        List<String[]> postParams = new ArrayList<String[]>();
        String[] postParam = new String[]{"grant_type", "client_credentials"};
        postParams.add(postParam);
        request.addParameters(postParams);

        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        return response;
    }

    public GenericHttpResponse authorizeWithResourceOwnerCredentials(final PasswordAuthentication clientCredentials, final String username, final String password) throws Exception {
        final HttpComponentsClient client = new HttpComponentsClient();
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v2/token"));
        params.setSslSocketFactory(getSSLSocketFactory());
        params.setPasswordAuthentication(clientCredentials);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        List<String[]> postParams = new ArrayList<String[]>();
        postParams.add(new String[]{"grant_type", "password"});
        postParams.add(new String[]{"username", username});
        postParams.add(new String[]{"password", password});
        request.addParameters(postParams);

        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        return response;
    }

    public GenericHttpResponse authorizeWithSAMLToken(final PasswordAuthentication clientCredentials, final PasswordAuthentication passwordAuthentication) throws Exception {
        final HttpComponentsClient client = new HttpComponentsClient();
        final String tokenRestrictionsEncoded = URLEncoder.encode(String.format("https://%s:8443/auth/oauth/v2/token", gatewayHost), "UTF-8");
        final GenericHttpRequestParams samlTokenParams = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/oauth/v2/samlTokenServer?audience_recipient_restriction=" + tokenRestrictionsEncoded));
        samlTokenParams.setSslSocketFactory(getSSLSocketFactory());
        samlTokenParams.setPasswordAuthentication(passwordAuthentication);
        samlTokenParams.setFollowRedirects(false);
        final GenericHttpRequest samlRequest = client.createRequest(HttpMethod.GET, samlTokenParams);
        final GenericHttpResponse samlResponse = samlRequest.getResponse();
        assertEquals(302, samlResponse.getStatus());
        final String locationHeader = samlResponse.getHeaders().getFirstValue("Location");
        final String samlToken = locationHeader.substring(locationHeader.indexOf("saml=") + 5, locationHeader.length());
        final String decodedSamlToken = URLDecoder.decode(samlToken, "UTF-8");
        final String base64EncodedToken = HexUtils.encodeBase64(decodedSamlToken.getBytes(Charsets.UTF8));

        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v2/token"));
        params.setSslSocketFactory(getSSLSocketFactory());
        params.setPasswordAuthentication(clientCredentials);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);

        List<String[]> postParams = new ArrayList<String[]>();
        postParams.add(new String[]{"grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer"});
        postParams.add(new String[]{"assertion", base64EncodedToken});
        request.addParameters(postParams);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        return response;
    }
}
