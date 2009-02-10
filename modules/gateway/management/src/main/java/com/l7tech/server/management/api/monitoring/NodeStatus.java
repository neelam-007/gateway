/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.NodeStateType;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class NodeStatus {
    private NodeStateType type;
    private Date startTime;
    private Date lastObservedTime;

    @Deprecated
    public NodeStatus() { }

    public NodeStatus(NodeStateType type, Date startTime, Date lastObservedTime) {
        this.type = type;
        this.startTime = startTime;
        this.lastObservedTime = lastObservedTime;
    }

    @XmlAttribute
    public NodeStateType getType() {
        return type;
    }

    @XmlAttribute
    public Date getStartTime() {
        return startTime;
    }

    @XmlAttribute
    public Date getLastObservedTime() {
        return lastObservedTime;
    }

    @Deprecated
    protected void setType(NodeStateType type) {
        this.type = type;
    }

    @Deprecated
    protected void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Deprecated
    protected void setLastObservedTime(Date lastObservedTime) {
        this.lastObservedTime = lastObservedTime;
    }
}
