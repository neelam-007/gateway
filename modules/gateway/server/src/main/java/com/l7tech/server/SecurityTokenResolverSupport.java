package com.l7tech.server;

import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.token.KerberosSigningSecurityToken;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * Support class for implementing SecurityTokenResolvers.
 */
public class SecurityTokenResolverSupport implements SecurityTokenResolver {

    @Override
    public KerberosSigningSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        return null;
    }

    @Override
    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        return null;
    }

    @Override
    public X509Certificate lookup(String thumbprint) {
        return null;
    }

    @Override
    public X509Certificate lookupByIssuerAndSerial(X500Principal issuer, BigInteger serial) {
        return null;
    }

    @Override
    public X509Certificate lookupByKeyName(String keyName) {
        return null;
    }

    @Override
    public X509Certificate lookupBySki(String ski) {
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByIssuerAndSerial(X500Principal issuer, BigInteger serial) {
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyBySki(String ski) {
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        return null;
    }

    @Override
    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
    }
}
