package com.l7tech.gateway.common.audit;

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

    @Override
    public AuditRecord findByPrimaryKey(long oid) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public Collection<AuditRecord> find( AuditSearchCriteria criteria) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public Collection<AuditRecordHeader> findHeaders( AuditSearchCriteria criteria) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public void deleteOldAuditRecords() {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public void doAuditArchive() {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    @Override
    public ClusterProperty getFtpAuditArchiveConfig() {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    @Override
    public void setFtpAuditArchiveConfig(ClusterProperty prop) throws UpdateException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    @Override
    public Collection<AuditRecord> findAuditRecords(String nodeid, Date startMsgDate, Date endMsgDate, int size) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public Date getLastAcknowledgedAuditDate() {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public Date markLastAcknowledgedAuditDate() {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public OpaqueId downloadAllAudits(long fromTime, long toTime, long[] serviceOids, int chunkSizeInBytes) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public DownloadChunk downloadNextChunk(OpaqueId context) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, Date startMsgDate, Date endMsgDate, int size) {
        return new SSGLogRecord[0];
    }

    @Override
    public int getSystemLogRefresh(int typeId) {
        return 5;
    }

    @Override
    public Level serverMessageAuditThreshold() {
        return Level.INFO;
    }

    @Override
    public Level serverDetailAuditThreshold() {
        return Level.INFO;
    }

    @Override
    public int serverMinimumPurgeAge() {
        return 168;
    }

    @Override
    public boolean isSigningEnabled() throws FindException {
        return false;
    }

    @Override
    public long hasNewAudits(Date date, Level level) {
        return 0;
    }

    @Override
    public boolean isAuditViewerPolicyAvailable() {
        return false;
    }

    @Override
    public String invokeAuditViewerPolicyForMessage(long auditId, boolean isRequest) {
        return null;
    }

    @Override
    public String invokeAuditViewerPolicyForDetail(long auditId, long detailMessageId) {
        return null;
    }
}