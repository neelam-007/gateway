/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.gateway;

import com.l7tech.server.management.config.node.ServiceNodeConfig;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Inherited {@link #_name} property holds the gateway's default hostname by default, but it can be changed to some
 * other value that's locally relevant
 * <p/>
 * Note that a Gateway is effectively a non-persistent singleton from the POV of a given Process Controller instance,
 * but it can be treated as a persistent entity so that gateway configurations can be CRUDded in the usual ways (e.g.
 * backup, restore)
 *
 * @author alex
 */
@Entity
@Table(name="pc_gateway")
public class PCGatewayConfig extends GatewayConfig {

    /**
     * The pool of IP addresses on this Gateway that the SSG software is permitted to use.  The PC is responsible for
     * ensuring that each IP:port tuple from this pool is allocated to exactly zero or one
     * {@link com.l7tech.server.management.config.node.PCServiceNodeConfig}.
     */
    private Set<IpAddressConfig> ipAddresses = new HashSet<IpAddressConfig>();

    @OneToMany(cascade=CascadeType.ALL, mappedBy = "gateway", fetch=FetchType.EAGER)
    public Set<IpAddressConfig> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(Set<IpAddressConfig> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    @Transient
    public Set<GatewayFeature> getFeatures() {
        final Set<GatewayFeature> features = new HashSet<GatewayFeature>();
        final Set<IpAddressConfig> ips = getIpAddresses();
        if (ips != null) features.addAll(ips);
        return features;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<gateway id=\"" + _oid + "\" name=\"" + _name + "\">\n");
        for (IpAddressConfig ipAddress : ipAddresses) {
            sb.append("  ").append(ipAddress).append("\n");
        }
        for (ServiceNodeConfig node : serviceNodes) {
            sb.append("  ").append(node).append("\n");
        }
        sb.append("</gateway>");
        return sb.toString();
    }

}
