package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Used to generate certificate chains for testing with a variety of key usage and extended key usage settings.
 */
public class TestCertificateGenerator {

    private CertGenParams c;
    private KeyGenParams k;

    private SecureRandom random;
    private KeyPair keyPair;

    public TestCertificateGenerator() {
        reset();
    }

    /** Issuer certificate, or null to create self-signed. */
    public Pair<X509Certificate, PrivateKey> issuer;

    private KeyPair getOrMakeKeyPair() {
        if (keyPair == null) {
            try {
                keyPair = new ParamsKeyGenerator(k, random).generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
        }
        return keyPair;
    }

    public PrivateKey getPrivateKey() {
        return getOrMakeKeyPair().getPrivate();
    }

    public TestCertificateGenerator reset() {
        random = new SecureRandom();
        keyPair = null;
        issuer = null;

        k = new KeyGenParams();
        k.setAlgorithm("RSA");
        k.setNamedParam("sect163k1");
        k.setKeySize(768);

        c = new CertGenParams();
        c.setSerialNumber(new BigInteger(64, random).abs());
        c.setNotBefore(new Date(new Date().getTime() - (10 * 60 * 1000L))); // default: 10 min ago
        c.setDaysUntilExpiry(20 * 365);
        c.setNotAfter(null);
        c.setSignatureAlgorithm(null);
        c.setSubjectDn("cn=test");
        c.setIncludeBasicConstraints(false);
        c.setKeyUsageBits(X509KeyUsage.digitalSignature | X509KeyUsage.keyEncipherment | X509KeyUsage.nonRepudiation);
        c.setIncludeKeyUsage(true);
        c.setKeyUsageCritical(true);
        c.setIncludeSki(true);
        c.setIncludeAki(true);
        c.setIncludeExtendedKeyUsage(true);
        c.setExtendedKeyUsageCritical(true);
        c.setExtendedKeyUsageKeyPurposeOids(Arrays.asList(KeyPurposeId.anyExtendedKeyUsage.getId()));
        c.setIncludeSubjectDirectoryAttributes(false);
        c.setSubjectDirectoryAttributesCritical(false);
        c.setCountryOfCitizenshipCountryCodes(Collections.<String>emptyList());
        return this;
    }


    public X509Certificate generate() throws GeneralSecurityException {
        return generateWithKey().left;
    }

    public Pair<X509Certificate, PrivateKey> generateWithKey() throws GeneralSecurityException {
        final KeyPair subjectKeyPair = getOrMakeKeyPair();
        final PublicKey subjectPublicKey = subjectKeyPair.getPublic();
        final PrivateKey subjectPrivateKey = subjectKeyPair.getPrivate();

        try {
            ParamsCertificateGenerator certgen = new ParamsCertificateGenerator(c);
            X509Certificate cert =
                    issuer == null
                            ? certgen.generateCertificate(subjectPublicKey, subjectPrivateKey, null)
                            : certgen.generateCertificate(subjectPublicKey, issuer.right, issuer.left);
            return new Pair<X509Certificate, PrivateKey>(asJdkCertificate(cert), subjectPrivateKey);
        } catch (CertificateGeneratorException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Configure the next key pair generation to use the provided SecureRandom instead of the system default.
     */
    public TestCertificateGenerator random(SecureRandom random) {
        this.random = random;
        return this;
    }

    /**
     * Configure the next key pair generation to produce an RSA keypair with the specified size in bits.
     */
    public TestCertificateGenerator keySize(int keyBits) {
        k.setAlgorithm("RSA");
        k.setKeySize(keyBits);
        return this;
    }

    /**
     * Configure the next key pair generation to produce an ECC keypair with the specified named curve.
     */
    public TestCertificateGenerator curveName(String curveName) {
        k.setAlgorithm("EC");
        k.setNamedParam(curveName);
        return this;
    }

    public TestCertificateGenerator issuer(X509Certificate issuerCert, PrivateKey issuerPrivateKey) {
        return issuer(new Pair<X509Certificate, PrivateKey>(issuerCert, issuerPrivateKey));
    }

    public TestCertificateGenerator issuer(Pair<X509Certificate, PrivateKey> issuer) {
        this.issuer = issuer;
        return this;
    }

    public TestCertificateGenerator subject(String subject) {
        c.setSubjectDn(subject);
        return this;
    }

    public TestCertificateGenerator noExtensions() {
        c.disableAllExtensions();
        return this;
    }

    public TestCertificateGenerator noBasicConstraints() {
        c.setIncludeBasicConstraints(false);
        return this;
    }

    public TestCertificateGenerator basicConstraintsNoCa() {
        c.setIncludeBasicConstraints(true);
        c.setBasicConstraintsCa(false);
        return this;
    }

    public TestCertificateGenerator basicConstraintsCa(int pathlen) {
        c.setIncludeBasicConstraints(true);
        c.setBasicConstraintsCa(true);
        c.setBasicConstratinsPathLength(pathlen);
        return this;
    }

    public TestCertificateGenerator noKeyUsage() {
        c.setIncludeKeyUsage(false);
        return this;
    }

    public TestCertificateGenerator keyUsage(boolean critical, int kubits) {
        c.setIncludeKeyUsage(true);
        c.setKeyUsageCritical(critical);
        c.setKeyUsageBits(kubits);
        return this;
    }

    public TestCertificateGenerator keyUsageCriticality(boolean keyUsageCriticality) {
        c.setKeyUsageCritical(keyUsageCriticality);
        return this;
    }

    public TestCertificateGenerator noExtKeyUsage() {
        c.setIncludeExtendedKeyUsage(false);
        return this;
    }

    public TestCertificateGenerator extKeyUsage(boolean critical, List<String> keyPurposeOids) {
        c.setIncludeExtendedKeyUsage(true);
        c.setExtendedKeyUsageCritical(critical);
        c.setExtendedKeyUsageKeyPurposeOids(keyPurposeOids);
        return this;
    }

    public TestCertificateGenerator extKeyUsage(boolean critical, String keyPurposeOid, String... moreKeyPurposeOids) {
        return extKeyUsage(critical, Arrays.asList(ArrayUtils.unshift(moreKeyPurposeOids, keyPurposeOid)));
    }

    public TestCertificateGenerator extKeyUsage(boolean critical, KeyPurposeId keyPurpose, KeyPurposeId... moreKeyPurposes) {
        List<String> purposes = new ArrayList<String>(moreKeyPurposes.length + 1);
        purposes.add(keyPurpose.getId());
        for (KeyPurposeId purpose : moreKeyPurposes)
            purposes.add(purpose.getId());
        return extKeyUsage(critical, purposes);
    }

    public void setExtendedKeyUsageKeyPurposeOids(List<String> extendedKeyUsageKeyPurposeOids) {
        c.setExtendedKeyUsageKeyPurposeOids(extendedKeyUsageKeyPurposeOids);
    }

    public boolean extendedKeyUsageCriticality() {
        return c.isExtendedKeyUsageCritical();
    }

    public TestCertificateGenerator extendedKeyUsageCriticality(boolean extendedKeyUsageCriticality) {
        c.setExtendedKeyUsageCritical(extendedKeyUsageCriticality);
        return this;
    }

    public TestCertificateGenerator countriesOfCitizenship(boolean critical, String... countryCodes) {
        c.setIncludeSubjectDirectoryAttributes(true);
        c.setSubjectDirectoryAttributesCritical(critical);
        c.setCountryOfCitizenshipCountryCodes(Arrays.asList(countryCodes));
        return this;
    }

    public TestCertificateGenerator daysUntilExpiry(int daysUntilExpiry) {
        c.setDaysUntilExpiry(daysUntilExpiry);
        return this;
    }

    public TestCertificateGenerator signatureAlgorithm(String sigalg) {
        c.setSignatureAlgorithm(sigalg);
        return this;
    }

    /**
     * Obtain the raw pending certificate generation parameters.
     * <p/>
     * This returns a live reference to the pending parameters.  Changes made to the returned parameters
     * will affect the future behavior of this TestCertificateGenerator.
     *
     * @return the pending cert gen params.  Never null.
     */
    public CertGenParams getCertGenParams() {
        return c;
    }

    /**
     * Obtain the raw pending key generation parameters.
     * <p/>
     * This returns a live reference to the pending parameters.  Changes made to the returned parameters
     * willa ffect the future behavior of this TestCertificateGenerator (unless it has already generated
     * its keypair or has been provided one to work with).
     *
     * @return the pending key gen params.  Never null.
     */
    public KeyGenParams getKeyGenParams() {
        return k;
    }

    /**
     * Convert the specified certificate to use the default (typically Sun JDK) certificate implementation,
     * if it isn't already doing so.
     * <p/>
     * The default implementation is locked-in the first time this method is called for the remainder of the lifetime
     * of this CertUtils class instance.
     *
     * @param cert the certificate to convert.  Required.
     * @return a certificate that uses the default X509Certificate implementation; possibly the original cert unchanged.
     * @throws java.security.cert.CertificateException if there is a problem encoding the passed-in certificate
     */
    public static X509Certificate asJdkCertificate(X509Certificate cert) throws CertificateException {
        return (X509Certificate) CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
    }

    private static void storeAsBcPkcs12(X509Certificate[] chain, PrivateKey privateKey, char[] p12Pass, String p12Alias, OutputStream out) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException {
        // The Clever Dan who wrote Bouncy Castle's JDKPKCS12KeyStore hardcoded the "BC" provider name everywhere,
        // instead of just using their own classes directly whenever such was unavoidable,
        // so we can't use their keystore implementation unless they are registered as a crypto provider
        // TODO appears to be fixed in Bouncy Castle 1.42, so we can remove this hack after we update our repo
        if (null == Security.getProvider("BC"))
            Security.addProvider(new BouncyCastleProvider());

        KeyStore ks = KeyStore.getInstance("PKCS12-DEF", "BC");
        ks.load(null, p12Pass);
        ks.setKeyEntry(p12Alias, privateKey, p12Pass, chain);
        ks.store(out, p12Pass);
    }

    private static Pair<X509Certificate[], PrivateKey> pkcs12ToSunChain(InputStream in, char[] p12Pass, String p12Alias) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(in, p12Pass);
        PrivateKey privateKey = null;
        java.security.cert.Certificate[] chain = null;
        if (p12Alias != null && ks.isKeyEntry(p12Alias)) {
            privateKey = (PrivateKey)ks.getKey(p12Alias, p12Pass);
            chain = ks.getCertificateChain(p12Alias);
        } else {
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    privateKey = (PrivateKey)ks.getKey(alias, p12Pass);
                    chain = ks.getCertificateChain(alias);
                }
            }
        }
        if (privateKey == null || chain == null)
            throw new KeyStoreException("Specified PKCS#12 KeyStore contained no key entries");
        X509Certificate[] x509Chain = castCertArrayToX509(chain);
        return new Pair<X509Certificate[], PrivateKey>(x509Chain, privateKey);
    }

