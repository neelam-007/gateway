/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditDetailMessage;

import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.List;

/**
 * An AuditContext stub implementation. Does absolutely nothing except manage minimal internal state.
 */
public class AuditContextStub implements AuditContext {
    public void setCurrentRecord(AuditRecord record) {
    }

    public void addDetail(AuditDetail detail, Object source) {
    }

    public Set<AuditDetailMessage.Hint> getHints() {
        return Collections.emptySet();
    }

    public void flush() {
    }

    public Map<Object, List<AuditDetail>> getDetails() {
        return Collections.emptyMap();
    }
}
