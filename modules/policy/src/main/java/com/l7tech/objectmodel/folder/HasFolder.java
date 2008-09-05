package com.l7tech.objectmodel.folder;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 8, 2008
 * Time: 8:45:51 AM
 * An entity which can have a folder reference oid implements this to allow any client code dealing with entities
 * to know it has a folder
 */
public interface HasFolder {
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

