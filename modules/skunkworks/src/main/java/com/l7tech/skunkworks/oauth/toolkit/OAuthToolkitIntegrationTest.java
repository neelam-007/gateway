package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.io.PermissiveX509TrustManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.xpath.*;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for the OAuth Tool Kit. Each test requires the SSG to be running with an installed OTK that includes
 * the default OTK client.
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 *
 * At minimum you need to change the BASE_URL and SIGNATURE strings.
 */
@Ignore
public class OAuthToolkitIntegrationTest {
    //private static final String BASE_URL = "daesm62t.l7tech.com";
    private static final String BASE_URL = "aleeoauth.l7tech.com";
    //private static final String SIGNATURE = "qBZINGunNf/GiqJkf6PXGUsorh8=";
    private static final String SIGNATURE = "pwvUKSVVqeU4nZsBfbm1Pr6lEpk=";
    private static final String CLIENT_REQUEST_TOKEN = "http://" + BASE_URL + ":8080/oauth/v1/client?state=request_token";
    private static final String REQUEST_TOKEN_ENDPOINT = "https://" + BASE_URL + ":8443/auth/oauth/v1/request";
    private static final String AUTHORIZE_ENDPOINT = "https://" + BASE_URL + ":8443/auth/oauth/v1/authorize";
    private static final String OTK_CLIENT_CALLBACK = "https://" + BASE_URL + ":8443/oauth/v1/client?state=authorized";
    private static final String AUTH_HEADER = "OAuth realm=\"http://" + BASE_URL + "\",oauth_consumer_key=\"acf89db2-994e-427b-ac2c-88e6101f9433\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"1344979213\",oauth_nonce=\"ca6e55f3-3e1c-41d6-8584-41c7e2611d34\",oauth_callback=\"https://" + BASE_URL + ":8443/oauth/v1/client?state=authorized\",oauth_version=\"1.0\",oauth_signature=\"" + SIGNATURE + "\"";
    private static final String USER = "admin";
    private static final String PASSWORD = "password";
    private static final String OTK_CLIENT_CONSUMER_KEY = "acf89db2-994e-427b-ac2c-88e6101f9433";

    private GenericHttpClient client;
    private PasswordAuthentication passwordAuthentication;

    @Before
    public void setup() {
        client = new CommonsHttpClient();
        passwordAuthentication = new PasswordAuthentication(USER, PASSWORD.toCharArray());
    }

    /**
     * Request a request token using the OTK client. The client will process the oauth request token request for you.
     */
    @Test
    public void requestTokenClient() throws Exception {
        final String oauthToken = getRequestTokenFromClient();
        assertFalse(oauthToken.isEmpty());
    }

    /**
     * Request a request token via OTK endpoint. You are responsible for passing the correct oauth parameters.
     */
    @Test
    public void requestTokenEndpoint() throws Exception {
        final Map<String, String> responseParameters = getRequestTokenFromEndpoint();
        assertParamsFromRequestTokenEndpoint(responseParameters);
    }

