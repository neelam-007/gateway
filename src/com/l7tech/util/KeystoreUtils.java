package com.l7tech.util;

import com.l7tech.logging.LogManager;
import com.l7tech.common.util.HexUtils;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;

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

    public String getRootKeystorePath() {
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + "/" + getProps().getProperty(ROOT_STORENAME);
    }

    public String getRootCertPath() {
        return getProps().getProperty(KSTORE_PATH_PROP_NAME) + "/" + getProps().getProperty(ROOT_CERTNAME);
    }

    public String getRootKeystorePasswd() {
        return getProps().getProperty(ROOT_STOREPASSWD);
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
