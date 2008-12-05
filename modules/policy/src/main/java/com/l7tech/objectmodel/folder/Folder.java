package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * Represents a service/policy folder.
 */
@XmlRootElement
public class Folder extends NamedEntityImp {
    private Folder parentFolder;

    public Folder(String name, Folder parentFolder) {
        this._name = name;
        this.parentFolder = parentFolder;
    }

    @Deprecated // For Serialization and persistence only
    public Folder() { }

    public Folder getParentFolder() {
        return parentFolder;
    }

    /**
     * Not deprecated, because setting the parent is the cleanest way to move a folder around.
     */
    public void setParentFolder(Folder parentFolder) {
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
        setParentFolder(folder.getParentFolder());
    }

}
