package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.mortbay.util.ajax.JSON;

import javax.persistence.*;
import java.util.Map;
import java.util.UUID;

/**
 * Encapsulates info of an SSG Cluster as known to the Enterprise Manager.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
@Entity
@Table(name="ssg_cluster")
public class SsgCluster extends NamedEntityImp implements JSON.Convertible {

    public static final int MAX_NAME_LENGTH = 128;
    public static final String ILLEGAL_CHARACTERS = "/";

    private String guid;

    /** The cluster's SSL host name; same as the cluster host name or the load balancer in front. */
    private String sslHostName;

    /** Port number of the administrative interface. */
    private int adminPort;

    private boolean trustStatus;

    private EnterpriseFolder parentFolder;

    public static void verifyLegalName(String name) throws InvalidNameException {
        if (name.length() == 0) throw new InvalidNameException("Name must not be empty.");
        if (name.length() > MAX_NAME_LENGTH) throw new InvalidNameException("Name must not exceed " + MAX_NAME_LENGTH + " characters");
        if (name.matches(ILLEGAL_CHARACTERS)) throw new InvalidNameException("Name must not contain these characters: " + ILLEGAL_CHARACTERS);
    }

    @Deprecated // For serialization and persistence only
    public SsgCluster() {
    }

    public SsgCluster(String name, String sslHostName, int adminPort, EnterpriseFolder parentFolder) {
        setGuid(UUID.randomUUID().toString());
        this._name = name;
        this.sslHostName = sslHostName;
        this.adminPort = adminPort;
        this.parentFolder = parentFolder;
    }

    @Override
    public void setName(String name) throws InvalidNameException {
        verifyLegalName(name);
        // TODO verify unique name amongst siblings
        super.setName(name);
    }

    @Column(name="admin_port", nullable=false)
    public int getAdminPort() {
        return adminPort;
    }

    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
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
        // TODO verify unique name amongst siblings
        this.parentFolder = parentFolder;
    }

    @Column(name="ssl_host_name", length=128, nullable=false)
    public String getSslHostName() {
        return sslHostName;
    }

    public void setSslHostName(String sslHostName) {
        this.sslHostName = sslHostName;
    }

    @Column(name="trust_status", nullable=false)
    public boolean getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(boolean trustStatus) {
        this.trustStatus = trustStatus;
    }

    @Override
    public String toString() {
        return _name;
    }

    // Implements JSON.Convertible
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, guid);
        output.add(JSONConstants.PARENT_ID, parentFolder.getGuid());
        output.add(JSONConstants.TYPE, JSONConstants.Entity.SSG_CLUSTER);
        output.add(JSONConstants.NAME, _name);
        output.add(JSONConstants.RBAC_CUD, true); // TODO Should be true only for user with administrator role.
// TODO       output.add(JSONConstants.ANCESTORS, );
        output.add(JSONConstants.TRUST_STATUS, trustStatus);
        output.add(JSONConstants.ACCESS_STATUS, false); // TODO
        output.add(JSONConstants.SSL_HOST_NAME, sslHostName);
        output.add(JSONConstants.ADMIN_PORT, Integer.toString(adminPort));
        output.add(JSONConstants.DB_HOSTS, new String[]{"X"}); // TODO
    }

    // Implements JSON.Convertible
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }
}
