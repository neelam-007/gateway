package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

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
        super( true, false, services, securityFilter, transactionManager, folderManager );
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
}
