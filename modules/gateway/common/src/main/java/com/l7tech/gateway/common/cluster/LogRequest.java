package com.l7tech.gateway.common.cluster;

import java.util.Date;

/*
 * This class encapsultes the request for retrieving logs from cluster nodes.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogRequest {

    private final String nodeId;
    private long startMsgNumber;
    private long endMsgNumber;
    private Date startMsgDate;
    private Date endMsgDate;
    private int retrievedLogCount;

    public LogRequest(String nodeId, long startMsgNumber, long endMsgNumber, Date startMsgDate, Date endMsgDate) {
        this.nodeId = nodeId;
        this.endMsgNumber = endMsgNumber;
        this.startMsgNumber = startMsgNumber;
        this.startMsgDate = startMsgDate;
        this.endMsgDate = endMsgDate;
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

    /**
     * Get the start date (may be null)
     *
     * @return the date if set
     */
    public Date getStartMsgDate() {
        return startMsgDate;
    }

    /**
     * Set the start date (may be null)
     *
     * @param startMsgDate the date to use
     */
    public void setStartMsgDate(Date startMsgDate) {
        this.startMsgDate = startMsgDate;
    }

    /**
     * Get the end date (may be null)
     *
     * @return the date if set
     */
    public Date getEndMsgDate() {
        return endMsgDate;
    }

    /**
     * Set the end date (may be null)
     *
     * @param endMsgDate the date to use
     */
    public void setEndMsgDate(Date endMsgDate) {
        this.endMsgDate = endMsgDate;
    }

}
