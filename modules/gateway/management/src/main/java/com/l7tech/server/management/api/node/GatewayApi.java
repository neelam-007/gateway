package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.EntityType;

import java.io.Serializable;
import java.util.Collection;
import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * API for management of Gateway from Enterprise Service Manager.
 *
 * @author steve
 */
@WebService(name="Gateway", targetNamespace="http://www.layer7tech.com/management/gateway")
public interface GatewayApi {

    /**
     * Get entities information
     * @param entityTypes: the types of entities retrieved
     * @return
     */
    @WebMethod(operationName="GetEntityInfo")
    @WebResult(name="EntityInfos", targetNamespace="http://www.layer7tech.com/management/gateway")
    Collection<EntityInfo> getEntityInfo(Collection<EntityType> entityTypes) throws GatewayException;
    
    /**
     * Get information on the Cluster
     * 
     * @return the Cluster information.
     */
    @WebMethod(operationName="GetClusterInfo")
    @WebResult(name="ClusterInfo", targetNamespace="http://www.layer7tech.com/management/gateway")
    ClusterInfo getClusterInfo();
    
    /**
     * Get information on all Gateways in the cluster.
     * 
     * @return The set of gateway information.
     */
    @WebMethod(operationName="GetGatewayInfo")
    @WebResult(name="GatewayInfos", targetNamespace="http://www.layer7tech.com/management/gateway")
    Collection<GatewayInfo> getGatewayInfo();
                    
    @XmlRootElement(name="ClusterInfo", namespace="http://www.layer7tech.com/management/gateway")
    static final class ClusterInfo implements Serializable {
        private String clusterHostname;
        private int clusterHttpPort;
        private int clusterHttpsPort;

        @XmlAttribute(name="clusterHostname")
        public String getClusterHostname() {
            return clusterHostname;
        }

        public void setClusterHostname(String clusterHostname) {
            this.clusterHostname = clusterHostname;
        }                

        @XmlAttribute(name="clusterHttpPort")
        public int getClusterHttpPort() {
            return clusterHttpPort;
        }

        public void setClusterHttpPort(int clusterHttpPort) {
            this.clusterHttpPort = clusterHttpPort;
        }

        @XmlAttribute(name="clusterHttpsPort")
        public int getClusterHttpsPort() {
            return clusterHttpsPort;
        }

        public void setClusterHttpsPort(int clusterHttpsPort) {
            this.clusterHttpsPort = clusterHttpsPort;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClusterInfo that = (ClusterInfo) o;

            if (clusterHttpPort != that.clusterHttpPort) return false;
            if (clusterHttpsPort != that.clusterHttpsPort) return false;
            if (clusterHostname != null ? !clusterHostname.equals(that.clusterHostname) : that.clusterHostname != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (clusterHostname != null ? clusterHostname.hashCode() : 0);
            result = 31 * result + clusterHttpPort;
            result = 31 * result + clusterHttpsPort;
            return result;
        }
    }

    @XmlRootElement(name="EntityInfo", namespace="http://www.layer7tech.com/management/gateway")
    static final class EntityInfo implements Serializable {
        private String id;
        private String parentId;
        private String name;
        private Integer version;
        private EntityType entityType;

        public EntityInfo() {
        }

        public EntityInfo( final EntityType entityType,
                           final String id,
                           final String name,
                           final String parentId,
                           final int version ) {
            this.entityType = entityType;
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.version = version;
        }

        @XmlAttribute(name="entityType")
        public EntityType getEntityType() {
            return entityType;
        }

        public void setEntityType(EntityType entityType) {
            this.entityType = entityType;
        }

        @XmlAttribute(name="id")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @XmlAttribute(name="name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlAttribute(name="parentId")
        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        @XmlAttribute(name="version")
        public int getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }
    
    @XmlRootElement(name="GatewayInfo", namespace="http://www.layer7tech.com/management/gateway")
    static final class GatewayInfo implements Serializable {
        private String id;
        private String name;
        private String softwareVersion;
        private String ipAddress;
        private long statusTimestamp;

        @XmlAttribute(name="id")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @XmlAttribute(name="name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlAttribute(name="softwareVersion")
        public String getSoftwareVersion() {
            return softwareVersion;
        }

        public void setSoftwareVersion(String softwareVersion) {
            this.softwareVersion = softwareVersion;
        }

        @XmlAttribute(name="ipAddress")
        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        @XmlAttribute(name="statusTimestamp")
        public long getStatusTimestamp() {
            return statusTimestamp;
        }

        public void setStatusTimestamp(long statusTimestamp) {
            this.statusTimestamp = statusTimestamp;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GatewayInfo that = (GatewayInfo) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (softwareVersion != null ? !softwareVersion.equals(that.softwareVersion) : that.softwareVersion != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (id != null ? id.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (softwareVersion != null ? softwareVersion.hashCode() : 0);
            result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
            return result;
        }
    }

    static class GatewayException extends Exception {
        public GatewayException(String message) {
            super(message);
        }

        public GatewayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
