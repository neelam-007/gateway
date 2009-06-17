package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

/**
 * Used to generate certificate chains for testing with a variety of key usage and extended key usage settings.
 */
public class TestCertificateGenerator {

    private SecureRandom random;
    private BigInteger serialNumber;
    private Date notBefore;
    private Date notAfter;
    private int daysUntilExpiry;
    private String signatureAlgorithm;
    private X509Name subjectDn;
    private int rsaBits;
    private KeyPair keyPair;
    private int keyUsageBits;
    private BasicConstraints basicConstraints;
    private String eccCurveName;
    private boolean useECC;
    private boolean includeKeyUsage;
    private boolean keyUsageCriticality;
    private boolean includeSki;
    private boolean includeAki;
    private boolean includeExtendedKeyUsage;
    private boolean extendedKeyUsageCriticality;
    private boolean includeSubjectDirectoryAttributes;
    private boolean subjectDirectoryAttributesCriticality;
    private List<String> extendedKeyUsageKeyPurposeOids;
    private List<String> countryOfCitizenshipCountryCodes;

    public TestCertificateGenerator() {
        reset();
    }

    /** Issuer certificate, or null to create self-signed. */
    public Pair<X509Certificate, PrivateKey> issuer;

    private KeyPair getOrMakeKeyPair() {
        if (keyPair == null) {
            try {
                if (useECC) {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                    kpg.initialize(new ECGenParameterSpec(eccCurveName), random);
                    keyPair = kpg.generateKeyPair();
                } else {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(rsaBits, random);
                    keyPair = kpg.generateKeyPair();
                }
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
        serialNumber = new BigInteger(64, random).abs();
        notBefore = new Date(new Date().getTime() - (10 * 60 * 1000L)); // default: 10 min ago
        daysUntilExpiry = 20 * 365;
        notAfter = null;
        useECC = false;
        eccCurveName = "sect163k1";
        signatureAlgorithm = null;
        subjectDn = new X509Name("cn=test");
        rsaBits = 768;
        keyPair = null;
        basicConstraints = new BasicConstraints(false);
        keyUsageBits = X509KeyUsage.digitalSignature | X509KeyUsage.keyEncipherment | X509KeyUsage.nonRepudiation;
        includeKeyUsage = true;
        keyUsageCriticality = true;
        includeSki = true;
        includeAki = true;
        includeExtendedKeyUsage = true;
        extendedKeyUsageCriticality = true;
        extendedKeyUsageKeyPurposeOids = Arrays.asList(KeyPurposeId.anyExtendedKeyUsage.getId());
        issuer = null;
        includeSubjectDirectoryAttributes = false;
        subjectDirectoryAttributesCriticality = false;
        countryOfCitizenshipCountryCodes = Collections.emptyList();
        return this;
    }


    public X509Certificate generate() throws GeneralSecurityException {
        return generateWithKey().left;
    }

    public Pair<X509Certificate, PrivateKey> generateWithKey() throws GeneralSecurityException {
        final KeyPair subjectKeyPair = getOrMakeKeyPair();
        final PublicKey subjectPublicKey = subjectKeyPair.getPublic();
        final PrivateKey subjectPrivateKey = subjectKeyPair.getPrivate();

        if (signatureAlgorithm == null)
            signatureAlgorithm = BouncyCastleCertUtils.getSigAlg(useECC, null);

        X509Name issuerDn;
        PublicKey issuerPublicKey;
        PrivateKey issuerPrivateKey;
        if (issuer == null) {
            // Self-signed
            issuerDn = subjectDn;
            issuerPrivateKey = subjectPrivateKey;
            issuerPublicKey = subjectPublicKey;
        } else {
            // Specified signing cert chain
            issuerDn = new X509Name(issuer.left.getSubjectDN().getName());
            issuerPrivateKey = issuer.right;
            issuerPublicKey = issuer.left.getPublicKey();
        }

        if (notAfter == null)
            notAfter = new Date(notBefore.getTime() + (daysUntilExpiry * 24 * 60 * 60 * 1000L));

        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();

        certgen.setSerialNumber(serialNumber);
        certgen.setNotBefore(notBefore);
        certgen.setNotAfter(notAfter);
        certgen.setSignatureAlgorithm(signatureAlgorithm);
        certgen.setSubjectDN(subjectDn);
        certgen.setIssuerDN(issuerDn);
        certgen.setPublicKey(subjectPublicKey);

        if (basicConstraints != null)
            certgen.addExtension(X509Extensions.BasicConstraints.getId(), true, basicConstraints);

        if (includeKeyUsage)
            certgen.addExtension(X509Extensions.KeyUsage, keyUsageCriticality, new X509KeyUsage(keyUsageBits));

        if (includeExtendedKeyUsage)
            certgen.addExtension(X509Extensions.ExtendedKeyUsage, extendedKeyUsageCriticality, createExtendedKeyUsage(extendedKeyUsageKeyPurposeOids));

        if (includeSki)
            certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), false, createSki(subjectPublicKey));

        if (includeAki)
            certgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), false, createAki(issuerPublicKey));

        if (includeSubjectDirectoryAttributes)
            certgen.addExtension(X509Extensions.SubjectDirectoryAttributes.getId(), subjectDirectoryAttributesCriticality, createSubjectDirectoryAttributes(countryOfCitizenshipCountryCodes));

        serialNumber = serialNumber.add(BigInteger.ONE);
        X509Certificate generatedCert = certgen.generate(issuerPrivateKey, random);

        // Ensure cert and private key are using the Sun implementation
        return new Pair<X509Certificate, PrivateKey>(asJdkCertificate(generatedCert), subjectPrivateKey);
    }

    private SubjectDirectoryAttributes createSubjectDirectoryAttributes(List<String> citizenshipCountryCodes) {
        Vector<Attribute> attrs = new Vector<Attribute>();

        // Add countries of citizenship
        if (citizenshipCountryCodes != null) for (String code : citizenshipCountryCodes)
            attrs.add(new Attribute(X509Name.COUNTRY_OF_CITIZENSHIP, new DERSet(new DERPrintableString(code))));

        // Add further supported attrs here, if any

        return new SubjectDirectoryAttributes(attrs);
    }

    /**
     * Configure the next key pair generation to produce an RSA keypair with the specified size in bits.
     */
    public TestCertificateGenerator keySize(int keyBits) {
        this.useECC = false;
        this.rsaBits = keyBits;
        return this;
    }

    /**
     * Configure the next key pair generation to produce an ECC keypair with the specified named curve.
     */
    public TestCertificateGenerator curveName(String curveName) {
        this.useECC = true;
        this.eccCurveName = curveName;
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
        this.subjectDn = new X509Name(true, subject);
        return this;
    }

    public TestCertificateGenerator noExtensions() {
        includeAki = false;
        includeSki = false;
        basicConstraints = null;
        includeKeyUsage = false;
        includeExtendedKeyUsage = false;
        return this;
    }

    public TestCertificateGenerator noBasicConstraints() {
        basicConstraints = null;
        return this;
    }

    public TestCertificateGenerator basicConstraintsNoCa() {
        basicConstraints = new BasicConstraints(false);
        return this;
    }

    public TestCertificateGenerator basicConstraintsCa(int pathlen) {
        basicConstraints = new BasicConstraints(pathlen);
        return this;
    }

    public TestCertificateGenerator noKeyUsage() {
        includeKeyUsage = false;
        return this;
    }

    public TestCertificateGenerator keyUsage(boolean critical, int kubits) {
        includeKeyUsage = true;
        keyUsageCriticality = critical;
        keyUsageBits = kubits;
        return this;
    }

    public void setKeyUsageBits(int keyUsageBits) {
        this.keyUsageBits = keyUsageBits;
    }

    public int getKeyUsageBits() {
        return keyUsageBits;
    }

    public boolean keyUsageCriticality() {
        return keyUsageCriticality;
    }

    public TestCertificateGenerator keyUsageCriticality(boolean keyUsageCriticality) {
        this.keyUsageCriticality = keyUsageCriticality;
        return this;
    }

    public TestCertificateGenerator noExtKeyUsage() {
        includeExtendedKeyUsage = false;
        return this;
    }

    public TestCertificateGenerator extKeyUsage(boolean critical, List<String> keyPurposeOids) {
        includeExtendedKeyUsage = true;
        extendedKeyUsageCriticality = critical;
        extendedKeyUsageKeyPurposeOids = keyPurposeOids;
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
        this.extendedKeyUsageKeyPurposeOids = extendedKeyUsageKeyPurposeOids;
    }

    public List<String> getExtendedKeyUsageKeyPurposeOids() {
        return extendedKeyUsageKeyPurposeOids;
    }

    public boolean extendedKeyUsageCriticality() {
        return extendedKeyUsageCriticality;
    }

    public TestCertificateGenerator extendedKeyUsageCriticality(boolean extendedKeyUsageCriticality) {
        this.extendedKeyUsageCriticality = extendedKeyUsageCriticality;
        return this;
    }

    public TestCertificateGenerator countriesOfCitizenship(boolean critical, String... countryCodes) {
        includeSubjectDirectoryAttributes = true;
        subjectDirectoryAttributesCriticality = critical;
        countryOfCitizenshipCountryCodes = Arrays.asList(countryCodes);
        return this;
    }

    public TestCertificateGenerator daysUntilExpiry(int daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
        return this;
    }

    public TestCertificateGenerator signatureAlgorithm(String sigalg) {
        this.signatureAlgorithm = sigalg;
        return this;
    }

    public void setCountryOfCitizenshipCountryCodes(List<String> countryOfCitizenshipCountryCodes) {
        this.countryOfCitizenshipCountryCodes = countryOfCitizenshipCountryCodes;
    }

    public List<String> getCountryOfCitizenshipCountryCodes() {
        return countryOfCitizenshipCountryCodes;
    }

    private AuthorityKeyIdentifier createAki(PublicKey issuerPublicKey) throws InvalidKeyException {
        return new AuthorityKeyIdentifier(makeSubjectPublicKeyInfo(issuerPublicKey));
    }

    private SubjectKeyIdentifier createSki(PublicKey subjectPublicKey) throws InvalidKeyException {
        return new SubjectKeyIdentifier(makeSubjectPublicKeyInfo(subjectPublicKey));
    }

    private ExtendedKeyUsage createExtendedKeyUsage(List<String> keyPurposeOids) {
        Collection<DERObject> derObjects = new ArrayList<DERObject>();

        for (String oid : keyPurposeOids)
            derObjects.add(new DERObjectIdentifier(oid));

        return new ExtendedKeyUsage(new Vector<DERObject>(derObjects));
    }

    private SubjectPublicKeyInfo makeSubjectPublicKeyInfo(PublicKey publicKey) throws InvalidKeyException {
        ASN1InputStream asn1Stream = null;
        try {
            asn1Stream = new ASN1InputStream(new ByteArrayInputStream(publicKey.getEncoded()));
            return new SubjectPublicKeyInfo(ASN1Sequence.getInstance(asn1Stream.readObject()));
        } catch (IOException e) {
            throw new InvalidKeyException(e); // shouldn't be possible
        } finally {
            ResourceUtils.closeQuietly(asn1Stream);
        }
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
