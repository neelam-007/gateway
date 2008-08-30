/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.host;

import com.l7tech.server.management.config.node.NodeConfig;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Inherited {@link #_name} property holds the host's default hostname by default, but it can be changed to some
 * other value that's locally relevant
 * <p/>
 * Note that a Host is effectively a singleton from the POV of a given Process Controller instance, but it can be
 * treated as a persistent entity so that host configurations can be CRUDded in the usual ways (e.g. backup, restore)
 *
 * @author alex
 */
@Entity
@Table(name="pc_host")
public class PCHostConfig extends HostConfig {

    /**
     * The pool of IP addresses on this Host that the SSG software is permitted to use.  The PC is responsible for
     * ensuring that each IP:port tuple from this pool is allocated to exactly zero or one
     * {@link com.l7tech.server.management.config.node.PCNodeConfig}.
     */
    private Set<IpAddressConfig> ipAddresses = new HashSet<IpAddressConfig>();

    @OneToMany(cascade=CascadeType.ALL, mappedBy = "host", fetch=FetchType.EAGER)
    public Set<IpAddressConfig> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(Set<IpAddressConfig> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    @Transient
    public Set<HostFeature> getFeatures() {
        final Set<HostFeature> features = new HashSet<HostFeature>();
        final Set<IpAddressConfig> ips = getIpAddresses();
        if (ips != null) features.addAll(ips);
        return features;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<host id=\"" + guid + "\" name=\"" + name + "\">\n");
        for (IpAddressConfig ipAddress : ipAddresses) {
            sb.append("  ").append(ipAddress).append("\n");
        }
        for (NodeConfig node : nodes.values()) {
            sb.append("  ").append(node).append("\n");
        }
        sb.append("</host>");
        return sb.toString();
    }

}
