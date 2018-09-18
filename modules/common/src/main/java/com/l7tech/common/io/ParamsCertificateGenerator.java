package com.l7tech.common.io;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.operator.AlgorithmNameFinder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.*;
import java.util.*;

/**
 * Parameter driven X.509 certificate generator.
 * Encapsulates generating a new certificate from a CertGenParams.
 * <p/>
 * This class is not threadsafe.
 */
public class ParamsCertificateGenerator {
    private final CertGenParams c;

    static final boolean PREFER_SHA1_SIG = ConfigFactory.getBooleanProperty( "com.l7tech.security.cert.alwaysSignWithSha1", false );
    private static final SecureRandom DEFAULT_RAND = new SecureRandom();
    private final SecureRandom rand;
    private final String signatureProviderName;

    /**
     * Create a CertificateGenerator that will use the specified CertGenParams.
     *
     * @param certGenparams the params to use.  Must contain a non-null subject DN.  Everything else can be defaulted.
     * @throws CertificateGeneratorException if the specified params does not contain a subject DN.
     */
    public ParamsCertificateGenerator(CertGenParams certGenparams) throws CertificateGeneratorException {
        this(certGenparams, DEFAULT_RAND, null);
    }

    /**
     * Create a CertificateGenerator that will use the specified CertGenParams, random number generator, and cert gen provider.
     *
     * @param certGenparams the params to use.  Must contain a non-null subject DN.  Everything else can be defaulted.
     * @param rand a SecureRandom instance to use, or null to use a default.
     * @param signatureProviderName name of Provider for Signature implementation when signing cert, or null to use default.
     * @throws CertificateGeneratorException if the specified params does not contain a subject DN.
     */
    public ParamsCertificateGenerator(CertGenParams certGenparams, SecureRandom rand, String signatureProviderName) throws CertificateGeneratorException {
        this.c = certGenparams;
        if (c.getSubjectDn() == null)
            throw new CertificateGeneratorException("A subject DN is required");
        this.rand = rand == null ? DEFAULT_RAND : rand;
        this.signatureProviderName = signatureProviderName;
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

        final X500Name subjectX500Name = X500Name.getInstance(c.getSubjectDn().getEncoded());

        // If the hash algorithm is set to "Automatic", then it will be reset to null and the signature algorithm wil be determined
        // by using the previous manner (See the method getSignAlg).
        String hashAlgorithm = c.getHashAlgorithm();
        if ("Automatic" .equals(hashAlgorithm)) {
            hashAlgorithm = null;
        }

        final String sigAlg = c.getSignatureAlgorithm() != null ? c.getSignatureAlgorithm()
                : getSigAlg(issuerCertificate == null ? subjectPublicKey : issuerCertificate.getPublicKey(), hashAlgorithm, null);

        final X500Name issuerX500Name;
        if (issuerCertificate != null) {
            try {
                issuerX500Name = new JcaX509CertificateHolder(issuerCertificate).getSubject();
            } catch (CertificateEncodingException e) {
                throw new CertificateGeneratorException(e);
            }
        } else {
            issuerX500Name = subjectX500Name;
        }

        final PublicKey issuerPublicKey = issuerCertificate != null ? issuerCertificate.getPublicKey() : subjectPublicKey;
        final int daysUntilExpiry = c.getDaysUntilExpiry() > 0 ? c.getDaysUntilExpiry() : 5 * 365; // default: five years
        final Date notBefore = c.getNotBefore() != null ? c.getNotBefore()
                : new Date(new Date().getTime() - (10 * 60 * 1000L)); // default: 10 min ago
        final Date notAfter = c.getNotAfter() != null ? c.getNotAfter()
                : new Date(notBefore.getTime() + (daysUntilExpiry * 24 * 60 * 60 * 1000L)); // default: daysUntilExpiry days after notBefore
        final BigInteger serialNumber = c.getSerialNumber() != null ? c.getSerialNumber() : new BigInteger(64, rand).abs();

        final X509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
                issuerX500Name,
                serialNumber,
                notBefore,
                notAfter,
                subjectX500Name,
                subjectPublicKey);

        try {

            if (c.isIncludeBasicConstraints())
                certBldr.addExtension(Extension.basicConstraints, true, createBasicConstraints());

            if (c.isIncludeKeyUsage())
                certBldr.addExtension(Extension.keyUsage, c.isKeyUsageCritical(), new X509KeyUsage(c.getKeyUsageBits()));

            if (c.isIncludeExtendedKeyUsage())
                certBldr.addExtension(Extension.extendedKeyUsage, c.isExtendedKeyUsageCritical(), createExtendedKeyUsage(c.getExtendedKeyUsageKeyPurposeOids()));

            if (c.isIncludeSki())
                certBldr.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(subjectPublicKey));

