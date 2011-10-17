package com.l7tech.server.folder;

import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;

import java.util.List;

/**
 * Cache for Folder entities
 */
public interface FolderCache {

    /**
     * Find a folder by primary key.
     *
     * @param oid The primary key for the folder.
     * @return The folder or none if not found.
     */
    Option<Folder> findByPrimaryKey( long oid );

    /**
     * Find the path to a folder by primary key.
     *
     * @param oid The primary key for the folder.
     * @return The path to the folder or an empty list if not found.
     */
    List<Folder> findPathByPrimaryKey(long oid);
}
