package com.l7tech.security;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to generate certificate chains for testing with a variety of key usage and extended key usage settings.
 */
public class TestCertificateGenerator {
    private static final Logger logger = Logger.getLogger(TestCertificateGenerator.class.getName());

    private static final SecureRandom random = new SecureRandom();

    private BigInteger serialNumber;
    private Date notBefore;
    private Date notAfter;
    private String signatureAlgorithm;
    private X509Name subjectDn;
    private int rsaBits;
    private KeyPair keyPair;
    private int basicConstraitsPathLen;
    private int keyUsageBits;
    private boolean includeBasicConstraints;
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

    private X509Certificate certificate;

    /** Issuer certificate, or null to create self-signed. */
    public SignerInfo issuer;

    public TestCertificateGenerator() {
        reset();
    }

    private KeyPair getOrMakeKeyPair() {
        if (keyPair == null) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(rsaBits);
                keyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return keyPair;
    }

    public PrivateKey getPrivateKey() {
        return getOrMakeKeyPair().getPrivate();
    }

    public TestCertificateGenerator reset() {
        serialNumber = new BigInteger(64, random).abs();
        notBefore = new Date(new Date().getTime() - (10 * 60 * 1000L)); // default: 10 min ago
        notAfter = new Date(notBefore.getTime() + (20 * 365 * 24 * 60 * 60 * 1000L)); // default: 20 years from now
        signatureAlgorithm = "SHA1WithRSA";
        subjectDn = new X509Name("cn=test");
        rsaBits = 512;
        keyPair = null;
        basicConstraitsPathLen = -1;
        keyUsageBits = X509KeyUsage.digitalSignature | X509KeyUsage.keyEncipherment | X509KeyUsage.nonRepudiation;
        includeBasicConstraints = true;
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


    public X509Certificate generate() throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, CertificateException {
        return generateWithKey().left;
    }

    public Pair<X509Certificate, PrivateKey> generateWithKey() throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, CertificateException
    {
        final KeyPair subjectKeyPair = getOrMakeKeyPair();
        final PublicKey subjectPublicKey = subjectKeyPair.getPublic();
        final PrivateKey subjectPrivateKey = subjectKeyPair.getPrivate();

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
            issuerDn = new X509Name(issuer.getCertificateChain()[0].getSubjectDN().getName());
            issuerPrivateKey = issuer.getPrivate();
            issuerPublicKey = issuer.getPublic();
        }

        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();

        certgen.setSerialNumber(serialNumber);
        certgen.setNotBefore(notBefore);
        certgen.setNotAfter(notAfter);
        certgen.setSignatureAlgorithm(signatureAlgorithm);
        certgen.setSubjectDN(subjectDn);
        certgen.setIssuerDN(issuerDn);
        certgen.setPublicKey(subjectPublicKey);

        if (includeBasicConstraints) {
            BasicConstraints bc = basicConstraitsPathLen < 0
                    ? new BasicConstraints(false)
                    : new BasicConstraints(basicConstraitsPathLen);
            certgen.addExtension(X509Extensions.BasicConstraints.getId(), true, bc);
        }

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
        certificate = asJdkCertificate(certgen.generate(issuerPrivateKey, random));
        return new Pair<X509Certificate, PrivateKey>(certificate, subjectPrivateKey);
    }

    private SubjectDirectoryAttributes createSubjectDirectoryAttributes(List<String> citizenshipCountryCodes) {
        Vector<Attribute> attrs = new Vector<Attribute>();

        // Add countries of citizenship
        if (citizenshipCountryCodes != null) for (String code : citizenshipCountryCodes)
            attrs.add(new Attribute(X509Name.COUNTRY_OF_CITIZENSHIP, new DERSet(new DERPrintableString(code))));

        // Add further supported attrs here, if any

        return new SubjectDirectoryAttributes(attrs);
    }

    public TestCertificateGenerator issuer(X509Certificate issuerCert, PrivateKey issuerPrivateKey) {
        return issuer(new SignerInfo(issuerPrivateKey, new X509Certificate[] { issuerCert }));
    }

    public TestCertificateGenerator issuer(SignerInfo issuer) {
        this.issuer = issuer;
        return this;
    }

    public TestCertificateGenerator subject(String subject) {
        this.subjectDn = new X509Name(subject);
        return this;
    }

    public TestCertificateGenerator noExtensions() {
        includeAki = false;
        includeSki = false;
        includeBasicConstraints = false;
        includeKeyUsage = false;
        includeExtendedKeyUsage = false;
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

    public TestCertificateGenerator countriesOfCitizenship(boolean critical, String... countryCodes) {
        includeSubjectDirectoryAttributes = true;
        subjectDirectoryAttributesCriticality = critical;
        countryOfCitizenshipCountryCodes = Arrays.asList(countryCodes);
        return this;
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

    private static java.security.cert.Certificate makeDefaultCert() {
        try {
            return CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(TEST_CERT.getBytes()));
        } catch (CertificateException e) {
            logger.log(Level.SEVERE, "Unable to parse test cert with default cert factory: " + ExceptionUtils.getMessage(e), e);
            throw new RuntimeException(e);
        }
    }
    private static final String TEST_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBizCCATWgAwIBAgIJAJPS9fuRnwndMA0GCSqGSIb3DQEBBQUAMA8xDTALBgNVBAMMBHRlc3Qw\n" +
            "HhcNMDgxMTI1MTkxMDE2WhcNMjgxMTIwMTkxMDE2WjAPMQ0wCwYDVQQDDAR0ZXN0MFwwDQYJKoZI\n" +
            "hvcNAQEBBQADSwAwSAJBAL0+cM3u6rCSF+vHrqRZ2f3P6tuMEei3okxpDyltPLYzc32bNHeKVS2z\n" +
            "Ky9bBQZUpDv0mvEhuP3nS2khTiQEr3UCAwEAAaN0MHIwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8E\n" +
            "BAMCBeAwEgYDVR0lAQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQUN960qUtXq/yA+L8NB30LIHq1M4Iw\n" +
            "HwYDVR0jBBgwFoAUN960qUtXq/yA+L8NB30LIHq1M4IwDQYJKoZIhvcNAQEFBQADQQARnSLmMwWb\n" +
            "bYpq2duVZgrVB6dAgr/Tfe9fPVrxxR0bw2NQOOc00g3GElzR7s1TKU9dK2xt0aM2w7WFzn3lhMSp\n" +
            "-----END CERTIFICATE-----";
    private static final java.security.cert.Certificate defaultCert = makeDefaultCert();
    private static final Class defaultCertClass = defaultCert.getClass();

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
        if (defaultCertClass == cert.getClass())
            return cert;
        return (X509Certificate)CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
    }
}
