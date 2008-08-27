package com.l7tech.server.folder;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.Folder;

import java.util.Collection;

/**
 * Manager interface for managing service/policy folders.
 */
public interface FolderManager extends EntityManager<Folder, FolderHeader> {
    /**
     * Find the folder headers containing the OrganizationHeader supplied in parameter entityHeaders
     * @param headers Access to view a folder is determined by having access to an entity within a folder, or
     * subfolder. The entity is either a service or a policy. headers is the total list of possible entities
     * on which to determine what folders a user can see
     * @return The collection of headers
     * @throws com.l7tech.objectmodel.FindException if an error occurs
     */
    Collection<FolderHeader> findFolderHeaders(Iterable<? extends OrganizationHeader> headers) throws FindException;

}