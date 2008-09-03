/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.*;

/**
 * Inherited {@link #_name} is the database name.
 * @author alex
 */
@Entity
@Table(name="pc_db")
public class DatabaseConfig extends NamedEntityImp {
    private NodeConfig node;
    private DatabaseType type = DatabaseType.NODE_ALL;
    private Vendor vendor = Vendor.MYSQL;
    private NodeConfig.ClusterType clusterType = NodeConfig.ClusterType.STANDALONE;
    private String host;
    private int port;
    private String databaseAdminUsername;
    private String databaseAdminPassword;
    private String nodeUsername;
    private String nodePassword;
    private int ordinal;

    public DatabaseConfig() {
    }

    /**
     * Create a copy of the configuration but not identity of the given configuration.
     *
     * @param config The config to copy.
     */
    public DatabaseConfig( final DatabaseConfig config ) {
        setType( config.getType() );
        setVendor( config.getVendor() );
        setHost( config.getHost() );
        setPort( config.getPort() );
        setName( config.getName() );
        setNodeUsername( config.getNodeUsername() );
        setNodePassword( config.getNodePassword() );
        setDatabaseAdminUsername( config.getDatabaseAdminUsername() );
        setDatabaseAdminPassword( config.getDatabaseAdminPassword() );
    }

    public DatabaseConfig( final String host,
                           final int port,
                           final String name,
                           final String nodeUsername,
                           final String nodePassword ) {
        setHost( host );
        setPort( port );
        setName( name );
        setNodeUsername( nodeUsername );
        setNodePassword( nodePassword );
    }

    @ManyToOne(optional=false)
    public NodeConfig getNode() {
        return node;
    }

    public void setNode(NodeConfig node) {
        this.node = node;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNodePassword() {
        return nodePassword;
    }

    public void setNodePassword(String nodePassword) {
        this.nodePassword = nodePassword;
    }

    public String getNodeUsername() {
        return nodeUsername;
    }

    public void setNodeUsername(String nodeUsername) {
        this.nodeUsername = nodeUsername;
    }

    @Enumerated(EnumType.STRING)
    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    @Enumerated(EnumType.STRING)
    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }

    @Enumerated(EnumType.STRING)
    public NodeConfig.ClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(NodeConfig.ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    @Column(updatable = false)
    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    @Transient
    public String getDatabaseAdminPassword() {
        return databaseAdminPassword;
    }

    public void setDatabaseAdminPassword(String databaseAdminPassword) {
        this.databaseAdminPassword = databaseAdminPassword;
    }

    @Transient
    public String getDatabaseAdminUsername() {
        return databaseAdminUsername;
    }

    public void setDatabaseAdminUsername(String databaseAdminUsername) {
        this.databaseAdminUsername = databaseAdminUsername;
    }

    public enum Vendor {
        MYSQL("jdbc:mysql://${host}:${port}/${db}", 3306, "root", "mysql");

        public String getDefaultAdminUsername() {
            return defaultAdminUsername;
        }

        public String getDefaultAdminDatabase() {
            return defaultAdminDatabase;
        }

        private final String defaultAdminUsername;
        private final String defaultAdminDatabase;
        private final int port;
        private final String urlTemplate;

        private Vendor(String urlTemplate, int port, String defaultAdminUsername, String defaultAdminDatabase) {
            this.urlTemplate = urlTemplate;
            this.port = port;
            this.defaultAdminUsername = defaultAdminUsername;
            this.defaultAdminDatabase = defaultAdminDatabase;
        }

        public int getPort() {
            return port;
        }

        public String getUrlTemplate() {
            return urlTemplate;
        }
    }


    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DatabaseConfig that = (DatabaseConfig)o;

        if (port != that.port) return false;
        if (clusterType != that.clusterType) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (node != null ? !node.equals(that.node) : that.node != null) return false;
        if (nodePassword != null ? !nodePassword.equals(that.nodePassword) : that.nodePassword != null) return false;
        if (type != that.type) return false;
        if (nodeUsername != null ? !nodeUsername.equals(that.nodeUsername) : that.nodeUsername != null) return false;
        if (vendor != that.vendor) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (node != null ? node.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (vendor != null ? vendor.hashCode() : 0);
        result = 31 * result + (clusterType != null ? clusterType.hashCode() : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (nodeUsername != null ? nodeUsername.hashCode() : 0);
        result = 31 * result + (nodePassword != null ? nodePassword.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<database ");
        sb.append("id=\"").append(_oid).append("\" ");
        sb.append("ordinal=\"").append(ordinal).append("\" ");
        sb.append("name=\"").append(_name).append("\" ");
        sb.append("host=\"").append(host).append("\" ");
        if (port != 0) sb.append("port=\"").append(port).append("\" ");
        sb.append("vendor=\"").append(vendor).append("\" ");
        sb.append("type=\"").append(type).append("\" ");
        sb.append("clusterType=\"").append(clusterType).append("\" ");
        sb.append("/>");
        return sb.toString();
    }
}