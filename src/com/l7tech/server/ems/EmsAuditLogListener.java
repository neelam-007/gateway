package com.l7tech.server.ems;

import com.l7tech.server.audit.AuditLogListener;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.AuditRecord;

/**
 * EMS implementation of AuditLogListener.
 * Currently this class does nothing.
 */
public class EmsAuditLogListener implements AuditLogListener {
    public void notifyDetailCreated(String source, AuditDetailMessage message, String[] params, Throwable thrown) {
    }

    public void notifyDetailFlushed(String source, AuditDetailMessage message, String[] params, Throwable thrown) {
    }

    public void notifyRecordFlushed(AuditRecord record, boolean header) {
    }
}
