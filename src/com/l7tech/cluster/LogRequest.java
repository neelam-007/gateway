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

    public LogRequest(String nodeId, long startMsgNumber, long endMsgNumber) {
        this.nodeId = nodeId;
        this.endMsgNumber = endMsgNumber;
        this.startMsgNumber = startMsgNumber;
    }

    public String getNodeId() {
        return nodeId;
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
