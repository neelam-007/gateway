package com.l7tech.security.xml;

import com.l7tech.security.token.KerberosSecurityToken;

import java.security.cert.X509Certificate;

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

    public X509Certificate lookup(String thumbprint) {
        for (SecurityTokenResolver delegate : delegates) {
            X509Certificate result = delegate.lookup(thumbprint);
            if (result != null) return result;
        }
        return null;
    }

    public X509Certificate lookupBySki(String ski) {
        for (SecurityTokenResolver delegate : delegates) {
            X509Certificate result = delegate.lookupBySki(ski);
            if (result != null) return result;
        }
        return null;
    }

    public X509Certificate lookupByKeyName(String keyName) {
        for (SecurityTokenResolver delegate : delegates) {
            X509Certificate result = delegate.lookupByKeyName(keyName);
            if (result != null) return result;
        }
        return null;
    }

    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyByCert(cert);
            if (result != null) return result;
        }
        return null;
    }

    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyByX509Thumbprint(thumbprint);
            if (result != null) return result;
        }
        return null;
    }

    public SignerInfo lookupPrivateKeyBySki(String ski) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyBySki(ski);
            if (result != null) return result;
        }
        return null;
    }

    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        for (SecurityTokenResolver delegate : delegates) {
            SignerInfo result = delegate.lookupPrivateKeyByKeyName(keyName);
            if (result != null) return result;
        }
        return null;
    }

    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        for (SecurityTokenResolver delegate : delegates) {
            byte[] result = delegate.getSecretKeyByEncryptedKeySha1(encryptedKeySha1);
            if (result != null) return result;
        }
        return null;
    }

    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        // Hmm, what do we do here?
        for (SecurityTokenResolver delegate : delegates)
            delegate.putSecretKeyByEncryptedKeySha1(encryptedKeySha1, secretKey);
    }

    public KerberosSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        for (SecurityTokenResolver delegate : delegates) {
            KerberosSecurityToken result = delegate.getKerberosTokenBySha1(kerberosSha1);
            if (result != null) return result;
        }
        return null;
    }


    protected SecurityTokenResolver[] getDelegates() {
        return delegates;
    }
}