            if (c.isIncludeAki())
                certBldr.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyIdentifier(issuerPublicKey));

            if (c.isIncludeSubjectDirectoryAttributes())
                certBldr.addExtension(Extension.subjectDirectoryAttributes, c.isSubjectDirectoryAttributesCritical(), createSubjectDirectoryAttributes(c.getCountryOfCitizenshipCountryCodes()));

            if (c.isIncludeCertificatePolicies())
                certBldr.addExtension(Extension.certificatePolicies, c.isCertificatePoliciesCritical(), createCertificatePolicies(c.getCertificatePolicies()));

            if (c.isIncludeSubjectAlternativeName()){
                certBldr.addExtension(Extension.subjectAlternativeName, c.isSubjectAlternativeNameCritical(), createSubjectAlternativeName(c.getSubjectAlternativeNames()));
            }

            if (c.isIncludeAuthorityInfoAccess()) {
                certBldr.addExtension(Extension.authorityInfoAccess, c.isAuthorityInfoAccessCritical(), createAuthorityInfoAccess(c.getAuthorityInfoAccessOcspUrls()));
            }

            if (c.isIncludeCrlDistributionPoints()) {
                certBldr.addExtension(Extension.cRLDistributionPoints, c.isCrlDistributionPointsCritical(), createCrlDistributionPoints(c.getCrlDistributionPointsUrls()));
            }

            final String sigAlgName;
            // Test if sigAlgName is an OID.  This test allows for backwards compatibility with the old behaviour of sigAlg storing/using OID
            if (isOID(sigAlg)) {
                final AlgorithmNameFinder nameFinder = new DefaultAlgorithmNameFinder();
                sigAlgName = nameFinder.getAlgorithmName(new ASN1ObjectIdentifier(sigAlg));
            } else {
                sigAlgName = sigAlg;
            }

            final ContentSigner signer;
            final X509Certificate x509Certificate;
            if (signatureProviderName == null) {
                signer = new JcaContentSignerBuilder(sigAlgName).build(issuerPrivateKey);
                x509Certificate = new JcaX509CertificateConverter().getCertificate(certBldr.build(signer));

            } else {
                signer = new JcaContentSignerBuilder(sigAlgName).setProvider(signatureProviderName).build(issuerPrivateKey);
                x509Certificate = new JcaX509CertificateConverter().setProvider(signatureProviderName)
                        .getCertificate(certBldr.build(signer));
            }

            return x509Certificate;

        } catch (OperatorCreationException | CertificateException | IOException e) {
            throw new CertificateGeneratorException(e);
        }
    }

    /**
     * Returns whether the string provided is an oid.
     * @param oid
     * @return true the string provided is an oid.  False otherwise.
     */
    private boolean isOID(String oid) {
        try {
            new ASN1ObjectIdentifier(oid);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    protected BasicConstraints createBasicConstraints() {
        final BasicConstraints bc;
        if (c.isBasicConstraintsCa()) {
            Integer pathLen = c.getBasicConstraintsPathLength();
            if (pathLen == null) pathLen = 0;
            bc = new BasicConstraints(pathLen);
        } else {
            bc = new BasicConstraints(false);
        }
        return bc;
    }

    protected AuthorityKeyIdentifier createAuthorityKeyIdentifier(PublicKey issuerPublicKey) throws CertificateGeneratorException  {
        try {
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            return extUtils.createAuthorityKeyIdentifier(issuerPublicKey);

        } catch (NoSuchAlgorithmException e) {
            throw new CertificateGeneratorException("Unable to create AKI from issuer public key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected SubjectKeyIdentifier createSubjectKeyIdentifier(final PublicKey subjectPublicKey) throws CertificateGeneratorException {
        try {
            return new JcaX509ExtensionUtils().createSubjectKeyIdentifier(subjectPublicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateGeneratorException("Unable to create SKI from subject public key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected ExtendedKeyUsage createExtendedKeyUsage(final List<String> keyPurposeOids) {
        int i = 0;
        final KeyPurposeId[] kps = new KeyPurposeId[keyPurposeOids.size()];
        for (final String oid : keyPurposeOids) {
            kps[i++] = KeyPurposeId.getInstance(new ASN1ObjectIdentifier(oid));
        }

        return new ExtendedKeyUsage(kps);
    }

    protected SubjectDirectoryAttributes createSubjectDirectoryAttributes(List<String> citizenshipCountryCodes) {
        final List<Attribute> attrs = new ArrayList<>();

        // Add countries of citizenship
        if (citizenshipCountryCodes != null) for (String code : citizenshipCountryCodes){
            attrs.add(new Attribute(BCStyle.COUNTRY_OF_CITIZENSHIP, new DERSet(new DERPrintableString(code))));
        }

        // Add further supported attrs here, if any

        return new SubjectDirectoryAttributes(new Vector<>(attrs));
    }

    private DERSequence createCertificatePolicies(final List<String> certificatePolicies) {
        return new DERSequence(
            Functions.map(certificatePolicies, (Functions.Unary<PolicyInformation, String>) certificatePolicyOID ->
                    new PolicyInformation(new ASN1ObjectIdentifier(certificatePolicyOID))).toArray(new ASN1Object[certificatePolicies.size()])
        );
    }

    private ASN1Encodable createSubjectAlternativeName(final List<X509GeneralName> x509GeneralNameList) {

        final List<GeneralName> generalNames = Functions.map(x509GeneralNameList, (Functions.Unary<GeneralName, X509GeneralName>) name -> {
            if (name.isString()) {
                X509GeneralName.Type x509GeneralNameType = name.getType();
                // handle the behaviour when X509v3CertificateBuilder adds the Subject Alternative Name extension, that the directory name
                // which gets stored in the Bouncy Castle's GeneralName has to be the reverse order of RFC2253.
                if (x509GeneralNameType == X509GeneralName.Type.directoryName) {
                    try {
                        // extracting the directory name and encoding it this way seems to work.
                        // The encoded value when inspected is the reverse order.
                        final ASN1Encodable asn1Encodable = ASN1Primitive.fromByteArray(new X500Principal(name.getStringVal()).getEncoded());
                        return new GeneralName(name.getType().getTag(), asn1Encodable);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Invalid encoded value for X509GeneralName: " + ExceptionUtils.getMessage(e), e);
                    }
                } else {
                    return new GeneralName(name.getType().getTag(), name.getStringVal());
                }
            } else {
                try {
                    return new GeneralName(name.getType().getTag(), ASN1Primitive.fromByteArray(name.getDerVal()));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Invalid DER value for X509GeneralName: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });

        return new GeneralNames(generalNames.toArray(new GeneralName[generalNames.size()]));
    }

    private ASN1Encodable createCrlDistributionPoints(final List<List<String>> crlDistributionPointsUrls) {
        final List<DistributionPoint> distPoints = new ArrayList<>();

        for (final List<String> urls : crlDistributionPointsUrls) {
            final List<GeneralName> generalNames = new ArrayList<>();
            for (String url : urls) {
                generalNames.add(new GeneralName(GeneralName.uniformResourceIdentifier, url));
            }
            distPoints.add(new DistributionPoint(new DistributionPointName(new GeneralNames(generalNames.toArray(new GeneralName[generalNames.size()]))), null, null));
        }

        return new CRLDistPoint(distPoints.toArray(new DistributionPoint[distPoints.size()]));
    }

    private ASN1Encodable createAuthorityInfoAccess(final List<String> authorityInfoAccessUrls) {
        final List<AccessDescription> accessDescriptions = new ArrayList<>();
        for (final String url : authorityInfoAccessUrls) {
            accessDescriptions.add(new AccessDescription(AccessDescription.id_ad_ocsp, new GeneralName(GeneralName.uniformResourceIdentifier, url)));
        }
        return new AuthorityInformationAccess(accessDescriptions.toArray(new AccessDescription[accessDescriptions.size()]));
    }

    /**
     * Find the best sig alg available with the current signature provider for the specified algorithm (EC=true, RSA=false)
     * using the specified provider.
     *
     * @param publicKey the public key that will be used to verify the signature, corresponding to the privateKey that will be used to perform the signature.  Required.
     * @param hashAlgorithm a hash algorithm used for hashing.  If it is null, then it will be set to the default value, "SHA384" (This case is derived from the previous default behavior.)
     * @param signatureProvider  a specified Provider to use for the Signature algorithm, or null to use the default.
     * @return the signature algorithm name, ie "SHA384withECDSA".
     */
    public static String getSigAlg(PublicKey publicKey, String hashAlgorithm, Provider signatureProvider) {
        String strongSigAlg;
        String weakSigAlg;

        // If hashAlgorithm is not specified (i.e. null), then we go back the previous default behavior, i.e. set strongSignAlg to SHA384WithXXX, where XXX is keyAlg.
        if (hashAlgorithm == null) hashAlgorithm = "SHA384";

        final String keyAlg = publicKey.getAlgorithm();
        if (publicKey instanceof ECKey || "EC".equalsIgnoreCase(keyAlg))  {
            strongSigAlg = hashAlgorithm + "withECDSA";
            weakSigAlg = "SHA1withECDSA";
        } else if (publicKey instanceof RSAKey || "RSA".equalsIgnoreCase(keyAlg)) {
            strongSigAlg = hashAlgorithm + "withRSA";
            weakSigAlg = "SHA1withRSA";
        } else if (publicKey instanceof DSAKey || "DSA".equalsIgnoreCase(keyAlg)) {
            strongSigAlg = hashAlgorithm + "withDSA";
            weakSigAlg = "SHA1withDSA";
        } else {
            strongSigAlg = hashAlgorithm + "with" + keyAlg;
            weakSigAlg = "SHA1with" + keyAlg;
        }

        if (PREFER_SHA1_SIG || isShortKey(publicKey))
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

    public static boolean isShortKey(PublicKey publicKey) {
        if (publicKey instanceof ECPublicKey) {
            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            int keySize = ecPublicKey.getParams().getCurve().getField().getFieldSize();
            return keySize < 384; // Too small for SHA384withECDSA, use SHA1withECDSA instead
        } else if (publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            int keySize = CertUtils.getRsaKeyBits(rsaPublicKey);
            return keySize < 768; // Too small for SHA384withRSA, use SHA1withRSA instead
        } else {
            // Assume it's a small key
            return true;
        }
    }
}