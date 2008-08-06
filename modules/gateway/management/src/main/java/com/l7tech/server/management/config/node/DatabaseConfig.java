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
    private ServiceNodeConfig node;
    private DatabaseType type = DatabaseType.GATEWAY_ALL;
    private Vendor vendor = Vendor.MYSQL;
    private ServiceNodeConfig.ClusterType clusterType = ServiceNodeConfig.ClusterType.STANDALONE;
    private String host;
    private int port;
    private String username;
    private String configurationPassword;
    private String serviceNodePassword;
    private int ordinal;

    @ManyToOne(optional=false)
    public ServiceNodeConfig getNode() {
        return node;
    }

    public void setNode(ServiceNodeConfig node) {
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

    public String getServiceNodePassword() {
        return serviceNodePassword;
    }

    public void setServiceNodePassword(String serviceNodePassword) {
        this.serviceNodePassword = serviceNodePassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
    public ServiceNodeConfig.ClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(ServiceNodeConfig.ClusterType clusterType) {
        this.clusterType = clusterType;
    }

    @Column(updatable = false)
    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public String getConfigurationPassword() {
        return configurationPassword;
    }

    public void setConfigurationPassword(String configurationPassword) {
        this.configurationPassword = configurationPassword;
    }

    public enum Vendor {
        MYSQL("jdbc:mysql://${host}:${port}/${db}", 3306);

        private final int port;
        private final String urlTemplate;

        private Vendor(String urlTemplate, int port) {
            this.urlTemplate = urlTemplate;
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        public String getUrlTemplate() {
            return urlTemplate;
        }
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DatabaseConfig that = (DatabaseConfig)o;

        if (port != that.port) return false;
        if (clusterType != that.clusterType) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (node != null ? !node.equals(that.node) : that.node != null) return false;
        if (serviceNodePassword != null ? !serviceNodePassword.equals(that.serviceNodePassword) : that.serviceNodePassword != null) return false;
        if (type != that.type) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
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
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (serviceNodePassword != null ? serviceNodePassword.hashCode() : 0);
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