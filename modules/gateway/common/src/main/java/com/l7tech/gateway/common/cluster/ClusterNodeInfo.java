package com.l7tech.gateway.common.cluster;

import com.l7tech.objectmodel.imp.NamedEntityImp;

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
public class ClusterNodeInfo extends NamedEntityImp implements Comparable<ClusterNodeInfo> {

    /**
     * Identifier for the node.
     *
     * <p>This is an opaque id generated from the unique mac address / partition
     * name combination.</p>
     */
    public String getNodeIdentifier() {
        return nodeId;
    }

    /**
     * Identifier for the node
     */
    public void setNodeIdentifier(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * mac address of the node
     */
    public String getMac() {
        return mac;
    }

    /**
     * mac address of the node
     */
    public void setMac(String mac) {
        this.mac = mac;
    }

    /**
     * Get the name of the partition.
     *
     * @return The partition name.
     */
    public String getPartitionName() {
        return partitionName;
    }

    /**
     * Set the name of the partition.
     *
     * @param partitionName The partition name.
     */
    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
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
     * Get the port for cluster rmi.
     *
     * @return The cluster rmi port.
     */
    public int getClusterPort() {
        return clusterPort;
    }

    /**
     * Set the port for cluster rmi.
     *
     * @param clusterPort The cluster rmi port.
     */
    public void setClusterPort(int clusterPort) {
        this.clusterPort = clusterPort;
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
     * how long has this been running (in ms). this is always calculated at server side.
     */
    public long getUptime() {
        return uptime;
    }

    /**
     * how long has this been running (in ms). this is always calculated at server side.
     */
    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    /**
     * the timestamp of when this node last booted
     */
    public long getBootTime() {
        return boottime;
    }

    /**
     * the timestamp of when this node last booted
     */
    public void setBootTime(long timestamp) {
        this.boottime = timestamp;
        if (uptime == 0) {
            uptime = System.currentTimeMillis() - boottime;
        }
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
     * A multicast address for use in the DistributedMessageIdManager.
     */
    public String getMulticastAddress() {
        return multicastAddress;
    }

    /**
     * A multicast address for use in the DistributedMessageIdManager.
     */ 
    public void setMulticastAddress(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public String toString() {
        return _name + " [" + address + "]";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ClusterNodeInfo that = (ClusterNodeInfo)o;

        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (mac != null ? !mac.equals(that.mac) : that.mac != null) return false;
        if (multicastAddress != null ? !multicastAddress.equals(that.multicastAddress) : that.multicastAddress != null)
            return false;
        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (mac != null ? mac.hashCode() : 0);
        result = 29 * result + (address != null ? address.hashCode() : 0);
        result = 29 * result + (_name != null ? _name.hashCode() : 0);
        result = 29 * result + (multicastAddress != null ? multicastAddress.hashCode() : 0);
        return result;
    }

    public int compareTo(ClusterNodeInfo cni) {
        int result = 0;

        String name1 = getName();
        String name2 = cni.getName();

        if (name1 == null && name2 == null) {
            result = 0;
        }
        else if (name1 == null) {
            result = -1;
        }
        else if (name2 == null) {
            result = 1;
        }
        else {
            result = name1.toLowerCase().compareTo(name2.toLowerCase());
        }

        return result;
    }

    private static final long serialVersionUID = 3387085760350960428L;

    private String nodeId;
    private String mac;
    private String partitionName;
    private String address;
    private String multicastAddress;
    private int clusterPort;
    private boolean isMaster;
    private long boottime;
    private long uptime;
    private double avgLoad;
    private long lastUpdateTimeStamp;
}
