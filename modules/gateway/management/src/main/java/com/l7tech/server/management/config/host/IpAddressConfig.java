/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.host;

import javax.persistence.*;

@Entity
@Table(name="pc_ipaddr")
public class IpAddressConfig extends HostFeature {
    private int id;
    private PCHostConfig host;
    private String interfaceName;
    private String ipAddress;
    private String netmask;

    public IpAddressConfig(PCHostConfig parent) {
        super(parent, HostFeatureType.IP);
    }

    protected IpAddressConfig() {
    }

    @Id @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public PCHostConfig getHost() {
        return host;
    }

    public void setHost(PCHostConfig host) {
        this.host = host;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<ip ");
        sb.append("if=\"").append(interfaceName).append("\" ");
        sb.append("ip=\"").append(ipAddress).append("\" ");
        if (netmask != null) sb.append("mask=").append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IpAddressConfig that = (IpAddressConfig)o;

        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (interfaceName != null ? !interfaceName.equals(that.interfaceName) : that.interfaceName != null)
            return false;
        if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
        if (netmask != null ? !netmask.equals(that.netmask) : that.netmask != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (interfaceName != null ? interfaceName.hashCode() : 0);
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + (netmask != null ? netmask.hashCode() : 0);
        return result;
    }
}
