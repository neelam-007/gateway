package com.l7tech.server.tomcat;

import com.l7tech.common.security.SingleCertX509KeyManager;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.apache.tomcat.util.net.jsse.JSSESocketFactory;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Extends Tomcat's JSSESocketFactory with the ability to get the private key from an SsgKeyStoreManager instance.
 */
public class SsgJSSESocketFactory extends JSSESocketFactory {
    protected static final Logger logger = Logger.getLogger(SsgJSSESocketFactory.class.getName());

    public static final String ATTR_CIPHERNAMES = "ciphers"; // comma separated list of enabled ciphers, ie TLS_RSA_WITH_AES_128_CBC_SHA
    public static final String ATTR_KEYSTOREOID = "keystoreOid"; // identifies a keystore available from SsgKeyStoreManager instead of one from disk
    public static final String ATTR_KEYALIAS = "keyAlias"; // alias of private key within the keystore
    public static final String ATTR_SSGKEYSTOREMANAGER = "ssgKeyStoreManager"; // a reference to the ssgKeyStoreManager bean since we don't have
                                                                               // access to the application context because of how we are instantiated

    private static final String TRUSTSTORE_PASS = "changeit";
    private static KeyStore emptyKeyStore = null;

    public SsgJSSESocketFactory() {
        setAttribute("truststorePass", TRUSTSTORE_PASS);
        setAttribute("truststoreType", "JKS");
        setAttribute("truststoreAlgorithm", "AXPK");
    }

    private Long getKeystoreOid() {
        Object value = attributes.get(ATTR_KEYSTOREOID);
        if (value instanceof String) {
            String s = (String)value;
            return Long.parseLong(s);
        } else if (value instanceof Long) {
            return (Long)value;
        } else {
            return null;
        }
    }

    private SsgKeyStoreManager getSsgKeyStoreManager() {
        Object value = attributes.get(ATTR_SSGKEYSTOREMANAGER);
        return value instanceof SsgKeyStoreManager ? (SsgKeyStoreManager)value : null;
    }

    protected KeyManager[] getKeyManagers(String keystoreType, String algorithm, String keyAlias) throws Exception {
        // If we have a keystore OID and an ssgKeyStoreManager,
        // get the key from the ssgKeyStoreManager instead of using the usual procedure
        Long keystoreOid = getKeystoreOid();
        if (keystoreOid != null) {
            SsgKeyStoreManager ksm = getSsgKeyStoreManager();
            if (ksm == null)
                throw new IllegalStateException("Unable to create SSL socket -- a keystoreOid was specified, but no SsgKeyStoreManager instance was provided");
            SsgKeyEntry keyEntry = ksm.lookupKeyByKeyAlias(keyAlias, keystoreOid);
            X509Certificate[] certChain = keyEntry.getCertificateChain();
            PrivateKey privateKey = keyEntry.getPrivateKey();
            return new KeyManager[] { new SingleCertX509KeyManager(certChain, privateKey) };
        }
        return super.getKeyManagers(keystoreType, algorithm, keyAlias);
    }


    protected synchronized KeyStore getTrustStore(String keystoreType) throws IOException {
        if (emptyKeyStore != null)
            return emptyKeyStore;

        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, TRUSTSTORE_PASS.toCharArray());
            return emptyKeyStore = ks;
        } catch (KeyStoreException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (CertificateException e) {
            throw new IOException(e);
        }
    }

    public Socket acceptSocket(ServerSocket socket) throws IOException {
        return SsgServerSocketFactory.wrapSocket(super.acceptSocket(socket));
    }
}
