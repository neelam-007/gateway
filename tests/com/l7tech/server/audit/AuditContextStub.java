/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditDetailMessage;

import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashSet;

/**
 * An AuditContext stub implementation. Does absolutely nothing except manage minimal internal state.
 */
public class AuditContextStub implements AuditContextStubInt {

    //- PUBLIC

    public AuditContextStub() {
        details = new LinkedHashSet<AuditDetail>();
    }

    public void setCurrentRecord(AuditRecord record) {
        this.record = record;
    }

    public void addDetail(AuditDetail detail, Object source) {
        details.add(detail);
    }

    public Set<AuditDetailMessage.Hint> getHints() {
        return Collections.emptySet();
    }

    public void flush() {
        if (record != null) {
            for (AuditDetail detail : details) {
                detail.setAuditRecord(record);
            }

            record.setDetails(details);
            lastRecord = record;
        }

        record = null;
        details = new LinkedHashSet<AuditDetail>();
    }

    public Map<Object, List<AuditDetail>> getDetails() {
        return Collections.emptyMap();
    }

    public AuditRecord getLastRecord() {
        return lastRecord;
    }

    //- PRIVATE

    private AuditRecord lastRecord;
    private AuditRecord record;
    private Set<AuditDetail> details;
}
