package com.l7tech.console.util;

import com.l7tech.gateway.common.audit.ExternalAuditRecordHeader;
import com.l7tech.gateway.common.audit.AuditRecordHeader;

import java.io.IOException;

/**
 * AbstractAuditMessage implementation for AuditRecord headers.
 */
public class AuditHeaderMessage extends AbstractAuditMessage {

    //- PUBLIC

    public AuditHeaderMessage( final AuditRecordHeader header ) {
        this.header = header;
    }

    @Override
    public long getMsgNumber() {
        return header.getOid();
    }

    @Override
    public long getTimestamp() {
        return header.getTimestamp();
    }

    @Override
    public String getSeverity() {
        return header.getLevel().toString();
    }

    @Override
    public String getMsgDetails(){
        return header.getMessage() == null ? "" : header.getMessage();
    }

    @Override
    public String getReqId() {
        return null;
    }

    @Override
    public String getServiceName() {
        return header.getServiceName();
    }

    @Override
    public String getNodeId() {
        return header.getNodeId();
    }

    @Override
    public String getSignature() {
        return header.getSignature();
    }

    @Override
    public byte[] getSignatureDigest() throws IOException {
        return header.getSignatureDigest();
    }

    public void setSignatureDigest(byte [] signatureDigest) {
        header.setSignatureDigest(signatureDigest);
    }

    public boolean isDigestWasSkipped() {
        return digestWasSkipped;
    }

    public void setDigestWasSkipped(boolean digestWasSkipped) {
        this.digestWasSkipped = digestWasSkipped;
    }
    
    public String getGuid(){
        return header instanceof ExternalAuditRecordHeader ? ((ExternalAuditRecordHeader) header).getGuid() : null;
    }

    //- PRIVATE

    private final AuditRecordHeader header;
    /**
     * Record whether the digest of this record was skipped possibly due to the related audit record
     * containing an audited message which is too large to read as part of the validate signature query.
     */
    private boolean digestWasSkipped;

}
