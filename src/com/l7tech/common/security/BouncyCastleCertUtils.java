package com.l7tech.common.security;

import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.*;
import java.math.BigInteger;
import java.util.Random;
import java.util.Calendar;

import com.l7tech.common.security.prov.bc.BouncyCastleCertificateRequest;

/**
 * Certificate utility methods that require static imports of Bouncy Castle classes.
 */
public class BouncyCastleCertUtils {

    /**
     * Generate a self-signed certificate from the specified KeyPair, which is expected to be RSA.
     * The certificate will use the SHA1withRSA signature algorithm.
     *
     * @param dn            the DN for the new self-signed cert.  Required.
     * @param expiryDays    number of days before the cert should expire.  Required.
     * @param keyPair       an RSA key pair to use for the certificate.  Required.
     * @return the new self-signed certificate.  Never null.
     * @throws CertificateEncodingException  if there is a problem producing the new cert
     * @throws NoSuchAlgorithmException      if a required crypto algorithm is unavailable
     * @throws SignatureException            if there is a problem signing the new cert
     * @throws InvalidKeyException           if there is a problem with the provided key pair           
     */
    public static X509Certificate generateSelfSignedCertificate(LdapName dn, int expiryDays, KeyPair keyPair) throws CertificateEncodingException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        X500Principal dnName = new X500Principal(dn.toString());

        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(new Random().nextInt(2000000) + 1));
        certGen.setIssuerDN(dnName);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        certGen.setNotBefore(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, expiryDays);
        certGen.setNotAfter(cal.getTime());
        certGen.setSubjectDN(dnName);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA1withRSA");

        return certGen.generate(keyPair.getPrivate());
    }

    /**
     * Create a PKCS#10 certification request using the specified DN and key pair.
     *
     * @param dn  the DN for the new CSR.  Required.
     * @param keyPair  an RSA key pair to use for the CSR.  Required.
     * @return a new PKCS#10 certification request including the specified DN and public key, signed with the
     *         specified private key.  Never null.
     * @throws SignatureException        if there is a problem signing the cert
     * @throws InvalidKeyException       if there is a problem with the provided key pair
     * @throws NoSuchProviderException   if the current asymmetric JCE provider is incorrect
     * @throws NoSuchAlgorithmException  if a required algorithm is not available in the current asymmetric JCE provider
     */
    public static CertificateRequest makeCertificateRequest(LdapName dn, KeyPair keyPair) throws SignatureException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        X509Name subject = new X509Name(dn.toString());
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        PKCS10CertificationRequest certReq = null;
        certReq = new PKCS10CertificationRequest("SHA1withRSA", subject, publicKey, attrs, privateKey);
        return new BouncyCastleCertificateRequest(certReq, JceProvider.getAsymmetricJceProvider().getName());
    }
}
