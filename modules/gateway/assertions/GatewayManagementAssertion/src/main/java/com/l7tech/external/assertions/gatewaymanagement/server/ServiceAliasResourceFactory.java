package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.AliasManager;
import com.l7tech.util.Option;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.Collections;

import static com.l7tech.util.Option.optional;

/**
 *
 */
@ResourceFactory.ResourceType(type = ServiceAliasMO.class)
public class ServiceAliasResourceFactory extends SecurityZoneableEntityManagerResourceFactory<ServiceAliasMO, PublishedServiceAlias, AliasHeader<PublishedService>> {

    //- PUBLIC

    public ServiceAliasResourceFactory(final RbacServices services,
                                       final SecurityFilter securityFilter,
                                       final PlatformTransactionManager transactionManager,
                                       final AliasManager<PublishedServiceAlias, PublishedService, ServiceHeader> serviceAliasManager,
                                       final ServiceResourceFactory serviceResourceFactory,
                                       final FolderResourceFactory folderResourceFactory,
                                       final SecurityZoneManager securityZoneManager) {
        super(false, false, services, securityFilter, transactionManager, serviceAliasManager, securityZoneManager);
        this.serviceAliasManager = serviceAliasManager;
        this.serviceResourceFactory = serviceResourceFactory;
        this.folderResourceFactory = folderResourceFactory;
    }

    //- PROTECTED

    @Override
    protected ServiceAliasMO asResource(final PublishedServiceAlias serviceAlias) {
        ServiceAliasMO serviceAliasRes = ManagedObjectFactory.createServiceAlias();

        serviceAliasRes.setId(serviceAlias.getId());
        serviceAliasRes.setVersion(serviceAlias.getVersion());
        serviceAliasRes.setFolderId(getFolderId(serviceAlias));
        serviceAliasRes.setServiceReference(new ManagedObjectReference(ServiceMO.class, serviceAlias.getEntityGoid().toString()));

        // handle SecurityZone
        doSecurityZoneAsResource(serviceAliasRes, serviceAlias);

        return serviceAliasRes;
    }

    @Override
    protected PublishedServiceAlias fromResource(final Object resource) throws InvalidResourceException {
        if (!(resource instanceof ServiceAliasMO))
            throw new InvalidResourceException(ExceptionType.UNEXPECTED_TYPE, "expected policy alias");

        final ServiceAliasMO PublishedServiceAliasResource = (ServiceAliasMO) resource;

        final Option<Folder> parentFolder = folderResourceFactory.getFolder(optional(PublishedServiceAliasResource.getFolderId()));
        if (!parentFolder.isSome())
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Parent folder not found");
        folderResourceFactory.checkMovePermitted(null,parentFolder.some());

        // policy alias cannot be in same folder as the original
        final PublishedService service;
        final String serviceID = PublishedServiceAliasResource.getServiceReference().getId();
        try {
            service = serviceResourceFactory.selectEntity(Collections.singletonMap(IDENTITY_SELECTOR,serviceID) );
            if (isRootFolder(service.getFolder()) ? isRootFolder(parentFolder.some()) : service.getFolder().equals(parentFolder.some()))
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Cannot create alias in the same folder as original");
        } catch (NullPointerException | ResourceNotFoundException e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "invalid policy reference");
        }

        // policy alias referencing same policy cannot be in same folder
        try{
            PublishedServiceAlias checkAlias = serviceAliasManager.findAliasByEntityAndFolder(toInternalId(serviceID), parentFolder.some().getGoid());
            if( checkAlias != null )
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES,"Alias of service " + serviceID + " already exists in folder " + parentFolder.some().getGoid());
        } catch (FindException | InvalidResourceSelectors e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Unable to check for existing alias");
        }

        final PublishedServiceAlias PublishedServiceAliasEntity = new PublishedServiceAlias(service, parentFolder.some());
        // handle SecurityZone
        doSecurityZoneFromResource(PublishedServiceAliasResource, PublishedServiceAliasEntity);

        return PublishedServiceAliasEntity;
    }

    @Override
    protected void updateEntity(final PublishedServiceAlias oldEntity, final PublishedServiceAlias newEntity) throws InvalidResourceException {

        // Disallow changing the referenced policy
        if (!oldEntity.getEntityGoid().equals(newEntity.getEntityGoid())) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "unable to change policy reference of an existing policy alias");
        }

        // policy alias cannot be in same folder as the original
        final Goid serviceID = newEntity.getEntityGoid();
        final PublishedService service;
        try {
            service = serviceResourceFactory.selectEntity(Collections.singletonMap(IDENTITY_SELECTOR, newEntity.getEntityGoid().toString()));
            if (isRootFolder(service.getFolder()) ? isRootFolder(newEntity.getFolder()) : service.getFolder().equals(newEntity.getFolder()))
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Cannot have alias in the same folder as original");
        } catch (ResourceNotFoundException e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "invalid policy reference");
        }

        // policy alias referencing same policy cannot be in same folder
        try{
            PublishedServiceAlias checkAlias = serviceAliasManager.findAliasByEntityAndFolder(serviceID, newEntity.getFolder().getGoid());
            if( checkAlias != null )
                throw new InvalidResourceException(ExceptionType.INVALID_VALUES,"Alias of service " + serviceID + " already exists in folder " + newEntity.getFolder().getGoid());
        } catch (FindException e) {
            throw new InvalidResourceException(ExceptionType.INVALID_VALUES, "Unable to check for existing alias");
        }

        // update the entity
        if (newEntity.getFolder() != null) {
            oldEntity.setFolder(folderResourceFactory.checkMovePermitted(oldEntity.getFolder(), newEntity.getFolder()));
        }
        oldEntity.setSecurityZone(newEntity.getSecurityZone());
    }



    //- PACKAGE


    //- PRIVATE
    private boolean isRootFolder(Folder folder) {
        return folder == null || folder.getId().equals(ROOT_FOLDER_OID) || folder.getId().equals(Folder.ROOT_FOLDER_ID.toString());
    }

    private static final String ROOT_FOLDER_OID = "-5002";
    private FolderResourceFactory folderResourceFactory;
    private AliasManager<PublishedServiceAlias, PublishedService, ServiceHeader> serviceAliasManager;
    private ServiceResourceFactory serviceResourceFactory;

}
