/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Child entity of a {@link com.l7tech.server.management.config.host.PCHostConfig} describing one of the service
 * nodes hosted on the Gateway.
 * <p/>
 * Note: the inherited {@link #name} property is used here as the partition name
 * <p/>
 * TODO someday configure a subset of the configuration available in this database (e.g. a folder)
 * @author alex
 */
public class PCNodeConfig extends NodeConfig {
    /** Is this node intended to be the sole user of the Tarari card on this Node? */
    private boolean tarariOwner;

    /** Is this node intended to be the sole user of the SCA card on this Node? */
    private boolean scaOwner;

    /** TCP port for intra-cluster RMI communications */
    private int rmiPort;

    /** URI suffix for Process Controller API */
    private String processControllerApiUrl;

    /**
     * Used to combine multiple {@link DatabaseConfig} host:ports for each DatabaseType into a single JDBC URL.
     *
     * Must contain {0} to hold the primary host:port; may contain additional {1}, {2} etc. for secondary host:ports.
     */
    private Map<DatabaseType, String> databaseUrlTemplate = new HashMap<DatabaseType, String>();

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

    public Map<DatabaseType, String> getDatabaseUrlTemplate() {
        return databaseUrlTemplate;
    }

    public void setDatabaseUrlTemplate(Map<DatabaseType, String> databaseUrlTemplate) {
        this.databaseUrlTemplate = databaseUrlTemplate;
    }

    public int getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(int rmiPort) {
        this.rmiPort = rmiPort;
    }

    @Override
    public Set<NodeFeature> getFeatures() {
        final Set<NodeFeature> features = new HashSet<NodeFeature>();
        features.add(new RmiPortFeature(this, getRmiPort()));
        features.add(new ProcessControllerApiUrlFeature(this, getProcessControllerApiUrl()));
        features.add(new TarariFeature(this, isTarariOwner()));
        features.add(new ScaFeature(this, isScaOwner()));
        return features;
    }

    public String getProcessControllerApiUrl() {
        return processControllerApiUrl;
    }

    public void setProcessControllerApiUrl(String processControllerApiUrl) {
        this.processControllerApiUrl = processControllerApiUrl;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PCNodeConfig that = (PCNodeConfig)o;

        if (!enabled != !that.enabled) return false;
        if (scaOwner != that.scaOwner) return false;
        if (tarariOwner != that.tarariOwner) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (!enabled ? 1 : 0);
        result = 31 * result + (tarariOwner ? 1 : 0);
        result = 31 * result + (scaOwner ? 1 : 0);
        result = 31 * result + (databaseUrlTemplate != null ? databaseUrlTemplate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<node ");
        sb.append("id=\"").append(guid).append("\" ");
        sb.append("name=\"").append(name).append("\" ");
        sb.append("disabled=\"").append(!enabled).append("\" ");
        sb.append(">\n");
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
        sb.append("  </node>");
        return sb.toString();
    }

}
