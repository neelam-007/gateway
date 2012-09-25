package com.l7tech.gateway.common.audit;

import java.util.logging.Level;

/**
 * Used for audit retrieval
 */
public class AuditRecordGuidHeader extends AuditRecordHeader {

    private String guid;

    public AuditRecordGuidHeader(final AuditRecord auditRecord, String guid){
        this( guid,
              auditRecord.getOid(),
              auditRecord instanceof MessageSummaryAuditRecord ? auditRecord.getName() : "",
              auditRecord.getMessage(),
              null,
              auditRecord.getSignature(),
              auditRecord.getNodeId(),
              auditRecord.getMillis(),
              auditRecord.getLevel(),
              auditRecord.getVersion() );
    }

    public AuditRecordGuidHeader(final AuditRecordGuidHeader auditRecordHeader) {
        this( auditRecordHeader.getGuid(),
              auditRecordHeader.getOid(),
              auditRecordHeader.getName(),
              auditRecordHeader.getDescription(),
              auditRecordHeader.getSignatureDigest(),
              auditRecordHeader.getSignature(),
              auditRecordHeader.getNodeId(),
              auditRecordHeader.getTimestamp(),
              auditRecordHeader.getLevel(),
              auditRecordHeader.getVersion() );
    }

    /**
     *
     * @param name should be empty if not an message summary audit record
     */
    public AuditRecordGuidHeader(String guid, long id, String name, String description, byte[] signatureDigest, String signature, String nodeId, long timestamp, Level level, int version) {
        super( id, name, description,  signatureDigest, signature, nodeId, timestamp, level, version);
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final AuditRecordGuidHeader that = (AuditRecordGuidHeader) o;

        if(guid != null ? !guid.equals(that.getGuid()) : that.getGuid() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (guid != null ? guid.hashCode() : 0);

        return result;
    }

    @Override
    public String toString(){
        return getClass().getName() +
                " Guid: " + guid +
                super.toString();
    }
}
