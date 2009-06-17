/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.bc;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Uses the Bouncy Castle library to process PKCS#10 certificate requests and create signed
 * certificates.
 * <p/>
 * Although this uses Bouncy Castle for ASN.1, PKCS#10, and X.509 parsing and generation,
 * the underlying crypto implementation does not necessarily have to be Bouncy Castle.
 * The provider name is specified when the instance is constructed.
 */
public class BouncyCastleRsaSignerEngine implements RsaSignerEngine {
    private static final Logger logger = Logger.getLogger(BouncyCastleRsaSignerEngine.class.getName());

    static {
        // Make sure our provider is installed.  Probably unnecessary.
        JceProvider.init();
    }

    private PrivateKey caPrivateKey;
    private X509Certificate caCert;
    private static final SecureRandom random = new SecureRandom();
    private final String signatureProvider;

    /**
     * Constructor for the RsaCertificateSigner object sets all fields to their most common usage using
     * the specified CA key and cert, and using the specified JCE provider for crypto operations.
     *
     * @param caPrivateKey  PrivateKey to use when signing certs.  Required.
     * @param caCert        Certificate to use when signing certs.  Required.  No need for an entire chain here
     *                      since we do not support intermediate CA certs.
     * @param signatureProvider  name of JCE provider implementation to use for Signature algorithm.
     * @param rsaProvider  name of JCE provider implementation to use for RSA public key extraction.
     */
    public BouncyCastleRsaSignerEngine(PrivateKey caPrivateKey, X509Certificate caCert, String signatureProvider, String rsaProvider) {
        this.caPrivateKey = caPrivateKey;
        this.caCert = caCert;
        this.signatureProvider = signatureProvider;
    }

    public static boolean isUsingEcc(PublicKey publicKey) {
        String publicKeyAlg = publicKey.getAlgorithm();
        return "EC".equalsIgnoreCase(publicKeyAlg) || "ECDSA".equalsIgnoreCase(publicKeyAlg);
    }

    // Find the best sig alg available on the current signature provider for the specified algorithm (EC=true, RSA=false)
    public static String getSigAlg(boolean usingEcc) {
        String strongSigAlg = usingEcc ? "SHA384withECDSA" : "SHA384withRSA";
        String weakSigAlg = usingEcc ? "SHA1withECDSA" : "SHA1withRSA";
        String sigAlg = strongSigAlg;

        try {
            Signature.getInstance(strongSigAlg, JceProvider.getSignatureProvider());
        } catch (NoSuchAlgorithmException e) {
            // Not available; fall back to weak sig alg
            sigAlg = weakSigAlg;
        }
        return sigAlg;
    }