    /**
     * OAuth params are in the Authorization header instead of request body.
     */
    @Test
    public void requestTokenEndpointAuthorizationHeader() throws Exception {
        System.out.println("Requesting request token from " + REQUEST_TOKEN_ENDPOINT + " using Authorization header");

        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(REQUEST_TOKEN_ENDPOINT));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        requestParams.setExtraHeaders(new HttpHeader[]{new GenericHttpHeader("Authorization", AUTH_HEADER)});
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, requestParams);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        final Map<String, String> responseParameters = extractParamsFromString(response.getAsString(false, Integer.MAX_VALUE));
        assertParamsFromRequestTokenEndpoint(responseParameters);
    }

    /**
     * Signature doesn't pass verification.
     */
    @Test
    public void requestTokenEndpointInvalidSignature() throws Exception {
        final Map<String, String> parameters = createDefaultParameterMap();
        parameters.put("oauth_signature", "invalidSignature");
        final GenericHttpResponse response = getRequestTokenResponseFromEndpoint(parameters);
        assertEquals(500, response.getStatus());
    }

    /**
     * Consumer key isn't recognized by the OTK.
     */
    @Test
    public void requestTokenEndpointUnrecognizedConsumerKey() throws Exception {
        final Map<String, String> parameters = createDefaultParameterMap();
        parameters.put("oauth_consumer_key", "unregisteredClient");
        final GenericHttpResponse response = getRequestTokenResponseFromEndpoint(parameters);
        assertEquals(500, response.getStatus());
    }

    /**
     * Currently OTK only supports HMAC-SHA1.
     */
    @Test
    public void requestTokenEndpointSignatureMethodNotHMACSHA1() throws Exception {
        final Map<String, String> parameters = createDefaultParameterMap();
        parameters.put("oauth_signature_method", "RSA-SHA1");
        final GenericHttpResponse response = getRequestTokenResponseFromEndpoint(parameters);
        assertEquals(400, response.getStatus());
    }

    /**
     * Check required parameters.
     */
    @Test
    public void requestTokenEndpointMissingParameters() throws Exception {
        assertMissingParamReturns400("oauth_consumer_key");
        assertMissingParamReturns400("oauth_signature_method");
        assertMissingParamReturns400("oauth_timestamp");
        assertMissingParamReturns400("oauth_nonce");
        assertMissingParamReturns400("oauth_signature");
    }

    /**
     * Authorize a request token using the OTK endpoint.
     */
    @Test
    public void authorizeRequestToken() throws Exception {
        // must first get a non-expired request token
        final String requestToken = getRequestTokenFromEndpoint().get("oauth_token");

        final GenericHttpResponse authenticateResponse = authorizeRequestToken(requestToken);
        assertEquals(200, authenticateResponse.getStatus());
        System.out.println("Request token authentication successful");
    }

    /**
     * Token not specified.
     */
    @Test
    public void authorizeRequestTokenMissingToken() throws Exception {
        final GenericHttpResponse authenticateResponse = authorizeRequestToken(null);
        assertEquals(401, authenticateResponse.getStatus());
    }

    /**
     * Username/password not recognized.
     */
    @Test
    public void authorizeRequestTokenUnauthorized() throws Exception {
        final String requestToken = getRequestTokenFromEndpoint().get("oauth_token");

        final GenericHttpResponse authenticateResponse = authorizeRequestToken(requestToken,
                new PasswordAuthentication("unauthorized", "unauthorized".toCharArray()));
        assertEquals(401, authenticateResponse.getStatus());
    }

    /**
     * Obtain an access token via OTK client.
     * <p/>
     * Using the client so that we can use the default callback url to verify the access token.
     */
    @Test
    public void accessTokenClient() throws Exception {
        // must first get a non-expired request token
        final String requestToken = getRequestTokenFromClient();

        // then authorize the request token
        final GenericHttpResponse authenticateResponse = authorizeRequestToken(requestToken);
        assertEquals(200, authenticateResponse.getStatus());
        System.out.println("Request token authentication successful");

        // finally get the access token
        System.out.println("Requesting access token from: " + AUTHORIZE_ENDPOINT);
        final GenericHttpRequestParams grantedParams = new GenericHttpRequestParams(new URL(AUTHORIZE_ENDPOINT +
                "?state=granted&oauth_token=" + requestToken));
        grantedParams.setFollowRedirects(true);
        grantedParams.setSslSocketFactory(getSSLSocketFactory());
        grantedParams.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest grantedRequest = client.createRequest(HttpMethod.GET, grantedParams);

        // when the granted request is sent it is redirected to the callback url
        final GenericHttpResponse grantedResponse = grantedRequest.getResponse();
        assertEquals(200, grantedResponse.getStatus());

        // response is in html format
        final String html = grantedResponse.getAsString(false, Integer.MAX_VALUE);

        final String accessToken = getTokenFromHtmlForm(html);
        assertFalse(accessToken.isEmpty());
        assertFalse(accessToken.equalsIgnoreCase(requestToken));
        System.out.println("Received access token: " + accessToken);
    }

    private String getRequestTokenFromClient() throws Exception {
        System.out.println("Requesting request token from " + CLIENT_REQUEST_TOKEN);
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(CLIENT_REQUEST_TOKEN));
        requestParams.setFollowRedirects(true);
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        final GenericHttpRequest request = client.createRequest(HttpMethod.GET, requestParams);

        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());

        // response is in html format
        final String html = response.getAsString(false, Integer.MAX_VALUE);
        final String token = getTokenFromHtmlForm(html);
        System.out.println("Received oauth_token: " + token);
        return token;
    }

    private Map<String, String> getRequestTokenFromEndpoint() throws Exception {
        // signature generated using OTK client consumer secret
        final GenericHttpResponse response = getRequestTokenResponseFromEndpoint(createDefaultParameterMap());
        assertEquals(200, response.getStatus());

        // expected response format: oauth_token=49f0589b-7b8c-4b3d-a416-e9b0fc6bf401&oauth_token_secret=2d288801-7e33-49ca-8f3c-0c39df56664c&oauth_callback_confirmed=true
        final String responseAsString = response.getAsString(false, Integer.MAX_VALUE);
        return extractParamsFromString(responseAsString);
    }

    private Map<String, String> extractParamsFromString(final String responseAsString) {
        final String[] pairs = responseAsString.split("&");
        final Map<String, String> responseParameters = new HashMap<String, String>(pairs.length);
        for (int i = 0; i < pairs.length; i++) {
            final String pair = pairs[i];
            final String[] keyValue = pair.split("=");
            responseParameters.put(keyValue[0], keyValue[1]);
        }
        return responseParameters;
    }

    private GenericHttpResponse getRequestTokenResponseFromEndpoint(final Map<String, String> parameters) throws Exception {
        System.out.println("Requesting request token from " + REQUEST_TOKEN_ENDPOINT);
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(new URL(REQUEST_TOKEN_ENDPOINT));
        requestParams.setSslSocketFactory(getSSLSocketFactory());
        requestParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, requestParams);
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }

        return request.getResponse();
    }

    private Map<String, String> createDefaultParameterMap() {
        final Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("oauth_consumer_key", OTK_CLIENT_CONSUMER_KEY);
        parameterMap.put("oauth_signature_method", "HMAC-SHA1");
        parameterMap.put("oauth_timestamp", "1344979213");
        parameterMap.put("oauth_nonce", "ca6e55f3-3e1c-41d6-8584-41c7e2611d34");
        parameterMap.put("oauth_callback", OTK_CLIENT_CALLBACK);
        parameterMap.put("oauth_version", "1.0");
        parameterMap.put("oauth_signature", SIGNATURE);
        return parameterMap;
    }

    private String getTokenFromHtmlForm(final String html) throws SAXException, XPathExpressionException {
        // oauth_token is inside an html form as a hidden field
        final Integer start = html.indexOf("<form ");
        final Integer end = html.indexOf("</form>") + 7;
        final String formXml = html.substring(start, end);
        final Document document = XmlUtil.parse(formXml);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final XPathExpression expression = xPath.compile("/form/input[@name='oauth_token']");
        final Node oathTokenNode = (Node) expression.evaluate(document, XPathConstants.NODE);
        return oathTokenNode.getAttributes().getNamedItem("value").getTextContent();
    }

    private GenericHttpResponse authorizeRequestToken(final String requestToken) throws Exception {
        return authorizeRequestToken(requestToken, passwordAuthentication);
    }

    private GenericHttpResponse authorizeRequestToken(final String requestToken, final PasswordAuthentication passwordAuthentication) throws Exception {
        System.out.println("Authenticating request token: " + requestToken + " via " + AUTHORIZE_ENDPOINT);
        String url = AUTHORIZE_ENDPOINT + "?state=authenticate";
        if (requestToken != null) {
            url = url + "&oauth_token=" + requestToken;
        }
        final GenericHttpRequestParams authenticateParams = new GenericHttpRequestParams(new URL(url));
        authenticateParams.setSslSocketFactory(getSSLSocketFactory());
        authenticateParams.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest authenticateRequest = client.createRequest(HttpMethod.GET, authenticateParams);

        return authenticateRequest.getResponse();
    }

    private void assertMissingParamReturns400(final String parameter) throws Exception {
        final Map<String, String> parameters = createDefaultParameterMap();
        parameters.remove(parameter);
        final GenericHttpResponse response = getRequestTokenResponseFromEndpoint(parameters);
        assertEquals(400, response.getStatus());
    }

    private void assertParamsFromRequestTokenEndpoint(final Map<String, String> responseParameters) {
        final String oauthToken = responseParameters.get("oauth_token");
        final String oauthTokenSecret = responseParameters.get("oauth_token_secret");
        final String callbackConfirmed = responseParameters.get("oauth_callback_confirmed").trim();
        assertFalse(oauthToken.isEmpty());
        assertFalse(oauthTokenSecret.isEmpty());
        assertTrue(callbackConfirmed.equalsIgnoreCase("true") || callbackConfirmed.equalsIgnoreCase("false"));
        System.out.println("Received oauth_token: " + oauthToken);
        System.out.println("Received oauth_token_secret: " + oauthTokenSecret);
        System.out.println("Callback confirmed: " + callbackConfirmed);
    }

    /**
     * Trust everything.
     */
    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = new KeyManager[]{};
        sslContext.init(keyManagers, new X509TrustManager[]{new PermissiveX509TrustManager()}, null);
        return sslContext.getSocketFactory();
    }
}
