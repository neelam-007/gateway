package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.Goid;

/**
 * An entity which can have a folder reference oid implements this to allow any client code dealing with entities
 * to know it has a folder
 *
 * @author darmstrong
 */
public interface HasFolderGoid {
    /**
     * @return Goid the folder goid. Can be null as some implementations may have entity instances which are not
     * associated with a folder
     */
    public Goid getFolderGoid();

    /**
     * @param folderGoid the folder goid to associate with an entity. May be null.
     */
    public void setFolderGoid(Goid folderGoid);
}

