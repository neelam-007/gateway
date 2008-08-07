/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.HasFeatures;
import org.hibernate.annotations.IndexColumn;

import javax.persistence.*;
import java.util.*;

/** @author alex */
@Entity
public class NodeConfig extends NamedEntityImp implements HasFeatures<NodeFeature> {
    protected HostConfig host;
    protected List<DatabaseConfig> databases = new ArrayList<DatabaseConfig>();
    /**
     * Map of SSG Connector to node-local IP address
     */
    protected transient Set<ConnectorConfig> connectors = new HashSet<ConnectorConfig>();

    @ManyToOne(cascade= CascadeType.ALL, optional=false)
    public HostConfig getHost() {
        return host;
    }

    public void setHost(HostConfig host) {
        this.host = host;
    }

    @OneToMany(cascade=CascadeType.ALL, mappedBy = "node", fetch = FetchType.EAGER)
    public Set<ConnectorConfig> getConnectors() {
        return connectors;
    }

    public void setConnectors(Set<ConnectorConfig> connectors) {
        this.connectors = connectors;
    }

    @OneToMany(cascade=CascadeType.ALL, mappedBy = "node", fetch=FetchType.EAGER)
    @IndexColumn(name="ordinal", nullable=false)
    public List<DatabaseConfig> getDatabases() {
        return databases;
    }

    public void setDatabases(List<DatabaseConfig> databases) {
        this.databases = databases;
    }

    @Transient
    public Set<NodeFeature> getFeatures() {
        return Collections.emptySet();
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
