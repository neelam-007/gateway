package com.l7tech.security.cert;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertificateGeneratorException;
import com.l7tech.common.io.ParamsCertificateGenerator;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.util.ConfigFactory;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Certificate utility methods that require static imports of Bouncy Castle classes.
 */
public class BouncyCastleCertUtils  {

    // true to set attrs to null (pre-6.0-2 behavior); false to set attrs to empty DERSet (Bug #10534)
    private static final boolean omitAttrs = ConfigFactory.getBooleanProperty( "com.l7tech.security.cert.csr.omitAttrs", false );

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
        ASN1Set attrs = omitAttrs ? null : new DERSet(new ASN1EncodableVector());
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String sigAlg = certGenParams.getSignatureAlgorithm();
        if (sigAlg == null)
            sigAlg = ParamsCertificateGenerator.getSigAlg(publicKey, certGenParams.getHashAlgorithm(), sigProvider);

        // Generate request
        final PKCS10CertificationRequest certReq = sigProvider == null
                ? new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey, null)
                : new PKCS10CertificationRequest(sigAlg, subject, publicKey, attrs, privateKey, sigProvider.getName());
        return new BouncyCastleCertificateRequest(certReq, publicKey);
    }

    /**
     * Generate a CertificateRequest using the specified Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     * @param provider provider to use for crypto operations, or null to use best preferences.
     * @return a new CertificateRequest instance.  Never null.
     * @throws java.security.InvalidKeyException  if a CSR cannot be created using the specified keypair
     * @throws java.security.SignatureException   if the CSR cannot be signed
     */
    public static CertificateRequest makeCertificateRequest(String username, KeyPair keyPair, Provider provider) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = omitAttrs ? null : new DERSet(new ASN1EncodableVector());
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        try {
            PKCS10CertificationRequest certReq = new PKCS10CertificationRequest(JceProvider.DEFAULT_CSR_SIG_ALG, subject, publicKey, attrs, privateKey, provider == null ? null : provider.getName());
            return new BouncyCastleCertificateRequest(certReq, publicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
