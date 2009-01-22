package com.l7tech.server;

import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.Folder;

/**
 * Extension of HibernateEntityManager that supports use of Folders.
 */
public abstract class FolderSupportHibernateEntityManager<ET extends PersistentEntity, HT extends EntityHeader> extends HibernateEntityManager<ET,HT> {

    //- PUBLIC

    /**
     * Update method that does not permit update of Folder.
     *
     * @param entity The entity to update.
     * @throws UpdateException If the user attempts to modify the entities folder.
     */
    @Override
    public void update( final ET entity ) throws UpdateException {
        try {
            HasFolder entityHasFolder = (HasFolder) entity;
            HasFolder currentEntityHasFolder = (HasFolder) this.findByPrimaryKey( entity.getOid() );

            if ( currentEntityHasFolder != null && ( entityHasFolder.getFolder() != null || currentEntityHasFolder.getFolder() != null) ) {
                if ( (entityHasFolder.getFolder() == null && currentEntityHasFolder.getFolder() != null ) ||
                     (entityHasFolder.getFolder() != null && currentEntityHasFolder.getFolder() == null ) ||
                     (entityHasFolder.getFolder().getOid() != currentEntityHasFolder.getFolder().getOid()) ) {
                    throw new UpdateException( "Folder update not permitted." );
                }
            }
        } catch ( FindException fe ) {
            throw new UpdateException( "Error when checking folder.", fe );
        }

        super.update(entity);
    }

    //- PROTECTED

    /**
     * Update the parent folder for the given entity.
     *
     * @param entityId The ID for the entity to update
     * @param folder The parent folder must not be null.
     */
    protected void setParentFolderForEntity( final long entityId, final Folder folder ) throws UpdateException {
        if ( folder == null ) throw new UpdateException("Folder is required but missing.");

        try {
            ET entity = this.findByPrimaryKey( entityId );
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
}