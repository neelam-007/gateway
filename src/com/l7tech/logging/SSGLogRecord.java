package com.l7tech.logging;

import com.l7tech.common.RequestId;

import java.util.logging.LogRecord;
import java.util.logging.Level;

/**
 * Log record that can be persisted.
 * It extends normal LogRecords by reffering to a request id and a node id.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 13, 2004<br/>
 * $Id$<br/>
 *
 */
public class SSGLogRecord extends LogRecord {

    public SSGLogRecord() {
        super(Level.FINEST, null);
    }

    public SSGLogRecord(LogRecord record) {
        super(record.getLevel(), record.getMessage());
        setLoggerName(record.getLoggerName());
        setMillis(record.getMillis());
        setSequenceNumber(record.getSequenceNumber());
        setSourceClassName(record.getSourceClassName());
        setSourceMethodName(record.getSourceMethodName());
    }

    public SSGLogRecord(LogRecord record, RequestId reqId, String nodeId) {
        super(record.getLevel(), record.getMessage());
        setLoggerName(record.getLoggerName());
        setMillis(record.getMillis());
        setSequenceNumber(record.getSequenceNumber());
        setSourceClassName(record.getSourceClassName());
        setSourceMethodName(record.getSourceMethodName());
        setReqId(reqId);
        setNodeId(nodeId);
    }

    /**
     * in a cluster, this is the node on which this log was generated
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * in a cluster, this is the node on which this log was generated
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * the id of the request being processed when this log record was generated
     */
    public RequestId getReqId() {
        if (requestId == null || requestId.length() <= 0) return null;
        return new RequestId(requestId);
    }

    /**
     * the id of the request being processed when this log record was generated
     */
    public void setReqId(RequestId arg) {
        requestId = arg.toString();
    }

    /**
     * for serialization purposes only
     */
    public String getStrLvl() {
        return getLevel().getName();
    }

    /**
     * for serialization purposes only
     */
    public void setStrLvl(String arg) {
        setLevel(Level.parse(arg));
    }

    /**
     * for serialization purposes only
     */
    public String getStrRequestId() {
        return requestId;
    }

    /**
     * for serialization purposes only
     */
    public void setStrRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String requestId;
    public String nodeId;
}
