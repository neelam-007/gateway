package com.l7tech.server;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreInfo;
import com.l7tech.common.util.ResourceUtils;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
    public static final String KSTORE_TYPE = "keystoretype";

    public static final String PS = System.getProperty("file.separator");
    private final ServerConfig serverConfig;

    public KeystoreUtils(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public byte[] readSSLCert() throws IOException {
        String sslCertPath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(SSL_CERT_NAME);
        InputStream certStream = null;
        byte[] cert;
        try {
            certStream = new FileInputStream(sslCertPath);
            cert = HexUtils.slurpStreamLocalBuffer(certStream);
        } finally {
            ResourceUtils.closeQuietly(certStream);
        }
        return cert;
    }

    public byte[] readRootCert() throws IOException {
        String sslCertPath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(ROOT_CERTNAME);
        InputStream certStream = null;
        byte[] cert;
        try {
            certStream = new FileInputStream(sslCertPath);
            cert = HexUtils.slurpStreamLocalBuffer(certStream);
        } finally {
            ResourceUtils.closeQuietly(certStream);
        }
        return cert;
    }

    public synchronized X509Certificate getRootCert() throws IOException, CertificateException {
        if (cachedRootCert == null) {
            cachedRootCert = CertUtils.decodeCert(this.readRootCert());
        }
        return cachedRootCert;
    }

    public synchronized X509Certificate getSslCert() throws IOException, CertificateException {
        if (cachedSslCert == null) {
            cachedSslCert = CertUtils.decodeCert(readSSLCert());
        }
        return cachedSslCert;
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

    public String getSslKeystorePath() {
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(SSL_KSTORE_NAME);
    }

    public String getSslCertPath() {
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(SSL_CERT_NAME);
    }

    public String getSslKeystorePasswd() {
        return getProps().getProperty(SSL_KSTORE_PASSWD);
    }

    public KeystoreInfo getSslKeystoreInfo() {
        return new KeystoreInfo(getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(SSL_KSTORE_NAME),
                                getProps().getProperty(SSL_KSTORE_PASSWD),
                                getKeyStoreType());
    }

    public String getKeyStoreType() {
        String type = getProps().getProperty(KSTORE_TYPE);
        if ( type == null || type.length() == 0 ) type = KeyStore.getDefaultType();
        return type;
    }


    public KeyStore getSSLKeyStore() throws KeyStoreException {
        FileInputStream fis = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(getKeyStoreType());
            String sslkeystorepath = getProps().getProperty(KSTORE_PATH_PROP_NAME) + PS + getProps().getProperty(SSL_KSTORE_NAME);
            String sslkeystorepassword = getProps().getProperty(SSL_KSTORE_PASSWD);
            fis = new FileInputStream(sslkeystorepath);
            keyStore.load(fis, sslkeystorepassword.toCharArray());
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
        finally {
            ResourceUtils.closeQuietly(fis);
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
     * Returns the <code>KeyManagerFactory</code> based on SSG SSL Private Key as a source of key material.
     * @return
     * @throws NoSuchAlgorithmException if the algorithm (X.509) is not available in the default provider
     * @throws UnrecoverableKeyException if the key cannot be recovered (e.g. the given password is wrong).
     * @throws KeyStoreException if keystore operaiton fails
     */
    public KeyManagerFactory getSSLKeyManagerFactory()
      throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        // uses SunX509
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(getSSLKeyStore(), getSslKeystorePasswd().toCharArray());
        return kmf;
    }

    /**
     * Load the <code>KeyStore</code> from a given keystore file that is protected with a specified
     * password.
     *
     * @param path     the keystore file path
     * @param password the keystore password
     * @param keystoreType the keystore type
     * @return
     * @throws KeyStoreException        if the requested store (default type) is not available
     * @throws IOException              if there is an I/O error or file does not exist
     * @throws NoSuchAlgorithmException if the keystore integrity check algorithm is not available
     * @throws CertificateException     if there is an certificate while loading the certificate(s)
     */
    public static KeyStore getKeyStore(String path, char[] password, String keystoreType)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            keyStore.load(fis, password);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return keyStore;
    }

    /**
     * Load the <code>KeyStore</code> from a given keystore file that is protected with a specified
     * password.
     *
     * <b>Note:</b> Assumes that the keystore is of the default type!  Do not use in Bridge or Gateway!
     *
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
        return getKeyStore(path, password, KeyStore.getDefaultType());
    }

    /**
     * Returns the Ssl signer info containing the private key, cert and the public
     * key from this keystore.
     * 
     * @return the <code>SignerInfo</code> instance
     */
    public SignerInfo getSslSignerInfo() throws IOException {
        byte[] buf = readSSLCert();
        X509Certificate[] certChain;
        try {
            certChain = CertUtils.decodeCertChain(buf);
        } catch (CertificateException e) {
            String msg = "cannot generate cert from cert file";
            logger.severe(msg);
            IOException ioe = new IOException(msg);
            ioe.initCause(e);
            throw ioe;
        }

        PrivateKey pkey = null;
        try {
            pkey = getSSLPrivateKey();
        } catch (KeyStoreException e) {
            String msg = "cannot get ssl private key";
            logger.severe(msg);
            IOException ioe = new IOException(msg);
            ioe.initCause(e);
            throw ioe;
        }
        return new SignerInfo(pkey, certChain);
    }

    private synchronized Properties getProps() {
        if (props == null) {
            String propsPath = serverConfig.getPropertyCached(ServerConfig.PARAM_KEYSTORE);
            InputStream fileInputStream = null;
            InputStream resInputStream = null;
            try {
                if (propsPath != null && propsPath.length() > 0) {
                    try {
                        fileInputStream = new FileInputStream(propsPath);
                        logger.info("Loading keystore properties from " + propsPath);
                    } catch (FileNotFoundException fnfe) {
                    }
                    if (fileInputStream == null) logger.info("Keystore properties file '" + propsPath + "' could not be found. Will try loading as resource");
                }

                if (fileInputStream == null) {
                    resInputStream = getClass().getResourceAsStream(DEFAULT_PROPS_RESOURCE);
                    if(resInputStream==null) {
                        logger.log(Level.SEVERE, "Keystore properties not found (file or resource)");
                        throw new RuntimeException("Missing keystore properties.");
                    }
                    logger.info("Loading keystore properties as resource");
                }

                props = new Properties();
                try {
                    props.load(fileInputStream!=null ? fileInputStream : resInputStream);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Cannot load keystore properties", e);
                    throw new RuntimeException(e);
                }
            }
            finally {
                ResourceUtils.closeQuietly(fileInputStream);
                ResourceUtils.closeQuietly(resInputStream);
            }
        }
        return props;
    }

    private Properties props = null;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private X509Certificate cachedRootCert;
    private X509Certificate cachedSslCert;
}
