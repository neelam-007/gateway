package com.l7tech.cluster;

/*
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

    public String getNodeId() {
        return nodeId;
    }

    public int getRetrievedLogCount() {
        return retrievedLogCount;
    }

    public void setRetrievedLogCount(int retrievedLogCount) {
        this.retrievedLogCount = retrievedLogCount;
    }

    public void addRetrievedLogCount(int count) {
        retrievedLogCount += count;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getEndMsgNumber() {
        return endMsgNumber;
    }

    public void setEndMsgNumber(long endMsgNumber) {
        this.endMsgNumber = endMsgNumber;
    }

    public long getStartMsgNumber() {
        return startMsgNumber;
    }

    public void setStartMsgNumber(long startMsgNumber) {
        this.startMsgNumber = startMsgNumber;
    }

}
