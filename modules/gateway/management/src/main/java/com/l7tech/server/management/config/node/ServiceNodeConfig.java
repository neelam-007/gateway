/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.management.config.gateway.GatewayConfig;
import com.l7tech.server.management.config.gateway.HasFeatures;
import org.hibernate.annotations.IndexColumn;

import javax.persistence.*;
import java.util.*;

/** @author alex */
@Entity
public class ServiceNodeConfig extends NamedEntityImp implements HasFeatures<ServiceNodeFeature> {
    protected GatewayConfig gateway;
    protected List<DatabaseConfig> databases = new ArrayList<DatabaseConfig>();
    /**
     * Map of SSG Connector to node-local IP address
     */
    protected transient Set<ConnectorConfig> connectors = new HashSet<ConnectorConfig>();

    @ManyToOne(cascade= CascadeType.ALL, optional=false)
    public GatewayConfig getGateway() {
        return gateway;
    }

    public void setGateway(GatewayConfig gateway) {
        this.gateway = gateway;
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
    public Set<ServiceNodeFeature> getFeatures() {
        return Collections.emptySet();
    }

    /** @author alex */
    public static enum ClusterType {
        /** The database is on this Gateway and is not replicated */
        STANDALONE,

        /** The replication master for this partition is on this gateway */
        REPL_MASTER,

        /** The replication slave for this partition is on this gateway */
        REPL_SLAVE
    }
}
