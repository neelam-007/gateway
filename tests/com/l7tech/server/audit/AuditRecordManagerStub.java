/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.objectmodel.*;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.audit.AuditRecord;

import java.util.Collection;

/**
 *
 * @author emil
 * @version Feb 17, 2005
 */
public class AuditRecordManagerStub extends EntityManagerStub<AuditRecord> implements AuditRecordManager {
    public Collection<AuditRecord> find(AuditSearchCriteria criteria) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void deleteOldAuditRecords() throws DeleteException {
        throw new UnsupportedOperationException();
    }

    public void update(AuditRecord entity) throws UpdateException {
        throw new UnsupportedOperationException();
    }
}
