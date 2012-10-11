package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;
import static com.l7tech.skunkworks.oauth.toolkit.OAuthToollkitTestUtility.*;

/**
 * Integration tests for the OAuth Tool Kit that are not specific to OAuth 1.0 or 2.0.
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 * <p/>
 * At minimum you need to change the BASE_URL.
 */
@Ignore
public class OAuthToolkitIntegrationTest {
    private static final String BASE_URL = "localhost";
    private static final String USER = "admin";
    private static final String PASSWORD = "password";
    private static final String OAUTH_TOOLKIT_INTEGRATION_TEST = "OAuthToolkitIntegrationTest";
    private static final String CONFIDENTIAL = "confidential";
    private static final String ENABLED = "ENABLED";
    private static final String OOB = "oob";
    private static final String ALL = "ALL";
    private static final String CLIENT_IDENT = "client_ident";
    private static final String CLIENT_KEY = "client_key";
    private static final String EXPIRATION = "expiration";
    private GenericHttpClient client;
    private PasswordAuthentication passwordAuthentication;
    private XPath xPath;

    @Before
    public void setup() {
        client = new CommonsHttpClient();
        passwordAuthentication = new PasswordAuthentication(USER, PASSWORD.toCharArray());
        xPath = XPathFactory.newInstance().newXPath();
    }

