package com.l7tech.server;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SecurityTokenResolver that looks up certificates using an identity provider.
 *
 * <p>If the users certificate is not found then the resolution request is passed on to a delegate.</p>
 */
@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
public class IdentityProviderDelegatingSecurityTokenResolver implements SecurityTokenResolver, InitializingBean {

    //- PUBLIC

    public IdentityProviderDelegatingSecurityTokenResolver( final IdentityProviderFactory identityProviderFactory,
                                                            final SecurityTokenResolver resolver ) {
        if (identityProviderFactory == null) throw new IllegalArgumentException("IdentityProviderFactory is required");
        if (resolver == null) throw new IllegalArgumentException("SecurityTokenResolver is required");
        
        this.identityProviderFactory = identityProviderFactory;
        this.delegate = resolver;
    }

    public X509Certificate lookup(String thumbprint) {
        return delegate.lookup(thumbprint);
    }

    public X509Certificate lookupBySki(String ski) {
        return delegate.lookupBySki(ski);
    }

    public X509Certificate lookupByKeyName(String keyName) {
        return delegate.lookupByKeyName(keyName);
    }

    public X509Certificate lookupByIssuerAndSerial(X500Principal issuer, BigInteger serial) {
        X509Certificate certificate = null;

        for (AuthenticatingIdentityProvider provider : providers) {
            try {
                certificate = provider.findCertByIssuerAndSerial( issuer, serial );
            } catch (FindException e) {
                logger.log(Level.WARNING,
                           String.format("Unable to find certs in %s: %s", provider.getConfig().getName(), ExceptionUtils.getMessage(e)),
                           ExceptionUtils.getDebugException(e));
                continue;
            }
            if ( certificate != null ) break;            
        }

        if ( certificate == null ) {
            certificate = delegate.lookupByIssuerAndSerial(issuer, serial);
        }

        return certificate;
    }

    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        return delegate.lookupPrivateKeyByCert(cert);
    }

    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        return delegate.lookupPrivateKeyByX509Thumbprint(thumbprint);
    }

    public SignerInfo lookupPrivateKeyBySki(String ski) {
        return delegate.lookupPrivateKeyBySki(ski);
    }

    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        return delegate.lookupPrivateKeyByKeyName(keyName);
    }

    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        return delegate.getSecretKeyByEncryptedKeySha1(encryptedKeySha1);
    }

    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        delegate.putSecretKeyByEncryptedKeySha1(encryptedKeySha1, secretKey);
    }

    public KerberosSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        return delegate.getKerberosTokenBySha1(kerberosSha1);
    }

    public void afterPropertiesSet() throws Exception {
        initiateLDAPGetters();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityProviderDelegatingSecurityTokenResolver.class.getName());

    private final IdentityProviderFactory identityProviderFactory;
    private final SecurityTokenResolver delegate;
    private volatile Collection<AuthenticatingIdentityProvider> providers = Collections.emptyList();

    private synchronized void initiateLDAPGetters() {
        buildAuthenticatingProviderList();

        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                buildAuthenticatingProviderList();
            }
        }, 10000, 47533);
    }

    private void buildAuthenticatingProviderList() {
        logger.fine("Rebuilding the list of LDAP providers that might contain certs");
        List<AuthenticatingIdentityProvider> tmp = new ArrayList<AuthenticatingIdentityProvider>();
        try {
            for (IdentityProvider provider : identityProviderFactory.findAllIdentityProviders()) {
                if (provider instanceof AuthenticatingIdentityProvider) {
                    tmp.add((AuthenticatingIdentityProvider) provider);
                }
            }

            providers = Collections.unmodifiableList(tmp);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Problem getting ldap cert getters. " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }
}
