package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;

/**
 * Extension of HibernateEntityManager that supports use of Folders.
 */
public abstract class FolderSupportHibernateEntityManager<ET extends PersistentEntity, HT extends EntityHeader> extends HibernateEntityManager<ET,HT> {

    //- PUBLIC

    /**
     * Update method that does not permit update of Folder.
     *
     * @param entity The entity to update.
     * @throws com.l7tech.objectmodel.UpdateException If the user attempts to modify the entities folder.
     */
    @Override
    public void update( final ET entity ) throws UpdateException {
        try {
            HasFolder entityHasFolder = (HasFolder) entity;
            HasFolder currentEntityHasFolder = (HasFolder) this.findByPrimaryKey( entity.getGoid() );

            if (changesExistingFolder(currentEntityHasFolder, entityHasFolder)) {
                throw new UpdateException( "Folder update not permitted." );
            }
        } catch ( FindException fe ) {
            throw new UpdateException( "Error when checking folder.", fe );
        }

        super.update(entity);
    }

    /**
     * Bypass to super.update(), not performing folder permission checks.
     * Callers must check that they have permission to update folders before calling this.
     *
     * @param entity The entity to be updated.
     */
    public void updateWithFolder( final ET entity ) throws UpdateException {
        super.update(entity);
    }

    //- PROTECTED

    /**
     * Update the parent folder for the given entity.
     *
     * @param entityGoid The ID for the entity to update
     * @param folder The parent folder must not be null.
     */
    protected void setParentFolderForEntity( final Goid entityGoid, final Folder folder ) throws UpdateException {
        if ( folder == null ) throw new UpdateException("Folder is required but missing.");

        try {
            ET entity = this.findByPrimaryKey( entityGoid );
            if ( entity != null ) {
                HasFolder entityHasFolder = (HasFolder) entity;
                entityHasFolder.setFolder( folder );
                super.update( entity );
            } else {
                throw new UpdateException( "Entity not found." );
            }
        } catch ( FindException fe ) {
            throw new UpdateException( "Error when updating folder.", fe );
        }
    }

    protected static boolean changesExistingFolder(HasFolder existingEntity, HasFolder updatedEntity) {
        return existingEntity != null && ( updatedEntity.getFolder() != null || existingEntity.getFolder() != null) &&
                ( (updatedEntity.getFolder() == null && existingEntity.getFolder() != null ) ||
                        (updatedEntity.getFolder() != null && existingEntity.getFolder() == null ) ||
                        (!updatedEntity.getFolder().getGoid().equals(existingEntity.getFolder().getGoid())) );
    }
}