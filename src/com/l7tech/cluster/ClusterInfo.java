package com.l7tech.cluster;

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
public class ClusterInfo {
    
    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean getIsMaster() {
        return isMaster;
    }

    public void setIsMaster(boolean master) {
        isMaster = master;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public double getAvgLoad() {
        return avgLoad;
    }

    public void setAvgLoad(double avgLoad) {
        this.avgLoad = avgLoad;
    }

    public long getLastUpdateTimeStamp() {
        return lastUpdateTimeStamp;
    }

    public void setLastUpdateTimeStamp(long lastUpdateTimeStamp) {
        this.lastUpdateTimeStamp = lastUpdateTimeStamp;
    }

    public String getName() {
        return name;
    }

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
