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
    public Long getFolderOid();

    public void setFolderOid(long folderOid);
}

