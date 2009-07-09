package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Certificate utility methods that require static imports of Bouncy Castle classes.
 */
public class BouncyCastleCertUtils {

    /**
     * Generate a self-signed certificate from the specified KeyPair and the specified cert generation parameters.
     *
     * @param certGenParams configuration of the cert to generate.  Required.  Must have a non-null subjectDn.
     * @param keyPair       an RSA key pair to use for the certificate.  Required.
     * @return the new self-signed certificate.  Never null.
     * @throws CertificateGeneratorException  if there is a problem producing the new cert
     */
    public static X509Certificate generateSelfSignedCertificate(CertGenParams certGenParams, KeyPair keyPair) throws CertificateGeneratorException {
        return new ParamsCertificateGenerator(certGenParams).generateCertificate(keyPair.getPublic(), keyPair.getPrivate(), null);
    }

    public static boolean isUsingEcc(PublicKey publicKey) {
        String publicKeyAlg = publicKey.getAlgorithm();
        return "EC".equalsIgnoreCase(publicKeyAlg) || "ECDSA".equalsIgnoreCase(publicKeyAlg);
    }

    /**
     * Create a PKCS#10 certification request using the specified DN and key pair.
     *
     * @param certGenParams parameters describing the CSR to create.  Required.
     *                      Must contain a non-null subjectDn.
     *                      May contain a non-null signatureAlgorithm; if not, a default will be picked.
     * @param keyPair  a key pair to use for the CSR.  Required.
     * @return a new PKCS#10 certification request including the specified DN and public key, signed with the
     *         specified private key.  Never null.
     * @throws SignatureException        if there is a problem signing the cert
     * @throws InvalidKeyException       if there is a problem with the provided key pair
     * @throws NoSuchProviderException   if the current asymmetric JCE provider is incorrect
     * @throws NoSuchAlgorithmException  if a required algorithm is not available in the current asymmetric JCE provider
     */
    public static CertificateRequest makeCertificateRequest(CertGenParams certGenParams, KeyPair keyPair) throws SignatureException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        if (certGenParams.getSubjectDn() == null)
            throw new IllegalArgumentException("certGenParams must include a subject DN for the CSR");
        Provider sigProvider = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_CSR_SIGNING);
        X500Principal subject = certGenParams.getSubjectDn();
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String sigAlg = certGenParams.getSignatureAlgorithm();
        if (sigAlg == null)
            sigAlg = ParamsCertificateGenerator.getSigAlg(isUsingEcc(publicKey), sigProvider);

        // Generate request
        final PKCS10CertificationRequest certReq = sigProvider == null
                ? new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey, null)
                : new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey, sigProvider.getName());
        return new BouncyCastleCertificateRequest(certReq, publicKey);
    }
}
