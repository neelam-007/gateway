package com.l7tech.console.util;

import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * AbstractAuditMessage subclass for AuditRecords.
 */
public class AuditMessage extends AbstractAuditMessage {

    //- PUBLIC

    public AuditMessage( final AuditRecord auditRecord ) {
        this.auditRecord = auditRecord;
    }

    public AuditMessage( final AuditRecord auditRecord, final String nodeName ) {
        this.auditRecord = auditRecord;
        setNodeName( nodeName );
    }

    @Override
    public Goid getMsgNumber() {
        return auditRecord.getGoid();
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
    public Pair<byte[],byte[]> getSignatureDigest() throws IOException {

        byte[] currentDigest =  auditRecord.computeSignatureDigest();
        byte[] oldDigest =  auditRecord.computeOldIdSignatureDigest();

        Pair<byte[],byte[]> digestvalue = new Pair<byte[],byte[]>(currentDigest,oldDigest);

        return digestvalue;
    }

    //- PRIVATE

    private final AuditRecord auditRecord;
}
