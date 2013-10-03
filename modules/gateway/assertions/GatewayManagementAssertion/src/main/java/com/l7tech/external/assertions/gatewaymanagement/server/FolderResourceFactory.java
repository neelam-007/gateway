package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.InvalidParentFolderException;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.Either;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.Map;

import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.extract;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;

/**
 * 
 */
@ResourceFactory.ResourceType(type=FolderMO.class)
public class FolderResourceFactory extends SecurityZoneableEntityManagerResourceFactory<FolderMO, Folder, FolderHeader> {
    //The old root folder oid
    private static final String ROOT_FOLDER_OID = "-5002";

    //- PUBLIC

    public FolderResourceFactory( final RbacServices services,
                                  final SecurityFilter securityFilter,
                                  final PlatformTransactionManager transactionManager,
                                  final FolderManager folderManager,
                                  final SecurityZoneManager securityZoneManager ) {
        super( false, false, services, securityFilter, transactionManager, folderManager, securityZoneManager );
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

        // handle SecurityZone
        doSecurityZoneAsResource( folderRes, folder );

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
                    folderResource.getFolderId()==null ? null : selectEntity( Collections.singletonMap( IDENTITY_SELECTOR, handleRootFolderOid(folderResource.getFolderId()) ) ) );
        } catch ( ResourceNotFoundException e ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid parent folder");
        }

        // handle SecurityZone
        doSecurityZoneFromResource( folderResource, folderEntity );

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
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
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
        final String id = handleRootFolderOid(selectorMap.get(IDENTITY_SELECTOR));

        if ( id == null ) {
            throw new InvalidResourceSelectors();
        }

        if ( id != null ) {
            try {
                folder = folderManager.findByPrimaryKey( toInternalId(id) );
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }


        // Verify all selectors match (selectors must be AND'd)
        if ( folder != null ) {
            if ( id != null && !id.equalsIgnoreCase(folder.getId())) {
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

    /**
     * This will return the root folder goid if the root folder oid ig given. Otherwise it will return the id given.
     *
     * @param id The id to check to see if it is a root folder oid
     * @return the id, or the root folder goid if the id is -5002
     */
    private String handleRootFolderOid(String id) {
        return ROOT_FOLDER_OID.equals(id) ? Folder.ROOT_FOLDER_ID.toHexString() : id;
    }

    /**
     * This is overridden so that the old root folder oid can still be handled properly.
     */
    @Override
    public FolderMO putResource( final Map<String, String> selectorMap, final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        final String id = selectorMap.get( IDENTITY_SELECTOR );
        selectorMap.put(IDENTITY_SELECTOR, handleRootFolderOid(id));
        if ( resource instanceof ManagedObject) {
            final ManagedObject managedResource = (ManagedObject) resource;
            managedResource.setId(handleRootFolderOid(managedResource.getId()));
        }
        return super.putResource(selectorMap, resource);
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
        final Goid folderGoid = folderId.map( new Unary<Goid,String>() {
            @Override
            public Goid call( final String value ) {
                try {
                    return GoidUpgradeMapper.mapId(EntityType.FOLDER, handleRootFolderOid(value));
                } catch( IllegalArgumentException nfe ) {
                    return PersistentEntity.DEFAULT_GOID; // will not match any folder
                }
            }
        } ).orSome( Folder.ROOT_FOLDER_ID );

        try {
            return some( selectEntity( Collections.singletonMap( IDENTITY_SELECTOR, Goid.toString( folderGoid ) ) ) );
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

        if ( oldFolder != null && Goid.equals(oldFolder.getGoid(), newFolder.getGoid()) ) {
            result = oldFolder;
        }

        if ( result == null ) {
            // consistent with FolderAdmin permissions
            checkPermitted( OperationType.UPDATE, null, newFolder );
            if(oldFolder!=null) checkPermitted( OperationType.UPDATE, null, oldFolder );
            result = newFolder;
        }

        return result;
    }

    //- PRIVATE

    private FolderManager folderManager;

    private void checkRoot( final Folder folder ) throws ObjectModelException {
        if ( Folder.ROOT_FOLDER_ID.equals(folder.getGoid()) ) throw new ConstraintViolationException("Cannot update root folder");
    }
}
