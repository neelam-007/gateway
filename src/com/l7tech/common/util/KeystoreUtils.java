package com.l7tech.common.util;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.server.ServerConfig;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Knows about the location and passwords for keystores, server certificates, etc.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003
 */
public class KeystoreUtils {
    public static final String DEFAULT_PROPS_RESOURCE = "/keystore.properties";

    public static final String KSTORE_PATH_PROP_NAME = "keystoredir";
    public static final String SSL_CERT_NAME = "sslcert";
    public static final String ROOT_STORENAME = "rootcakstorename";
    public static final String ROOT_CERTNAME = "rootcacert";
    public static final String ROOT_STOREPASSWD = "rootcakspasswd";
    public static final String SSL_KSTORE_NAME = "sslkstorename";
    public static final String SSL_KSTORE_PASSWD = "sslkspasswd";
    public static final String TOMCATALIAS = "tomcat";
    private static final String KSTORE_TYPE = "keystoretype";

    public static final String PS = System.getProperty("file.separator");

    public static KeystoreUtils getInstance() {
        return SingletonHolder.singleton;
    }

    public byte[] readSSLCert() throws IOException {
        String sslCertPath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(SSL_CERT_NAME);
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
        String sslCertPath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(ROOT_CERTNAME);
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
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(ROOT_STORENAME);
    }

    public String getRootCertPath() {
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(ROOT_CERTNAME);
    }

    public String getRootKeystorePasswd() {
        return getProps().getProperty(ROOT_STOREPASSWD);
    }

    public String getKeyStoreType() {
        return getProps().getProperty(KSTORE_TYPE);
    }


    public KeyStore getSSLKeyStore() throws KeyStoreException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream fis = null;
            String sslkeystorepath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(SSL_KSTORE_NAME);
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

    /**
     * Load the <code>KeyStore</code> from a given keystore file that is protected with a specified
     * password.
     * @param path     the keystore file path
     * @param password the keystore password
     * @return
     * @throws KeyStoreException        if the requested store (default type) is not available
     * @throws IOException              if there is an I/O error or file does not exist
     * @throws NoSuchAlgorithmException if the keystore integrity check algorithm is not available
     * @throws CertificateException     if there is an certificate while loading the certificate(s)
     */
    public static KeyStore getKeyStore(String path, char[] password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = new FileInputStream(path);
        try {
            keyStore.load(fis, password);
        } finally {
            fis.close();
        }
        return keyStore;
    }


    /**
     * Returns the signer info containing the private key, cert and the public
     * key from this keystore.
     * 
     * @return the <code>SignerInfo</code> instance
     */
    public SignerInfo getSignerInfo() throws IOException {
        byte[] buf = KeystoreUtils.getInstance().readSSLCert();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        X509Certificate[] certChain;
        try {
            Collection certChainC = CertificateFactory.getInstance("X.509").generateCertificates(bais);
            certChain = (X509Certificate[])new ArrayList(certChainC).toArray( new X509Certificate[0] );
        } catch (CertificateException e) {
            String msg = "cannot generate cert from cert file";
            logger.severe(msg);
            IOException ioe = new IOException(msg);
            ioe.initCause(e);
            throw ioe;
        }

        PrivateKey pkey = null;
        try {
            pkey = KeystoreUtils.getInstance().getSSLPrivateKey();
        } catch (KeyStoreException e) {
            String msg = "cannot get ssl private key";
            logger.severe(msg);
            IOException ioe = new IOException(msg);
            ioe.initCause(e);
            throw ioe;
        }
        return new SignerInfo(pkey, certChain);
    }

    private static class SingletonHolder {
        private static KeystoreUtils singleton = new KeystoreUtils();
    }

    protected KeystoreUtils() {
    }

    private synchronized Properties getProps() {
        if (props == null) {
            String propsPath = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_KEYSTORE);
            InputStream inputStream = null;
            if (propsPath != null && propsPath.length() > 0) {
                File f = new File(propsPath);
                if (f.exists()) {
                    try {
                        inputStream = new FileInputStream(propsPath);
                        logger.info("Loading keystore properties from " + propsPath);
                    } catch (FileNotFoundException fnfe) {
                    }
                }
                if (inputStream == null) logger.warning("Keystore properties file " + inputStream + " could not be found, using default properties");
            }

            if (inputStream == null) {
                inputStream = getClass().getResourceAsStream(DEFAULT_PROPS_RESOURCE);
                logger.info("Loading keystore properties as resource");
            }

            props = new Properties();
            try {
                props.load(inputStream);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot load keystore properties", e);
                throw new RuntimeException(e);
            }
        }
        return props;
    }

    private Properties props = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
