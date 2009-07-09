package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Parameter driven X.509 certificate generator.
 * Encapsulates generating a new certificate from a CertGenParams.
 * <p/>
 * This class is not threadsafe.
 */
public class ParamsCertificateGenerator {
    private final CertGenParams c;

    static final boolean PREFER_SHA1_SIG = SyspropUtil.getBoolean("com.l7tech.security.cert.alwaysSignWithSha1", false);
    private static final SecureRandom rand = new SecureRandom();

    /**
     * Create a CertificateGenerator that will use the specified CertGenParams.
     *
     * @param certGenparams the params to use.  Must contain a non-null subject DN.  Everything else can be defaulted.
     * @throws CertificateGeneratorException if the specified params does not contain a subject DN.
     */
    public ParamsCertificateGenerator(CertGenParams certGenparams) throws CertificateGeneratorException {
        this.c = certGenparams;
        if (c.getSubjectDn() == null)
            throw new CertificateGeneratorException("A subject DN is required");
    }

    /**
     * Generate a new X.509 certificate, using the current parameters, signed by the specified issuer certificate.
     *
     * @param subjectPublicKey the public key to certify.  Required.
     * @param issuerPrivateKey the private key to use to sign the new certificate.  Required.
     *                         If issuerCertificate is provided, this must correspond to the issuer public key.
     * @param issuerCertificate the certificate of the entity certifying this public key, or null to generate a self-signed certificate.
     * @return the generated certificate.  Never null.
     * @throws CertificateGeneratorException if a certificate cannot be generated using the current CertGenParams with the provided arguments.
     */
    public X509Certificate generateCertificate(PublicKey subjectPublicKey, PrivateKey issuerPrivateKey, X509Certificate issuerCertificate) throws CertificateGeneratorException {
        X500Principal subjectDn = c.getSubjectDn();
        String sigAlg = c.getSignatureAlgorithm() != null ? c.getSignatureAlgorithm()
                :  getSigAlg("EC".equals(issuerPrivateKey.getAlgorithm()), null);
        X500Principal issuerDn = issuerCertificate != null ? issuerCertificate.getSubjectX500Principal() : subjectDn;
        PublicKey issuerPublicKey = issuerCertificate != null ? issuerCertificate.getPublicKey() : subjectPublicKey;
        int daysUntilExpiry = c.getDaysUntilExpiry() > 0 ? c.getDaysUntilExpiry() : 5 * 365; // default: five years
        Date notBefore = c.getNotBefore() != null ? c.getNotBefore()
                : new Date(new Date().getTime() - (10 * 60 * 1000L)); // default: 10 min ago
        Date notAfter = c.getNotAfter() != null ? c.getNotAfter()
                : new Date(notBefore.getTime() + (daysUntilExpiry * 24 * 60 * 60 * 1000L)); // default: daysUntilExpiry days after notBefore
        BigInteger serialNumber = c.getSerialNumber() != null ? c.getSerialNumber() : new BigInteger(64, rand).abs();

        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();

        certgen.setSerialNumber(serialNumber);
        certgen.setNotBefore(notBefore);
        certgen.setNotAfter(notAfter);
        certgen.setSignatureAlgorithm(sigAlg);
        certgen.setSubjectDN(subjectDn);
        certgen.setIssuerDN(issuerDn);
        certgen.setPublicKey(subjectPublicKey);

        if (c.isIncludeBasicConstraints())
            certgen.addExtension(X509Extensions.BasicConstraints.getId(), true, createBasicConstraints());

        if (c.isIncludeKeyUsage())
            certgen.addExtension(X509Extensions.KeyUsage, c.isKeyUsageCritical(), new X509KeyUsage(c.getKeyUsageBits()));

        if (c.isIncludeExtendedKeyUsage())
            certgen.addExtension(X509Extensions.ExtendedKeyUsage, c.isExtendedKeyUsageCritical(), createExtendedKeyUsage(c.getExtendedKeyUsageKeyPurposeOids()));

        if (c.isIncludeSki())
            certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), false, createSki(subjectPublicKey));

        if (c.isIncludeAki())
            certgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), false, createAki(issuerPublicKey));

        if (c.isIncludeSubjectDirectoryAttributes())
            certgen.addExtension(X509Extensions.SubjectDirectoryAttributes.getId(), c.isSubjectDirectoryAttributesCritical(), createSubjectDirectoryAttributes(c.getCountryOfCitizenshipCountryCodes()));

        try {
            Provider prov = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_CERTIFICATE_GENERATOR);
            return prov == null
                    ? certgen.generate(issuerPrivateKey)
                    : certgen.generate(issuerPrivateKey, prov.getName());
        } catch (CertificateEncodingException e) {
            throw new CertificateGeneratorException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateGeneratorException(e);
        } catch (SignatureException e) {
            throw new CertificateGeneratorException(e);
        } catch (InvalidKeyException e) {
            throw new CertificateGeneratorException(e);
        } catch (NoSuchProviderException e) {
            throw new CertificateGeneratorException(e);
        }
    }

    protected BasicConstraints createBasicConstraints() {
        final BasicConstraints bc;
        if (c.isBasicConstraintsCa()) {
            Integer pathLen = c.getBasicConstratinsPathLength();
            if (pathLen == null) pathLen = 0;
            bc = new BasicConstraints(pathLen);
        } else {
            bc = new BasicConstraints(false);
        }
        return bc;
    }

    protected AuthorityKeyIdentifier createAki(PublicKey issuerPublicKey) throws CertificateGeneratorException {
        try {
            return new AuthorityKeyIdentifierStructure(issuerPublicKey);
        } catch (InvalidKeyException e) {
            throw new CertificateGeneratorException("Unable to create AKI from issuer public key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected SubjectKeyIdentifier createSki(PublicKey subjectPublicKey) throws CertificateGeneratorException {
        try {
            return new SubjectKeyIdentifierStructure(subjectPublicKey);
        } catch (CertificateParsingException e) {
            throw new CertificateGeneratorException("Unable to create SKI from subject public key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected ExtendedKeyUsage createExtendedKeyUsage(List<String> keyPurposeOids) {
        Collection<DERObject> derObjects = new ArrayList<DERObject>();

        for (String oid : keyPurposeOids)
            derObjects.add(new DERObjectIdentifier(oid));

        return new ExtendedKeyUsage(new Vector<DERObject>(derObjects));
    }

    protected SubjectDirectoryAttributes createSubjectDirectoryAttributes(List<String> citizenshipCountryCodes) {
        Vector<Attribute> attrs = new Vector<Attribute>();

        // Add countries of citizenship
        if (citizenshipCountryCodes != null) for (String code : citizenshipCountryCodes)
            attrs.add(new Attribute(X509Name.COUNTRY_OF_CITIZENSHIP, new DERSet(new DERPrintableString(code))));

        // Add further supported attrs here, if any

        return new SubjectDirectoryAttributes(attrs);
    }

    /**
     * Find the best sig alg available with the current signature provider for the specified algorithm (EC=true, RSA=false)
     * using the specified provider.
     *
     * @param usingEcc true if we should attempt to find a working "*withECDSA" signature.  Otherwise we'll try to find
     *                 a "*withRSA" signature.
     * @param signatureProvider  a specified Provider to use for the Signature algorithm, or null to use the default.
     * @return
     */
    public static String getSigAlg(boolean usingEcc, Provider signatureProvider) {
        String strongSigAlg = usingEcc ? "SHA384withECDSA" : "SHA384withRSA";
        String weakSigAlg = usingEcc ? "SHA1withECDSA" : "SHA1withRSA";
        if (PREFER_SHA1_SIG)
            return weakSigAlg;

        String sigAlg = strongSigAlg;
        try {
            if (signatureProvider == null)
                Signature.getInstance(strongSigAlg);
            else
                Signature.getInstance(strongSigAlg, signatureProvider);
        } catch (NoSuchAlgorithmException e) {
            // Not available; fall back to weak sig alg
            sigAlg = weakSigAlg;
        }
        return sigAlg;
    }
}
