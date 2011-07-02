package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.AuditFactory;

/**
 * Resolves services using case insensitive URI comparisons.
 */
public class CaseInsensitiveUriResolver extends UriResolver {
    public CaseInsensitiveUriResolver( final AuditFactory auditorFactory ) {
        super( auditorFactory, false );
    }
}
