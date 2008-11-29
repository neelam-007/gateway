package com.l7tech.objectmodel.folder;

/**
 * An entity which can have a folder reference oid implements this to allow any client code dealing with entities
 * to know it has a folder
 *
 * @author darmstrong
 */
public interface HasFolderOid {
    /**
     * @return Long the folder oid. Can be null as some implementations may have entity instances which are not
     * associated with a folder
     */
    public Long getFolderOid();

    /**
     * @param folderOid the folder oid to associate with an entity. May be null.
     */
    public void setFolderOid(Long folderOid);
}

