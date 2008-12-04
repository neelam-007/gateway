package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.*;

import java.util.List;

/**
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public interface EnterpriseFolderManager extends EntityManager<EnterpriseFolder, EntityHeader> {

    /**
     * Creates an enterprise folder.
     *
     * @param name              must conform to name rules
     * @param parentFolder      use <code>null</code> if creating root folder
     * @return never <code>null</code>
     * @throws InvalidNameException if <code>name</code> does not conform to name rules
     * @throws SaveException if the new folder cannot be persisted
     */
    EnterpriseFolder create(String name, EnterpriseFolder parentFolder) throws InvalidNameException, SaveException, FindException;

    /**
     * Creates an enterprise folder.
     *
     * @param name              must conform to name rules
     * @param parentFolderGuid  use <code>null</code> if creating root folder
     * @return never <code>null</code>
     * @throws FindException if failed to determine if a folder with <code>parentFolderGuid</code> exists
     * @throws InvalidNameException if <code>name</code> does not conform to name rules
     * @throws SaveException if the new folder cannot be persisted
     */
    EnterpriseFolder create(String name, String parentFolderGuid) throws FindException, InvalidNameException, SaveException;

    /**
     * Rename the name of the folder with the guid.
     * @param name: the new name of the folder
     * @param guid: the guid of the folder
     * @throws FindException if failed to determine if a folder with <code>parentFolderGuid</code> exists
     * @throws InvalidNameException if <code>name</code> does not conform to name rules
     * @throws SaveException if the new folder cannot be persisted
     */
    void renameByGuid(String name, String guid) throws FindException, UpdateException;

    /**
     * Deletes a folder with the given guid without Cascade Deletion.  If the folder has descendents, then the folder
     * won't be deleted and an exception will throw.
     *
     * Note: this method is the default deletion method.
     *
     * @param guid      GUID of the folder
     * @throws FindException if no folder with such GUID
     * @throws DeleteException if database delete failed
     */
    void deleteByGuid(String guid) throws FindException, DeleteException;

    /**
     * Deletes a folder with the given guid depending on the cascade flag.
     *
     * @param guid      GUID of the folder
     * @param deleteByCascade: if the flag is true, then all descendents will be deleted.
     *                         Otherwise, the method is the same as the default deletion method.
     * @throws FindException if no folder with such GUID
     * @throws DeleteException if database delete failed
     */
    void deleteByGuid(String guid, boolean deleteByCascade) throws FindException, DeleteException;

    /**
     * Finds the root folder.
     *
     * @return the root folder
     * @throws FindException if failed to query database
     */
    EnterpriseFolder findRootFolder() throws FindException;

    /**
     * Finds a folder with the given GUID.
     *
     * @param guid  the folder GUID
     * @return the folder found; <code>null</code> if no folder with the given GUID
     * @throws FindException if failed to query database
     */
    EnterpriseFolder findByGuid(String guid) throws FindException;

    /**
     * Find child folders of a given folder guid.
     *
     * @param parentFolderGuid  the guid of the parent folder
     * @return list of child folders
     * @throws FindException if failed to query database
     */
    List<EnterpriseFolder> findChildFolders(String parentFolderGuid) throws FindException;

    /**
     * Find child folders of a given folder.
     *
     * @param parentFolder  the parent folder
     * @return list of child folders
     * @throws FindException if failed to query database
     */
    List<EnterpriseFolder> findChildFolders(final EnterpriseFolder parentFolder) throws FindException;
}
