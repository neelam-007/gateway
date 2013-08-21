package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.HexUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
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
 * @author rmak
 */
public class JmsPropertyMapper {

    //- PUBLIC

    /**
     * Create a new JmsPropertyMapper.
     *
     * @param trustedCertManager    the source for trusted certificates
     * @param defaultKey         the source for SSG SSL certificate
     */
    public JmsPropertyMapper(TrustedCertManager trustedCertManager,
                             DefaultKey defaultKey) {
        this.trustedCertManager = trustedCertManager;
        this.defaultKey = defaultKey;
        this.random = new Random(System.currentTimeMillis());
    }

    /**
     * Substitute values in the given properties.
     *
     * <p>Most stand-in values contains just a single string token. But some,
     * e.g. VALUE_KEYSTORE, VALUE_KEYSTORE_BYTES and VALUE_KEYSTORE_PASSWORD,
     * have additional parameter tokens separated by tab characters.
     *
     * @param properties The properties to modify.
     * @return true if substitution had occurred
     */
    public boolean substitutePropertyValues(Map<Object, Object> properties) {
        boolean modified = false;

        try {
            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                final String[] values = property.getValue().toString().split("\t");

                if (JmsConnection.VALUE_KEYSTORE.equals(values[0]) ||
                    JmsConnection.VALUE_KEYSTORE_BYTES.equals(values[0]) ||
                    JmsConnection.VALUE_KEYSTORE_PASSWORD.equals(values[0])) {
                    try {
                        Goid ssgKeystoreId = PersistentEntity.DEFAULT_GOID;
                        String ssgKeyAlias = null;
                        if (values.length == 1) {
                            // version 3.7 format - no SSG keystore ID and key alias specified; defaults to the SSG SSL private key and cert
                        } else if (values.length == 3) {
                            // version 4.0 format - with tab-separated SSG keystore ID and key alias
                            ssgKeystoreId = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, (values[1]));
                            ssgKeyAlias = values[2];
                        } else {
                            logger.log(Level.WARNING, "Unexpected format of substitutable property value. Expected format is \"" + values[0] + "<tab><Gateway kesytore ID><tab><Gateway key alias>\". Encountered: \"" + property.getValue() + "\"");
                        }
                        final KeyStoreInfo keyStoreInfo = buildKeystoreInfo(ssgKeystoreId, ssgKeyAlias, properties);
                        if (JmsConnection.VALUE_KEYSTORE.equals(values[0])) {
                            property.setValue(keyStoreInfo.keystore);
                        } else if (JmsConnection.VALUE_KEYSTORE_BYTES.equals(values[0])) {
                            property.setValue(keyStoreInfo.keystoreBytes);
                        } else if (JmsConnection.VALUE_KEYSTORE_PASSWORD.equals(values[0])) {
                            property.setValue(keyStoreInfo.password);
                        }
                        modified = true;
                    } catch (NumberFormatException e) {
                        logger.log(Level.WARNING, "Unable to parse Gateway keystore ID.", e);
                    }
                } else if (JmsConnection.VALUE_TRUSTED_LIST.equals(values[0])) {
                    property.setValue(buildTrustedList());
                    modified = true;
                } else if (JmsConnection.VALUE_BOOLEAN_TRUE.equals(values[0])) {
                    property.setValue(Boolean.TRUE);
                    modified = true;
                } else if (JmsConnection.VALUE_BOOLEAN_FALSE.equals(values[0])) {
                    property.setValue(Boolean.FALSE);
                    modified = true;
                }
            }
        } catch (KeyStoreException kse) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Error processing key store.: " + ExceptionUtils.getMessage(kse), ExceptionUtils.getDebugException(kse));
        }

        if (logger.isLoggable(Level.FINE)) {
            StringBuffer propBuffer = new StringBuffer(512);

            for (Map.Entry<Object, Object> property : properties.entrySet()) {
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

    private final TrustedCertManager trustedCertManager;
    private final DefaultKey defaultKey;
    private final Random random;

    /** Collection of KeyStoreInfo objects constructed.
        Map key is the String form of SSG keystore ID followed by one tab character and SSG key alias.
        Map value is the KeyStoreInfo object constructed for that key. */
    private final Map<String, KeyStoreInfo> keyStoreInfos = new HashMap<String, KeyStoreInfo>();

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
        Vector<X509Certificate> trustedCertList = new Vector<X509Certificate>(); // TIBCO requires vector

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
     * Build a KeyStore that contains a copy of a specific private key & cert from SSG's keystore.
     *
     * @param ssgKeystoreId     ID of SSG keystore to find the key
     * @param ssgKeyAlias       key alias in the specified SSG keystore
     * @param properties        used to set alias (optional), etc
     * @return The newly populated KeyStoreInfo
     * @throws KeyStoreException If an error occurs
     */
    private KeyStoreInfo buildKeystoreInfo(Goid ssgKeystoreId, String ssgKeyAlias, Map properties) throws KeyStoreException {
        final String whichKey = ssgKeystoreId + "\t" + ssgKeyAlias;
        KeyStoreInfo keyStoreInfo = keyStoreInfos.get(whichKey);
        if (keyStoreInfo == null) {
            try {
                X509Certificate[] certChain;
                PrivateKey privateKey;
                String alias;
                SsgKeyEntry keyEntry = defaultKey.lookupKeyByKeyAlias(ssgKeyAlias, ssgKeystoreId);
                certChain = keyEntry.getCertificateChain();
                privateKey = keyEntry.getPrivateKey();

                if (!"PKCS#8".equals(privateKey.getFormat())) {
                    logger.warning("Private key encoding is not PKCS#8 -- possible hardware keystore?  Use of this key with JMS provider is unlikely to work as expected");
                }

                alias = (String) properties.get(JmsConnection.PROP_KEYSTORE_ALIAS);
                if (alias == null) alias = ssgKeyAlias;
                if (alias == null) alias = "jms";

                // Creates new keystore and populates it.
                final String password = buildPassword();
                final KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), certChain);

                // Saves to a KeyStoreInfo object.
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                keyStore.store(baos, password.toCharArray());
                keyStoreInfo = new KeyStoreInfo();
                keyStoreInfo.keystoreBytes = baos.toByteArray();
                keyStoreInfo.keystore = keyStore;
                keyStoreInfo.password = password;
            } catch (IOException e) {
                throw new KeyStoreException("Error creating keystore: " + ExceptionUtils.getMessage(e), e);
            } catch (FindException e) {
                throw new KeyStoreException("Error creating keystore: " + ExceptionUtils.getMessage(e), e);
            } catch (NoSuchAlgorithmException e) {
                throw new KeyStoreException("Error creating keystore: " + ExceptionUtils.getMessage(e), e);
            } catch (CertificateException e) {
                throw new KeyStoreException("Error creating keystore: " + ExceptionUtils.getMessage(e), e);
            } catch (UnrecoverableKeyException e) {
                throw new KeyStoreException("Error creating keystore: " + ExceptionUtils.getMessage(e), e);
            } 

            keyStoreInfos.put(whichKey, keyStoreInfo);
        }

        return keyStoreInfo;
    }
}
