package com.l7tech.common.util;

import com.l7tech.logging.LogManager;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.FileUtils;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 10:16:08 AM
 *
 * Knows about the location and passwords for keystores, server certificates, etc
 */
public class KeystoreUtils {
    public static final String PROPS_PATH = "/keystore.properties";

    public static final String KSTORE_PATH_PROP_NAME = "keystoredir";
    public static final String SSL_CERT_NAME = "sslcert";
    public static final String ROOT_STORENAME = "rootcakstorename";
    public static final String ROOT_CERTNAME = "rootcacert";
    public static final String ROOT_STOREPASSWD = "rootcakspasswd";
    public static final String SSL_KSTORE_NAME = "sslkstorename";
    public static final String SSL_KSTORE_PASSWD = "sslkspasswd";
    public static final String TOMCATALIAS = "tomcat";

    public static KeystoreUtils getInstance() {
        return SingletonHolder.singleton;
    }

    public byte[] readSSLCert() throws IOException {
        String sslCertPath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + "/" + getProps().getProperty(SSL_CERT_NAME);
        InputStream certStream = new FileInputStream(sslCertPath);
        byte[] cert;
        try {
            cert = HexUtils.slurpStream(certStream, 16384);
        } finally {
            certStream.close();
        }
        return cert;
    }

    public byte[] readRootCert() throws IOException {
        String sslCertPath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + "/" + getProps().getProperty(ROOT_CERTNAME);
        InputStream certStream = new FileInputStream(sslCertPath);
        byte[] cert;
        try {
            cert = HexUtils.slurpStream(certStream, 16384);
        } finally {
            certStream.close();
        }
        return cert;
    }

    public String getRootKeystorePath() {
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + "/" + getProps().getProperty(ROOT_STORENAME);
    }

    public String getRootCertPath() {
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + "/" + getProps().getProperty(ROOT_CERTNAME);
    }

    public String getRootKeystorePasswd() {
        return getProps().getProperty(ROOT_STOREPASSWD);
    }

    public KeyStore getSSLKeyStore() throws KeyStoreException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream fis = null;
            String sslkeystorepath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + "/" + getProps().getProperty(SSL_KSTORE_NAME);
            String sslkeystorepassword = getProps().getProperty(SSL_KSTORE_PASSWD);
            fis = FileUtils.loadFileSafely(sslkeystorepath);
            keyStore.load(fis, sslkeystorepassword.toCharArray());
            fis.close();
            return keyStore;
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "error loading keystore", e);
            throw new KeyStoreException(e.getMessage());
        } catch (CertificateException e) {
            logger.log(Level.SEVERE, "error loading keystore", e);
            throw new KeyStoreException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "error loading keystore", e);
            throw new KeyStoreException(e.getMessage());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error loading keystore", e);
            throw new KeyStoreException(e.getMessage());
        }
    }

    public PrivateKey getSSLPrivateKey() throws KeyStoreException {
        KeyStore keystore = getSSLKeyStore();
        String sslkeystorepassword = getProps().getProperty(SSL_KSTORE_PASSWD);
        PrivateKey output = null;
        try {
            output = (PrivateKey)keystore.getKey(TOMCATALIAS, sslkeystorepassword.toCharArray());
        } catch (KeyStoreException e) {
            logger.log(Level.SEVERE, "error getting key", e);
            throw new KeyStoreException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "error getting key", e);
            throw new KeyStoreException(e.getMessage());
        } catch (UnrecoverableKeyException e) {
            logger.log(Level.SEVERE, "error getting key", e);
            throw new KeyStoreException(e.getMessage());
        }
        return output;
    }

    private static class SingletonHolder {
        private static KeystoreUtils singleton = new KeystoreUtils();
    }

    protected KeystoreUtils() {
        logger = LogManager.getInstance().getSystemLogger();
    }

    private synchronized Properties getProps() {
        if (props == null) {
            InputStream inputStream = getClass().getResourceAsStream(PROPS_PATH);
            props = new Properties();
            try {
                props.load(inputStream);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "cannot load props", e);
                throw new RuntimeException(e);
            }
        }
        return props;
    }

    private Properties props = null;
    private Logger logger = null;
}
