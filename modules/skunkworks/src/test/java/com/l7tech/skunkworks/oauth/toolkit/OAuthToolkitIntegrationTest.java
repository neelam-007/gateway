package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static com.l7tech.skunkworks.oauth.toolkit.OAuthToolkitTestUtility.getSSLSocketFactoryWithKeyManager;
import static org.junit.Assert.*;

/**
 * Integration tests for the OAuth Tool Kit that are not specific to OAuth 1.0 or 2.0.
 * <p/>
 * Modify static Strings as needed and remove the Ignore annotation to execute the tests.
 * <p/>
 * At minimum you need to change the BASE_URL.
 */
@Ignore
public class OAuthToolkitIntegrationTest extends OAuthToolkitSupport {
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
        final Map<String, String> storeParams = buildTempTokenParams();
        final String token = storeParams.get("token");

        store("token", storeParams);

        final String tokenXml = get("token", token, "https://" + BASE_URL + ":8443/oauth/tokenstore/getTemp");
        assertFalse(tokenXml.isEmpty());

        delete("temp_token", token, "token");
    }

    @Test
    @BugNumber(13149)
    public void storeTokenWithEscapeChars() throws Exception {
        final Map<String, String> storeParams = buildTempTokenParams();
        storeParams.put("callback", "http://localhost:8080/callback?p1=a&p2=b");
        final String token = storeParams.get("token");

        store("token", storeParams);

        final String tokenXml = get("token", token, "https://" + BASE_URL + ":8443/oauth/tokenstore/getTemp");
        assertFalse(tokenXml.isEmpty());

        delete("temp_token", token, "token");
    }

    @Test
    @BugNumber(13284)
    public void revokeClientKey() throws Exception {
        final Map<String, String> clientAndKeyParams = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = clientAndKeyParams.get(CLIENT_IDENT);
        final String clientKey = clientAndKeyParams.get(CLIENT_KEY);
        store("client", clientAndKeyParams);

        final Map<String, String> tempTokenParams = buildTempTokenParams();
        tempTokenParams.put("client_key", clientKey);
        final String tempToken = tempTokenParams.get("token");
        store("token", tempTokenParams);

        final Map<String, String> accessTokenParams = buildTokenParams();
        accessTokenParams.put("client_key", clientKey);
        final String accessToken = accessTokenParams.get("token");
        store("token", accessTokenParams);

        revoke("client_key", clientKey);

        assertClientKeyDoesNotExist(clientKey);
        assertTempTokenDoesNotExist(tempToken);
        assertTokenDoesNotExist(accessToken);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    @Test
    @BugNumber(13284)
    public void revokeClientKeysByClientIdentity() throws Exception {
        final Map<String, String> clientAndKeyParams = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = clientAndKeyParams.get(CLIENT_IDENT);
        final String clientKey = clientAndKeyParams.get(CLIENT_KEY);
        store("client", clientAndKeyParams);

        final Map<String, String> tempTokenParams = buildTempTokenParams();
        tempTokenParams.put("client_key", clientKey);
        final String tempToken = tempTokenParams.get("token");
        store("token", tempTokenParams);

        final Map<String, String> accessTokenParams = buildTokenParams();
        accessTokenParams.put("client_key", clientKey);
        final String accessToken = accessTokenParams.get("token");
        store("token", accessTokenParams);

        revoke("client_ident", clientIdentity);

        assertClientKeyDoesNotExist(clientKey);
        assertTempTokenDoesNotExist(tempToken);
        assertTokenDoesNotExist(accessToken);

        // restore initial state
        delete(CLIENT_IDENT, clientIdentity, "client");
    }

    @Test
    public void disableTokensByClientKey() throws Exception {
        final Map<String, String> clientAndKeyParams = buildClientAndKeyParams(OOB, OOB, ALL);
        final String clientIdentity = clientAndKeyParams.get(CLIENT_IDENT);
        final String clientKey = clientAndKeyParams.get(CLIENT_KEY);
        store("client", clientAndKeyParams);

        final Map<String, String> accessTokenParams = buildTokenParams();
        accessTokenParams.put("client_key", clientKey);
        final String accessToken = accessTokenParams.get("token");
        store("token", accessTokenParams);

        disable(CLIENT_KEY, clientKey);

        final String responseBody = get(TOKEN, accessToken, "https://" + BASE_URL + ":8443/oauth/tokenstore/get");
        assertTrue(responseBody.contains("<status>DISABLED</status>"));

        // restore initial state
        delete(TOKEN, accessToken, "token");
        delete(CLIENT_IDENT, clientIdentity, "client");
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

}
