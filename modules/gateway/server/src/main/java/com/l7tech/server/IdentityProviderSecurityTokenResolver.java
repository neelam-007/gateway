package com.l7tech.server;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
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
    public X509Certificate lookupByKeyName(String keyName) {
        final X500Principal dn;
        try {
            dn = new X500Principal(keyName);
        } catch (IllegalArgumentException e) {
            logger.log(Level.FINE, "keyName not a valid DN -- assuming no match");
            return null;
        }
        return doLookup( null, null, null, null, dn);
    }

    @Override
    public X509Certificate lookupByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        return doLookup( issuer, serial, null, null, null );
    }

    @Override
    public X509Certificate lookupBySki( final String ski ) {
        return doLookup( null, null, ski, null, null );
    }

    @Override
    public X509Certificate lookup( final String thumbprint ) {
        return doLookup( null, null, null, thumbprint, null );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initiateLDAPGetters();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityProviderSecurityTokenResolver.class.getName());

    private final IdentityProviderFactory identityProviderFactory;
    private volatile Collection<Goid> providers = Collections.emptyList();

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
        List<Goid> tmp = new ArrayList<Goid>();
        try {
            for (IdentityProvider provider : identityProviderFactory.findAllIdentityProviders()) {
                if (provider instanceof AuthenticatingIdentityProvider) {
                    tmp.add(provider.getConfig().getGoid());
                }
            }

            providers = Collections.unmodifiableList(tmp);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error loading identity providers " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    private AuthenticatingIdentityProvider getProvider( final Goid oid ) {
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

    private X509Certificate doLookup( @Nullable final X500Principal issuer, @Nullable final BigInteger serial, @Nullable final String ski, @Nullable final String thumbprint, @Nullable final X500Principal subject ) {
        X509Certificate certificate = null;

        for (Goid providerOid : providers) {
            AuthenticatingIdentityProvider provider = getProvider(providerOid);
            if ( provider != null ) {
                try {
                    if ( issuer != null && serial != null ) {
                        certificate = provider.findCertByIssuerAndSerial( issuer, serial );
                    } else if ( ski != null ) {
                        certificate = provider.findCertBySki( ski );
                    } else if ( thumbprint != null ) {
                        certificate = provider.findCertByThumbprintSHA1( thumbprint );
                    } else if ( subject != null ) {
                        certificate = provider.findCertBySubjectDn( subject );
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
