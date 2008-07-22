package com.l7tech.gateway.common.logging;

import com.l7tech.gateway.common.RequestId;

import java.io.Serializable;
import java.text.MessageFormat;
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
     * Constructs a <CODE>SSGLogRecord</CODE> given the log record and the node Id.
     *
     * @param record  the <CODE>LogRecord</CODE> containing the log information.                                         
     * @param nodeId  the id of the node generating the log.
     */
    public SSGLogRecord(LogRecord record, String nodeId) {
        super(record.getLevel(), record.getMessage());
        setLoggerName(record.getLoggerName());
        setParameters(record.getParameters());
        setMillis(record.getMillis());
        setThreadID(record.getThreadID());
        setSequenceNumber(record.getSequenceNumber());

        // Looks like this may be slow, so turning off for now:
        //
        // Note that if the client application has not specified an explicit
        // source method name and source class name, then the LogRecord class
        // will infer them automatically when they are first accessed (due to
        // a call on getSourceMethodName or getSourceClassName) by analyzing
        // the call stack.
        //
        //setSourceClassName(record.getSourceClassName());
        //setSourceMethodName(record.getSourceMethodName());
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
     * Get node Id. The node is the one on which this log was generated.
     *
     * @return String  the node id.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Set node Id. The node is the one on which this log was generated.
     *
     * @param nodeId  the node id.
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Get the id of the request being processed when this log record was generated.
     *
     * @return RequestId the id of the request associated with the log.
     * @see RequestId
     */
    public RequestId getReqId() {
        if (requestId == null || requestId.length() <= 0) return null;
        return new RequestId(requestId);
    }

    /**
     * Set the id of the request being processed when this log record was generated.
     * @param arg the <CODE>RequestId</CODE>.
     * @see RequestId
     */
    public void setReqId( RequestId arg) {
        if (arg == null) requestId = null;
        else requestId = arg.toString();
    }

    /**
     * Get the logging level of the log. For serialization purposes only.
     * @return String the logging level.
     */
    public String getStrLvl() {
        return getLevel().getName();
    }

    /**
     * Set the logging level of the log. For serialization purposes only.
     * @param arg  the logging level of the log.
     */
    public void setStrLvl(String arg) {
        setLevel(Level.parse(arg));
    }

    /**
     * Get the requestId of the log record. For serialization purposes only.
     * @return String the request Id.
     */
    public String getStrRequestId() {
        return requestId;
    }

    /**
     * Set the requestId of the log record. For serialization purposes only.
     * @param requestId the request Id.
     */
    public void setStrRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     *
     */
    public String getMessage() {
        String formattedMessage = super.getMessage();
        if(formattedMessage!=null && getParameters()!=null && getParameters().length>0) {
            try {
                formattedMessage = MessageFormat.format(formattedMessage, getParameters());
            }
            catch(IllegalArgumentException iae) {
                // then display the unformatted message
            }
        }
        return formattedMessage;
    }

    /**
     * Check if the two <CODE>SSGLogRecord</CODE> objects are the same.
     *
     * @param obj the object to be compared with.
     * @return TRUE if the two objects are the same. FALSE otherwise.
     */
    public boolean equals(Object obj) {
        SSGLogRecord theOtherOne;
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

    public int hashCode() {
        int result;
        result = (requestId != null ? requestId.hashCode() : 0);
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
        return result;
    }

    public long getOid() {
        return getSequenceNumber();
    }

    private static final long serialVersionUID = -2234601153074484000L;

    protected String requestId;
    protected String nodeId;
}
