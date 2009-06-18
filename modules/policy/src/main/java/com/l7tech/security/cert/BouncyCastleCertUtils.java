package com.l7tech.security.cert;

import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.util.SyspropUtil;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Random;

/**
 * Certificate utility methods that require static imports of Bouncy Castle classes.
 */
public class BouncyCastleCertUtils {
    // TODO remove this once the sig alg is fully controllable from the cert gen GUI
    private static final boolean PREFER_SHA1_SIG = SyspropUtil.getBoolean("com.l7tech.security.cert.alwaysSignWithSha1", false);

    /**
     * Generate a self-signed certificate from the specified KeyPair, which is expected to be RSA.
     * The certificate will use the SHA1withRSA signature algorithm.
     *
     * @param dn            the DN for the new self-signed cert.  Required.
     * @param expiryDays    number of days before the cert should expire.  Required.
     * @param keyPair       an RSA key pair to use for the certificate.  Required.
     * @param makeCaCert    true if the new certificate is intended to be used to sign other certs.  Normally false.
     *                      If this is true, the new certificate will have the "cA" basic constraint and the "keyCertSign" key usage.
     * @return the new self-signed certificate.  Never null.
     * @throws CertificateEncodingException  if there is a problem producing the new cert
     * @throws NoSuchAlgorithmException      if a required crypto algorithm is unavailable
     * @throws SignatureException            if there is a problem signing the new cert
     * @throws InvalidKeyException           if there is a problem with the provided key pair
     * @throws NoSuchProviderException       if the current asymmetric crypto provider doesn't exist
     * @throws CertificateParsingException   if a SKI cannot be created for the public key
     */
    public static X509Certificate generateSelfSignedCertificate(X500Principal dn, int expiryDays, KeyPair keyPair, boolean makeCaCert)
            throws CertificateEncodingException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, CertificateParsingException
    {
        return generateSelfSignedCertificate(dn, expiryDays, keyPair, makeCaCert,
                JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_CERTIFICATE_GENERATOR));
    }


    /**
     * Generate a self-signed certificate from the specified KeyPair, which is expected to be RSA.
     * The certificate will use the SHA1withRSA signature algorithm.
     *
     * @param dn            the DN for the new self-signed cert.  Required.
     * @param expiryDays    number of days before the cert should expire.  Required.
     * @param keyPair       an RSA key pair to use for the certificate.  Required.
     * @param makeCaCert    true if the new certificate is intended to be used to sign other certs.  Normally false.
     *                      If this is true, the new certificate will have the "cA" basic constraint and the "keyCertSign" key usage.
     * @param signatureProvider  Provider to use for signing the cert, or null to use current best-preference provider.
     * @return the new self-signed certificate.  Never null.
     * @throws CertificateEncodingException  if there is a problem producing the new cert
     * @throws NoSuchAlgorithmException      if a required crypto algorithm is unavailable
     * @throws SignatureException            if there is a problem signing the new cert
     * @throws InvalidKeyException           if there is a problem with the provided key pair
     * @throws NoSuchProviderException       if the current asymmetric crypto provider doesn't exist
     * @throws CertificateParsingException   if a SKI cannot be created for the public key
     */
    public static X509Certificate generateSelfSignedCertificate(X500Principal dn, int expiryDays, KeyPair keyPair, boolean makeCaCert, Provider signatureProvider)
            throws CertificateEncodingException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, CertificateParsingException {
        PublicKey publicKey = keyPair.getPublic();
        boolean usingEcc = isUsingEcc(publicKey);
        String sigAlg = getSigAlg(usingEcc, signatureProvider);

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSignatureAlgorithm(sigAlg);
        certGen.setSerialNumber(BigInteger.valueOf(new Random().nextInt(2000000) + 1));
        certGen.setIssuerDN(dn);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        certGen.setNotBefore(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, expiryDays);
        certGen.setNotAfter(cal.getTime());
        certGen.setSubjectDN(dn);
        certGen.setPublicKey(publicKey);

        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(publicKey));

        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(publicKey));

        if (makeCaCert) {
            certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(1));
            certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        }

        return signatureProvider != null ?
                certGen.generate(keyPair.getPrivate(), signatureProvider.getName()) :
                certGen.generate(keyPair.getPrivate());
    }

    public static boolean isUsingEcc(PublicKey publicKey) {
        String publicKeyAlg = publicKey.getAlgorithm();
        return "EC".equalsIgnoreCase(publicKeyAlg) || "ECDSA".equalsIgnoreCase(publicKeyAlg);
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

    /**
     * Create a PKCS#10 certification request using the specified DN and key pair.
     *
     * @param dn  the DN for the new CSR.  Required.
     * @param keyPair  a key pair to use for the CSR.  Required.
     * @return a new PKCS#10 certification request including the specified DN and public key, signed with the
     *         specified private key.  Never null.
     * @throws SignatureException        if there is a problem signing the cert
     * @throws InvalidKeyException       if there is a problem with the provided key pair
     * @throws NoSuchProviderException   if the current asymmetric JCE provider is incorrect
     * @throws NoSuchAlgorithmException  if a required algorithm is not available in the current asymmetric JCE provider
     */
    public static CertificateRequest makeCertificateRequest(X500Principal dn, KeyPair keyPair) 
            throws SignatureException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException
    {
        return makeCertificateRequest(dn, keyPair, JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_CSR_SIGNING));
    }

    /**
     * Create a PKCS#10 certification request using the specified DN and key pair.
     *
     * @param dn  the DN for the new CSR.  Required.
     * @param keyPair  a key pair to use for the CSR.  Required.
     * @param sigProvider  Provider to use for signature algorithm for signing the CSR, or null to use current best-preference provider.
     * @return a new PKCS#10 certification request including the specified DN and public key, signed with the
     *         specified private key.  Never null.
     * @throws SignatureException        if there is a problem signing the cert
     * @throws InvalidKeyException       if there is a problem with the provided key pair
     * @throws NoSuchProviderException   if the current asymmetric JCE provider is incorrect
     * @throws NoSuchAlgorithmException  if a required algorithm is not available in the current asymmetric JCE provider
     */
    public static CertificateRequest makeCertificateRequest(X500Principal dn, KeyPair keyPair, Provider sigProvider) throws SignatureException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        X509Name subject = new X509Name(dn.getName());
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String sigAlg = getSigAlg(isUsingEcc(publicKey), sigProvider);

        // Generate request
        final PKCS10CertificationRequest certReq = sigProvider == null
                ? new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey)
                : new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey, sigProvider.getName());
        return new BouncyCastleCertificateRequest(certReq, publicKey);
    }
}
