/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel.folder;

/**
 * An entity which can have a folder reference implements this to allow any client code dealing with entities
 * to know it has a folder
 */
public interface HasFolder {
    /**
     * @return the folder. Can be null as some implementations may have entity instances which are not
     * associated with a folder
     */
    public Folder getFolder();

    /**
     * @param folder the folder to associate with an entity. May be null.
     */
    public void setFolder(Folder folder);
}
