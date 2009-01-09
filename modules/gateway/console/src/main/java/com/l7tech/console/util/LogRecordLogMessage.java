package com.l7tech.console.util;

import com.l7tech.gateway.common.logging.SSGLogRecord;

/**
 * LogMessage subclass for LogRecords.
 */
public class LogRecordLogMessage extends LogMessage {

    //- PUBLIC

    public LogRecordLogMessage( final SSGLogRecord log ) {
        super( log.getMillis() );
        this.log = log;
    }

    @Override
    public long getMsgNumber() {
        return log.getSequenceNumber();
    }

    @Override
    public long getTimestamp() {
        return log.getMillis();
    }

    @Override
    public String getSeverity() {
        return log.getLevel().toString();
    }

    @Override
    public String getMsgClass() {
        return log.getSourceClassName();
    }

    @Override
    public String getMsgMethod() {
        return log.getSourceMethodName();
    }

    @Override
    public String getMsgDetails(){
        return log.getMessage() == null ? "" : log.getMessage();
    }

    @Override
    public String getReqId() {
        return (log.getReqId() == null) ? null : log.getReqId().toString();
    }

    @Override
    public String getNodeId() {
        return log.getNodeId();
    }

    @Override
    public int getThreadID() {
        return log.getThreadID();
    }

    public SSGLogRecord getSSGLogRecord() {
        return log;
    }

    //- PRIVATE

    private final SSGLogRecord log;
}