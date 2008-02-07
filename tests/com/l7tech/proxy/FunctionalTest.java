package com.l7tech.proxy;

import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Fire up a Client Proxy instance against a fake SSG and try sending messages through it.
 */
public class FunctionalTest {
    private static final Logger log = Logger.getLogger(FunctionalTest.class.getName());
    private static final String pingPrefix = "p";

    private BridgeTestHarness bt;


    /**
     * Starts up the SSG Faker and the Client Proxy.
     */
    @Before
    public void setUp() throws Exception {
        ResourceUtils.closeQuietly(bt);
        bt = new BridgeTestHarness();
        bt.reset();
    }

    /**
     * Shuts down the SSG Faker and the Client Proxy.
     */
    @After
    public void tearDown() {
        if (bt != null) bt.close();
        bt = null;
    }

    /*
     * Bounce a message off of the echo server, going through the client proxy.            kk
     */
    @Test
    public void testSimplePing() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(new Policy(new TrueAssertion(), "testpolicy"));
        bt.ssgFake.setSsgFile("/soap/ssg");
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);

        sendPing(payload, reqEnvelope, null, null);
    }

    private void sendPing(String payload, Document reqEnvelope, String chainUser, String chainPass) throws SAXException, IOException, InvalidDocumentFormatException {
        Document responseEnvelope = sendXml(reqEnvelope, chainUser, chainPass);

        log.info("Client:  I Sent: " + XmlUtil.nodeToFormattedString(reqEnvelope));
        log.info("Client:  I Got back: " + XmlUtil.nodeToFormattedString(responseEnvelope));

        Element respPing = SoapUtil.getPayloadElement(responseEnvelope);
        Element respPayloadEl = XmlUtil.findFirstChildElement(respPing);
        String respText = XmlUtil.getTextValue(respPayloadEl);
        assertEquals(payload, respText);
    }

    private Document sendXml(Document reqEnvelope, String chainUser, String chainPass) throws MalformedURLException, GenericHttpException, SAXException {
        SimpleHttpClient httpClient = new SimpleHttpClient(new UrlConnectionHttpClient());
        URL url = new URL(bt.proxyUrl + bt.ssg0ProxyEndpoint);
        final GenericHttpRequestParams params = new GenericHttpRequestParams(url);
        params.setPasswordAuthentication(chainUser == null || chainPass == null ? null : new PasswordAuthentication(chainUser, chainPass.toCharArray()));
        SimpleHttpClient.SimpleXmlResponse response = httpClient.postXml(params, reqEnvelope);
        return response.getDocument();
    }

    @Test
    public void testBasicAuthPing() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(new Policy(new HttpBasic(), "testpolicy"));
        URL url = new URL(bt.ssgUrl);
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);
        bt.ssgFake.setSsgAddress(url.getHost());
        bt.ssgFake.setSsgPort(url.getPort());
        bt.ssgFake.setSsgFile("/soap/ssg/basicauth");
        bt.ssgFake.setUsername("testuser");
        bt.ssgFake.getRuntime().setCachedPassword("testpassword".toCharArray());

        sendPing(payload, reqEnvelope, null, null);
    }

    @Test
    public void testSsgFault() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(null);
        URL url = new URL(bt.ssgUrl);
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);
        bt.ssgFake.setSsgAddress(url.getHost());
        bt.ssgFake.setSsgPort(url.getPort());
        bt.ssgFake.setSsgFile("/soap/ssg/throwfault");

        Document resp = sendXml(reqEnvelope, null, null);
        assertTrue(SoapFaultUtils.gatherSoapFaultDetail(resp) != null);
    }

    @Test
    public void testChainCredentials() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(new Policy(new HttpBasic(), "testpolicy"));
        URL url = new URL(bt.ssgUrl);
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);
        bt.ssgFake.setChainCredentialsFromClient(true);
        bt.ssgFake.setSsgAddress(url.getHost());
        bt.ssgFake.setSsgPort(url.getPort());
        bt.ssgFake.setSsgFile("/soap/ssg/basicauth");
        bt.ssgFake.setUsername("testuser");
        bt.ssgFake.getRuntime().setCachedPassword("testpassword".toCharArray());

        sendPing(payload, reqEnvelope, "bob", "asdfasdf");

        PasswordAuthentication pw = bt.ssgFaker.getGotCreds();
        assertNotNull(pw);
        assertEquals(pw.getUserName(), "bob");
        assertArrayEquals(pw.getPassword(), "asdfasdf".toCharArray());
    }

    @Test
    @Ignore("Reenable this test case when the SSB is able to use chained credentials to unlock private key during SSL handshake")
    public void testUnlockPrivateKeyWithChainCredentials() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(new Policy(new SslAssertion(true), "testpolicy"));
        URL url = new URL(bt.ssgUrl);
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);
        bt.ssgFake.setChainCredentialsFromClient(true);
        bt.ssgFake.setSsgAddress(url.getHost());
        bt.ssgFake.setSsgPort(url.getPort());
        bt.ssgFake.setSsgFile("/soap/ssg");
        // These passwords are right, it's just that the SSB can't use them correctly
        final SsgKeyStoreManagerStub ksm = new SsgKeyStoreManagerStub(bt.ssgFake, TestDocuments.getEttkClientCertificate(), TestDocuments.getEttkClientPrivateKey(), "keypass");
        bt.ssgFake.getRuntime().setSsgKeyStoreManager(ksm);

        // These passwords are right, it's just that the SSB can't use them correctly
        sendPing(payload, reqEnvelope, "bob", "keypass");
    }


    private static class SsgKeyStoreManagerStub extends SsgKeyStoreManager {
        private final Ssg ssg;
        private X509Certificate clientCert;
        private X509Certificate serverCert;
        private PrivateKey key;
        private char[] passphrase;
        private boolean unlocked;

        private SsgKeyStoreManagerStub(Ssg ssg, X509Certificate clientCert, PrivateKey key, String passphrase) {
            this.ssg = ssg;
            this.clientCert = clientCert;
            this.key = key;
            this.passphrase = passphrase.toCharArray();
        }

        public boolean isClientCertUnlocked() throws KeyStoreCorruptException {
            return unlocked;
        }

        public void deleteClientCert() throws IOException, KeyStoreException, KeyStoreCorruptException {
            clientCert = null;
            key = null;
            passphrase = null;
            unlocked = false;
        }

        public boolean isPasswordWorkedForPrivateKey() {
            return unlocked;
        }

        public boolean deleteStores() {
            try {
                deleteClientCert();
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e); // Can't happen
            }
        }

        public void saveSsgCertificate(X509Certificate cert)
        throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            serverCert = cert;
        }

        public void saveClientCertificate(PrivateKey privateKey, X509Certificate cert, char[] privateKeyPassword)
        throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            this.key = privateKey;
            this.clientCert = cert;
            this.passphrase = privateKeyPassword;
        }

        public void obtainClientCertificate(PasswordAuthentication credentials) throws BadCredentialsException,
            GeneralSecurityException, KeyStoreCorruptException, CertificateAlreadyIssuedException, IOException,
            ServerFeatureUnavailableException {
            throw new UnsupportedOperationException();
        }

        public String lookupClientCertUsername() {
            return CertUtils.getCn(clientCert);
        }

        protected X509Certificate getServerCert() throws KeyStoreCorruptException {
            return serverCert;
        }

        protected X509Certificate getClientCert() throws KeyStoreCorruptException {
            return clientCert;
        }

        public PrivateKey getClientCertPrivateKey(PasswordAuthentication passwordAuthentication) throws
            NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException,
            HttpChallengeRequiredException {
            if (passwordAuthentication == null) {
                passwordAuthentication = ssg.getRuntime().getCredentialManager().getCredentialsWithReasonHint(ssg, CredentialManager.ReasonHint.PRIVATE_KEY, false, false);
            }
            if (!Arrays.equals(passwordAuthentication.getPassword(), passphrase)) throw new BadCredentialsException();
            unlocked = true;
            return key;
        }

        protected boolean isClientCertAvailabile() throws KeyStoreCorruptException {
            return clientCert != null;
        }

        protected KeyStore getKeyStore(char[] password) throws KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        protected KeyStore getTrustStore() throws KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        public void importServerCertificate(File file)
        throws IOException, CertificateException, KeyStoreCorruptException, KeyStoreException {
            throw new UnsupportedOperationException();
        }

        public void importClientCertificate(File certFile, char[] pass, AliasPicker aliasPicker,
                                            char[] ssgPassword) throws IOException, GeneralSecurityException,
            KeyStoreCorruptException, AliasNotFoundException {
            throw new UnsupportedOperationException();
        }

    }
}
