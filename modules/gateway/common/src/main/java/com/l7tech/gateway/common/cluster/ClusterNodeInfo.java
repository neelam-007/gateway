package com.l7tech.gateway.common.cluster;

import com.l7tech.objectmodel.NameableEntity;
import com.l7tech.security.rbac.RbacAttribute;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
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
 *
 */
@Entity
@Proxy(lazy=false)
@Table(name="cluster_info")
public class ClusterNodeInfo implements Comparable<ClusterNodeInfo>, NameableEntity, Serializable {

    /**
     * Identifier for the node.
     *
     * <p>This is an opaque id that may have been generated from the unique mac address / partition
     * name combination or.</p>
     */
    @Id
    @Column(name="nodeid",nullable=false,length=32)
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
     * Name for the node
     */
    @RbacAttribute
    @Column(name="name", nullable=false, length=128)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * mac address of the node
     */
    @Column(name="mac",nullable=false,length=18)
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
     * direct ip address of this node
     */
    @Column(name="address",nullable=false,length=39)
    public String getAddress() {
        return address;
    }

    /**
     * direct ip address of this node
     */
    public void setAddress(String address) {
        this.address = address;
    }

    @Column(name="esm_address",nullable=false,length=39)
    public String getEsmAddress() {
        return esmAddress;
    }

    public void setEsmAddress( final String esmAddress ) {
        this.esmAddress = esmAddress;
    }

    /**
     * how long has this been running (in ms). this is always calculated at server side.
     */
    @Transient
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
    @Column(name="uptime",nullable=false) // This is not a bug, it does map to uptime
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
    @Column(name="avgload",nullable=false)
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
    @Column(name="statustimestamp",nullable=false)
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
    @Column(name="multicast_address",length=39)
    public String getMulticastAddress() {
        return multicastAddress;
    }

    /**
     * A multicast address for use in the DistributedMessageIdManager.
     */ 
    public void setMulticastAddress(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    @Transient
    public String getId() {
        return getNodeIdentifier();
    }

    public String toString() {
        return name + " [" + address + "]";
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClusterNodeInfo that = (ClusterNodeInfo) o;

        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (esmAddress != null ? !esmAddress.equals(that.esmAddress) : that.esmAddress != null) return false;
        if (mac != null ? !mac.equals(that.mac) : that.mac != null) return false;
        if (multicastAddress != null ? !multicastAddress.equals(that.multicastAddress) : that.multicastAddress != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (nodeId != null ? !nodeId.equals(that.nodeId) : that.nodeId != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (nodeId != null ? nodeId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (mac != null ? mac.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (esmAddress != null ? esmAddress.hashCode() : 0);
        result = 31 * result + (multicastAddress != null ? multicastAddress.hashCode() : 0);
        return result;
    }

    public int compareTo(ClusterNodeInfo cni) {
        int result;

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
    private String name;
    private String mac;
    private String address;
    private String esmAddress;
    private String multicastAddress;
    private long boottime;
    private long uptime;
    private double avgLoad;
    private long lastUpdateTimeStamp;
}
