package com.l7tech.gateway.common.audit;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

import java.util.logging.Level;

/**
 * Used for audit retrieval
 */
public class ExternalAuditRecordHeader extends AuditRecordHeader {

    private String guid;
    private String recordType;

    public ExternalAuditRecordHeader(final AuditRecord auditRecord, String guid){
        this( guid,
              AuditRecordUtils.AuditRecordType(auditRecord),
              auditRecord.getGoid(),
              auditRecord instanceof MessageSummaryAuditRecord ? auditRecord.getName() : "",
              auditRecord.getMessage(),
              null,
              auditRecord.getSignature(),
              auditRecord.getNodeId(),
              auditRecord.getMillis(),
              auditRecord.getLevel(),
              auditRecord.getVersion() );
    }

    public ExternalAuditRecordHeader(final ExternalAuditRecordHeader externalAuditRecordHeader) {
        this( externalAuditRecordHeader.getGuid(),
              externalAuditRecordHeader.getRecordType(),
              externalAuditRecordHeader.getGoid(),
              externalAuditRecordHeader.getName(),
              externalAuditRecordHeader.getDescription(),
              externalAuditRecordHeader.getSignatureDigest(),
              externalAuditRecordHeader.getSignature(),
              externalAuditRecordHeader.getNodeId(),
              externalAuditRecordHeader.getTimestamp(),
              externalAuditRecordHeader.getLevel(),
              externalAuditRecordHeader.getVersion() );
    }

    /**
     *
     * @param name should be empty if not an message summary audit record
     */
    public ExternalAuditRecordHeader(String guid, String recordType, Goid id, String name, String description, Pair<byte[],byte[]> signatureDigest, String signature, String nodeId, long timestamp, Level level, int version) {
        super( id, name, description,  signatureDigest, signature, nodeId, timestamp, level, version);
        this.guid = guid;
        this.recordType = recordType;
    }

    public String getGuid() {
        return guid;
    }

    public String getRecordType() {
        return recordType;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ExternalAuditRecordHeader that = (ExternalAuditRecordHeader) o;

        if(guid != null ? !guid.equals(that.getGuid()) : that.getGuid() != null) return false;
        if(recordType != null ? !recordType.equals(that.getRecordType()) : that.getRecordType() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result += 31 * result + (recordType != null ? recordType.hashCode() : 0);

        return result;
    }

    @Override
    public String toString(){
        return getClass().getName() +
                " Guid: " + guid +
                " Record Type: " + recordType +
                super.toString();
    }
}
