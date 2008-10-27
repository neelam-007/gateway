/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.config.HasFeatures;
import com.l7tech.server.management.config.PCEntity;
import com.l7tech.server.management.config.host.HostConfig;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElement;
import java.util.*;

/**
 * Node configuration common subset.
 *
 * @author alex
 */
public class NodeConfig extends PCEntity implements HasFeatures<NodeFeature> {
    protected HostConfig host;
    protected SoftwareVersion softwareVersion;
    protected List<DatabaseConfig> databases = new ArrayList<DatabaseConfig>();
    protected boolean enabled = true;

    /**
     * The hostname of the load balancer in front of this partition's nodes
     */
    protected String clusterHostname;

    /**
     * The passphrase for this clusters protected data
     */
    protected String clusterPassphrase;    
    
    @XmlTransient
    public HostConfig getHost() {
        return host;
    }

    public void setHost(HostConfig host) {
        this.host = host;
    }

    @XmlElement(name="database")
    @XmlElementWrapper(name="databases")
    public List<DatabaseConfig> getDatabases() {
        return databases;
    }

    /**
     * Find the DatabaseConfig that is one of the given types.
     *
     * @param databaseType The desired db type
     * @param clusterTypes The acceptable types in preference order.
     * @return The config or null if not found.
     */
    public DatabaseConfig getDatabase( final DatabaseType databaseType,
                                       final ClusterType... clusterTypes ) {
        DatabaseConfig config = null;

        List<DatabaseConfig> configs = databases;
        if ( configs != null ) {
            out: for ( ClusterType clusterType : clusterTypes ) {
                for ( DatabaseConfig dbConfig : configs ) {
                    if ( dbConfig.getType() == databaseType && dbConfig.getClusterType() == clusterType ) {
                        config = dbConfig;
                        break out;
                    }
                }
            }
        }

        return config;
    }

    public void setDatabases(List<DatabaseConfig> databases) {
        this.databases = databases;
    }

    @XmlTransient
    public Set<NodeFeature> getFeatures() {
        return Collections.emptySet();
    }

    @XmlTransient
    public SoftwareVersion getSoftwareVersion() {
        return softwareVersion;
    }

    public void setVersionString(String s) throws NumberFormatException {
        softwareVersion = (s == null ? null : SoftwareVersion.fromString(s));
    }

    public String getVersionString() {
        return softwareVersion == null ? null : softwareVersion.toString();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSoftwareVersion(SoftwareVersion softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getClusterHostname() {
        return clusterHostname;
    }

    public void setClusterHostname(String clusterHostname) {
        this.clusterHostname = clusterHostname;
    }

    public String getClusterPassphrase() {
        return clusterPassphrase;
    }

    public void setClusterPassphrase(String clusterPassphrase) {
        this.clusterPassphrase = clusterPassphrase;
    }
    
    /** @author alex */
    public static enum ClusterType {
        /** The database is on this Host and is not replicated */
        STANDALONE,

        /** The replication master for this partition is on this host */
        REPL_MASTER,

        /** The replication slave for this partition is on this host */
        REPL_SLAVE
    }
}
