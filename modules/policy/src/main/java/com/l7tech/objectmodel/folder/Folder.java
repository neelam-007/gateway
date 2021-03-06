package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.security.rbac.RbacAttribute;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * Represents a service/policy folder.
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="folder")
public class Folder extends ZoneableNamedEntityImp implements HasFolder {
    private Folder parentFolder;
    private static final int MAX_NESTING_CHECK_LEVEL = 1000;
    public static final Goid ROOT_FOLDER_ID = new Goid(0,-5002);

    public Folder(String name, Folder parentFolder) {
        this._name = name;
        this.parentFolder = parentFolder;
    }

    /**
     * Create a copy of the given folder.
     *
     * <p>This will copy the identity of the orginal, if you don't want this
     * you will need to reset the id and version.</p>
     *
     * @param folder The policy to duplicate.
     */
    public Folder(final Folder folder) {
        super(folder);
        this.parentFolder = folder.getFolder();
    }

    @Deprecated
    @Override
    public void setId(String id) {
        checkLocked();
        if ("-5002".equals(id)) {
            setGoid(ROOT_FOLDER_ID);
        } else {
            super.setId(id);
        }
    }

    /**
     * Create a copy of the given folder.
     *
     *
     * <p>This will copy the identity of the orginal, if you don't want this
     * you will need to reset the id and version.</p>
     *
     * @param folder The policy to duplicate.
     * @param readOnly True for a read-only copy
     */
    public Folder( final Folder folder, final boolean readOnly ) {
        this( folder );
        if (readOnly) this.lock();
    }

    @Deprecated // For Serialization and persistence only
    public Folder() { }

    @RbacAttribute
    @Size(min=1,max=128)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Override
    @Migration(mapName = NONE, mapValue = NONE)
    @ManyToOne
    @JoinColumn(name="parent_folder_goid")
    public Folder getFolder() {
        return parentFolder;
    }

    @Override
    @Deprecated // For Serialization and persistence only; don't want exceptions thrown, like the alternative reParent() does
    public void setFolder(Folder parentFolder) {
        checkLocked();
        this.parentFolder = parentFolder;
    }

    public void reParent(Folder parentFolder) throws InvalidParentFolderException {
        checkLocked();
        if (Goid.equals(parentFolder.getGoid(), getGoid()) || isParentOf(parentFolder))
            throw new InvalidParentFolderException("The destination folder is a subfolder of the source folder");
        this.parentFolder = parentFolder;
    }

    /**
     * @return true if this folder is an ancestor of the targetFolder, false otherwise
     */
    @Transient
    public boolean isParentOf(Folder targetFolder) {
        return getNesting(targetFolder) > 0;
    }

    /**
     * @return -1 if the current folder is not an ancestor of the maybeChild folder,
     *          0 if the current folder is the same as the maybeChild folder, or
     *          the nesting level between the maybeChild folder and the current folder, if the current folder is an ancestor of the maybeChild folder
     */
    @Transient
    public int getNesting(Folder maybeChild) {
        if (maybeChild == null)
            return -1;

        if (Goid.equals(getGoid(), maybeChild.getGoid()))
            return 0;

        int nesting = 0;
        Folder parent = maybeChild.getFolder();
        while(parent != null && nesting++ < MAX_NESTING_CHECK_LEVEL) {
            if (Goid.equals(parent.getGoid(), getGoid()))
                return nesting;
            parent = parent.getFolder();
        }
        return -1;
    }

    @Transient
    public String getPath() {
        if (parentFolder == null) return "/";

        StringBuilder sb = new StringBuilder();
        Folder current = this;
        int nesting = 0;
        while(current.getFolder() != null && nesting++ < MAX_NESTING_CHECK_LEVEL) {
            sb.append(new StringBuilder(current.getName()).reverse()).append("/");
            current = current.getFolder();
        }
        return sb.reverse().toString();
    }
}
