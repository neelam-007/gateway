package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.mortbay.util.ajax.JSON;

import javax.persistence.*;
import java.util.Map;
import java.util.UUID;

/**
 * Enterprise folders are used for organizing SSG Clusters into a tree.
 * An enterprise folder can contain 0 or more SSG Clusters, plus 0 or more child folders.
 * Sibling folders and SSG Clusters must have different names.
 * The tree has a single root, which is a folder with a <code>null</code> parent folder.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
@Entity
@Table(name="enterprise_folder")
public class EnterpriseFolder extends NamedEntityImp implements JSON.Convertible {

    public static final int MAX_NAME_LENGTH = 128;
    public static final String ILLEGAL_CHARACTERS = "/";
    public static final String DEFAULT_ROOT_FOLDER_NAME = "My Enterprise";

    private String guid;

    /** <code>null</code> if this is root folder. */
    private EnterpriseFolder parentFolder;

    public static void verifyLegalName(String name) throws InvalidNameException {
        if (name.length() == 0) throw new InvalidNameException("Name must not be empty.");
        if (name.length() > MAX_NAME_LENGTH) throw new InvalidNameException("Name must not exceed " + MAX_NAME_LENGTH + " characters");
        if (name.matches(ILLEGAL_CHARACTERS)) throw new InvalidNameException("Name must not contain these characters: " + ILLEGAL_CHARACTERS);
    }

    public static void verifyNoDuplicateName(String name, EnterpriseFolder folder) throws InvalidNameException {
// TODO
//        for (EnterpriseFolder childFolder : folder.getChildFolders()) {
//            if (childFolder.getName().equals(name)) {
//                throw new InvalidNameException("A child folder of the same name (" + name + ") already exists in this folder (" + folder.getName() + ").");
//            }
//        }

//        for (SSGCluster childCluster : folder.getChildSSGClusters()) {
//            if (childCluster.getName().equals(name)) {
//                throw new InvalidNameException("An SSG Cluster of the same name (" + name + ") already exists in this folder (" + folder.getName() + ").");
//            }
//        }
    }

    @Deprecated // For serialization and persistence only
    public EnterpriseFolder() {
    }

    /**
     *
     * @param name
     * @param parentFolder      <code>null</code> if creating root folder
     * @throws InvalidNameException
     */
    public EnterpriseFolder(String name, EnterpriseFolder parentFolder) throws InvalidNameException {
        setName(name);
        setParentFolder(parentFolder);
        setGuid(UUID.randomUUID().toString());
    }

    @Override
    public void setName(String name) throws InvalidNameException {
        verifyLegalName(name);
        verifyNoDuplicateName(name, parentFolder);
        super.setName(name);
    }

    @Column(name="guid", length=36, nullable=false, unique=true, updatable=false)
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @ManyToOne
    @JoinColumn(name="parent_folder_oid", nullable=true)
    public EnterpriseFolder getParentFolder() {
        return parentFolder;
    }

    /**
     * @param folder    the parent folder; use null if this is the root folder
     * @throws InvalidNameException if the target parent folder already has an entity of the same name
     */
    public void setParentFolder(EnterpriseFolder folder) throws InvalidNameException {
        if (folder == null) {
            // TODO verify root folder does not exists already
        } else {
            verifyNoDuplicateName(_name, folder);
        }
        parentFolder = folder;
    }

    @Transient
    public boolean isRoot() {
        return parentFolder == null;
    }

    @Override
    public String toString() {
        return _name;
    }

    // Implements JSON.Convertible
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, guid);
        output.add(JSONConstants.PARENT_ID, parentFolder == null ? null : parentFolder.getGuid());
        output.add(JSONConstants.TYPE, JSONConstants.Entity.ENTERPRISE_FOLDER);
        output.add(JSONConstants.NAME, _name);
        output.add(JSONConstants.MOVABLE, !isRoot());
        output.add(JSONConstants.ACCESS_STATUS, true); // Hard-coded to true until we have folder RBAC.
    }

    // Implements JSON.Convertible
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }
}
