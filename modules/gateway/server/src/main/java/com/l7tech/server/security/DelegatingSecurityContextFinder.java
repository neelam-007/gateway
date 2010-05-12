package com.l7tech.server.security;

import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;

import java.util.Collection;
import java.util.Collections;

/**
 * SecurityContextFinder that delegates to a collection of underlying finders.
 *
 * <p>The security context identifiers should be prefixed for each underlying
 * finder to ensure that there is no duplication.</p>
 */
public class DelegatingSecurityContextFinder implements SecurityContextFinder {

    //- PUBLIC

    public DelegatingSecurityContextFinder( final Collection<SecurityContextFinder> securityContextFinders ) {
        this.securityContextFinders = Collections.unmodifiableCollection(securityContextFinders);
    }

    @Override
    public SecurityContext getSecurityContext( final String securityContextIdentifier ) {
        SecurityContext context = null;

        for ( final SecurityContextFinder finder : securityContextFinders ) {
            context = finder.getSecurityContext( securityContextIdentifier );
            if ( context != null ) break;
        }

        return context;
    }

    //- PRIVATE

    private final Collection<SecurityContextFinder> securityContextFinders;
}
