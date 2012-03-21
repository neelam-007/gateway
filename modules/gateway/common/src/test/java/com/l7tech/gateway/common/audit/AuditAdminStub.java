package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
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
    public AuditRecord findByPrimaryKey(long oid) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public List<AuditRecordHeader> findHeaders( AuditSearchCriteria criteria) throws FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public Map<Long, byte[]> getDigestsForAuditRecords(Collection<Long> auditRecordIds) throws FindException {
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
    public void deleteJdbcAuditSink(JdbcAuditSink entity) throws DeleteException, FindException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public Collection<JdbcAuditSink> findAllJdbcAuditSinks() throws FindException {
        return null;
    }

    @Override
    public long saveJdbcAuditSink(JdbcAuditSink entity) throws SaveException, UpdateException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public String createJdbcAuditSinkSchema(JdbcAuditSink sinkConfig) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public JobId<String> testJdbcAuditSink(JdbcAuditSink sinkConfig) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }

    @Override
    public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning) {
        throw new UnsupportedOperationException("Not supported in stub mode");
    }
}
