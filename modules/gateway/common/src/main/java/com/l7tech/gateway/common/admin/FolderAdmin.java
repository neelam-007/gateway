package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;


/**
 * Admin interface for managing service/policy folders.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.FOLDER)
@Administrative
public interface FolderAdmin extends SecurityZoneEntityAdmin<Folder>{

    String ROLE_NAME_TYPE_SUFFIX = "Folder";
    
    /**
     * Retrieve all of the {@link Folder} headers.
     * This method is currently not Secured as there is no RBAC set up in 4.6 for access to entities of type
     * FOLDER. Anybody can call this method, but only users who have permissions on some entities in a folder
     * will get folders returned to them.
     *
     * @return Collection of {@link com.l7tech.objectmodel.folder.FolderHeader}s for services
     * @throws com.l7tech.objectmodel.FindException   if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Administrative(licensed=false)
    Collection<FolderHeader> findAllFolders() throws FindException;

    /**
     * Store the specified new or existing service/policy folder. If the specified {@link com.l7tech.objectmodel.folder.Folder} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     *
     * @param folder the service/policy folder to create or update.  Must not be null.
     * @return the unique object ID that was updated or created.
     * @throws com.l7tech.objectmodel.SaveException   if the requested information could not be saved
     * @throws com.l7tech.objectmodel.UpdateException if the requested information could not be updated
     */
    @Secured(stereotype= MethodStereotype.SAVE_OR_UPDATE, relevantArg=0)
    long saveFolder(Folder folder) throws UpdateException, SaveException, ConstraintViolationException;

    /**
     * Update the parent folder for the given entity.
     *
     * @param folder The target folder (may be null for root)
     * @param entity The entity to move (must not be null)
     * @throws UpdateException if the update fails
     * @throws ConstraintViolationException if the move causes a contraint violation (e.g. duplicate folder name)
     */
    @Secured(types=EntityType.FOLDER, stereotype=MethodStereotype.DELETE_MULTI) //TODO we need an UPDATE_MULTI
    void moveEntityToFolder( Folder folder, PersistentEntity entity ) throws UpdateException, ConstraintViolationException;

    /**
     * Delete a {@link com.l7tech.objectmodel.folder.Folder} by its unique identifier.

     * @param oid the unique identifier of the {@link com.l7tech.objectmodel.folder.Folder} to delete.
     * @throws com.l7tech.objectmodel.DeleteException if the requested information could not be deleted
     */
    @Secured(types=EntityType.FOLDER, stereotype=MethodStereotype.DELETE_BY_ID)
    void deleteFolder(long oid) throws FindException, DeleteException;

    /**
     * Find the folder with the specified OID.  May be null, indicating that no such folder exists.
     * @param oid the OID of the folder to search for
     * @return the folder with the specified OID, or null if no such folder exists.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.FOLDER, stereotype=MethodStereotype.FIND_ENTITY)
    @Administrative(licensed=false)            
    Folder findByPrimaryKey(long oid) throws FindException;
}
