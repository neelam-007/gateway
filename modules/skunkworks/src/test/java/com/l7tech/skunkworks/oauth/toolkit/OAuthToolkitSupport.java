package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.*;

import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.getSSLSocketFactoryWithKeyManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class OAuthToolkitSupport {
    protected static final String BASE_URL = "localhost";
    protected static final String USER = "admin";
    protected static final String PASSWORD = "password";
    protected static final String CONFIDENTIAL = "confidential";
    protected static final String ENABLED = "ENABLED";
    protected static final String OOB = "oob";
    protected static final String ALL = "ALL";
    protected static final String CLIENT_IDENT = "client_ident";
    protected static final String CLIENT_KEY = "client_key";
    protected static final String EXPIRATION = "expiration";
    protected static final String TOKEN = "token";
    protected static final String OAUTH_TOOLKIT_INTEGRATION_TEST = "OAuthToolkitIntegrationTest";
    protected GenericHttpClient client;
    protected PasswordAuthentication passwordAuthentication = new PasswordAuthentication(USER, PASSWORD.toCharArray());
    protected XPath xPath;

    @Before
    public void setupSupport() {
        client = new CommonsHttpClient();
        xPath = XPathFactory.newInstance().newXPath();
    }

    protected void store(final String type, final Map<String, String> parameters) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/" + type + "store/store"));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setPasswordAuthentication(passwordAuthentication);
        params.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("persisted", new String(IOUtils.slurpStream(response.getInputStream())));
    }

    protected String get(final String paramName, final String paramValue, final String endpoint) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(endpoint));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        request.addParameter(paramName, paramValue);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        return new String(IOUtils.slurpStream(response.getInputStream()));
    }

    private Map<String, String> buildCommonParams() throws Exception {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(CLIENT_IDENT, UUID.randomUUID().toString());
        params.put("registered_by", USER);
        return params;
    }

    protected Map<String, String> buildClientParams() throws Exception {
        final Map<String, String> params = buildCommonParams();
        params.put("description", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("name", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("org", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("type", CONFIDENTIAL);
        return params;
    }

    protected Map<String, String> buildKeyParams(final String callback, final String scope, final String environment) throws Exception {
        final Map<String, String> params = buildCommonParams();
        params.put(CLIENT_KEY, UUID.randomUUID().toString());
        params.put(EXPIRATION, String.valueOf(new Date().getTime()));
        params.put("secret", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("status", ENABLED);
        params.put("callback", callback);
        params.put("scope", scope);
        params.put("environment", environment);
        return params;
    }

    protected Map<String, String> buildUpdateKeyParams(final String clientKey, final String callback, final String scope, final String environment) throws Exception {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(CLIENT_KEY, clientKey);
        params.put("secret", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("status", ENABLED);
        params.put("callback", callback);
        params.put("scope", scope);
        params.put("environment", environment);
        return params;
    }

    protected Map<String, String> buildClientAndKeyParams(final String callback, final String scope, final String environment) throws Exception {
        final Map<String, String> params = buildClientParams();
        params.putAll(buildKeyParams(callback, scope, environment));
        return params;
    }

    protected Map<String, String> buildTokenParams() {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        final Map<String, String> storeParams = new HashMap<String, String>();
        storeParams.put("client_key", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("client_name", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("expiration", String.valueOf(calendar.getTimeInMillis()));
        storeParams.put("resource_owner", USER);
        storeParams.put("scope", "test_scope");
        storeParams.put("secret", UUID.randomUUID().toString());
        storeParams.put("status", ENABLED);
        storeParams.put("token", UUID.randomUUID().toString());
        return storeParams;
    }

    protected Map<String, String> buildTempTokenParams() {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        final Map<String, String> storeParams = new HashMap<String, String>();
        storeParams.put("callback", "oob");
        storeParams.put("client_key", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("client_name", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("expiration", String.valueOf(calendar.getTimeInMillis()));
        storeParams.put("resource_owner", USER);
        storeParams.put("scope", "test_scope");
        storeParams.put("secret", UUID.randomUUID().toString());
        storeParams.put("token", UUID.randomUUID().toString());
        return storeParams;
    }

    protected ClientKey getKey(final String clientKey) throws Exception {
        final String responseBody = get(CLIENT_KEY, clientKey, "https://" + BASE_URL + ":8443/oauth/clientstore/getKey");
        final Document document = XmlUtil.parse(responseBody);
        final ClientKey key = new ClientKey();
        key.setClientIdentity(getValue(document, "/values/value/client_ident"));
        key.setClientName(getValue(document, "/values/value/client_name"));
        key.setClientKey(getValue(document, "/values/value/client_key"));
        key.setSecret(getValue(document, "/values/value/secret"));
        key.setScope(getValue(document, "/values/value/scope"));
        key.setCallback(getValue(document, "/values/value/callback"));
        key.setEnvironment(getValue(document, "/values/value/environment"));
        key.setExpiration(getValue(document, "/values/value/expiration"));
        key.setStatus(getValue(document, "/values/value/status"));
        key.setCreated(getValue(document, "/values/value/created"));
        key.setCreatedBy(getValue(document, "/values/value/created_by"));
        return key;
    }

    protected void assertClientKeyDoesNotExist(final String clientKey) throws Exception {
        final String responseBody = get(CLIENT_KEY, clientKey, "https://" + BASE_URL + ":8443/oauth/clientstore/getKey");
        assertEquals("<values></values>", StringUtils.deleteWhitespace(responseBody));
    }

    protected void assertTokenDoesNotExist(final String token) throws Exception {
        final String responseBody = get(TOKEN, token, "https://" + BASE_URL + ":8443/oauth/tokenstore/get");
        assertEquals("<?xmlversion=\"1.0\"encoding=\"UTF-8\"?><values/>", StringUtils.deleteWhitespace(responseBody));
    }

    protected void assertTempTokenDoesNotExist(final String token) throws Exception {
        final String responseBody = get(TOKEN, token, "https://" + BASE_URL + ":8443/oauth/tokenstore/getTemp");
        assertEquals("<?xmlversion=\"1.0\"encoding=\"UTF-8\"?><values/>", StringUtils.deleteWhitespace(responseBody));
    }

    protected String getValue(final Document document, final String xpathExpression) throws Exception {
        final XPathExpression expression = xPath.compile(xpathExpression);
        final Node node = (Node) expression.evaluate(document, XPathConstants.NODE);
        return node.getTextContent();
    }

    protected void delete(final String paramName, final String paramValue, final String type) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/" + type + "store/delete"));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        request.addParameter(paramName, paramValue);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        assertEquals("1 " + type + "(s) deleted", new String(IOUtils.slurpStream(response.getInputStream())));
    }

    protected void revoke(final String paramName, final String paramValue) throws Exception {
        final GenericHttpRequestParams revokeParams = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/clientstore/revoke"));
        revokeParams.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        revokeParams.setPasswordAuthentication(passwordAuthentication);
        revokeParams.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest revokeRequest = client.createRequest(HttpMethod.POST, revokeParams);
        revokeRequest.addParameter(paramName, paramValue);
        final GenericHttpResponse revokeResponse = revokeRequest.getResponse();
        assertEquals(200, revokeResponse.getStatus());
        final String revokeResponseBody = new String(IOUtils.slurpStream(revokeResponse.getInputStream()));
        assertEquals("1 client_key(s) revoked", revokeResponseBody);
    }

    protected void disable(final String paramName, final String paramValue) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/tokenstore/disable"));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        request.addParameter(paramName, paramValue);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("1 token(s) disabled", new String(IOUtils.slurpStream(response.getInputStream())));
    }

    protected class ClientKey {
        private String clientIdentity;
        private String clientName;
        private String clientKey;
        private String secret;
        private String scope;
        private String callback;
        private String environment;
        private String expiration;
        private String status;
        private String created;
        private String createdBy;

        public String getClientIdentity() {
            return clientIdentity;
        }

        public void setClientIdentity(String clientIdentity) {
            this.clientIdentity = clientIdentity;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getClientKey() {
            return clientKey;
        }

        public void setClientKey(String clientKey) {
            this.clientKey = clientKey;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getCallback() {
            return callback;
        }

        public void setCallback(String callback) {
            this.callback = callback;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public String getExpiration() {
            return expiration;
        }

        public void setExpiration(String expiration) {
            this.expiration = expiration;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }

    protected class OAuthClient {
        private String clientIdentity;
        private String name;
        private String type;
        private String description;
        private String organization;
        private String registeredBy;
        private String created;

        public String getClientIdentity() {
            return clientIdentity;
        }

        public void setClientIdentity(final String clientIdentity) {
            this.clientIdentity = clientIdentity;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(final String organization) {
            this.organization = organization;
        }

        public String getRegisteredBy() {
            return registeredBy;
        }

        public void setRegisteredBy(String registeredBy) {
            this.registeredBy = registeredBy;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }
    }
}
