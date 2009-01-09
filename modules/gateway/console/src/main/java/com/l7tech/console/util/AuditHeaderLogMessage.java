package com.l7tech.console.util;

import com.l7tech.gateway.common.audit.AuditRecordHeader;

import java.io.IOException;

/**
 * LogMessage subclass for AuditRecords headers.
 */
public class AuditHeaderLogMessage extends LogMessage {

    //- PUBLIC

    public AuditHeaderLogMessage( final AuditRecordHeader header ) {
        super( header.getTimestamp() );
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
    public String getMsgClass() {
        return null;
    }

    @Override
    public String getMsgMethod() {
        return null;
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

    //- PRIVATE

    private final AuditRecordHeader header;

}
