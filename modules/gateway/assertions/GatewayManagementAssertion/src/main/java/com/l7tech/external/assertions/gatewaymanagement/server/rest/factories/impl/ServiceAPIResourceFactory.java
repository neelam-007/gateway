package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyHelper;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PublishedServiceTransformer;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
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
                    Pair<PublishedService, Collection<ServiceDocument>> newServiceEntity = factory.fromResource(resource);
                    PublishedService newService = newServiceEntity.left;
                    newService.setVersion(0);
                    beforeCreate(newService);

                    Goid id = serviceManager.save(newService);
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), true, comment, true);
                    saveServiceDocuments(id, newServiceEntity.right);

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
            serviceDocumentManager.delete(serviceDocument);
        }

        for (final ServiceDocument serviceDocument : serviceDocuments) {
            serviceDocument.setGoid(ServiceDocument.DEFAULT_GOID);
            serviceDocument.setServiceId(serviceGoid);
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
                    Pair<PublishedService, Collection<ServiceDocument>> newServiceEntity = serviceTransformer.convertToEntity(resource);
                    PublishedService newService = newServiceEntity.left;

                    newService.setVersion(0);
                    beforeCreate(newService);

                    serviceManager.save(Goid.parseGoid(id), newService);
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), true, comment, true);
                    saveServiceDocuments(Goid.parseGoid(id), newServiceEntity.right);

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

        service.setInternal(false);

        if (policy != null) {
            if (policy.getGuid() == null) {
                UUID guid = UUID.randomUUID();
                policy.setGuid(guid.toString());
            }

            if (policy.getName() == null) {
                policy.setName(service.generatePolicyName());
            }
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
                    Pair<PublishedService, Collection<ServiceDocument>> newServiceEntity = serviceTransformer.convertToEntity(resource);
                    PublishedService newService = newServiceEntity.left;
                    PublishedService oldService = serviceManager.findByPrimaryKey(Goid.parseGoid(id));

                    newService.setGoid(Goid.parseGoid(id));
                    newService.setFolder(checkMovePermitted(oldService.getFolder(), newService.getFolder()));
                    oldService.getPolicy().setXml(newService.getPolicy().getXml());
                    newService.setPolicy(oldService.getPolicy());
                    newService.setVersion(resource.getVersion());

                    serviceManager.update(newService);
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), active, comment, false);
                    saveServiceDocuments(Goid.parseGoid(id), newServiceEntity.right);

                    return factory.asResource(newServiceEntity);

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


            checkPermitted(OperationType.UPDATE, newFolder);
            if (oldFolder != null)
                checkPermitted(OperationType.UPDATE, oldFolder);
            result = newFolder;
        }

        return result;
    }

    private void checkPermitted(OperationType operation, Entity entity) {
        EntityType entityType = EntityType.findTypeByEntity(entity.getClass());
        try {
            if (!rbacServices.isPermittedForEntity(JaasUtils.getCurrentUser(), entity, operation, null)) {
                throw new PermissionDeniedException(operation, entityType);
            }
        } catch (FindException e) {
            throw (PermissionDeniedException) new PermissionDeniedException(operation, entityType, "Error in permission check.").initCause(e);

        }
    }

    public String getPolicyIdForService(String id) throws ResourceFactory.ResourceNotFoundException {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(Goid.parseGoid(id));
            if(service !=null){
                checkPermitted(OperationType.READ, service);
                return service.getPolicy().getId();
            }
            return null;
        } catch (FindException e) {
            throw new ResourceFactory.ResourceNotFoundException(ExceptionUtils.getMessage(e),e);
        }
    }
}