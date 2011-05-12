package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.management.api.node.GatewayApi;
import org.hibernate.annotations.*;
import org.mortbay.util.ajax.JSON;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;

/**
 * Encapsulates info of an SSG Cluster as known to the Enterprise Manager.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
@Entity
@Proxy(lazy=false)
@Table(name="ssg_cluster")
public class SsgCluster extends NamedEntityImp implements JSON.Convertible {

    public static final int MAX_NAME_LENGTH = 128;
    public static final String ILLEGAL_CHARACTERS = "/";

    private String guid;

    /** The cluster's SSL host name; same as the cluster host name or the load balancer in front. */
    private String sslHostName;

    private String ipAddress;

    /** Port number of the administrative interface. */
    private int adminPort;

    private int adminAppletPort;

    private String onlineStatus;

    private boolean trustStatus;

    private String statusMessage;

    private String dbHosts;

    private EnterpriseFolder parentFolder;

    private Set<SsgNode> nodes = new HashSet<SsgNode>();

    @Deprecated // For serialization and persistence only
    public SsgCluster() {
    }

    public SsgCluster(String name, String guid, EnterpriseFolder parentFolder) {
        this._name = name;
        this.sslHostName = "";
        this.guid = guid;
        this.parentFolder = parentFolder;
    }

    public SsgCluster(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) {
        setGuid(UUID.randomUUID().toString());
        this._name = name;
        this.sslHostName = sslHostName;
        this.adminPort = adminPort;
        this.parentFolder = parentFolder;
    }

    @Column(name="admin_port", nullable=false)
    public int getAdminPort() {
        return adminPort;
    }

    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
    }

    @Column(name="admin_applet_port")
    public int getAdminAppletPort() {
        return adminAppletPort;
    }

    public void setAdminAppletPort(int adminAppletPort) {
        this.adminAppletPort = adminAppletPort;
    }

    @Column(name="guid", length=36, unique=true, nullable=false)
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="parent_folder_oid", nullable=false)
    public EnterpriseFolder getParentFolder() {
        return parentFolder;
    }

    /**
     * @param parentFolder   the parent folder; must not be null
     */
    public void setParentFolder(EnterpriseFolder parentFolder) {
        this.parentFolder = parentFolder;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="ssgCluster")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE_ORPHAN, org.hibernate.annotations.CascadeType.ALL})
    @OnDelete(action= OnDeleteAction.CASCADE)
    @BatchSize(size=50)
    public Set<SsgNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<SsgNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * Get the node with the given identifier or null if not found.
     *
     * @param nodeGuid The GUID of the node to access.
     * @return The node or null
     */
    public SsgNode getNode( final String nodeGuid ) {
        SsgNode ssgNode = null;

        Set<SsgNode> nodes = getNodes();
        if ( nodes != null && nodeGuid != null ) {
            for ( SsgNode node : nodes ) {
                if ( nodeGuid.equals( node.getGuid() ) ) {
                    ssgNode = node;
                    break;
                }
            }
        }

        return ssgNode;
    }

    /**
     * Get the set of nodes that are currently operational.
     *
     * @return The set of operational SsgNodes.
     */
    @Transient
    public Set<SsgNode> getAvailableNodes() {
        Set<SsgNode> anodes = new LinkedHashSet<SsgNode>();

        Set<SsgNode> nodes = getNodes();
        if ( nodes != null ) {
            for ( SsgNode node : nodes ) {
                if ( JSONConstants.SsgNodeOnlineState.ON.equals( node.getOnlineStatus() ) ) {                
                    anodes.add( node );
                }
            }
        }

        return anodes;
    }

    @Column(name="ssl_host_name", length=128, nullable=false)
    public String getSslHostName() {
        return sslHostName;
    }

    public void setSslHostName(String sslHostName) {
        this.sslHostName = sslHostName;
    }

    @Column(name="ip_address", length=39)
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Column(name="db_hosts", length=128)
    public String getDbHosts() {
        return dbHosts;
    }

    public void setDbHosts(String dbHosts) {
        this.dbHosts = dbHosts;
    }

    // Get a list of database host names.
    public Collection<String> obtainDbHosts() {
        Set<String> set = new HashSet<String>();

        if (dbHosts != null && !dbHosts.isEmpty()) {
            String[] hosts = dbHosts.split("\\,");
            set.addAll(Arrays.asList(hosts));
        }

        return set;
    }

    // Get db host names and store them a single string delimited by ','.
    public void storeDbHosts(Collection<String> dbHosts) {
        if (dbHosts != null && !dbHosts.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String host: dbHosts) {
                builder.append(host).append(",");
            }
            builder.deleteCharAt(builder.length()-1);
            setDbHosts(builder.toString());
        }
    }

    @Column(name="online_status", length=36)
    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    @Column(name="trust_status", nullable=false)
    public boolean getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(boolean trustStatus) {
        this.trustStatus = trustStatus;
    }

    @Column(name="status_message", length=255)
    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage( final String statusMessage ) {
        this.statusMessage = statusMessage;
    }

    /**
     * Is the cluster available? (at least partially up)
     *
     * @return true if available
     */
    @Transient
    public boolean isAvailable() {
        return getTrustStatus() && !"down".equals( getOnlineStatus() );
    }

    @Transient
    public boolean isOffline() {
        return adminPort == 0;
    }

    public Set<GatewayApi.GatewayInfo> obtainGatewayInfoSet() {
        Set<GatewayApi.GatewayInfo> infoSet = new HashSet<GatewayApi.GatewayInfo>();
        for (SsgNode node: nodes) {
            GatewayApi.GatewayInfo info = new GatewayApi.GatewayInfo();
            info.setId(node.getGuid());
            info.setIpAddress(node.getIpAddress());
            info.setName(node.getName());
            info.setSoftwareVersion(node.getSoftwareVersion());
            info.setProcessControllerPort(node.getProcessControllerPort());
            info.setGatewayPort(node.getGatewayPort());

            infoSet.add(info);
        }

        return infoSet;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SsgCluster that = (SsgCluster) o;

        if (adminPort != that.adminPort) return false;
        if (trustStatus != that.trustStatus) return false;
        if (dbHosts != null ? !dbHosts.equals(that.dbHosts) : that.dbHosts != null) return false;
        if (guid != null ? !guid.equals(that.guid) : that.guid != null) return false;
        if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
        if (parentFolder != null ? !parentFolder.equals(that.parentFolder) : that.parentFolder != null) return false;
        if (sslHostName != null ? !sslHostName.equals(that.sslHostName) : that.sslHostName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result = 31 * result + (sslHostName != null ? sslHostName.hashCode() : 0);
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + adminPort;
        result = 31 * result + adminAppletPort;
        result = 31 * result + (trustStatus ? 1 : 0);
        result = 31 * result + (dbHosts != null ? dbHosts.hashCode() : 0);
        result = 31 * result + (parentFolder != null ? parentFolder.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return _name;
    }

    /**
     * Get names of all ancestors.
     * @return a string list.
     */
    public List<String> ancestors() {
        List<String> list = new ArrayList<String>();

        EnterpriseFolder parent = parentFolder;
        while (parent != null) {
            list.add(0, parent.getName());
            parent = parent.getParentFolder();
        }

        return list;
    }

    // Implements JSON.Convertible
    @Override
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, guid);
        output.add(JSONConstants.PARENT_ID, parentFolder.getGuid());
        output.add(JSONConstants.TYPE, JSONConstants.EntityType.SSG_CLUSTER);
        output.add(JSONConstants.NAME, _name);
        output.add(JSONConstants.TRUST_STATUS, isOffline() ? null : trustStatus);
        output.add(JSONConstants.SSL_HOST_NAME, isOffline() ? null : sslHostName);
        output.add(JSONConstants.ADMIN_PORT, Integer.toString(adminPort));
        output.add(JSONConstants.ADMIN_APPLET_PORT, Integer.toString(adminAppletPort));
        output.add(JSONConstants.ONLINE_STATUS, isOffline() ? null : onlineStatus);
        output.add(JSONConstants.STATUS_MESSAGE, statusMessage );
        output.add(JSONConstants.CLUSTER_ANCESTORS, findAllAncestors());
        Collection<String> dbHosts = obtainDbHosts();
        if (dbHosts != null && !dbHosts.isEmpty()) {
            output.add(JSONConstants.DB_HOSTS, dbHosts);
        }
// TODO       output.add(JSONConstants.IP_ADDRESS, ...);
    }

    // Implements JSON.Convertible
    @Override
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }

    /**
     * Find all enterprise folders (i.e., ancestors) of the SSG cluster.
     * @return a list of enterprise folders' names.
     */
    private Object[] findAllAncestors() {
        List<String> ancestorNames = new ArrayList<String>();

        EnterpriseFolder parent = parentFolder;
        while (parent != null) {
            ancestorNames.add(0, parent.getName());
            parent = parent.getParentFolder();
        }

        return ancestorNames.toArray();
    }
}
