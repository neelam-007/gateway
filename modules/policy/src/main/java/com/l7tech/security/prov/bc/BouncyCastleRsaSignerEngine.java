/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.bc;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.X509GeneralName;
import com.l7tech.common.io.ParamsCertificateGenerator;
import com.l7tech.security.cert.BouncyCastleCertUtils;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.logging.Level;
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

    private final PrivateKey caPrivateKey;
    private final X509Certificate caCert;
    private final Provider signatureProvider;

    /**
     * Constructor for the RsaCertificateSigner object sets all fields to their most common usage using
     * the specified CA key and cert, and using the specified JCE provider for crypto operations.
     *
     * @param caPrivateKey  PrivateKey to use when signing certs.  Required.
     * @param caCert        Certificate to use when signing certs.  Required.  No need for an entire chain here
     *                      since we do not support intermediate CA certs.
     * @param signatureProvider Provider to use for Signature instances for verifying CSRs or signing certificates, or null to use current most-preference provider.
     */
    public BouncyCastleRsaSignerEngine(PrivateKey caPrivateKey, X509Certificate caCert, Provider signatureProvider) {
        this.caPrivateKey = caPrivateKey;
        this.caCert = caCert;
        this.signatureProvider = signatureProvider;
    }

    @Override
    public Certificate createCertificate(byte[] pkcs10req, CertGenParams certGenParams) throws Exception {
        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(pkcs10req);
        CertificationRequestInfo certReqInfo = pkcs10.toASN1Structure().getCertificationRequestInfo();

        if (certGenParams == null)
            certGenParams = new CertGenParams();

        if (certGenParams.getSubjectDn() == null)
            certGenParams.setSubjectDn(new X500Principal(certReqInfo.getSubject().getEncoded()));
        //extract SANs from the certReqInfo
        if(certGenParams.getSubjectAlternativeNames() == null) {
            logger.log(Level.FINE,"Extracting SANs from the CSR info...");
            List<X509GeneralName> sANs = BouncyCastleCertUtils.extractSubjectAlternativeNamesFromCsrInfoAttr(certReqInfo.getAttributes());
            if(!sANs.isEmpty()) {
                certGenParams.setIncludeSubjectAlternativeName(true);
                certGenParams.setSubjectAlternativeNames(sANs);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(sANs.size() + " SANs are added to the certificate");
                }
            }
        }
        logger.info("Signing cert for subject DN = " + certGenParams.getSubjectDn());

        PublicKey publicKey = getPublicKey(pkcs10);
        ContentVerifierProvider verifierProvider;
        if (signatureProvider == null) {
            verifierProvider =  new JcaContentVerifierProviderBuilder().build(publicKey);
        } else {
            verifierProvider =  new JcaContentVerifierProviderBuilder().setProvider(signatureProvider).build(publicKey);
        }

        if (!pkcs10.isSignatureValid(verifierProvider)) {
            logger.severe("POPO verification failed for " + certGenParams.getSubjectDn());
            throw new Exception("Verification of signature (popo) on PKCS10 request failed.");
        }

        X509Certificate cert = new ParamsCertificateGenerator(certGenParams).generateCertificate(publicKey, caPrivateKey, caCert);

        // Verify before returning
        // Convert to Sun cert first so BC won't screw us over by asking for some goofy BC-only algorithm names
        cert = (X509Certificate)CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
        if (signatureProvider == null) {
            cert.verify(caCert.getPublicKey());
        } else {
            cert.verify(caCert.getPublicKey(), signatureProvider.getName());
        }
        return cert;
    }

    public static PublicKey getPublicKey(PKCS10CertificationRequest pkcs10) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        CertificationRequestInfo reqInfo = pkcs10.toASN1Structure().getCertificationRequestInfo();
        SubjectPublicKeyInfo subjectPKInfo = reqInfo.getSubjectPublicKeyInfo();
        X509EncodedKeySpec xspec = new X509EncodedKeySpec(new DERBitString(subjectPKInfo).getBytes());
        AlgorithmIdentifier keyAlg = subjectPKInfo.getAlgorithm();

        String alg = keyAlg.getAlgorithm().getId();
        if (PKCSObjectIdentifiers.rsaEncryption.getId().equals(alg))
            alg = "RSA";
        else if (X9ObjectIdentifiers.id_dsa.getId().equals(alg))
            alg = "DSA";
        else if (X9ObjectIdentifiers.ecdsa_with_SHA1.getId().equals(alg) ||
                 X9ObjectIdentifiers.ellipticCurve.getId().equals(alg) ||
                 X9ObjectIdentifiers.id_ecPublicKey.getId().equals(alg))
            alg = "EC";

        return JceProvider.getInstance().getKeyFactory(alg).generatePublic(xspec);
    }
}