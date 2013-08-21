package com.l7tech.server.util;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.TestSSLSocketFactory;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import com.l7tech.server.identity.cert.TrustedCertServicesImpl;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.server.security.cert.TestCertValidationProcessor;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static junit.framework.Assert.assertEquals;

public class HttpClientFactoryTest {

    private HttpClientFactory httpClientFactory;

    @Before
    public void setUp() throws Exception {

        KeyStore keyStore = TestDocuments.getMockSSLServerKeyStore();

        final String alias = keyStore.aliases().nextElement();
        final PrivateKey key = (PrivateKey)keyStore.getKey(alias, "7layer]".toCharArray());
        final X509Certificate[] chain = TestDocuments.toX509Certificate(keyStore.getCertificateChain(alias));

        final DefaultKey defaultKey = new TestDefaultKey(new SsgKeyEntry( new Goid(0,-1), "alias", chain, key));

        TestTrustedCertManager trustedCertManager = new TestTrustedCertManager(defaultKey);
        TrustedCertServicesImpl trustedCertServices = new TrustedCertServicesImpl(trustedCertManager);
        TestCertValidationProcessor certValidationProcessor = new TestCertValidationProcessor();
        final SslClientTrustManager sslClientTrustManager = new SslClientTrustManager(trustedCertServices, certValidationProcessor, CertValidationProcessor.Facility.valueOf("ROUTING"));

        httpClientFactory = new HttpClientFactory(defaultKey, sslClientTrustManager, new PermissiveHostnameVerifier());

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreateHttpsClient() throws Exception {

        //Setup the MockWebServer
        MockWebServer server = new MockWebServer();
        server.useHttps(new TestSSLSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("test"));
        server.play();

        GenericHttpClient httpClient = httpClientFactory.createHttpClient();

        HttpMethod method = HttpMethod.POST;
        GenericHttpRequestParams params = new GenericHttpRequestParams(server.getUrl("/"));
        GenericHttpRequest request = httpClient.createRequest(method, params);
        assertEquals(200, request.getResponse().getStatus());

    }

    @Test
    public void testCreateHttpClient() throws Exception {

        //Setup the MockWebServer
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("test"));
        server.play();

        GenericHttpClient httpClient = httpClientFactory.createHttpClient();

        HttpMethod method = HttpMethod.POST;
        GenericHttpRequestParams params = new GenericHttpRequestParams(server.getUrl("/"));
        GenericHttpRequest request = httpClient.createRequest(method, params);
        assertEquals(200, request.getResponse().getStatus());

    }
}
