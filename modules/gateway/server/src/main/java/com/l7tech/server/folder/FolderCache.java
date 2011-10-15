package com.l7tech.server.folder;

import com.l7tech.objectmodel.folder.Folder;

import java.util.List;

/**
 * Cache for Folder entities
 */
public interface FolderCache {

    Folder findByPrimaryKey(long oid);

    List<Folder> findPathByPrimaryKey(long oid);
}
