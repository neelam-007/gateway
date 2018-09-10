package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.SecurityTokenResolverSupport;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 3-Feb-2009
 * Time: 12:42:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class DummySecurityTokenResolver extends SecurityTokenResolverSupport {
    private X509Certificate certificate;
    private PrivateKey privateKey;

    public DummySecurityTokenResolver(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    public X509Certificate lookup(String thumbprint) {
        return null;
    }

    public X509Certificate lookupBySki(final String ski) {
        try {
            final SubjectKeyIdentifier subjectKeyIdentifier = new JcaX509ExtensionUtils()
                    .createSubjectKeyIdentifier(certificate.getPublicKey());
            final String certSki = new String(
                    Base64.encodeBase64(subjectKeyIdentifier.getKeyIdentifier(), false), "UTF-8");
            if(ski.equals(certSki)) {
                return certificate;
            }
        } catch(Exception e) {
        }
        return null;
    }

    public X509Certificate lookupByKeyName(String keyName) {
        return null;
    }

    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        if(certificate.equals(cert)) {
            return new SignerInfo(privateKey, new X509Certificate[] {certificate});
        }
        return null;
    }

    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        return null;
    }

    public SignerInfo lookupPrivateKeyBySki(final String ski) {
        try {
            final SubjectKeyIdentifier subjectKeyIdentifier = new JcaX509ExtensionUtils()
                    .createSubjectKeyIdentifier(certificate.getPublicKey());
            final String certSki = new String(
                    Base64.encodeBase64(subjectKeyIdentifier.getKeyIdentifier(), false), "UTF-8");
            if(ski.equals(certSki)) {
                return new SignerInfo(privateKey, new X509Certificate[] {certificate});
            }
        } catch(Exception e) {
        }
        return null;
    }

    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        return null;
    }

    public X509Certificate lookupByIssuerAndSerial( X500Principal issuer, BigInteger serial ) {
        return null;
    }

    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        return null;
    }

    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
    }

    public KerberosSigningSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        return null;
    }
}
