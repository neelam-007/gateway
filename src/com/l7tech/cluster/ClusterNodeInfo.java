package com.l7tech.cluster;

import java.io.Serializable;

/**
 * Bean representation of a row in the cluster_info table.
 *
 * Rows are added and deleted at config time when a node is added or removed from the cluster. Properties
 * uptime, avgLoad and lastUpdateTimeStamp are updated by each node at regular interval.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 17, 2003<br/>
 * $Id$
 * 
 */
public class ClusterNodeInfo implements Serializable {

    /**
     * mac address of the node that uniquely identifies this node in the cluster
     */
    public String getMac() {
        return mac;
    }

    /**
     * mac address of the node that uniquely identifies this node in the cluster
     */
    public void setMac(String mac) {
        this.mac = mac;
    }

    /**
     * direct ip address of this node
     */
    public String getAddress() {
        return address;
    }

    /**
     * direct ip address of this node
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * whether or not this node has access to the CA key necessary for signing CSRs
     */
    public boolean getIsMaster() {
        return isMaster;
    }

    /**
     * whether or not this node has access to the CA key necessary for signing CSRs
     */
    public void setIsMaster(boolean master) {
        isMaster = master;
    }

    /**
     * the timestamp of when this node last booted
     */
    public long getUptime() {
        return uptime;
    }

    /**
     * the timestamp of when this node last booted
     */
    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    /**
     * the avg load of this node for the last minute
     */
    public double getAvgLoad() {
        return avgLoad;
    }

    /**
     * the avg load of this node for the last minute
     */
    public void setAvgLoad(double avgLoad) {
        this.avgLoad = avgLoad;
    }

    /**
     * the timestamp of when the avg load was last updated
     */
    public long getLastUpdateTimeStamp() {
        return lastUpdateTimeStamp;
    }

    /**
     * the timestamp of when the avg load was last updated
     */
    public void setLastUpdateTimeStamp(long lastUpdateTimeStamp) {
        this.lastUpdateTimeStamp = lastUpdateTimeStamp;
    }

    /**
     * name of the node (set at config time)
     */
    public String getName() {
        return name;
    }

    /**
     * name of the node (set at config time)
     */
    public void setName(String name) {
        this.name = name;
    }

    private String mac;
    private String address;
    private String name;
    private boolean isMaster;
    private long uptime;
    private double avgLoad;
    private long lastUpdateTimeStamp;
}
