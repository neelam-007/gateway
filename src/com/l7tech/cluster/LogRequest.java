package com.l7tech.cluster;

/*
 * This class encapsultes the request for retrieving logs from cluster nodes.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogRequest {

    private String nodeId;
    private long startMsgNumber;
    private long endMsgNumber;
    private int retrievedLogCount;

    public LogRequest(String nodeId, long startMsgNumber, long endMsgNumber) {
        this.nodeId = nodeId;
        this.endMsgNumber = endMsgNumber;
        this.startMsgNumber = startMsgNumber;
        retrievedLogCount = 0;
    }

    /**
     * Return the node Id.
     *
     * @return String  The node Id.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Return the retrieved log count.
     *
     * @return int  The retrieved log count.
     */
    public int getRetrievedLogCount() {
        return retrievedLogCount;
    }

    /**
     * Set the retrieved log count.
     *
     * @param retrievedLogCount  The value of the retrieved log count.
     */
    public void setRetrievedLogCount(int retrievedLogCount) {
        this.retrievedLogCount = retrievedLogCount;
    }

    /**
     * Add the retrieved log count.
     *
     * @param count  The value of the retrieved log count to be added.
     */
    public void addRetrievedLogCount(int count) {
        retrievedLogCount += count;
    }

    /**
     * Set the node Id.
     *
     * @param nodeId  The value of the node Id.
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Return the endMsgNumber.
     *
     * @return long  The endMsgNumber.
     */
    public long getEndMsgNumber() {
        return endMsgNumber;
    }

    /**
     * Set the endMsgNumber.
     *
     * @param endMsgNumber The value of the endMsgNumber.
     */
    public void setEndMsgNumber(long endMsgNumber) {
        this.endMsgNumber = endMsgNumber;
    }

    /**
     * Return the startMsgNumber.
     *
     * @return long  The value of the startMsgNumber.
     */
    public long getStartMsgNumber() {
        return startMsgNumber;
    }

    /**
     * Set the startMsgNumber
     *
     * @param startMsgNumber  The value of the startMsgNumber
     */
    public void setStartMsgNumber(long startMsgNumber) {
        this.startMsgNumber = startMsgNumber;
    }

}
