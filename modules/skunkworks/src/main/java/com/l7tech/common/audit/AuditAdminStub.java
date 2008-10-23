/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.audit;

import com.l7tech.util.OpaqueId;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.logging.SSGLogRecord;
import com.l7tech.gateway.common.cluster.ClusterProperty;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;

/**
 * @author mike
 */
public class AuditAdminStub implements AuditAdmin {
    public AuditAdminStub() {
        // TODO populate array of sample audit records
    }

    public AuditRecord findByPrimaryKey(long oid) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public Collection<AuditRecord> find( AuditSearchCriteria criteria) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public Collection<AuditRecordHeader> findHeaders( AuditSearchCriteria criteria) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public void deleteOldAuditRecords() {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public void doAuditArchive() {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public ClusterProperty getFtpAuditArchiveConfig() {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public void setFtpAuditArchiveConfig(ClusterProperty prop) throws UpdateException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public Collection<AuditRecord> findAuditRecords(String nodeid, Date startMsgDate, Date endMsgDate, int size) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public Date getLastAcknowledgedAuditDate() {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public Date markLastAcknowledgedAuditDate() {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public OpaqueId downloadAllAudits(long fromTime, long toTime, long[] serviceOids, int chunkSizeInBytes) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public DownloadChunk downloadNextChunk(OpaqueId context) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    public SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, Date startMsgDate, Date endMsgDate, int size) {
        return new SSGLogRecord[0];
    }

    public int getSystemLogRefresh(int typeId) {
        return 5;
    }

    public Level serverMessageAuditThreshold() {
        return Level.INFO;
    }

    public Level serverDetailAuditThreshold() {
        return Level.INFO;
    }

    public int serverMinimumPurgeAge() {
        return 168;
    }

    public void auditViewGatewayAuditsAction() {
    }

    public boolean hasNewAudits(Date date, Level level) {
        return false;  
    }
}