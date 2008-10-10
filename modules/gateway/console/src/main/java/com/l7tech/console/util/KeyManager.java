package com.l7tech.console.util;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.spring.remoting.http.SecureHttpClient;

import javax.net.ssl.X509KeyManager;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

/**
 * Key Manager that will be set into the SecureHTTPClient object which will be used to create the SSL connection.
 *
 * User: dlee
 * Date: Jul 23, 2008
 */
public class KeyManager {
    private static final Logger logger = Logger.getLogger(KeyManager.class.getName());
    private KeyStore keyStore;
    private X509Certificate[] cert;
    private X509Certificate choice;
    private PrivateKey privateKey;
    private final static String TRUST_PASSWORD = "password";

    public KeyManager() {
        SecureHttpClient.setKeyManager(buildKeyManager());
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Sets the certificate that will be used as client-certificate
     *
     * @param cert  The certificate to be used
     */
    public void selectedCert(X509Certificate cert) {
        choice = cert;
    }

    public X509Certificate getSelectedCert() {
        return choice;
    }

    /**
     * Builds the key manager to be used in Secure HTTP client.
     *
     * @return  X509KeyManager
     */
    private X509KeyManager buildKeyManager() {
        return new X509KeyManager() {
                public String[] getClientAliases(String string, Principal[] principals) {
                    return getClientAlias();//new String[] { "client" };
                }

                public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                    return "clientCert.";
                }

                public String[] getServerAliases(String string, Principal[] principals) {
                    return new String[0];
                }

                public String chooseServerAlias(String string, Principal[] principals, Socket socket) {
                    return null;
                }

                public X509Certificate[] getCertificateChain(String string) {
                    populateCertAndPrivateKeyValues();
                    return cert;
                }

                public PrivateKey getPrivateKey(String string) {
                    populateCertAndPrivateKeyValues();
                    return privateKey;
                }
            };
        }

    /**
     * Populate the certificate and certificate values based on the list from the trust store.
     */
    private void populateCertAndPrivateKeyValues() {
        try {
            final List<String> aliases = Collections.list(keyStore.aliases());
            for (String alias : aliases) {
                X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
                //System.out.println("IsCertificateEntry=" + keyStore.isCertificateEntry(alias) + " IsKeyEntry=" + keyStore.isKeyEntry(alias) + " for " + alias);
                if (certificate != null && CertUtils.certsAreEqual(choice, certificate)) {
                    cert = new X509Certificate[] {certificate } ;//keyStore.getCertificateChain(alias);//new X509Certificate[]{(X509Certificate) keyStore.getCertificate(alias)};
                    privateKey = (PrivateKey) keyStore.getKey(alias, TRUST_PASSWORD.toCharArray());
                }
            }
        } catch (Exception e) {
            logger.finest("Failed to retrieve private key and/or certificate");
        }
    }

    private String[] getClientAlias() {
        String[] aliases = new String[0];
        try {
            final List<String> list = Collections.list(keyStore.aliases());
            aliases = new String[list.size()];
            for (int i=0; i < list.size(); i++) {
                aliases[i] = list.get(i);
            }

        } catch (Exception e) {
            logger.finest("Failed to retireve client aliases.");
        }
        return aliases;
    }
}
