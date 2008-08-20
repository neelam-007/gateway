/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.server.EntityManagerStub;

import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author emil
 * @version Feb 17, 2005
 */
public class AuditRecordManagerStub extends EntityManagerStub<AuditRecord,EntityHeader> implements AuditRecordManager {
    public Collection<AuditRecord> find(AuditSearchCriteria criteria) throws FindException {
        return Collections.emptyList();
    }

    public void deleteOldAuditRecords() throws DeleteException {
        throw new UnsupportedOperationException();
    }

    public void update(AuditRecord entity) throws UpdateException {
        throw new UnsupportedOperationException();
    }

    public int findCount(AuditSearchCriteria criteria) throws FindException {
        return 0;
    }

    public Collection<AuditRecord> findPage(SortProperty sortProperty, boolean ascending, int offset, int count, AuditSearchCriteria criteria) throws FindException {
        return Collections.emptyList();
    }
}
