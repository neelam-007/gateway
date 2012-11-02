package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.util.OpaqueId;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.cluster.ClusterProperty;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author mike
 */
public class AuditAdminStub implements AuditAdmin {
    public AuditAdminStub() {
        // TODO populate array of sample audit records
    }

    @Override
    public AuditRecord findByPrimaryKey(String id, boolean fromInternal) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public List<AuditRecordHeader> findHeaders( AuditSearchCriteria criteria) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public Map<String, byte[]> getDigestsForAuditRecords(Collection<String> auditRecordIds, boolean fromPolicy) throws FindException {
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
    public boolean isAuditArchiveEnabled() {
        return true;
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
    public int getSystemLogRefresh() {
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
    public int getMaxDigestRecords() {
        return 0;
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
    public String invokeAuditViewerPolicyForDetail(long auditId, long ordinal) {
        return null;
    }

    @Override
    public Collection<String> getAllEntityClassNames() {
        return null;
    }

    @Override
    public String getExternalAuditsSchema(String connectionName, String auditRecordTableName, String auditDetailTableName) {
        return null;
    }

    @Override
    public AsyncAdminMethods.JobId<String> testAuditSinkSchema(String connectionName, String auditRecordTableName, String auditDetailTableName) {
        return null;
    }

    @Override
    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
        return null;
    }

    @Override
    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
        return null;
    }

    @Override
    public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning) {
        // do nothing
    }

    @Override
    public JobId<String> createExternalAuditDatabaseTables(String connectionName, String auditRecordTableName, String auditDetailTableName, String userName, String password) {
        return null;
    }
}
