package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.io.XmlUtil;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.utils.OAuthEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.xpath.*;
import java.net.PasswordAuthentication;
import java.net.URL;

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
    public String getAuthorizationUrl(OAuthConfig config) {
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

    public String authorize(final String consumerKey, final String callback) throws Exception {
        final CommonsHttpClient client = new CommonsHttpClient();

        final GenericHttpRequestParams sessionIdParams = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v2/authorize?client_id=" + consumerKey + "&redirect_uri=" + callback +
                "&response_type=code&scope=scope_test&state=state_test"));
        sessionIdParams.setFollowRedirects(true);
        sessionIdParams.setSslSocketFactory(SSLUtil.getSSLSocketFactory());
        final GenericHttpRequest sessionIdRequest = client.createRequest(HttpMethod.GET, sessionIdParams);
        final GenericHttpResponse sessionIdResponse = sessionIdRequest.getResponse();
        assertEquals(200, sessionIdResponse.getStatus());
        final String html = sessionIdResponse.getAsString(false, Integer.MAX_VALUE);
        final String sessionId = getSessionIdFromHtmlForm(html);
        assertFalse(sessionId.isEmpty());

        final GenericHttpRequestParams authParams = new GenericHttpRequestParams(new URL("https://" + gatewayHost +
                ":8443/auth/oauth/v2/authorize?action=Grant&username=admin&password=password&sessionID=" + sessionId));
        authParams.setFollowRedirects(true);
        authParams.setSslSocketFactory(SSLUtil.getSSLSocketFactory());
        final GenericHttpRequest authRequest = client.createRequest(HttpMethod.GET, authParams);
        final GenericHttpResponse authResponse = authRequest.getResponse();
        assertEquals(200, authResponse.getStatus());
        final String authCode = authResponse.getAsString(false, Integer.MAX_VALUE);
        assertFalse(authCode.isEmpty());
        return authCode;
    }

    private String getSessionIdFromHtmlForm(final String html) throws SAXException, XPathExpressionException {
        // sessionId is inside an html form as a hidden field
        final Integer start = html.indexOf("<form ");
        final Integer end = html.indexOf("</form>") + 7;
        final String formXml = html.substring(start, end);
        final Document document = XmlUtil.parse(formXml);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final XPathExpression expression = xPath.compile("/form/input[@name='sessionID']");
        final Node node = (Node) expression.evaluate(document, XPathConstants.NODE);
        return node.getAttributes().getNamedItem("value").getTextContent();
    }
}
