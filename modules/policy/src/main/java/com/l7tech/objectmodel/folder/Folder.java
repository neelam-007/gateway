package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.imp.NamedEntityImp;


/**
 * Represents a service/policy folder.
 */
public class Folder extends NamedEntityImp {
    private Folder parentFolder;

    public Folder getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(Folder parentFolder) {
        this.parentFolder = parentFolder;
    }

    @Deprecated // For Serialization and persistence only
    public Folder() {
    }

    public Folder(String name, Long parentFolderOid) {
        this._name = name;
        this.parentFolderOid = parentFolderOid;
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
        setParentFolderOid(folder.getParentFolderOid());
    }

    private Long parentFolderOid;

    public Long getParentFolderOid() {
        return parentFolderOid;
    }

    public void setParentFolderOid(Long parentFolderOid) {
        this.parentFolderOid = parentFolderOid;
    }
}
