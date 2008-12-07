package com.l7tech.server.folder;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;

/**
 * Manager interface for managing service/policy folders.
 */
public interface FolderManager extends EntityManager<Folder, FolderHeader> {

    /**
     * @return  The root folder
     * @throws FindException    Can't happen, but it will be thrown if there is no root node.
     */
    public Folder findRootFolder() throws FindException;
}