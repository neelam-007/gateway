package com.l7tech.proxy;

import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.PolicyManagerStub;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.SsgManagerStub;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.ssl.ClientProxyTrustManager;
import com.l7tech.client.ClientProxy;

import java.io.Closeable;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Encapsulates an SsgFaker and a test ClientProxy instance.
 */
public class BridgeTestHarness implements Closeable {
    private static final Logger log = Logger.getLogger(BridgeWsdlProxyTest.class.getName());

    final String ssg0ProxyEndpoint = "ssg0";
    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_THREADS = 4;
    private static final int MAX_THREADS = 20;

    SsgFaker ssgFaker;
    ClientProxy clientProxy;
    SsgManagerStub ssgManager;
    PolicyManagerStub policyManager = new PolicyManagerStub();
    String ssgUrl;
    String proxyUrl;
    Ssg ssgFake;

    public void reset() throws Exception {
        destroyFaker();
        destroyProxy();

        // Start the fake SSG
        ssgFaker = new SsgFaker();
        ssgUrl = ssgFaker.start();

        // Configure the client proxy
        ssgManager = new SsgManagerStub();
        ssgManager.clear();
        ssgFake = ssgManager.createSsg();
        ssgFake.setLocalEndpoint(ssg0ProxyEndpoint);
        ssgFake.setSsgAddress(new URL(ssgUrl).getHost());
        ssgFake.setSsgPort(new URL(ssgUrl).getPort());
        ssgFake.setSslPort(new URL(ssgFaker.getSslUrl()).getPort());
        ssgManager.add(ssgFake);

        // Make a do-nothing PolicyManager
        policyManager = new PolicyManagerStub();
        policyManager.setPolicy(new Policy(new TrueAssertion(), "testpolicy"));
        ssgFake.getRuntime().setPolicyManager(policyManager);
        MessageProcessor messageProcessor = new MessageProcessor();

        // Start the client proxy
        clientProxy = new ClientProxy(ssgManager, messageProcessor, DEFAULT_PORT, MIN_THREADS, MAX_THREADS);

        // Turn off server cert verification for the test
        ssgFake.getRuntime().setTrustManager(new ClientProxyTrustManager() {
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }
        });

        proxyUrl = clientProxy.start().toString();
    }

    private void destroyFaker() {
        if (ssgFaker != null) {
            ssgFaker.destroy();
            ssgFaker = null;
        }
    }

    private void destroyProxy() {
        if (clientProxy != null)
            clientProxy.destroy();
        clientProxy = null;
    }

    public void close() {
        try {
            destroyFaker();
        } finally {
            destroyProxy();
        }
    }
}
