package com.l7tech.security.xml;

import com.l7tech.security.token.KerberosSigningSecurityToken;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * Wraps an existing SecurityTokenResolver with a new one that delegates unknown resolutions to it.
 */
public class DelegatingSecurityTokenResolver implements SecurityTokenResolver {
    private final SecurityTokenResolver[] delegates;

    /**
     * Create a security token resolver that delegates to the specified resolvers in order.
     *
     * @param delegates delegate resolvers.  May be empty, although that would be pretty pointless
     */
    public DelegatingSecurityTokenResolver(SecurityTokenResolver... delegates) {
        this.delegates = delegates;
        if (delegates == null) throw new NullPointerException("delegates must not be null"); // can't happen
        for (SecurityTokenResolver delegate : delegates) {
            if (delegate == null)
                throw new IllegalArgumentException("delegate may not be null");
        }
    }

    @Override
    public X509Certificate lookup(String thumbprint) {
        for (SecurityTokenResolver delegate : delegates) {
            X509Certificate result = delegate.lookup(thumbprint);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public X509Certificate lookupBySki(String ski) {
        for (SecurityTokenResolver delegate : delegates) {
            X509Certificate result = delegate.lookupBySki(ski);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public X509Certificate lookupByKeyName(String keyName) {
        for (SecurityTokenResolver delegate : delegates) {
            X509Certificate result = delegate.lookupByKeyName(keyName);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public X509Certificate lookupByIssuerAndSerial( X500Principal issuer, BigInteger serial ) {
        for (SecurityTokenResolver delegate : delegates) {
            X509Certificate result = delegate.lookupByIssuerAndSerial( issuer, serial );
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyByCert(cert);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyByX509Thumbprint(thumbprint);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyBySki(String ski) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyBySki(ski);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyByKeyName(keyName);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public SignerInfo lookupPrivateKeyByIssuerAndSerial(X500Principal issuer, BigInteger serial) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyByIssuerAndSerial(issuer, serial);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        for (SecurityTokenResolver delegate : delegates) {
            byte[] result = delegate.getSecretKeyByEncryptedKeySha1(encryptedKeySha1);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        // Hmm, what do we do here?
        for (SecurityTokenResolver delegate : delegates)
            delegate.putSecretKeyByEncryptedKeySha1(encryptedKeySha1, secretKey);
    }

    @Override
    public KerberosSigningSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        for (SecurityTokenResolver delegate : delegates) {
            KerberosSigningSecurityToken result = delegate.getKerberosTokenBySha1(kerberosSha1);
            if (result != null) return result;
        }
        return null;
    }


    protected SecurityTokenResolver[] getDelegates() {
        return delegates;
    }
}
