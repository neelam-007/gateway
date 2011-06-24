package com.l7tech.security.xml;

import org.jetbrains.annotations.NotNull;

import java.security.cert.X509Certificate;

/**
 * Extension of the SecurityTokenResolver interface for contextual resolution.
 */
public interface ContextualSecurityTokenResolver extends SecurityTokenResolver {

    /**
     * Look up a certificate by its local identifier.
     *
     * @param identifier the identifier to lookup.
     * @return the certificate that was found, or null if one was not found.
     */
    X509Certificate lookupByIdentifier( String identifier );

    class Support {
        /**
         * Utility method to promote a SecurityTokenResolver to a ContextualSecurityTokenResolver;
         *
         * @param securityTokenResolver The resolver to promote.
         * @return The contextual resolver
         */
        public static ContextualSecurityTokenResolver asContextualResolver( @NotNull final SecurityTokenResolver securityTokenResolver ) {
            return securityTokenResolver instanceof ContextualSecurityTokenResolver ?
                    (ContextualSecurityTokenResolver) securityTokenResolver :
                    new DelegatingContextualSecurityTokenResolver( securityTokenResolver );
        }

        /**
         * Contextual security token resolver that delegates non-contextual resolution.
         */
        public static class DelegatingContextualSecurityTokenResolver extends DelegatingSecurityTokenResolver implements ContextualSecurityTokenResolver {
            public DelegatingContextualSecurityTokenResolver( final SecurityTokenResolver securityTokenResolver ) {
                super( securityTokenResolver );
            }

            @Override
            public X509Certificate lookupByIdentifier( final String identifier ) {
                return null;
            }
        }
    }
}