    @Test
    public void storeClient() throws Exception {
        final Map<String, String> params = buildClientParams();
        final String clientIdentity = params.get(CLIENT_IDENT);

        store("client", params);

        final OAuthClient client = getClient(clientIdentity);
        assertDefaultValues(clientIdentity, client);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    @Test
    public void storeKey() throws Exception {
        // must first store a client
        final Map<String, String> clientParams = buildClientParams();
        final String clientIdentity = clientParams.get(CLIENT_IDENT);
        store("client", clientParams);

        final Map<String, String> keyParams = buildKeyParams(OOB, OOB, ALL);
        final String clientKey = keyParams.get(CLIENT_KEY);
        final String expiration = keyParams.get(EXPIRATION);
        keyParams.put(CLIENT_IDENT, clientIdentity);
        store("client", keyParams);

        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If callback is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void storeKeyEmptyCallback() throws Exception {
        // must first store a client
        final Map<String, String> clientParams = buildClientParams();
        final String clientIdentity = clientParams.get(CLIENT_IDENT);
        store("client", clientParams);

        final Map<String, String> keyParams = buildKeyParams("", OOB, ALL);
        final String clientKey = keyParams.get(CLIENT_KEY);
        final String expiration = keyParams.get(EXPIRATION);
        keyParams.put(CLIENT_IDENT, clientIdentity);
        store("client", keyParams);

        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If scope is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void storeKeyEmptyScope() throws Exception {
        // must first store a client
        final Map<String, String> clientParams = buildClientParams();
        final String clientIdentity = clientParams.get(CLIENT_IDENT);
        store("client", clientParams);

        final Map<String, String> keyParams = buildKeyParams(OOB, "", ALL);
        final String clientKey = keyParams.get(CLIENT_KEY);
        final String expiration = keyParams.get(EXPIRATION);
        keyParams.put(CLIENT_IDENT, clientIdentity);
        store("client", keyParams);

        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If environment is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void storeKeyEmptyEnvironment() throws Exception {
        // must first store a client
        final Map<String, String> clientParams = buildClientParams();
        final String clientIdentity = clientParams.get(CLIENT_IDENT);
        store("client", clientParams);

        final Map<String, String> keyParams = buildKeyParams(OOB, OOB, "");
        final String clientKey = keyParams.get(CLIENT_KEY);
        final String expiration = keyParams.get(EXPIRATION);
        keyParams.put(CLIENT_IDENT, clientIdentity);
        store("client", keyParams);

        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    @Test
    public void storeClientAndKey() throws Exception {
        final Map<String, String> params = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = params.get(CLIENT_IDENT);
        final String clientKey = params.get(CLIENT_KEY);
        final String expiration = params.get(EXPIRATION);

        store("client", params);

        final OAuthClient client = getClient(clientIdentity);
        assertDefaultValues(clientIdentity, client);
        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If callback is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void storeClientAndKeyEmptyCallback() throws Exception {
        final Map<String, String> params = buildClientAndKeyParams("", OOB, ALL);
        final String clientIdentity = params.get(CLIENT_IDENT);
        final String clientKey = params.get(CLIENT_KEY);
        final String expiration = params.get(EXPIRATION);

        store("client", params);

        final OAuthClient client = getClient(clientIdentity);
        assertDefaultValues(clientIdentity, client);
        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If scope is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void storeClientAndKeyEmptyScope() throws Exception {
        final Map<String, String> params = buildClientAndKeyParams(OOB, "", ALL);
        final String clientIdentity = params.get(CLIENT_IDENT);
        final String clientKey = params.get(CLIENT_KEY);
        final String expiration = params.get(EXPIRATION);

        store("client", params);

        final OAuthClient client = getClient(clientIdentity);
        assertDefaultValues(clientIdentity, client);
        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If environment is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void storeClientAndKeyEmptyEnvironment() throws Exception {
        final Map<String, String> params = buildClientAndKeyParams(OOB, OOB, "");
        final String clientIdentity = params.get(CLIENT_IDENT);
        final String clientKey = params.get(CLIENT_KEY);
        final String expiration = params.get(EXPIRATION);

        store("client", params);

        final OAuthClient client = getClient(clientIdentity);
        assertDefaultValues(clientIdentity, client);
        final ClientKey key = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, key);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    @Test
    public void updateKey() throws Exception {
        // must first create a client and key
        final Map<String, String> storeParams = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = storeParams.get(CLIENT_IDENT);
        final String clientKey = storeParams.get(CLIENT_KEY);
        store("client", storeParams);

        final ClientKey beforeUpdate = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, storeParams.get(EXPIRATION), beforeUpdate);

        final Map<String, String> updateParams = buildUpdateKeyParams(clientKey, "test", "test", "test");
        update(updateParams);

        final ClientKey afterUpdate = getKey(clientKey);
        assertEquals("test", afterUpdate.getCallback());
        assertEquals("test", afterUpdate.getScope());
        assertEquals("test", afterUpdate.getEnvironment());

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If callback is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void updateKeyEmptyCallback() throws Exception {
        // must first create a client and key
        final Map<String, String> storeParams = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = storeParams.get(CLIENT_IDENT);
        final String clientKey = storeParams.get(CLIENT_KEY);
        final String expiration = storeParams.get(EXPIRATION);
        store("client", storeParams);

        final ClientKey beforeUpdate = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, beforeUpdate);

        final Map<String, String> updateParams = buildUpdateKeyParams(clientKey, "", OOB, ALL);
        update(updateParams);

        final ClientKey afterUpdate = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, afterUpdate);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If scope is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void updateKeyEmptyScope() throws Exception {
        // must first create a client and key
        final Map<String, String> storeParams = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = storeParams.get(CLIENT_IDENT);
        final String clientKey = storeParams.get(CLIENT_KEY);
        final String expiration = storeParams.get(EXPIRATION);
        store("client", storeParams);

        final ClientKey beforeUpdate = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, beforeUpdate);

        final Map<String, String> updateParams = buildUpdateKeyParams(clientKey, OOB, "", ALL);
        update(updateParams);

        final ClientKey afterUpdate = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, afterUpdate);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    /**
     * If environment is empty, should be given default value.
     */
    @Test
    @BugNumber(13084)
    public void updateKeyEmptyEnvironment() throws Exception {
        // must first create a client and key
        final Map<String, String> storeParams = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = storeParams.get(CLIENT_IDENT);
        final String clientKey = storeParams.get(CLIENT_KEY);
        final String expiration = storeParams.get(EXPIRATION);
        store("client", storeParams);

        final ClientKey beforeUpdate = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, beforeUpdate);

        final Map<String, String> updateParams = buildUpdateKeyParams(clientKey, OOB, OOB, "");
        update(updateParams);

        final ClientKey afterUpdate = getKey(clientKey);
        assertDefaultValues(clientIdentity, clientKey, expiration, afterUpdate);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    @Test
    public void storeToken() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        final String token = UUID.randomUUID().toString();
        final Map<String, String> storeParams = new HashMap<String, String>();
        storeParams.put("callback", "oob");
        storeParams.put("client_key", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("client_name", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("expiration", String.valueOf(calendar.getTimeInMillis()));
        storeParams.put("resource_owner", USER);
        storeParams.put("scope", "test_scope");
        storeParams.put("secret", UUID.randomUUID().toString());
        storeParams.put("token", token);

        store("token", storeParams);

        final String tokenXml = get("token", token, "https://" + BASE_URL + ":8443/oauth/tokenstore/getTemp");
        assertFalse(tokenXml.isEmpty());

        delete("temp_token", token, "token");
    }

    @Test
    @BugNumber(13149)
    public void storeTokenWithEscapeChars() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        final String token = UUID.randomUUID().toString();
        final Map<String, String> storeParams = new HashMap<String, String>();
        storeParams.put("callback", "http://localhost:8080/callback?p1=a&p2=b");
        storeParams.put("client_key", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("client_name", OAUTH_TOOLKIT_INTEGRATION_TEST);
        storeParams.put("expiration", String.valueOf(calendar.getTimeInMillis()));
        storeParams.put("resource_owner", USER);
        storeParams.put("scope", "test_scope");
        storeParams.put("secret", UUID.randomUUID().toString());
        storeParams.put("token", token);

        store("token", storeParams);

        final String tokenXml = get("token", token, "https://" + BASE_URL + ":8443/oauth/tokenstore/getTemp");
        assertFalse(tokenXml.isEmpty());

        delete("temp_token", token, "token");
    }

    private void assertDefaultValues(final String clientIdentity, final OAuthClient client) {
        assertEquals(clientIdentity, client.getClientIdentity());
        assertEquals(OAUTH_TOOLKIT_INTEGRATION_TEST, client.getDescription());
        assertEquals(OAUTH_TOOLKIT_INTEGRATION_TEST, client.getName());
        assertEquals(OAUTH_TOOLKIT_INTEGRATION_TEST, client.getOrganization());
        assertEquals(USER, client.getRegisteredBy());
        assertEquals(CONFIDENTIAL, client.getType());
        assertFalse(client.getCreated().isEmpty());
    }

    private void assertDefaultValues(final String clientIdentity, final String clientKey, final String expiration, final ClientKey key) {
        assertEquals(clientIdentity, key.getClientIdentity());
        assertEquals(OAUTH_TOOLKIT_INTEGRATION_TEST, key.getClientName());
        assertEquals(clientKey, key.getClientKey());
        assertFalse(key.getSecret().isEmpty());
        assertEquals(OOB, key.getScope());
        assertEquals(OOB, key.getCallback());
        assertEquals(ALL, key.getEnvironment());
        assertEquals(expiration, key.getExpiration());
        assertEquals(ENABLED, key.getStatus());
        assertFalse(key.getCreated().isEmpty());
        assertEquals(USER, key.getCreatedBy());
    }

    private void store(final String type, final Map<String, String> parameters) throws Exception {
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

    private void update(final Map<String, String> parameters) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/clientstore/update"));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setPasswordAuthentication(passwordAuthentication);
        params.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        // yes there is a typo in the policy
        assertEquals("1 client_keys(s) updated", new String(IOUtils.slurpStream(response.getInputStream())));
    }

    private Map<String, String> buildCommonParams() throws Exception {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(CLIENT_IDENT, UUID.randomUUID().toString());
        params.put("registered_by", USER);
        return params;
    }

    private Map<String, String> buildClientParams() throws Exception {
        final Map<String, String> params = buildCommonParams();
        params.put("description", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("name", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("org", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("type", CONFIDENTIAL);
        return params;
    }

    private Map<String, String> buildKeyParams(final String callback, final String scope, final String environment) throws Exception {
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

    private Map<String, String> buildUpdateKeyParams(final String clientKey, final String callback, final String scope, final String environment) throws Exception {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(CLIENT_KEY, clientKey);
        params.put("secret", OAUTH_TOOLKIT_INTEGRATION_TEST);
        params.put("status", ENABLED);
        params.put("callback", callback);
        params.put("scope", scope);
        params.put("environment", environment);
        return params;
    }

    private Map<String, String> buildClientAndKeyParams(final String callback, final String scope, final String environment) throws Exception {
        final Map<String, String> params = buildClientParams();
        params.putAll(buildKeyParams(callback, scope, environment));
        return params;
    }

    private void delete(final String paramName, final String paramValue, final String type) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/" + type + "store/delete"));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        request.addParameter(paramName, paramValue);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        assertEquals("1 " + type + "(s) deleted", new String(IOUtils.slurpStream(response.getInputStream())));
    }

    private String get(final String paramName, final String paramValue, final String endpoint) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(endpoint));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setPasswordAuthentication(passwordAuthentication);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        request.addParameter(paramName, paramValue);

        final GenericHttpResponse response = request.getResponse();

        assertEquals(200, response.getStatus());
        return new String(IOUtils.slurpStream(response.getInputStream()));
    }

    private ClientKey getKey(final String clientKey) throws Exception {
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

    private OAuthClient getClient(final String clientIdentity) throws Exception {
        final String responseBody = get(CLIENT_IDENT, clientIdentity, "https://" + BASE_URL + ":8443/oauth/clientstore/get");
        final Document document = XmlUtil.parse(responseBody);
        final OAuthClient client = new OAuthClient();
        client.setClientIdentity(getValue(document, "/values/value/client_ident"));
        client.setName(getValue(document, "/values/value/name"));
        client.setType(getValue(document, "/values/value/type"));
        client.setDescription(getValue(document, "/values/value/description"));
        client.setOrganization(getValue(document, "/values/value/organization"));
        client.setRegisteredBy(getValue(document, "/values/value/registered_by"));
        client.setCreated(getValue(document, "/values/value/created"));
        return client;
    }

    private String getValue(final Document document, final String xpathExpression) throws Exception {
        final XPathExpression expression = xPath.compile(xpathExpression);
        final Node node = (Node) expression.evaluate(document, XPathConstants.NODE);
        return node.getTextContent();
    }

    private class ClientKey {
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

    private class OAuthClient {
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
