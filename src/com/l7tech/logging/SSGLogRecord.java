package com.l7tech.logging;

import com.l7tech.common.RequestId;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log record that can be persisted.
 * It extends normal LogRecords by reffering to a request id and a node id.
 *
 *
 * <br/><br/>
 * @author flascell<br/>
 * @version $Id$<br/>
 *
 */
public class SSGLogRecord extends LogRecord implements Serializable {

    public SSGLogRecord() {
        super(Level.FINEST, null);
    }

    /**
     * Constructs a <CODE>SSGLogRecord</CODE> given the log record.
     *
     * @param record  the <CODE>LogRecord</CODE> containing the log information.
     */
    public SSGLogRecord(LogRecord record) {
        super(record.getLevel(), record.getMessage());
        setLoggerName(record.getLoggerName());
        setMillis(record.getMillis());
        //setSequenceNumber(record.getSequenceNumber());
        setSourceClassName(record.getSourceClassName());
        setSourceMethodName(record.getSourceMethodName());
    }

    /**
     * Constructs a <CODE>SSGLogRecord</CODE> given the log record, request Id, and the node Id.
     *
     * @param record  the <CODE>LogRecord</CODE> containing the log information.
     * @param reqId   the request id associated with the log.
     * @param nodeId  the id of the node generating the log.
     */
    public SSGLogRecord(LogRecord record, RequestId reqId, String nodeId) {
        super(record.getLevel(), record.getMessage());
        setLoggerName(record.getLoggerName());
        setMillis(record.getMillis());
        //setSequenceNumber(record.getSequenceNumber());
        setSourceClassName(record.getSourceClassName());
        setSourceMethodName(record.getSourceMethodName());
        setReqId(reqId);
        setNodeId(nodeId);
    }

    /**
     * Constructs a <CODE>SSGLogRecord</CODE> given the log message, node Id, and the log level.
     *
     * @param level   the level of the log.
     * @param nodeId  the id of the node generating the log.
     * @param msg     the content of the log message.
     */
    public SSGLogRecord(Level level, String nodeId, String msg) {
        super(level, msg);
        this.nodeId = nodeId;
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
        if (arg == null) requestId = null;
        else requestId = arg.toString();
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

    /**
     * Check if the two <CODE>SSGLogRecord</CODE> objects are the same.
     *
     * @param obj the object to be compared with.
     * @return TRUE if the two objects are the same. FALSE otherwise.
     */
    public boolean equals(Object obj) {
        SSGLogRecord theOtherOne = null;
        if (obj instanceof SSGLogRecord) theOtherOne = (SSGLogRecord)obj;
        else return false;
        if (nodeId != null) {
            if (!nodeId.equals(theOtherOne.getNodeId())) return false;
        }
        if (requestId != null) {
            if (!requestId.equals(theOtherOne.getStrRequestId())) return false;
        }
        //return super.equals(obj);
        return true;
    }

    /**
     * Get the oid of the log record.
     *
     * @return long the oid of the log record.
     */
    public long getOid() {
        return oid;
    }

    /**
     * Set the oid of the log record.
     *
     * @param oid the oid of the log record.
     */
    public void setOid( long oid ) {
        this.oid = oid;
    }

    private long oid;
    protected String requestId;
    protected String nodeId;
}
