package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * Represents a service/policy folder.
 */
@XmlRootElement
public class Folder extends NamedEntityImp implements HasFolder {
    private Folder parentFolder;
    private static final int MAX_NESTING_CHECK_LEVEL = 1000;

    public Folder(String name, Folder parentFolder) {
        this._name = name;
        this.parentFolder = parentFolder;
    }

    @Deprecated // For Serialization and persistence only
    public Folder() { }

    @Override
    @Migration(mapName = NONE, mapValue = NONE)
    public Folder getFolder() {
        return parentFolder;
    }

    @Override
    @Deprecated // For Serialization and persistence only; don't want exceptions thrown, like the alternative reParent() does
    public void setFolder(Folder parentFolder) {
        this.parentFolder = parentFolder;
    }

    public void reParent(Folder parentFolder) throws InvalidParentFolderException {
        if (isParentOf(parentFolder))
            throw new InvalidParentFolderException("The destination folder is a subfolder of the source folder");
        this.parentFolder = parentFolder;
    }

    /**
     * Create a copy of the given policy.
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

    /**
     * @return true if this folder is an ancestor of the targetFolder, false otherwise
     */
    public boolean isParentOf(Folder targetFolder) {
        return getNesting(targetFolder) > 0;
    }

    /**
     * @return -1 if the current folder is not an ancestor of the maybeChild folder,
     *          0 if the current folder is the same as the maybeChild folder, or
     *          the nesting level between the maybeChild folder and the current folder, if the current folder is an ancestor of the maybeChild folder
     */
    public int getNesting(Folder maybeChild) {
        if (maybeChild == null)
            return -1;

        if (_oid == maybeChild.getOid())
            return 0;

        int nesting = 0;
        Folder parent = maybeChild.getFolder();
        while(parent != null && nesting++ < MAX_NESTING_CHECK_LEVEL) {
            if (parent.getOid() == _oid)
                return nesting;
            parent = parent.getFolder();
        }
        return -1;
    }

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
