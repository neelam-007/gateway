package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.*;

import org.mortbay.util.ajax.JSON;

import java.util.Map;
import java.net.InetAddress;

/**
 * Entity class for SSG Node
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 14, 2008
 */
@Entity
@Table(name="ssg_node")
public class SsgNode extends NamedEntityImp implements JSON.Convertible, Comparable {
    public static final int MAX_NAME_LENGTH = 128;
    public static final String ILLEGAL_CHARACTERS = "/";

    private String guid;
    private String ipAddress;
    private SsgCluster ssgCluster;
    private String onlineStatus;
    private boolean trustStatus;
    private String softwareVersion;

    @Column(name="guid", length=36, nullable=false)
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Column(name="ip_address", length=128, nullable=false)
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="ssg_cluster_oid", nullable=false)
    public SsgCluster getSsgCluster() {
        return ssgCluster;
    }

    public void setSsgCluster(SsgCluster ssgCluster) {
        this.ssgCluster = ssgCluster;
    }

    @Column(name="online_status", length=36)
    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    @Column(name="software_version", length=10)
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    @Column(name="trust_status")
    public boolean isTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(boolean trustStatus) {
        this.trustStatus = trustStatus;
    }

    @Override
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, guid);
        output.add(JSONConstants.PARENT_ID, ssgCluster.getGuid());
        output.add(JSONConstants.TYPE, JSONConstants.Entity.SSG_NODE);
        output.add(JSONConstants.VERSION, getSoftwareVersion());
        output.add(JSONConstants.NAME, _name);
        output.add(JSONConstants.RBAC_CUD, false);
        output.add(JSONConstants.ONLINE_STATUS, getOnlineStatus());
        output.add(JSONConstants.TRUST_STATUS, isTrustStatus());
        output.add(JSONConstants.SELF_HOST_NAME, obtainHostName());
        output.add(JSONConstants.IP_ADDRESS, getIpAddress());
    }

    @Override
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }

    private String obtainHostName() {
        try {
            InetAddress address = InetAddress.getByName(getIpAddress());
            return address.getHostName();
        } catch (Exception e) {
            return "Unkown Host Name";
        }
    }

     @Override
     public int compareTo(Object o) {
         return _name.compareTo(((SsgNode)o).getName());
     }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SsgNode ssgNode = (SsgNode) o;

        if (trustStatus != ssgNode.trustStatus) return false;
        if (guid != null ? !guid.equals(ssgNode.guid) : ssgNode.guid != null) return false;
        if (ipAddress != null ? !ipAddress.equals(ssgNode.ipAddress) : ssgNode.ipAddress != null) return false;
        if (onlineStatus != null ? !onlineStatus.equals(ssgNode.onlineStatus) : ssgNode.onlineStatus != null)
            return false;
        if (softwareVersion != null ? !softwareVersion.equals(ssgNode.softwareVersion) : ssgNode.softwareVersion != null)
            return false;
        if (ssgCluster != null ? !ssgCluster.equals(ssgNode.ssgCluster) : ssgNode.ssgCluster != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + (ssgCluster != null ? ssgCluster.hashCode() : 0);
        result = 31 * result + (onlineStatus != null ? onlineStatus.hashCode() : 0);
        result = 31 * result + (trustStatus ? 1 : 0);
        result = 31 * result + (softwareVersion != null ? softwareVersion.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return
            "SSG Node Name : " + getName() + "\n" +
            "Self Host Name: " + obtainHostName() + "\n" +
            "IP Address    : " + getIpAddress();
    }
}