    private static X509V3CertificateGenerator makeCertGenerator( X509Name subject, Date expiration,
                                                                 PublicKey publicKey, CertType type )
            throws InvalidKeyException
    {
        Calendar cal = Calendar.getInstance();
        // Set back startdate ten minutes to avoid some problems with wrongly set clocks.
        cal.add(Calendar.MINUTE, -10);
        Date firstDate = cal.getTime();

        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();
        byte[] serno = new byte[8];
        random.nextBytes(serno);
        certgen.setSerialNumber((new BigInteger(serno)).abs());
        certgen.setNotBefore(firstDate);
        certgen.setNotAfter(expiration);
        certgen.setSignatureAlgorithm(getSigAlg(isUsingEcc(publicKey)));

        certgen.setSubjectDN(subject);
        certgen.setPublicKey(publicKey);

        BasicConstraints bc = type == CertType.CA ? new BasicConstraints(0) : new BasicConstraints(false);
        certgen.addExtension(X509Extensions.BasicConstraints.getId(), true, bc);

        // Set key usage (signing only for CA certs)
        int usage = 0;
        if ( type == CertType.CA ) {
            usage |= X509KeyUsage.keyCertSign;
        } else {
            usage |= X509KeyUsage.digitalSignature;
            usage |= X509KeyUsage.keyEncipherment;
            usage |= X509KeyUsage.nonRepudiation;
        }

        certgen.addExtension(X509Extensions.KeyUsage, true, new X509KeyUsage(usage));

        // Add subject public key info for fingerprint (not critical)
        final SubjectPublicKeyInfo spki;
        try {
            spki = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(new ASN1InputStream(new ByteArrayInputStream(publicKey.getEncoded())).readObject()));
        } catch ( IOException e ) {
            throw new InvalidKeyException(e);
        }
        SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);
        certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), false, ski);

        return certgen;
    }

    /**
     * Creates and signs a self-signed {@link X509Certificate}.
     * @param subjectDn The distinguished name of the certificate's subject (usually 'cn=hostname')
     * @param validity The period during which the cert should be valid, in days.
     * @param keypair The keypair belonging to the certificate's subject.
     * @return The self-signed {@link X509Certificate}.
     * @throws SignatureException if the certificate cannot be found
     * @throws InvalidKeyException if all or part of the keypair is invalid
     */
    public static X509Certificate makeSelfSignedRootCertificate( String subjectDn, int validity, KeyPair keypair )
            throws SignatureException, InvalidKeyException
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, validity);
        Date lastDate = cal.getTime();
        X509Name subject = new X509Name(subjectDn);
        X509V3CertificateGenerator certgen = makeCertGenerator(subject, lastDate, keypair.getPublic(), CertType.CA);

        // Self-signed, issuer == subject
        certgen.setIssuerDN(subject);

        try {
            return certgen.generate(keypair.getPrivate(), JceProvider.getAsymmetricJceProvider().getName());
        } catch (NoSuchProviderException e) {
            throw new SignatureException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        } catch (CertificateEncodingException e) {
            throw new SignatureException(e);
        }
    }

    public static X509Certificate makeSignedCertificate(String subjectDn, Date expiration,
                                                        PublicKey subjectPublicKey,
                                                        X509Certificate caCert, PrivateKey caKey, CertType type)
            throws IOException, SignatureException, InvalidKeyException {
        X509Name subject = new X509Name(subjectDn);
        X509V3CertificateGenerator certgen = makeCertGenerator(subject, expiration, subjectPublicKey, type);
        certgen.setIssuerDN(new X509Name(caCert.getSubjectDN().getName()));

        // Add authority key info (fingerprint)
        SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(new ASN1InputStream(new ByteArrayInputStream(caCert.getPublicKey().getEncoded())).readObject()));
        AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
        certgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), false, aki);

        try {
            return certgen.generate(caKey, JceProvider.getSignatureProvider().getName());
        } catch (NoSuchProviderException e) {
            throw new SignatureException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        } catch (CertificateEncodingException e) {
            throw new SignatureException(e);
        }
    }

    public static X509Certificate makeSignedCertificate(String subjectDn, int validity,
                                                        PublicKey subjectPublicKey,
                                                        X509Certificate caCert, PrivateKey caKey, CertType type)
            throws IOException, SignatureException, InvalidKeyException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, validity);
        Date lastDate = cal.getTime();
        return makeSignedCertificate(subjectDn, lastDate, subjectPublicKey, caCert, caKey, type);
    }

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @param subject the subject for the cert, if null, use the subject contained in the csr
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    public Certificate createCertificate(byte[] pkcs10req, String subject) throws Exception {
        return createCertificate(pkcs10req, subject, -1);
    }

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @param expiration the desired expiration date of the cert, -1 to fallback on default
     * @param subject the subject for the cert, if null, use the subject contained in the csr
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    public Certificate createCertificate(byte[] pkcs10req, String subject, long expiration) throws Exception {
        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(pkcs10req);
        CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();
        String dn = subject;
        if (dn == null) {
            dn = certReqInfo.getSubject().toString();
        }
        logger.info("Signing cert for subject DN = " + dn);
        PublicKey publicKey = getPublicKey(pkcs10);
        if (!pkcs10.verify(publicKey, signatureProvider)) {
            logger.severe("POPO verification failed for " + dn);
            throw new Exception("Verification of signature (popo) on PKCS10 request failed.");
        }
        X509Certificate cert;
        if (expiration == -1) {
            cert = makeSignedCertificate(dn, CERT_DAYS_VALID, publicKey, caCert, caPrivateKey, CertType.CLIENT);
        } else {
            cert = makeSignedCertificate(dn, new Date(expiration), publicKey, caCert, caPrivateKey, CertType.CLIENT);
        }
        // Verify before returning
        // Convert to Sun cert first so BC won't screw us over by asking for some goofy BC-only algorithm names
        cert = (X509Certificate)CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
        cert.verify(caCert.getPublicKey(), signatureProvider);
        return cert;
    }


    public static PublicKey getPublicKey(PKCS10CertificationRequest pkcs10) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
        CertificationRequestInfo reqInfo = pkcs10.getCertificationRequestInfo();
        SubjectPublicKeyInfo subjectPKInfo = reqInfo.getSubjectPublicKeyInfo();
        X509EncodedKeySpec xspec = new X509EncodedKeySpec(new DERBitString(subjectPKInfo).getBytes());
        AlgorithmIdentifier keyAlg = subjectPKInfo.getAlgorithmId();

        String alg = keyAlg.getObjectId().getId();
        if (PKCSObjectIdentifiers.rsaEncryption.getId().equals(alg))
            alg = "RSA";
        else if (X9ObjectIdentifiers.id_dsa.getId().equals(alg))
            alg = "DSA";
        else if (X9ObjectIdentifiers.ecdsa_with_SHA1.getId().equals(alg) ||
                 X9ObjectIdentifiers.ellipticCurve.equals(alg) ||
                 X9ObjectIdentifiers.id_ecPublicKey.getId().equals(alg))
            alg = "EC";

        return KeyFactory.getInstance(alg).generatePublic(xspec);
    }
}
