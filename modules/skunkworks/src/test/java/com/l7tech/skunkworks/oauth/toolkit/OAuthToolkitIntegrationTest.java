package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import java.net.URL;
import java.util.*;

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
        // if the xml were invalid, it would not be parsable
        final Document parsed = XmlUtil.parse(tokenXml);
        assertNotNull(parsed);

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

    @Test
    @BugNumber(13304)
    public void storeAndGetSession() throws Exception {
        final Map<String, String> storeParams = buildStoreSessionParams(true);
        final String cacheKey = storeParams.get("cacheKey");
        final GenericHttpResponse storeResponse = requestSession("store", storeParams);
        assertEquals(200, storeResponse.getStatus());
        final String storeResponseBody = new String(IOUtils.slurpStream(storeResponse.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, true);

        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<foundxmlns=\"http://ns.l7tech.com/2012/11/otk-session\"><value>OAuthToolkitIntegrationTest</value><location>cache</location></found>", StringUtils.deleteWhitespace(getResponseBody));
    }

    /**
     * Do not provide optional parameters so that defaults are used.
     */
    @Test
    @BugNumber(13304)
    public void storeAndGetSessionWithDefaultValues() throws Exception {
        final Map<String, String> storeParams = buildStoreSessionParams(false);
        final String cacheKey = storeParams.get("cacheKey");
        final GenericHttpResponse storeResponse = requestSession("store", storeParams);
        assertEquals(200, storeResponse.getStatus());
        final String storeResponseBody = new String(IOUtils.slurpStream(storeResponse.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, false);

        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<foundxmlns=\"http://ns.l7tech.com/2012/11/otk-session\"><value>OAuthToolkitIntegrationTest</value><location>cache</location></found>", StringUtils.deleteWhitespace(getResponseBody));
    }

    @Test
    @BugNumber(13304)
    public void sessionInvalidOperation() throws Exception {
        final GenericHttpResponse response = requestSession("invalid", Collections.<String, String>emptyMap());
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals("Unsupported operation", responseBody);
    }

    @Test
    @BugNumber(13304)
    public void storeSessionMissingKey() throws Exception {
        final Map<String, String> params = buildStoreSessionParams(true);
        params.remove("cacheKey");
        final GenericHttpResponse response = requestSession("store", params);
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals("Invalid request", responseBody);
    }

    @Test
    @BugNumber(13304)
    public void storeSessionMissingValue() throws Exception {
        final Map<String, String> params = buildStoreSessionParams(true);
        params.remove("value");
        final GenericHttpResponse response = requestSession("store", params);
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals("Invalid request", responseBody);
    }

    @Test
    @BugNumber(13304)
    public void getSessionMissingKey() throws Exception {
        final Map<String, String> params = buildGetSessionParams(null, true);
        final GenericHttpResponse response = requestSession("get", params);
        assertEquals(400, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals("Invalid request", responseBody);
    }

    /**
     * If not found, should still return 200 status to be consistent with other SecureZone-Storage endpoints.
     */
    @Test
    @BugNumber(13304)
    public void getSessionNotFound() throws Exception {
        final Map<String, String> params = buildGetSessionParams("notFound", true);
        final GenericHttpResponse response = requestSession("get", params);
        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertEquals("<found xmlns=\"http://ns.l7tech.com/2012/11/otk-session\" />", responseBody);
    }

    /**
     * Storing same cacheKey + cacheId twice will update the entry.
     */
    @Test
    @BugNumber(13304)
    public void updateAndGetSession() throws Exception {
        // store once
        final Map<String, String> storeParams1 = buildStoreSessionParams(false);
        final String cacheKey = storeParams1.get("cacheKey");
        final GenericHttpResponse storeResponse1 = requestSession("store", storeParams1);
        assertEquals(200, storeResponse1.getStatus());
        final String storeResponseBody1 = new String(IOUtils.slurpStream(storeResponse1.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody1);

        // store again (update)
        final Map<String, String> storeParams2 = buildStoreSessionParams(false);
        storeParams2.put("cacheKey", cacheKey);
        storeParams2.put("value", "updated");
        final GenericHttpResponse storeResponse2 = requestSession("store", storeParams2);
        assertEquals(200, storeResponse2.getStatus());
        final String storeResponseBody2 = new String(IOUtils.slurpStream(storeResponse2.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody2);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, false);

        // should be updated value
        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<foundxmlns=\"http://ns.l7tech.com/2012/11/otk-session\"><value>updated</value><location>cache</location></found>", StringUtils.deleteWhitespace(getResponseBody));
    }

    /**
     * If cache entry is expired, the session should come from the database.
     */
    @Test
    @BugNumber(13304)
    public void storeAndGetSessionFromDatabase() throws Exception {
        final Map<String, String> storeParams = buildStoreSessionParams(true);
        // cache expires after 1 second
        storeParams.put("cacheAge", "1");
        // db entry expires after 60 seconds
        storeParams.put("dbAge", "60");
        final String cacheKey = storeParams.get("cacheKey");
        final GenericHttpResponse storeResponse = requestSession("store", storeParams);
        assertEquals(200, storeResponse.getStatus());
        final String storeResponseBody = new String(IOUtils.slurpStream(storeResponse.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody);

        // wait for 3 seconds
        Thread.sleep(3 * 1000);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, true);
        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<foundxmlns=\"http://ns.l7tech.com/2012/11/otk-session\"><value>OAuthToolkitIntegrationTest</value><location>database</location></found>", StringUtils.deleteWhitespace(getResponseBody));
    }

    @Test
    @BugNumber(13304)
    public void storeAndGetSessionExpired() throws Exception {
        final Map<String, String> storeParams = buildStoreSessionParams(true);
        // expires in 1 second
        storeParams.put("cacheAge", "1");
        storeParams.put("dbAge", "1");
        final String cacheKey = storeParams.get("cacheKey");
        final GenericHttpResponse storeResponse = requestSession("store", storeParams);
        assertEquals(200, storeResponse.getStatus());
        final String storeResponseBody = new String(IOUtils.slurpStream(storeResponse.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody);

        // sleep for 3 seconds
        Thread.sleep(3 * 1000);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, true);
        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<foundxmlns=\"http://ns.l7tech.com/2012/11/otk-session\"/>", StringUtils.deleteWhitespace(getResponseBody));
    }

    @Test
    @BugNumber(13304)
    public void deleteExpiredSessions() throws Exception {
        final GenericHttpResponse response = requestSession("deleteExpired", Collections.<String, String>emptyMap());
        assertEquals(200, response.getStatus());
        final String responseBody = new String(IOUtils.slurpStream(response.getInputStream()));
        assertTrue(responseBody.contains("expired sessions deleted."));
    }

    @Test
    @BugNumber(13304)
    public void storeAndGetSessionSpecialCharactersFromCache() throws Exception {
        final Map<String, String> storeParams = buildStoreSessionParams(true);
        final String specialCharacters = "<storeMe>https://localhost:8443/some/url?param1=one&param2=2</storeMe>";
        final String encoded = "%3CstoreMe%3Ehttps%3A%2F%2Flocalhost%3A8443%2Fsome%2Furl%3Fparam1%3Done%26param2%3D2%3C%2FstoreMe%3E";
        storeParams.put("value", specialCharacters);
        final String cacheKey = storeParams.get("cacheKey");
        final GenericHttpResponse storeResponse = requestSession("store", storeParams);
        assertEquals(200, storeResponse.getStatus());
        final String storeResponseBody = new String(IOUtils.slurpStream(storeResponse.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, true);

        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<foundxmlns=\"http://ns.l7tech.com/2012/11/otk-session\"><value>" + encoded + "</value><location>cache</location></found>",
                StringUtils.deleteWhitespace(getResponseBody));
    }

    @Test
    @BugNumber(13304)
    public void storeAndGetSessionSpecialCharactersFromDatabase() throws Exception {
        final Map<String, String> storeParams = buildStoreSessionParams(true);
        final String specialCharacters = "<storeMe>https://localhost:8443/some/url?param1=one&param2=2</storeMe>";
        final String encoded = "%3CstoreMe%3Ehttps%3A%2F%2Flocalhost%3A8443%2Fsome%2Furl%3Fparam1%3Done%26param2%3D2%3C%2FstoreMe%3E";
        storeParams.put("value", specialCharacters);
        // cache expires in 1 second
        storeParams.put("cacheAge", "1");
        storeParams.put("dbAge", "60");
        final String cacheKey = storeParams.get("cacheKey");
        final GenericHttpResponse storeResponse = requestSession("store", storeParams);
        assertEquals(200, storeResponse.getStatus());
        final String storeResponseBody = new String(IOUtils.slurpStream(storeResponse.getInputStream()));
        assertEquals("Key " + cacheKey + " added to session.", storeResponseBody);

        // sleep for 3 seconds
        Thread.sleep(3 * 1000);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, true);
        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<foundxmlns=\"http://ns.l7tech.com/2012/11/otk-session\"><value>" + encoded + "</value><location>database</location></found>",
                StringUtils.deleteWhitespace(getResponseBody));
    }

    @Test
    @BugNumber(13435)
    public void deleteSession() throws Exception {
        final Map<String, String> storeParams = buildStoreSessionParams(true);
        final String cacheKey = storeParams.get("cacheKey");
        final GenericHttpResponse storeResponse = requestSession("store", storeParams);
        assertEquals(200, storeResponse.getStatus());

        final Map<String, String> deleteParams = new HashMap<String, String>();
        deleteParams.put("cacheKey", cacheKey);
        deleteParams.put("cacheId", "OAuthToolkitIntegrationTest");
        final GenericHttpResponse deleteResponse = requestSession("delete", deleteParams);
        assertEquals(200, deleteResponse.getStatus());
        final String deleteResponseBody = new String(IOUtils.slurpStream(deleteResponse.getInputStream()));
        assertEquals("Session deleted.", deleteResponseBody);

        final Map<String, String> getParams = buildGetSessionParams(cacheKey, true);
        final GenericHttpResponse getResponse = requestSession("get", getParams);
        assertEquals(200, getResponse.getStatus());
        final String getResponseBody = new String(IOUtils.slurpStream(getResponse.getInputStream()));
        assertEquals("<found xmlns=\"http://ns.l7tech.com/2012/11/otk-session\" />", getResponseBody);
    }

    /**
     * Should not fail if session doesn't exist.
     */
    @Test
    @BugNumber(13435)
    public void deleteSessionDoesNotExist() throws Exception {
        final Map<String, String> deleteParams = new HashMap<String, String>();
        deleteParams.put("cacheKey", "something");
        deleteParams.put("cacheId", "OAuthToolkitIntegrationTest");
        final GenericHttpResponse deleteResponse = requestSession("delete", deleteParams);
        assertEquals(200, deleteResponse.getStatus());
        final String deleteResponseBody = new String(IOUtils.slurpStream(deleteResponse.getInputStream()));
        assertEquals("Session deleted.", deleteResponseBody);
    }

    @Test
    @BugNumber(13435)
    public void deleteSessionDefaultCacheId() throws Exception {
        final Map<String, String> deleteParams = new HashMap<String, String>();
        // do not set cacheId param
        deleteParams.put("cacheKey", "something");
        final GenericHttpResponse deleteResponse = requestSession("delete", deleteParams);
        assertEquals(200, deleteResponse.getStatus());
        final String deleteResponseBody = new String(IOUtils.slurpStream(deleteResponse.getInputStream()));
        assertEquals("Session deleted.", deleteResponseBody);
    }

    @Test
    @BugNumber(13435)
    public void deleteSessionNoCacheKey() throws Exception {
        final Map<String, String> deleteParams = new HashMap<String, String>();
        // do not set cacheKey param
        deleteParams.put("cacheId", "OAuthToolkitIntegrationTest");
        final GenericHttpResponse deleteResponse = requestSession("delete", deleteParams);
        assertEquals(400, deleteResponse.getStatus());
        final String deleteResponseBody = new String(IOUtils.slurpStream(deleteResponse.getInputStream()));
        assertEquals("Invalid request", deleteResponseBody);
    }

    private Map<String, String> buildStoreSessionParams(final boolean includeOptionalParams) {
        final Map<String, String> storeParams = new HashMap<String, String>();
        storeParams.put("cacheKey", UUID.randomUUID().toString());
        storeParams.put("value", "OAuthToolkitIntegrationTest");
        if (includeOptionalParams) {
            storeParams.put("cacheId", "OAuthToolkitIntegrationTest");
            storeParams.put("cacheAge", "60");
            storeParams.put("cacheMaxEntries", "10");
            storeParams.put("cacheMaxSize", "100000");
            storeParams.put("dbAge", "60");
        }
        return storeParams;
    }

    private Map<String, String> buildGetSessionParams(final String cacheKey, final boolean includeOptionalParams) {
        final Map<String, String> getParams = new HashMap<String, String>();
        if (cacheKey != null) {
            getParams.put("cacheKey", cacheKey);
        }
        if (includeOptionalParams) {
            getParams.put("cacheId", "OAuthToolkitIntegrationTest");
            getParams.put("cacheAge", "60");
        }
        return getParams;
    }

    private GenericHttpResponse requestSession(final String operation, final Map<String, String> parameters) throws Exception {
        final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("https://" + BASE_URL + ":8443/oauth/session/" + operation));
        params.setSslSocketFactory(getSSLSocketFactoryWithKeyManager());
        params.setContentType(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED);
        final GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        List<String[]> postParams = new ArrayList<String[]>();
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            String[] postParam = new String[2];
            postParam[0] = entry.getKey();
            postParam[1] = entry.getValue();
        }
        request.addParameters(postParams);
        return request.getResponse();
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
        List<String[]> postParams = new ArrayList<String[]>();
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            String[] postParam = new String[2];
            postParam[0] = entry.getKey();
            postParam[1] = entry.getValue();
        }
        request.addParameters(postParams);
        final GenericHttpResponse response = request.getResponse();
        assertEquals(200, response.getStatus());
        // yes there is a typo in the policy
        assertEquals("1 client_keys(s) updated", new String(IOUtils.slurpStream(response.getInputStream())));
    }

    private OAuthClient getClient(final String clientIdentity) throws Exception {
        final String responseBody = get(CLIENT_IDENT, clientIdentity, "https://" + BASE_URL + ":8443/oauth/clientstore/get");
        final Document document = XmlUtil.parse(responseBody);
        final OAuthClient client = new OAuthClient();
        client.setClientIdentity(getValue(document, "/ns:values/ns:value/ns:client_ident"));
        client.setName(getValue(document, "/ns:values/ns:value/ns:name"));
        client.setType(getValue(document, "/ns:values/ns:value/ns:type"));
        client.setDescription(getValue(document, "/ns:values/ns:value/ns:description"));
        client.setOrganization(getValue(document, "/ns:values/ns:value/ns:organization"));
        client.setRegisteredBy(getValue(document, "/ns:values/ns:value/ns:registered_by"));
        client.setCreated(getValue(document, "/ns:values/ns:value/ns:created"));
        return client;
    }

}
