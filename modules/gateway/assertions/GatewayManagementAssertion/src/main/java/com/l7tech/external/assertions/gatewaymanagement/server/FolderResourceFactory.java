package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
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
    protected void beforeCreateEntity( final EntityBag<Folder> folderEntityBag ) throws ObjectModelException {
        checkRoot( folderEntityBag.getEntity() );
    }

    //- PRIVATE

    private static final int ROOT_FOLDER_OID = -5002;

    private void checkRoot( final Folder folder ) throws ObjectModelException {
        if ( folder.getOid() == (long) ROOT_FOLDER_OID ) throw new ConstraintViolationException("Cannot update root folder");
    }
}
