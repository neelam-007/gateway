package com.l7tech.gateway.common.audit;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

import java.util.logging.Level;
import java.util.Arrays;

/**
 * Used for audit retrieval
 */
public class AuditRecordHeader extends EntityHeader {

    private String signature;
    private Pair<byte[],byte[]> signatureDigest;
    private String nodeId;
    private long timestamp;
    private Level level;

    public AuditRecordHeader(final AuditRecord auditRecord){
        this( auditRecord.getGoid(),
              auditRecord instanceof MessageSummaryAuditRecord ? auditRecord.getName() : "",
              auditRecord.getMessage(),
              null,
              auditRecord.getSignature(),
              auditRecord.getNodeId(),
              auditRecord.getMillis(),
              auditRecord.getLevel(),
              auditRecord.getVersion() );
    }

    public AuditRecordHeader(final AuditRecordHeader auditRecordHeader) {
        this( auditRecordHeader.getGoid(),
              auditRecordHeader.getName(),
              auditRecordHeader.getDescription(),
              auditRecordHeader.getSignatureDigest(),
              auditRecordHeader.getSignature(),
              auditRecordHeader.getNodeId(),
              auditRecordHeader.getTimestamp(),
              auditRecordHeader.getLevel(),
              auditRecordHeader.getVersion() );
    }

    //todo: version should not be required.
    public AuditRecordHeader(Goid id, String name, String description, Pair<byte[],byte[]> signatureDigest, String signature, String nodeId, long timestamp, Level level, int version) {
        super(id, EntityType.AUDIT_RECORD, name, description, version);
        this.signatureDigest = signatureDigest;
        this.signature = signature;
        this.nodeId = nodeId;
        this.timestamp = timestamp;
        this.level = level;
    }

    public Pair<byte[],byte[]> getSignatureDigest() {
        return signatureDigest;
    }

    public void setSignatureDigest(Pair<byte[],byte[]> signatureDigest) {
        this.signatureDigest = signatureDigest;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getNodeId() {
        return nodeId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getServiceName() {
        return super.getName();
    }

    public String getMessage() {
        return super.getDescription();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final AuditRecordHeader that = (AuditRecordHeader) o;

        if(signatureDigest != null ? !Arrays.equals(signatureDigest.right, that.signatureDigest.right) || !Arrays.equals(signatureDigest.left, that.signatureDigest.left) : that.getSignatureDigest() != null) return false;
        if(signature != null ? !signature.equals(that.getSignature()) : that.getSignature() != null) return false;
        if (nodeId != null ? !nodeId.equals(that.getNodeId()) : that.getNodeId() != null) return false;
        if (timestamp != that.getTimestamp()) return false;
        if (level != null ? !level.equals(that.getLevel()) : that.getLevel() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (signatureDigest != null ? Arrays.hashCode(signatureDigest.right) + Arrays.hashCode(signatureDigest.left) : 0);
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (level != null ? level.hashCode() : 0);
        return result;
    }

    @Override
    public String toString(){
        return getClass().getName() +
                " Node: " + nodeId +
                " Time: " + timestamp +
                " Severity: " + level.toString() +
                " Service: " + getServiceName() +
                " Message: " + getDescription() +
                " Signature: " + getSignature();
    }
}
