package com.l7tech.server.event.admin;

import com.l7tech.server.audit.AdminAuditConstants;
import org.springframework.context.ApplicationEvent;

import java.util.logging.Level;

/**
 * Implementations are events in the lifecycle of a {@link com.l7tech.objectmodel.GoidEntity}.
 *
 * @author alex
 */
public abstract class AdminEvent extends ApplicationEvent {
    protected static final int MESSAGE_MAX_LENGTH = 255;
    private final Level level;
    private boolean auditIgnore;
    protected String note;

    public AdminEvent( final Object source ) {
        this( source, null, AdminAuditConstants.DEFAULT_LEVEL );
    }

    public AdminEvent( final Object source,
                       final String note ) {
        this( source, note, AdminAuditConstants.DEFAULT_LEVEL );
    }

    public AdminEvent( final Object source,
                       final String note,
                       final Level level ) {
        super( source );
        this.note = note;
        this.level = level;
    }

    public String getNote() {
        return note;
    }

    public Level getMinimumLevel() {
        return level;
    }

    /**
     * @return true if the audit listener should ignore this event.
     */
    public boolean isAuditIgnore() {
        return auditIgnore;
    }

    /**
     * Set this as a "system" event.  Events flagged as system events do not get audited.
     * The PersistenceEventInterceptor sets this flag on certain events to suppress excess auditing.
     *
     * @param auditIgnore  true if the audit listener should ignore this event.
     */
    public void setAuditIgnore(boolean auditIgnore) {
        this.auditIgnore = auditIgnore;
    }
}
