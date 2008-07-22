package com.l7tech.server.event.admin;

import java.util.logging.Level;

/**
 * Audit event for revocation of gateway user certificates.
 */
public class AuditRevokeAllUserCertificates extends AdminEvent {
    public AuditRevokeAllUserCertificates(Object source, int revokedCount) {
        super(source, "Revoked all user certificates ("+revokedCount+").");
    }

    public Level getMinimumLevel() {
        return Level.WARNING;
    }
}
