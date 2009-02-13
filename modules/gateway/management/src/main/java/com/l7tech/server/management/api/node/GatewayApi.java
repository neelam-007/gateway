package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.EntityType;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;

/**
 * API for management of Gateway from Enterprise Service Manager.
 *
 * @author steve
 */
@WebService(name="Gateway", targetNamespace="http://www.layer7tech.com/management/gateway")
public interface GatewayApi {

    /**
     * Get entities information.
     *
     * @param entityTypes: the types of entities retrieved, or null to retrieve all entity types currently supported
     *                     by this method on this Gateway.
     * @return a Collection containing one EntityInfo for each Entity instance on this Gateway cluster of one of the
     *         requested types.  May be empty but never null.
     */
    @WebMethod(operationName="GetEntityInfo")
    @WebResult(name="EntityInfos", targetNamespace="http://www.layer7tech.com/management/gateway")
    Collection<EntityInfo> getEntityInfo(  @WebParam(name="EntityTypes") Collection<EntityType> entityTypes) throws GatewayException;
    
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
        private int adminAppletPort;

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

        @XmlAttribute(name="adminAppletPort")
        public int getAdminAppletPort() {
            return adminAppletPort;
        }

        public void setAdminAppletPort(int adminAppletPort) {
            this.adminAppletPort = adminAppletPort;
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
    static final class EntityInfo implements Serializable, Comparable {
        private String id;
        private String externalId;
        private String relatedId; // Store the OID of a real published service / policy fragment if the entity is an alias.  relatedId = null if the entity is not an alias.
        private String parentId;
        private String name;
        private String description;
        private Integer version;
        private EntityType entityType;
        private String[] operations;

        public EntityInfo() {
        }

        public EntityInfo( final EntityType entityType,
                           final String externalId,
                           final String id,
                           final String relatedId,
                           final String name,
                           final String description,
                           final String parentId,
                           final Integer version ) {
            this.entityType = entityType;
            this.externalId = externalId;
            this.id = id;
            this.relatedId = relatedId;
            this.name = name;
            this.description = description;
            this.parentId = parentId;
            this.version = version;
        }

        @XmlAttribute(name="operations")
        public String[] getOperations() {
            return operations;
        }

        public void setOperations(String[] operations) {
            this.operations = operations;
        }

        @XmlAttribute(name="entityType")
        public EntityType getEntityType() {
            return entityType;
        }

        public void setEntityType(EntityType entityType) {
            this.entityType = entityType;
        }

        @XmlAttribute(name="externalId")
        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        @XmlAttribute(name="id")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @XmlAttribute(name="relatedId")
        public String getRelatedId() {
            return relatedId;
        }

        public void setRelatedId(String relatedId) {
            this.relatedId = relatedId;
        }

        @XmlAttribute(name="name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlAttribute(name="description")
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @XmlAttribute(name="parentId")
        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        @XmlAttribute(name="version")
        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        @Override
        public int compareTo(Object o) {
            return name.compareToIgnoreCase(((EntityInfo)o).getName());
        }
    }
    
    @XmlRootElement(name="GatewayInfo", namespace="http://www.layer7tech.com/management/gateway")
    static final class GatewayInfo implements Serializable {
        private String id;
        private String name;
        private String softwareVersion;
        private String ipAddress;
        private long statusTimestamp;
        private int gatewayPort;
        private int processControllerPort;

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

        @XmlAttribute(name="gatewayPort")
        public int getGatewayPort() {
            return gatewayPort;
        }

        public void setGatewayPort(int gatewayPort) {
            this.gatewayPort = gatewayPort;
        }

        @XmlAttribute(name="processControllerPort")
        public int getProcessControllerPort() {
            return processControllerPort;
        }

        public void setProcessControllerPort(int processControllerPort) {
            this.processControllerPort = processControllerPort;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GatewayInfo that = (GatewayInfo) o;

            if (gatewayPort != that.gatewayPort) return false;
            if (processControllerPort != that.processControllerPort) return false;
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
            result = 31 * result + gatewayPort;
            result = 31 * result + processControllerPort;
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
