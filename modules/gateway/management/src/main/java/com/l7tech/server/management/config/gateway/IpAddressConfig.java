/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.gateway;

import com.l7tech.objectmodel.NamedEntity;

import javax.persistence.*;

@Entity
@Table(name="pc_ipaddr")
public class IpAddressConfig extends GatewayFeature implements NamedEntity {
    private long oid;
    private String name;
    private int version;
    private PCGatewayConfig gateway;
    private String interfaceName;
    private String ipAddress;
    private String netmask;

    public IpAddressConfig(PCGatewayConfig parent) {
        super(parent, GatewayFeatureType.IP);
    }

    protected IpAddressConfig() {
    }

    @ManyToOne(optional=false)
    public PCGatewayConfig getGateway() {
        return gateway;
    }

    public void setGateway(PCGatewayConfig gateway) {
        this.gateway = gateway;
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
        sb.append("id=\"").append(oid).append('"').append(' ');
        sb.append("version=\"").append(version).append('"').append(' ');
        sb.append("name=\"").append(name).append('"').append(' ');
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

        if (gateway != null ? !gateway.equals(that.gateway) : that.gateway != null) return false;
        if (interfaceName != null ? !interfaceName.equals(that.interfaceName) : that.interfaceName != null)
            return false;
        if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
        if (netmask != null ? !netmask.equals(that.netmask) : that.netmask != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (gateway != null ? gateway.hashCode() : 0);
        result = 31 * result + (interfaceName != null ? interfaceName.hashCode() : 0);
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + (netmask != null ? netmask.hashCode() : 0);
        return result;
    }

    @Id
    @GeneratedValue
    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getId() {
        return Long.toString(oid);
    }

    public void setId(String id) throws NumberFormatException {
        oid = Long.valueOf(id);
    }
}
