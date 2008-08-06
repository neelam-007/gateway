/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import org.hibernate.annotations.CollectionOfElements;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Child entity of a {@link com.l7tech.server.management.config.gateway.PCGatewayConfig} describing one of the service
 * nodes hosted on the Gateway.
 * <p/>
 * Note: the inherited {@link #_name} property is used here as the partition name
 * <p/>
 * TODO someday configure a subset of the configuration available in this database (e.g. a folder)
 * @author alex
 */
@Entity
@Table(name = "pc_node")
public class PCServiceNodeConfig extends ServiceNodeConfig {

    /**
     * The hostname of the load balancer in front of this partition's nodes
     * <p/>
     * TODO is it useful/relevant to know which ports on the LB  
     */
    private String clusterHostname;

    private boolean disabled;
    /** Is this node intended to be the sole user of the Tarari card on this Gateway? */
    private boolean tarariOwner;

    /** Is this node intended to be the sole user of the SCA card on this Gateway? */
    private boolean scaOwner;

    /** TCP port for intra-cluster RMI communications */
    private int rmiPort;

    /** TCP port for Process Controller API */
    private int processControllerApiPort;

    /**
     * Used to combine multiple {@link DatabaseConfig} host:ports for each DatabaseType into a single JDBC URL.
     *
     * Must contain {0} to hold the primary host:port; may contain additional {1}, {2} etc. for secondary host:ports.
     */
    private Map<DatabaseType, String> databaseUrlTemplate = new HashMap<DatabaseType, String>();

    /**
     * Locally cached copy of keystores configured for this partition
     */
    private transient Set<SsgKeyEntry> keystores = new HashSet<SsgKeyEntry>();

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isTarariOwner() {
        return tarariOwner;
    }

    public void setTarariOwner(boolean tarariOwner) {
        this.tarariOwner = tarariOwner;
    }

    public boolean isScaOwner() {
        return scaOwner;
    }

    public void setScaOwner(boolean scaOwner) {
        this.scaOwner = scaOwner;
    }

    @SuppressWarnings({ "JpaModelErrorInspection" })
    @CollectionOfElements(fetch=FetchType.EAGER)
    @JoinTable(name="pc_node_dburls", joinColumns = @JoinColumn(name="node_id"))
    @Column(name="url")
    public Map<DatabaseType, String> getDatabaseUrlTemplate() {
        return databaseUrlTemplate;
    }

    public void setDatabaseUrlTemplate(Map<DatabaseType, String> databaseUrlTemplate) {
        this.databaseUrlTemplate = databaseUrlTemplate;
    }

    @Transient
    public Set<SsgKeyEntry> getKeystores() {
        return keystores;
    }

    public void setKeystores(Set<SsgKeyEntry> keystores) {
        this.keystores = keystores;
    }

    public String getClusterHostname() {
        return clusterHostname;
    }

    public void setClusterHostname(String clusterHostname) {
        this.clusterHostname = clusterHostname;
    }

    public int getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(int rmiPort) {
        this.rmiPort = rmiPort;
    }

    public int getProcessControllerApiPort() {
        return processControllerApiPort;
    }

    public void setProcessControllerApiPort(int processControllerApiPort) {
        this.processControllerApiPort = processControllerApiPort;
    }

    @Override
    @Transient
    public Set<ServiceNodeFeature> getFeatures() {
        final Set<ServiceNodeFeature> features = new HashSet<ServiceNodeFeature>();
        features.add(new RmiPortFeature(this, getRmiPort()));
        features.add(new ProcessControllerApiPortFeature(this, getProcessControllerApiPort()));
        features.add(new TarariFeature(this, isTarariOwner()));
        features.add(new ScaFeature(this, isScaOwner()));
        return features;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PCServiceNodeConfig that = (PCServiceNodeConfig)o;

        if (disabled != that.disabled) return false;
        if (scaOwner != that.scaOwner) return false;
        if (tarariOwner != that.tarariOwner) return false;
        if (gateway != null ? !gateway.equals(that.gateway) : that.gateway != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (gateway != null ? gateway.hashCode() : 0);
        result = 31 * result + (disabled ? 1 : 0);
        result = 31 * result + (tarariOwner ? 1 : 0);
        result = 31 * result + (scaOwner ? 1 : 0);
        result = 31 * result + (databaseUrlTemplate != null ? databaseUrlTemplate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<serviceNode ");
        sb.append("id=\"").append(_oid).append("\" ");
        sb.append("name=\"").append(_name).append("\" ");
        sb.append("disabled=\"").append(disabled).append("\" ");
        sb.append(">\n");
        if (!connectors.isEmpty()) {
            sb.append("    <connectors>\n");
            for (SsgConnector conn : connectors) {
                sb.append("      ").append(conn).append("\n");
            }
            sb.append("    </connectors>\n");
        }
        if (!databaseUrlTemplate.isEmpty()) {
            sb.append("    <urls>\n");
            for (DatabaseType databaseType : databaseUrlTemplate.keySet()) {
                sb.append("      <url type=\"").append(databaseType).append("\" url=\"").append(databaseUrlTemplate.get(databaseType)).append("\"/>\n");
            }
            sb.append("    </urls>\n");
        }
        if (!databases.isEmpty()) {
            sb.append("    <databases>\n");
            for (DatabaseConfig db : databases) {
                sb.append("      ").append(db).append("\n");
            }
            sb.append("    </databases>\n");
        }
        sb.append("  </serviceNode>");
        return sb.toString();
    }
}
