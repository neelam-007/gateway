package com.l7tech.server;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.xml.SecurityTokenResolver;
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
    public X509Certificate lookupByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        return doLookup( issuer, serial, null, null );
    }

    @Override
    public X509Certificate lookupBySki( final String ski ) {
        return doLookup( null, null, ski, null );
    }

    @Override
    public X509Certificate lookup( final String thumbprint ) {
        return doLookup( null, null, null, thumbprint );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initiateLDAPGetters();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityProviderSecurityTokenResolver.class.getName());

    private final IdentityProviderFactory identityProviderFactory;
    private volatile Collection<Long> providers = Collections.emptyList();

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
        logger.fine("Rebuilding the list of identity providers that might contain certs");
        List<Long> tmp = new ArrayList<Long>();
        try {
            for (IdentityProvider provider : identityProviderFactory.findAllIdentityProviders()) {
                if (provider instanceof AuthenticatingIdentityProvider) {
                    tmp.add(provider.getConfig().getOid());
                }
            }

            providers = Collections.unmodifiableList(tmp);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error loading identity providers " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    private AuthenticatingIdentityProvider getProvider( final long oid ) {
        AuthenticatingIdentityProvider authProvider = null;

        try {
            IdentityProvider provider = identityProviderFactory.getProvider(oid);
            if ( provider instanceof AuthenticatingIdentityProvider ) {
                authProvider = (AuthenticatingIdentityProvider) provider;    
            }
        } catch ( FindException e ) {
            logger.log(Level.WARNING, "Error loading identity provider " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }

        return authProvider;
    }

    private X509Certificate doLookup( final X500Principal issuer, final BigInteger serial, final String ski, final String thumbprint ) {
        X509Certificate certificate = null;

        for (Long providerOid : providers) {
            AuthenticatingIdentityProvider provider = getProvider(providerOid);
            if ( provider != null ) {
                try {
                    if ( issuer != null && serial != null ) {
                        certificate = provider.findCertByIssuerAndSerial( issuer, serial );
                    } else if ( ski != null ) {
                        certificate = provider.findCertBySki( ski );
                    } else if ( thumbprint != null ) {
                        certificate = provider.findCertByThumbprintSHA1( thumbprint );
                    }

                    if ( certificate != null ) break;
                } catch (FindException e) {
                    logger.log(Level.WARNING,
                               String.format("Unable to find certs in %s: %s", provider.getConfig().getName(), ExceptionUtils.getMessage(e)),
                               ExceptionUtils.getDebugException(e));
                }
            }
        }

        return certificate;
    }
}
