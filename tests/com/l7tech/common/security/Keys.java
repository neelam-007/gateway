package com.l7tech.common.security;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.X509V3CertificateGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.Date;
import java.util.Calendar;
import java.math.BigInteger;

import com.l7tech.common.security.xml.SignerInfo;

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
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1WITHRSA";

    private KeyPair keyPair;
    private SecureRandom secureRandom;
    private boolean debug = false;

    /**
     * Instantiate <code>Keys</code> (private and public key pair) with
     * default key size of 512.
     * 
     * @throws NoSuchAlgorithmException when algorithm requested is not
     *                                  available
     * @throws InvalidAlgorithmParameterException
     *                                  if invalid algorithm
     *                                  parameters requested (wrong key size value for example)
     */
    public Keys()
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        this(512);
    }

    /**
     * Instantiate private and public key pair specifying a key size.
     * 
     * @param size the key size. Must be in 512-1024 range, and modulo 64.
     * @throws NoSuchAlgorithmException when algorithm requested is not
     *                                  available
     * @throws InvalidAlgorithmParameterException
     *                                  if invalid algorithm
     *                                  parameters requested (wrong key size value for example)
     */
    public Keys(int size)
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
    }


    /**
     * Create a selfsigned X509Certificate for a public key
     * 
     * @param notBefore the notBefore date from the validity period of the certificate.
     * @param notAfter  the notAfter date from the validity period of the certificate.
     * @param subject   Distinguished Name of the subject of this certificate
     * @return the X509 certificate
     * @throws SignatureException  thrown on signing related error
     * @throws InvalidKeyException thrown on invalid key
     *                             * @exception IllegalArgumentException if an parameter is <b>null</b>
     */
    public X509Certificate generateSelfSignedCertificate(Date notBefore, Date notAfter, String subject)
      throws SignatureException, InvalidKeyException, IllegalArgumentException {
        if (notBefore == null || notAfter == null ||
          subject == null) {
            throw new IllegalArgumentException();
        }
        PrivateKey signKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setPublicKey(publicKey);

        certGen.setSubjectDN(new X509Principal(subject));
        certGen.setIssuerDN(new X509Principal(subject)); // self signed

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
     * return the key as a <code>SignerInfo</code> instance for
     * the given subject
     * 
     * @return the signer info instance
     */
    public SignerInfo asSignerInfo(String subject)
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

        X509Certificate certificate = generateSelfSignedCertificate(notBefore, notAfter, subject);
        return new SignerInfo(keyPair.getPrivate(), new X509Certificate[] { certificate });

    }

    /**
     * Get the secure random object, initializes the SecureRandom object
     * on first access.
     */
    protected SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
            if (debug) System.out.println("SecureRandom parameters: Provider:" + secureRandom.getProvider().getName());
        }
        return secureRandom;
    }
}
