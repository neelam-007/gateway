/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.server.audit.AuditContext;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditDetail;

/**
 * An AuditContext stub implementation. Does absolutely nothing except manage minimal internal state.
 */
public class AuditContextStub implements AuditContext {
    private boolean flushed;
    private boolean closed;

    public void setCurrentRecord(AuditRecord record) {
        if (closed) throw new IllegalStateException();
    }

    public void addDetail(AuditDetail detail) {
        if (closed) throw new IllegalStateException();
    }

    public void flush() {
        this.flushed = true;
    }

    public void close() {
        if (!flushed) flush();
        this.closed = true;
    }

    public boolean isFlushed() {
        return flushed;
    }

    public boolean isClosed() {
        return closed;
    }
}
