/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.host;

import com.l7tech.server.management.config.HasFeatures;
import com.l7tech.server.management.config.PCEntity;
import com.l7tech.server.management.config.node.NodeConfig;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/** @author alex */
@Entity
public abstract class HostConfig extends PCEntity implements HasFeatures<HostFeature> {
    private String localHostname;
    private HostType hostType;
    private OSType osType;

    /**
     * The set of {@link com.l7tech.server.management.config.node.NodeConfig}s hosted on this Host.
     */
    protected Set<NodeConfig> nodes = new HashSet<NodeConfig>();

    public HostType getHostType() {
        return hostType;
    }

    public void setHostType(HostType hostType) {
        this.hostType = hostType;
    }

    public OSType getOsType() {
        return osType;
    }

    public void setOsType(OSType osType) {
        this.osType = osType;
    }

    @OneToMany(mappedBy="host", cascade= CascadeType.ALL, fetch= FetchType.EAGER)
    public Set<NodeConfig> getNodes() {
        return nodes;
    }

    public void setNodes(Set<NodeConfig> nodes) {
        this.nodes = nodes;
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

        HostConfig host = (HostConfig)o;

        if (hostType != host.hostType) return false;
        if (osType != host.osType) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (hostType != null ? hostType.hashCode() : 0);
        result = 31 * result + (osType != null ? osType.hashCode() : 0);
        return result;
    }

    public enum HostType {
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
