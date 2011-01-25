package com.l7tech.server.service.resolution;

import com.l7tech.server.audit.Auditor;

/**
 * Resolves services using case insensitive URI comparisons.
 */
public class CaseInsensitiveUriResolver extends UriResolver {
    public CaseInsensitiveUriResolver( final Auditor.AuditorFactory auditorFactory ) {
        super( auditorFactory, false );
    }
}
