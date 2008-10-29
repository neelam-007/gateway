/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/**
 * DatabaseConfiguration properties bean.
 *
 * @author alex
 */
public class DatabaseConfig {
    private DatabaseConfig parent;
    private DatabaseType type = DatabaseType.NODE_ALL;
    private Vendor vendor = Vendor.MYSQL;
    private NodeConfig.ClusterType clusterType = NodeConfig.ClusterType.STANDALONE;
    private String name;
    private String host;
    private int port;
    private String databaseAdminUsername;
    private String databaseAdminPassword;
    private String nodeUsername;
    private String nodePassword;

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

    public DatabaseConfig getParent() {
        return parent;
    }

    public void setParent(DatabaseConfig parent) {
        if (parent == this) throw new IllegalArgumentException();
        this.parent = parent;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        if ( port != 0 || parent == null ) {
            return port;
        } else {
            return parent.getPort();
        }
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        if ( name != null || parent == null ) {
            return name;
        } else {
            return parent.getName();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodePassword() {
        if ( nodePassword != null || parent == null ) {
            return nodePassword;
        } else {
            return parent.getNodePassword();
        }
    }

    public void setNodePassword(String nodePassword) {
        this.nodePassword = nodePassword;
    }

    public String getNodeUsername() {
        if ( nodeUsername != null || parent == null ) {
            return nodeUsername;
        } else {
            return parent.getNodeUsername();
        }
    }

    public void setNodeUsername(String nodeUsername) {
        this.nodeUsername = nodeUsername;
    }

    public Vendor getVendor() {
        if ( vendor != null || parent == null ) {
            return vendor;
        } else {
            return parent.getVendor();
        }
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public DatabaseType getType() {
        if ( type != null || parent == null ) {
            return type;
        } else {
            return parent.getType();
        }
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }

    public NodeConfig.ClusterType getClusterType() {
        if ( clusterType != null || parent == null ) {
            return clusterType;
        } else {
            return parent.getClusterType();
        }
    }

    public void setClusterType(NodeConfig.ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    public String getDatabaseAdminPassword() {
        if ( databaseAdminPassword != null || parent == null ) {
            return databaseAdminPassword;
        } else {
            return parent.getDatabaseAdminPassword();
        }
    }

    public void setDatabaseAdminPassword(String databaseAdminPassword) {
        this.databaseAdminPassword = databaseAdminPassword;
    }

    public String getDatabaseAdminUsername() {
        if ( databaseAdminUsername != null || parent == null ) {
            return databaseAdminUsername;
        } else {
            return parent.getDatabaseAdminUsername();
        }
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

        DatabaseConfig that = (DatabaseConfig) o;

        if (port != that.port) return false;
        if (clusterType != that.clusterType) return false;
        if (databaseAdminPassword != null ? !databaseAdminPassword.equals(that.databaseAdminPassword) : that.databaseAdminPassword != null)
            return false;
        if (databaseAdminUsername != null ? !databaseAdminUsername.equals(that.databaseAdminUsername) : that.databaseAdminUsername != null)
            return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (nodePassword != null ? !nodePassword.equals(that.nodePassword) : that.nodePassword != null) return false;
        if (nodeUsername != null ? !nodeUsername.equals(that.nodeUsername) : that.nodeUsername != null) return false;
        if (parent != null ? !parent.equals(that.parent) : that.parent != null) return false;
        if (type != that.type) return false;
        if (vendor != that.vendor) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (parent != null ? parent.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (vendor != null ? vendor.hashCode() : 0);
        result = 31 * result + (clusterType != null ? clusterType.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (databaseAdminUsername != null ? databaseAdminUsername.hashCode() : 0);
        result = 31 * result + (databaseAdminPassword != null ? databaseAdminPassword.hashCode() : 0);
        result = 31 * result + (nodeUsername != null ? nodeUsername.hashCode() : 0);
        result = 31 * result + (nodePassword != null ? nodePassword.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<database ");
        sb.append("host=\"").append(host).append("\" ");
        if (port != 0) sb.append("port=\"").append(port).append("\" ");
        sb.append("vendor=\"").append(vendor).append("\" ");
        sb.append("type=\"").append(type).append("\" ");
        sb.append("name=\"").append(name).append("\" ");
        sb.append("clusterType=\"").append(clusterType).append("\" ");
        sb.append("/>");
        return sb.toString();
    }
}