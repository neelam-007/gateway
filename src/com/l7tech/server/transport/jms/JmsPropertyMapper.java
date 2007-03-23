package com.l7tech.server.transport.jms;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.server.KeystoreUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maps JMS initial context property values.
 *
 * <p>
 * The mapper is run against the Properties obtained from a JMSConnection. This
 * enables integration with the Gateways trust store and private keys.
 * </p>
 *
 * @author Steve Jones
 */
public class JmsPropertyMapper {

    //- PUBLIC

    public static final String PREFIX = "com.l7tech.server.jms.prop";
    public static final String VALUE_KEYSTORE = PREFIX + ".keystore";
    public static final String VALUE_KEYSTORE_BYTES = PREFIX + ".keystore.bytes";
    public static final String VALUE_KEYSTORE_PASSWORD = PREFIX + ".keystore.password";
    public static final String VALUE_TRUSTED_LIST = PREFIX + ".trustedcert.listx509"; // actually a Vector
    public static final String VALUE_BOOLEAN_TRUE = PREFIX + ".boolean.true";
    public static final String VALUE_BOOLEAN_FALSE = PREFIX + ".boolean.false";

    public static final String PROP_KEYSTORE_ALIAS = PREFIX + ".keystore.alias";
    public static final String PROP_CUSTOMIZER = PREFIX + ".customizer.class";

    /**
     * Create a new JmsPropertyMapper.
     *
     * @param keystoreUtils The keystoreUtils to use
     * @param trustedCertManager The source for trusted certificates
     */
    public JmsPropertyMapper(KeystoreUtils keystoreUtils,
                             TrustedCertManager trustedCertManager) {
        this.keystoreUtils = keystoreUtils;
        this.trustedCertManager = trustedCertManager;
        this.random = new Random(System.currentTimeMillis());
    }

    /**
     * Substitute values in the given properties.
     *
     * @param properties The properties to modify.
     * @return
     */
    public boolean substitutePropertyValues(Map properties) {
        boolean modified = false;
        KeyStoreInfo ksi = null;

        try {
            for (Map.Entry property : (Set<Map.Entry>) properties.entrySet()) {
                Object value = property.getValue();
                if (VALUE_KEYSTORE_PASSWORD.equals(value)) {
                    if (ksi == null)
                        ksi = buildKeystoreInfo(properties);
                    property.setValue(ksi.password);
                }
                else if (VALUE_KEYSTORE_BYTES.equals(value)) {
                    if (ksi == null)
                        ksi = buildKeystoreInfo(properties);
                    property.setValue(ksi.keystoreBytes);
                }
                else if (VALUE_KEYSTORE.equals(value)) {
                    if (ksi == null)
                        ksi = buildKeystoreInfo(properties);
                    property.setValue(ksi.keystore);
                }
                else if (VALUE_TRUSTED_LIST.equals(value)) {
                    property.setValue(buildTrustedList());
                }
                else if (VALUE_BOOLEAN_TRUE.equals(value)) {
                    property.setValue(Boolean.TRUE);
                }
                else if (VALUE_BOOLEAN_FALSE.equals(value)) {
                    property.setValue(Boolean.FALSE);
                }
            }
        }
        catch(KeyStoreException kse) {
            logger.log(Level.WARNING, "Error processing key store.", kse);
        }

        if (logger.isLoggable(Level.FINE)) {
            StringBuffer propBuffer = new StringBuffer(512);

            for (Map.Entry property : (Set<Map.Entry>) properties.entrySet()) {
                Object key = property.getKey();
                Object value = property.getValue();
                
                propBuffer.append(key);
                propBuffer.append('=');
                propBuffer.append(value);
                propBuffer.append('\n');
            }

            logger.log(Level.FINE, "Mapped JMS properties:\n" + propBuffer.toString());
        }

        return modified;
    }


    //- PRIVATE

    private static final Logger logger = Logger.getLogger(JmsPropertyMapper.class.getName());

    private final KeystoreUtils keystoreUtils;
    private final TrustedCertManager trustedCertManager;
    private final Random random;

    /**
     * KeyStore data / metadata
     */
    private static final class KeyStoreInfo {
        private String password;
        private KeyStore keystore;
        private byte[] keystoreBytes;
    }

    /**
     * Generate a password for use with an in-memory keystore.
     *
     * @return A generated password.
     */
    private String buildPassword() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);        
        return HexUtils.encodeBase64(randomBytes);
    }

    /**
     * Build a list of trusted certificates.
     *
     * <p>NOTE: TIBCO requires a Vector</p>
     *
     * <p>We only return certificates trusted for signing server SSL
     * certificates. This is because any self signed certificate MUST NOT
     * be trusted as issuer certificates.</p>
     *
     * @return The List of trusted X509Certificates.
     */
    private List buildTrustedList() {
        Vector trustedCertList = new Vector(); // TIBCO requires vector

        try {
            Collection<TrustedCert> trustedCerts = trustedCertManager.findAll();
            for (TrustedCert trustedCert : trustedCerts) {
                if (trustedCert.isTrustedForSigningServerCerts()) {
                    trustedCertList.add(trustedCert.getCertificate());
                }
            }
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Error building trusted certificates.");
        }

        return trustedCertList;
    }

    /**
     * Build a KeyStore
     *
     * @param properties Used to set alias (optional), etc
     * @return The newly populated KeyStoreInfo
     * @throws KeyStoreException If an error occurs
     */
    private KeyStoreInfo buildKeystoreInfo(Map properties) throws KeyStoreException {
        try {
            KeyStoreInfo keyStoreInfo = new KeyStoreInfo();
            keyStoreInfo.password = buildPassword();

            // create empty keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            // populate with identity
            X509Certificate clientCert = keystoreUtils.getSslCert();
            PrivateKey clientKey = keystoreUtils.getSSLPrivateKey();
            String alias = (String) properties.get(PROP_KEYSTORE_ALIAS);
            if (alias == null)
                alias = "jms";
            keyStore.setKeyEntry(alias, clientKey, keyStoreInfo.password.toCharArray(), new Certificate[]{clientCert});

            // save
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyStore.store(baos, keyStoreInfo.password.toCharArray());
            keyStoreInfo.keystoreBytes = baos.toByteArray();
            keyStoreInfo.keystore = keyStore;

            return keyStoreInfo;
        }
        catch (IOException ioe) {
            throw new KeyStoreException("Error creating keystore", ioe);
        }
        catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreException("Error creating keystore", nsae);
        }
        catch (CertificateException ce) {
            throw new KeyStoreException("Error creating keystore", ce);
        }
    }
}
