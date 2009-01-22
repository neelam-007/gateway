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

    public boolean isParentOf(Folder targetFolder) {
        if (targetFolder == null)
            return false;

        int nesting = 0;
        Folder parent = targetFolder.getFolder();
        while(parent != null && nesting++ < MAX_NESTING_CHECK_LEVEL) {
            if (parent.getOid() == _oid)
                return true;
            parent = parent.getFolder();
        }
        return false;
    }
}
