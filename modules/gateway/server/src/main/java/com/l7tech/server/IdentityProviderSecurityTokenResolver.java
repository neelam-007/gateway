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
public class IdentityProviderSecurityTokenResolver extends SecurityTokenResolverSupport implements SecurityTokenResolver, InitializingBean {

    //- PUBLIC

    public IdentityProviderSecurityTokenResolver( final IdentityProviderFactory identityProviderFactory  ) {
        if (identityProviderFactory == null) throw new IllegalArgumentException("IdentityProviderFactory is required");

        this.identityProviderFactory = identityProviderFactory;
    }

    @Override
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

        return certificate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initiateLDAPGetters();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityProviderSecurityTokenResolver.class.getName());

    private final IdentityProviderFactory identityProviderFactory;
    private volatile Collection<AuthenticatingIdentityProvider> providers = Collections.emptyList();

    private synchronized void initiateLDAPGetters() {
        buildAuthenticatingProviderList();

        Background.scheduleRepeated(new TimerTask() {
            @Override
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
