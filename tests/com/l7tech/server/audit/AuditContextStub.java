/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.AuditRecord;

/**
 * An AuditContext stub implementation. Does absolutely nothing except manage minimal internal state.
 */
public class AuditContextStub implements AuditContext {
    public void setCurrentRecord(AuditRecord record) {
    }

    public void addDetail(AuditDetail detail) {
    }

    public void flush() {
    }
}
