package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.InvalidParentFolderException;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Option;

import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.extract;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

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
        super( false, true, services, securityFilter, transactionManager, folderManager );
        this.folderManager = folderManager;
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
        if ( newEntity.getFolder() != null ) {
            try {
                oldEntity.reParent( checkMovePermitted( oldEntity.getFolder(), newEntity.getFolder() ) );
            } catch ( InvalidParentFolderException e ) {
                throw new InvalidResourceException( ExceptionType.INVALID_VALUES, "invalid parent folder");
            }
        }
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

    @Override
    public FolderMO getResource(final Map<String, String> selectorMap) throws ResourceNotFoundException {
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,FolderMO>>(){
            @Override
            public Either<ResourceNotFoundException,FolderMO> execute() throws ObjectModelException {
                try {
                    Folder folder = selectFolder(selectorMap);
                    EntityBag<Folder> entityBag = new EntityBag<Folder>(folder);
                    checkPermitted( OperationType.READ, null, entityBag.getEntity() );
                    return right( identify( asResource( entityBag ), entityBag.getEntity() ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    private Folder selectFolder(Map<String, String> selectorMap) throws ResourceAccessException, ResourceNotFoundException {

        Folder folder = null;
        final String id = selectorMap.get( IDENTITY_SELECTOR );
        final String name = selectorMap.get( NAME_SELECTOR );

        if ( id == null && name == null ) {
            throw new InvalidResourceSelectors();
        }

        if ( id != null ) {
            try {
                folder = folderManager.findByPrimaryKey( toInternalId(id) );
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        if ( folder == null &&  name != null ) {
            EntityHeader header = new EntityHeader();
            header.setDescription(name);
            try {
                folder = folderManager.findByHeader(header);
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        // Verify all selectors match (selectors must be AND'd)
        if ( folder != null ) {
            if ( id != null && !id.equalsIgnoreCase(folder.getId())) {
                folder = null;
            } else if ( name != null && !(name.equalsIgnoreCase(folder.getName()) || name.equalsIgnoreCase(folder.getPath().substring(1)))) {
                folder = null;
            }
        }

        if ( folder != null ) {
            folder = filterEntity( folder );
        }

        if ( folder == null ) {
            throw new ResourceNotFoundException("Resource not found " + selectorMap);
        } else {
            EntityContext.setEntityInfo( getType(), folder.getId() );
        }

        return folder;
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
    private FolderManager folderManager;

    private void checkRoot( final Folder folder ) throws ObjectModelException {
        if ( folder.getOid() == ROOT_FOLDER_OID ) throw new ConstraintViolationException("Cannot update root folder");
    }
}
