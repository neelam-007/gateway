package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

/**
 * 
 */
@ResourceFactory.ResourceType(type=FolderMO.class)
public class FolderResourceFactory extends EntityManagerResourceFactory<FolderMO, Folder, FolderHeader> {

    //- PUBLIC

    public FolderResourceFactory( final RbacServices services,
                                  final SecurityFilter securityFilter,
                                  final PlatformTransactionManager transactionManager,
                                  final FolderManager folderManager ) {
        super( false, false, services, securityFilter, transactionManager, folderManager );
    }

    //- PROTECTED

    @Override
    protected FolderMO asResource( final Folder folder ) {
        FolderMO folderRes = ManagedObjectFactory.createFolder();

        folderRes.setId( folder.getId() );
        folderRes.setVersion( folder.getVersion() );
        folderRes.setFolderId( getFolderId( folder ) );
        folderRes.setName( folder.getName() );

        return folderRes;
    }

    @Override
    protected Folder fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof FolderMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected folder");

        final FolderMO folderResource = (FolderMO) resource;

        final Folder folderEntity;
        try {
            folderEntity = new Folder(
                    asName(folderResource.getName()),
                    folderResource.getFolderId()==null ? null : selectEntity( Collections.singletonMap( IDENTITY_SELECTOR, folderResource.getFolderId() ) ) );
        } catch ( ResourceNotFoundException e ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid parent folder");
        }

        return folderEntity;
    }

    @Override
    protected void updateEntity( final Folder oldEntity, final Folder newEntity ) throws InvalidResourceException {
        oldEntity.setName( newEntity.getName() );
    }

    @Override
    protected void beforeUpdateEntity( final EntityBag<Folder> folderEntityBag ) throws ObjectModelException {
        checkRoot( folderEntityBag.getEntity() );
    }

    @Override
    protected void beforeDeleteEntity( final EntityBag<Folder> folderEntityBag ) throws ObjectModelException {
        checkRoot( folderEntityBag.getEntity() );
    }

    //- PACKAGE

    /**
     * Get the folder with the given identifier.
     *
     * <p>The caller is expected to provide transactional context.</p>
     *
     * @param folderId The folder identifier.
     * @return The optional folder
     */
    @NotNull
    Option<Folder> getFolder( @NotNull final Option<String> folderId ) {
        final long folderOid = folderId.map( new Unary<Long,String>() {
            @Override
            public Long call( final String value ) {
                try {
                    return Long.parseLong( value );
                } catch( NumberFormatException nfe ) {
                    return PersistentEntity.DEFAULT_OID; // will not match any folder
                }
            }
        } ).orSome( ROOT_FOLDER_OID );

        try {
            return some( selectEntity( Collections.singletonMap( IDENTITY_SELECTOR, Long.toString( folderOid ) ) ) );
        } catch ( ResourceNotFoundException e ) {
            return none();
        }
    }

    /**
     * Verify that moving an entity between the given folders is permitted.
     *
     * <p>This will fail with a security exception if not permitted.</p>
     *
     * @param oldFolder The old folder
     * @param newFolder The new folder
     * @return The new folder if different from the old folder
     */
    @NotNull
    Folder checkMovePermitted( @Nullable final Folder oldFolder,
                               @NotNull  final Folder newFolder ) {
        Folder result = null;

        if ( oldFolder != null && oldFolder.getOid() == newFolder.getOid() ) {
            result = oldFolder;
        }

        if ( result == null ) {
            checkPermittedForAnyEntity( OperationType.DELETE, EntityType.FOLDER ); // consistent with FolderAdmin permissions
            result = newFolder;
        }

        return result;
    }

    //- PRIVATE

    private static final long ROOT_FOLDER_OID = -5002L;

    private void checkRoot( final Folder folder ) throws ObjectModelException {
        if ( folder.getOid() == ROOT_FOLDER_OID ) throw new ConstraintViolationException("Cannot update root folder");
    }
}
