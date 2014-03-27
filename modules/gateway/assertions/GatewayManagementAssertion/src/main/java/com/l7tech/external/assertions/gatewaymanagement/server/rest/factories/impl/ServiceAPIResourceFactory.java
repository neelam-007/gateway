package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyHelper;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PublishedServiceTransformer;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.server.bundling.PublishedServiceContainer;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.util.Collection;
import java.util.UUID;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class ServiceAPIResourceFactory extends WsmanBaseResourceFactory<ServiceMO, ServiceResourceFactory> {

    public ServiceAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SERVICE.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ServiceMO getResourceTemplate() {
        ServiceMO serviceMO = ManagedObjectFactory.createService();

        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setEnabled(true);
        serviceDetail.setFolderId("Folder ID");
        serviceDetail.setName("Service Name");

        serviceMO.setServiceDetail(serviceDetail);
        return serviceMO;
    }

    @Inject
    private PolicyVersionManager policyVersionManager;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private ServiceManager serviceManager;

    @Inject
    private PublishedServiceTransformer serviceTransformer;

    @Inject
    private PolicyHelper policyHelper;

    @Inject
    private ServiceDocumentManager serviceDocumentManager;

    @Inject
    private RbacServices rbacServices;

    @Inject
    private RbacAccessService rbacAccessService;

    @Override
    public String createResource(@NotNull ServiceMO resource) throws ResourceFactory.InvalidResourceException {
        return createResource(resource, null);
    }

    @Override
    public void createResource(@NotNull String id, @NotNull ServiceMO resource) throws ResourceFactory.InvalidResourceException {
        createResource(id, resource, null);
    }

    @Override
    public void updateResource(@NotNull String id, @NotNull ServiceMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        updateResource(id, resource, null, true);
    }

    public String createResource(@NotNull final ServiceMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        validateCreateResource(null, resource);
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        return tt.execute(new TransactionCallback<String>() {
            @Override
            public String doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    PublishedServiceContainer newServiceEntity = (PublishedServiceContainer)serviceTransformer.convertFromMO(resource,true);
                    PublishedService newService = newServiceEntity.getEntity();
                    newService.setVersion(0);
                    rbacAccessService.validatePermitted(newService, OperationType.CREATE);

                    beforeCreate(newService);

                    Goid id = serviceManager.save(newService);
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), true, comment, true);
                    saveServiceDocuments(id, newServiceEntity.getServiceDocuments());

                    resource.setId(id.toString());
                    return id.toString();

                } catch (ResourceFactory.InvalidResourceException e) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException(e);
                } catch (ObjectModelException ome) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException("Unable to create service.", ome);
                }
            }
        });
    }

    private void saveServiceDocuments(Goid serviceGoid, Collection<ServiceDocument> serviceDocuments) throws DeleteException, SaveException, FindException {
        if (serviceDocuments == null) return;

        final Collection<ServiceDocument> existingServiceDocuments = serviceDocumentManager.findByServiceId(serviceGoid);
        for (final ServiceDocument serviceDocument : existingServiceDocuments) {
            rbacAccessService.validatePermitted(serviceDocument, OperationType.DELETE);
            serviceDocumentManager.delete(serviceDocument);
        }

        for (final ServiceDocument serviceDocument : serviceDocuments) {
            serviceDocument.setGoid(ServiceDocument.DEFAULT_GOID);
            serviceDocument.setServiceId(serviceGoid);
            rbacAccessService.validatePermitted(serviceDocument, OperationType.CREATE);
            serviceDocumentManager.save(serviceDocument);
        }
    }

    public void createResource(@NotNull final String id, @NotNull final ServiceMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        validateCreateResource(id, resource);
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    PublishedServiceContainer newServiceEntity = (PublishedServiceContainer)serviceTransformer.convertFromMO(resource);
                    PublishedService newService = newServiceEntity.getEntity();

                    newService.setVersion(0);
                    rbacAccessService.validatePermitted(newService, OperationType.CREATE);

                    beforeCreate(newService);

                    serviceManager.save(Goid.parseGoid(id), newService);
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), true, comment, true);
                    saveServiceDocuments(Goid.parseGoid(id), newServiceEntity.getServiceDocuments());

                    return null;

                } catch (ResourceFactory.InvalidResourceException e) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException(e);
                } catch (ObjectModelException ome) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException("Unable to create service.", ome);
                }
            }
        });
        resource.setId(id);
    }

    private void beforeCreate(PublishedService service) throws SaveException {
        final Policy policy = service.getPolicy();

        try {
            policyHelper.checkPolicyAssertionAccess(policy);
        } catch (ResourceFactory.InvalidResourceException e) {
            throw new SaveException(e);
        }

        //need to set the policy GUID if this is not set creating a new service will fail
        if ( policy.getGuid() == null ) {
            UUID guid = UUID.randomUUID();
            policy.setGuid(guid.toString());
        }
    }

    private void validateCreateResource(@Nullable String id, ServiceMO resource) {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when creating a new entity, or id must equal new entity id");
        }
    }

    public void updateResource(final @NotNull String id, final @NotNull ServiceMO resource, final String comment, final boolean active) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when updating a new entity, or id must equal entity id");
        }

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    PublishedServiceContainer newServiceEntity = (PublishedServiceContainer)serviceTransformer.convertFromMO(resource);
                    PublishedService newService = newServiceEntity.getEntity();
                    PublishedService oldService = serviceManager.findByPrimaryKey(Goid.parseGoid(id));
                    if(oldService != null)
                        rbacAccessService.validatePermitted(oldService, OperationType.UPDATE);

                    newService.setGoid(Goid.parseGoid(id));
                    newService.setFolder(checkMovePermitted(oldService.getFolder(), newService.getFolder()));
                    oldService.getPolicy().setXml(newService.getPolicy().getXml());
                    newService.setPolicy(oldService.getPolicy());
                    newService.setVersion(resource.getVersion());

                    serviceManager.update(newService);
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), active, comment, false);
                    saveServiceDocuments(Goid.parseGoid(id), newServiceEntity.getServiceDocuments());

                    return serviceTransformer.convertToMO(newServiceEntity.getEntity());

                } catch (ResourceFactory.InvalidResourceException e) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException(e);
                } catch (ObjectModelException ome) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException("Unable to update service.", ome);
                }
            }
        });
    }

    Folder checkMovePermitted(@Nullable final Folder oldFolder,
                              @NotNull final Folder newFolder) {
        Folder result = null;

        if (oldFolder != null && Goid.equals(oldFolder.getGoid(), newFolder.getGoid())) {
            result = oldFolder;
        }

        if (result == null) {
            // consistent with FolderAdmin permissions


            rbacAccessService.validatePermitted(newFolder, OperationType.UPDATE);
            if (oldFolder != null)
                rbacAccessService.validatePermitted(oldFolder, OperationType.UPDATE);
            result = newFolder;
        }

        return result;
    }

    public String getPolicyIdForService(String id) throws ResourceFactory.ResourceNotFoundException {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(Goid.parseGoid(id));
            if(service !=null){
                rbacAccessService.validatePermitted(service, OperationType.READ);
                return service.getPolicy().getId();
            }
            return null;
        } catch (FindException e) {
            throw new ResourceFactory.ResourceNotFoundException(ExceptionUtils.getMessage(e),e);
        }
    }

    /**
     * This will find if a service exists and if the currently authenticated user has the operationType access to this
     * service. This will throw an exception if the service doesn't exist or the user does not have access to the service.
     *
     * @param id            The id of the service.
     * @param operationType The operation type that the current user needs to have permissions for on this service
     * @throws ResourceFactory.ResourceNotFoundException
     *                      This is thrown if the service cannot be found
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InsufficientPermissionsException
     *                      This is thrown if the user does not have sufficient permissions to access the service
     */
    public void validateExistsAndHasAccess(@NotNull final String id, @NotNull final OperationType operationType) throws ResourceFactory.ResourceNotFoundException {
        RestResourceFactoryUtils.transactional(transactionManager, true, new Functions.NullaryVoidThrows<ResourceFactory.ResourceNotFoundException>() {
            @Override
            public void call() throws ResourceFactory.ResourceNotFoundException {
                try {
                    PublishedService service = serviceManager.findByPrimaryKey(Goid.parseGoid(id));
                    if (service == null) {
                        throw new ResourceFactory.ResourceNotFoundException("Could not find service with id: " + id);
                    }
                    rbacAccessService.validatePermitted(service, operationType);
                } catch (FindException e) {
                    throw new ResourceFactory.ResourceNotFoundException("Could not find service with id: " + id, e);
                }
            }
        });
    }
}