/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.util.Config;

import java.util.Collection;
import java.util.Collections;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author emil
 * @version Feb 17, 2005
 */
public class AuditRecordManagerStub extends EntityManagerStub<AuditRecord,AuditRecordHeader> implements AuditRecordManager {

    @Override
    public Config getAuditValidatedConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, byte[]> getDigestForAuditRecords(Collection<Long> auditRecordIds) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException{
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteOldAuditRecords(long minAge) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(AuditRecord entity) throws UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int findCount(AuditSearchCriteria criteria) throws FindException {
        return 0;
    }

    @Override
    public Collection<AuditRecord> findPage(SortProperty sortProperty, boolean ascending, int offset, int count, AuditSearchCriteria criteria) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public long getMinOid(long oid) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMessageLimitSize() {
        return 0;
    }

    @Override
    public int deleteRangeByOid(long start, long end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMaxTableSpace() throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCurrentUsage() throws FindException {
        throw new UnsupportedOperationException();
    }
}
