package com.l7tech.common.security;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.server.util.SetKeys;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.X509V3CertificateGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Properties;

/**
 * Test class that generates key pair and self signed certificate
 * programatically.
 * <p/>
 * Useful for testing (no need to prepare keys/cert using keytool).
 * Uses bouncycastle specific api to generate the certificate.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Keys {
    /** static initializer that initializes jce provider */
    private static final Provider bcp = new BouncyCastleProvider();

    static {
        Security.addProvider(bcp);
    }

    /** the algorithms supported are fixed * */
    public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA1WITHRSA";

    /** keystore type */
    public static final String KEYSTORE_TYPE = KeyStore.getDefaultType();

    private KeyPair keyPair;
    private SecureRandom secureRandom;
    private boolean debug = false;
    private X509Principal principal;
    /**
     * Instantiate <code>Keys</code> (private and public key pair) with
     * default key size of 512 for a given subject.
     *
     * @param subject Distinguished Name of the subject of this certificate
     * @throws NoSuchAlgorithmException when algorithm requested is not
     *                                  available
     * @throws InvalidAlgorithmParameterException
     *                                  if invalid algorithm
     *                                  parameters requested (wrong key size value for example)
     */
    public Keys(String subject)
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        this(subject, 512);
    }

    /**
     * Instantiate private and public key pair specifying a key size.
     * 
     * @param subject Distinguished Name of the subject of this certificate
     * @param size the key size. Must be in 512-1024 range, and modulo 64.
     * @throws NoSuchAlgorithmException when algorithm requested is not
     *                                  available
     * @throws InvalidAlgorithmParameterException
     *                                  if invalid algorithm
     *                                  parameters requested (wrong key size value for example)
     */
    public Keys(String subject, int size)
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (!(size >= 512 && size <= 1024)) {
            throw new InvalidAlgorithmParameterException("invalid key size");
        }
        if (size % 64 != 0) {
            throw new InvalidAlgorithmParameterException("invalid key value (modulo 64 expected)");
        }
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyGen.initialize(size);
        keyPair = keyGen.genKeyPair();

        principal = new X509Principal(subject);
    }


    /**
     * Create a selfsigned X509Certificate for a public key
     * 
     * @param notBefore the notBefore date from the validity period of the certificate.
     * @param notAfter  the notAfter date from the validity period of the certificate.
     * @return the X509 certificate
     * @throws SignatureException  thrown on signing related error
     * @throws InvalidKeyException thrown on invalid key
     *                             * @exception IllegalArgumentException if an parameter is <b>null</b>
     */
    public X509Certificate generateSelfSignedCertificate(Date notBefore, Date notAfter)
      throws SignatureException, InvalidKeyException, IllegalArgumentException {
        if (notBefore == null || notAfter == null) {
            throw new IllegalArgumentException();
        }
        PrivateKey signKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setPublicKey(publicKey);

        certGen.setSubjectDN(principal);
        certGen.setIssuerDN(principal); // self signed

        certGen.setNotBefore(notBefore);
        certGen.setNotAfter(notAfter);


        certGen.setSignatureAlgorithm(SIGNATURE_ALGORITHM);
        certGen.setSerialNumber(new BigInteger(10, new Random()));
        if (debug) System.out.println("sign certificate");
        X509Certificate signedCertificate = certGen.generateX509Certificate(signKey, getSecureRandom());
        if (debug) System.out.println("Certificate:\n" + signedCertificate.toString());

        return signedCertificate;
    }

    /**
     * return the key and cert as a <code>SignerInfo</code> instance for
     * the given subject
     * 
     * @return the signer info instance
     */
    public SignerInfo asSignerInfo()
      throws SignatureException, InvalidKeyException {
        Date notBefore = new Date();
        Calendar cal = Calendar.getInstance();
        // clear the time part
        cal.setTime(notBefore);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        notBefore = cal.getTime();
        cal.roll(Calendar.YEAR, 1);
        Date notAfter = cal.getTime();

        X509Certificate certificate = generateSelfSignedCertificate(notBefore, notAfter);
        return new SignerInfo(keyPair.getPrivate(), new X509Certificate[] { certificate });
    }

    /**
     *  Write the keys to the keystore specified, with the alias name, protected with the password
     *
     * @param keystore the keystore file
     * @param alias  the keystore alias of the key pair
     * @param password the keystore password
     * @return the keystore created
     * @throws SignatureException thrown on signature related error
     * @throws InvalidKeyException on invalid key
     * @throws KeyStoreException on kestore error (requested keystore not available)
     * @throws IOException on IO error with the keystore file
     * @throws NoSuchAlgorithmException if apropriate algorithm cannotbe found
     * @throws CertificateException if the certifacte coud not be stored
     */
    public KeyStore write(String keystore, String alias, char[] password)
      throws SignatureException, InvalidKeyException,
             KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        File file = new File(keystore);
        if (file.exists()) {
           if (!file.delete()) {
               throw new IOException("Could not delete the existing keystore "+keystore);
           }
        }
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        ks.load(null, password);
        SignerInfo si = asSignerInfo();
        ks.setKeyEntry(alias, si.getPrivate(), password, si.getCertificateChain());
        FileOutputStream fo = new FileOutputStream(file);
        ks.store(fo, password);
        fo.close();
        return ks;
    }

    /**
     * Create the test ssg keystore properties, and set the system
     * property 'com.l7tech.server.keystorePropertiesPath' soe the ServerConfig
     * can pickup those properties in test mode.
     *
     * @return the properties that correspond to the '/keystore.properties'
     */
    public static Properties createTestSsgKeystoreProperties() throws IOException {

        File f = File.createTempFile("blee", null);
        f.deleteOnExit();

        File propertiesPath = f.getParentFile();

        if (!propertiesPath.isDirectory()) {
            throw new IOException("properties path is not a directory");
        }
        final String kPath = propertiesPath.getAbsolutePath();
        final String keystorePropertiesPath = kPath +"/keystore.properties";

        final String password = "password";
        try {
            SetKeys.NewCa.main(new String[] {"localhost",
                                             kPath,
                                             password,
                                             password,
                                             KeyStore.getDefaultType()});
        } catch (Exception e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }

        final Properties sprop = System.getProperties();
        sprop.setProperty("com.l7tech.server.keystorePropertiesPath", keystorePropertiesPath);

        Properties keystoreProps = new Properties();
        keystoreProps.put(KeystoreUtils.KSTORE_PATH_PROP_NAME, kPath);

        keystoreProps.put(KeystoreUtils.SSL_KSTORE_NAME, "ssl.ks");
        keystoreProps.put(KeystoreUtils.SSL_CERT_NAME, "ssl.cer");
        keystoreProps.put(KeystoreUtils.SSL_KSTORE_PASSWD, password);

        keystoreProps.put(KeystoreUtils.ROOT_STORENAME, "ca.ks");
        keystoreProps.put(KeystoreUtils.ROOT_CERTNAME, "ca.cer");
        keystoreProps.put(KeystoreUtils.ROOT_STOREPASSWD, password);
        keystoreProps.put(KeystoreUtils.KSTORE_TYPE, Keys.KEYSTORE_TYPE);
        FileOutputStream fo = new FileOutputStream(keystorePropertiesPath);
        keystoreProps.store(fo, "test keystore properties");
        fo.close();

        return keystoreProps;
    }

    /**
     * Get the secure random object, initializes the SecureRandom object
     * on first access.
     */
    private SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
            if (debug) System.out.println("SecureRandom parameters: Provider:" + secureRandom.getProvider().getName());
        }
        return secureRandom;
    }
}
