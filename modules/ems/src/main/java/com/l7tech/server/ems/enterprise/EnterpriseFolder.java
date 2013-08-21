package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Proxy;
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
@Proxy(lazy=false)
@Table(name="enterprise_folder")
public class EnterpriseFolder extends NamedEntityImp implements JSON.Convertible {

    public static final int MAX_NAME_LENGTH = 128;
    public static final String ILLEGAL_CHARACTERS = "/";
    public static final String DEFAULT_ROOT_FOLDER_NAME = "My Enterprise";

    private String guid;

    /** <code>null</code> if this is root folder. */
    private EnterpriseFolder parentFolder;

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
    public void setParentFolder(EnterpriseFolder folder) {
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
    @Override
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, guid);
        output.add(JSONConstants.PARENT_ID, parentFolder == null ? null : parentFolder.getGuid());
        output.add(JSONConstants.TYPE, JSONConstants.EntityType.ENTERPRISE_FOLDER);
        output.add(JSONConstants.NAME, _name);
    }

    // Implements JSON.Convertible
    @Override
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }
}
