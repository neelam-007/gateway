package com.l7tech.server;

import com.l7tech.server.security.MasterPasswordManager;
import com.l7tech.common.TestDocuments;

import javax.net.ssl.KeyManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.io.IOException;

/**
 * 
 */
public class TestKeystoreUtils extends KeystoreUtils {

    //- PUBLIC

    public TestKeystoreUtils(ServerConfig serverConfig, MasterPasswordManager masterPasswordManager) {
        super( serverConfig, masterPasswordManager );
    }

    @Override
    public String getSslKeyStoreType() {
        return "PKCS12";
    }

    @Override
    public String getSslKeystorePasswd() {
        return "password";
    }

    @Override
    public String getSslKeystorePath() {
        return "/";
    }

    @Override
    public X509Certificate getSslCert() {
        try {
            return TestDocuments.getDotNetServerCertificate();
        } catch (Exception e) {
            throw new RuntimeException("Error getting certificate", e);
        }
    }

    @Override
    public byte[] readSSLCert() {
        try {
            return getSslCert().getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Error getting encoded certificate", e);
        }
    }

    @Override
    public PrivateKey getSSLPrivateKey() throws KeyStoreException {
        try {
            return TestDocuments.getDotNetServerPrivateKey();
        } catch (Exception e) {
            throw new RuntimeException("Error getting key", e);
        }
    }

    @Override
    public synchronized X509Certificate getRootCert() throws IOException, CertificateException {
        try {
            return TestDocuments.getDotNetServerCertificate();
        } catch (Exception e) {
            throw new RuntimeException("Error getting certificate", e);
        }
    }

    @Override
    public String getSSLAlias() {
        return "tomcat";
    }

    @Override
    public X509Certificate[] getSSLCertChain() throws KeyStoreException {
        return new X509Certificate[]{ getSslCert() };
    }

    //- PRIVATE
}
