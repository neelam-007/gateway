package com.l7tech.server.folder;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.RoleAwareGoidEntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.FolderedEntityManager;

/**
 * Manager interface for managing service/policy folders.
 */
public interface FolderManager extends FolderedEntityManager<Folder, FolderHeader>, RoleAwareGoidEntityManager<Folder> {

    /**
     * @return  The root folder
     * @throws FindException    Can't happen, but it will be thrown if there is no root node.
     */
    public Folder findRootFolder() throws FindException;

    public void addManageFolderRole( Folder folder ) throws SaveException;

    public void addReadonlyFolderRole( Folder folder ) throws SaveException;
}