    private static X509Certificate[] castCertArrayToX509(Certificate[] chain) throws KeyStoreException {
        X509Certificate[] x509Chain = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            if (!(chain[i] instanceof X509Certificate))
                throw new KeyStoreException("Specified certificate chain contained a non-X.509 certificate");
            x509Chain[i] = (X509Certificate)chain[i];
        }
        return x509Chain;
    }

    public static void saveAsPkcs12(X509Certificate[] certChain, PrivateKey privateKey, OutputStream out, String pkcs12Password)
            throws IOException, GeneralSecurityException
    {
        storeAsBcPkcs12(certChain, privateKey, pkcs12Password.toCharArray(), "entry", out);
    }

    public static void saveAsPkcs12(X509Certificate[] certChain, PrivateKey privateKey, String pathname, String pkcs12Password)
            throws IOException, GeneralSecurityException
    {
        FileOutputStream fos = new FileOutputStream(pathname);
        try {
            saveAsPkcs12(certChain, privateKey, fos, pkcs12Password);
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    public static Pair<X509Certificate[], PrivateKey> loadFromPkcs12(InputStream in, String pkcs12Password)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException {
        return pkcs12ToSunChain(in, pkcs12Password.toCharArray(), null);
    }

    public static Pair<X509Certificate[], PrivateKey> loadFromPkcs12(String pathname, String pkcs12Password)
            throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException
    {
        InputStream fis = new FileInputStream(pathname);
        try {
            return loadFromPkcs12(fis, pkcs12Password);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }
}
