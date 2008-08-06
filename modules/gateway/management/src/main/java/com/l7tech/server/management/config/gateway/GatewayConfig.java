/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.gateway;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.management.config.node.ServiceNodeConfig;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/** @author alex */
@Entity
public abstract class GatewayConfig extends NamedEntityImp implements HasFeatures<GatewayFeature> {
    private String localHostname;
    private GatewayType gatewayType;
    private OSType osType;
    /**
     * The set of {@link ServiceNodeConfig}s hosted on this Gateway.
     */
    protected Set<ServiceNodeConfig> serviceNodes = new HashSet<ServiceNodeConfig>();

    public GatewayType getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(GatewayType gatewayType) {
        this.gatewayType = gatewayType;
    }

    public OSType getOsType() {
        return osType;
    }

    public void setOsType(OSType osType) {
        this.osType = osType;
    }

    @OneToMany(mappedBy="gateway", cascade= CascadeType.ALL, fetch= FetchType.EAGER)
    public Set<ServiceNodeConfig> getServiceNodes() {
        return serviceNodes;
    }

    public void setServiceNodes(Set<ServiceNodeConfig> serviceNodes) {
        this.serviceNodes = serviceNodes;
    }

    public String getLocalHostname() {
        return localHostname;
    }

    public void setLocalHostname(String localHostname) {
        this.localHostname = localHostname;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GatewayConfig gateway = (GatewayConfig)o;

        if (gatewayType != gateway.gatewayType) return false;
        if (osType != gateway.osType) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (gatewayType != null ? gatewayType.hashCode() : 0);
        result = 31 * result + (osType != null ? osType.hashCode() : 0);
        return result;
    }

    public enum GatewayType {
        APPLIANCE,
        SOFTWARE,
    }

    public enum OSType {
        RHEL,
        SUSE,
        SOLARIS,
        WINDOWS
    }
}
