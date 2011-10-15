package com.l7tech.server.folder;

import com.l7tech.objectmodel.folder.Folder;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class FolderCacheStub implements FolderCache {

    @Override
    public Folder findByPrimaryKey( final long oid ) {
        return null;
    }

    @Override
    public List<Folder> findPathByPrimaryKey( final long oid ) {
        return Collections.emptyList();
    }
}
