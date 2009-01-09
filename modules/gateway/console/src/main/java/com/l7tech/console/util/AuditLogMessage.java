package com.l7tech.console.util;

import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * LogMessage subclass for AuditRecords.
 */
public class AuditLogMessage extends LogMessage {

    //- PUBLIC

    public AuditLogMessage( final AuditRecord auditRecord ) {
        super( auditRecord.getMillis() );
        this.auditRecord = auditRecord;
    }

    @Override
    public long getMsgNumber() {
        return auditRecord.getOid();
    }

    @Override
    public long getTimestamp() {
        return auditRecord.getMillis();
    }

    @Override
    public String getSeverity() {
        return auditRecord.getLevel().toString();
    }

    @Override
    public String getMsgClass() {
        return auditRecord.getSourceClassName();
    }

    @Override
    public String getMsgMethod() {
        return auditRecord.getSourceMethodName();
    }

    @Override
    public String getMsgDetails(){
        return auditRecord.getMessage() == null ? "" : auditRecord.getMessage();
    }

    @Override
    public String getReqId() {
        return (auditRecord.getReqId() == null) ? null : auditRecord.getReqId().toString();
    }

    @Override
    public String getServiceName() {
        if ( auditRecord instanceof MessageSummaryAuditRecord ) {
            return auditRecord.getName();
        } else {
            return super.getServiceName();
        }
    }

    @Override
    public String getNodeId() {
        return auditRecord.getNodeId();
    }

    public AuditRecord getAuditRecord() {
        return auditRecord;
    }

    @Override
    public String getSignature() {
        return auditRecord.getSignature();
    }

    @Override
    public byte[] getSignatureDigest() throws IOException {
        byte[] digestvalue = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("should not happen", e);
        }

        try {
            auditRecord.serializeSignableProperties(baos);
            digestvalue = digest.digest(baos.toByteArray());
        }  finally {
            ResourceUtils.closeQuietly( baos );
        }

        return digestvalue;
    }

    //- PRIVATE

    private final AuditRecord auditRecord;
}
