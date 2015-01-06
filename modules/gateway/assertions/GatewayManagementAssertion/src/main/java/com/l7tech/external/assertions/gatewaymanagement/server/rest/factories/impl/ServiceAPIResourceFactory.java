package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyHelper;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PublishedServiceTransformer;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.server.bundling.PublishedServiceContainer;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class ServiceAPIResourceFactory extends WsmanBaseResourceFactory<ServiceMO, ServiceResourceFactory> {

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
    private RbacAccessService rbacAccessService;

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
    public String createResource(@NotNull final ServiceMO resource) throws ResourceFactory.InvalidResourceException {
        return createResource(null, resource, null);
    }

    public String createResource(@NotNull final ServiceMO resource, @Nullable final String comment) throws ResourceFactory.InvalidResourceException {
        return createResource(null, resource, comment);
    }

    @Override
    public void createResource(@NotNull final String id, @NotNull final ServiceMO resource) throws ResourceFactory.InvalidResourceException {
        createResource(id, resource, null);
    }

    public void updateResource(@NotNull final String id, @NotNull final ServiceMO resource) throws ResourceFactory.ResourceFactoryException {
        updateResource(id, resource, null, true);
    }

    @NotNull
    public String createResource(@Nullable final String id, @NotNull final ServiceMO resource, @Nullable final String comment) throws ResourceFactory.InvalidResourceException {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new InvalidArgumentException("id", "Must not specify an ID when creating a new entity, or id must equal new entity id");
        }
        final String savedId = RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryThrows<String, ResourceFactory.InvalidResourceException>() {
            @Override
            public String call() throws ResourceFactory.InvalidResourceException {
                try {
                    final PublishedServiceContainer newServiceEntity = (PublishedServiceContainer)serviceTransformer.convertFromMO(resource, null);
                    final PublishedService newService = newServiceEntity.getEntity();
                    newService.setVersion(0);

                    //check for permissions to create service.
                    rbacAccessService.validatePermitted(newService, OperationType.CREATE);

                    //check assertion access
                    final Policy policy = newService.getPolicy();
                    policyHelper.checkPolicyAssertionAccess(policy);

                    //need to set the policy GUID if this is not set creating a new service will fail
                    if ( policy.getGuid() == null ) {
                        policy.setGuid(UUID.randomUUID().toString());
                    }

                    RestResourceFactoryUtils.validate(newService, Collections.<String, String>emptyMap());
                    final Goid savedId;
                    if (id == null) {
                        savedId = serviceManager.save(newService);
                    } else {
                        savedId = Goid.parseGoid(id);
                        serviceManager.save(savedId, newService);
                    }
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), true, comment, true);
                    saveServiceDocuments(savedId, newServiceEntity.getServiceDocuments());
                    serviceManager.createRoles(newService);

                    return savedId.toString();
                } catch (ObjectModelException ome) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Unable to create service: " + ome.getMessage());
                }
            }
        });
        resource.setId(savedId);
        return savedId;
    }

    private void saveServiceDocuments(@NotNull final Goid serviceGoid, @Nullable final Collection<ServiceDocument> serviceDocuments) throws DeleteException, SaveException, FindException {
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

    public void updateResource(@NotNull final String id, @NotNull final ServiceMO resource, @Nullable final String comment, final boolean active) throws ResourceFactory.ResourceFactoryException {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new InvalidArgumentException("id", "Must not specify an ID when updating a new entity, or id must equal entity id");
        }

        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceFactoryException>() {
            @Override
            public void call() throws ResourceFactory.ResourceFactoryException {
                try {
                    final PublishedServiceContainer newServiceEntity = (PublishedServiceContainer) serviceTransformer.convertFromMO(resource, null);
                    final PublishedService newService = newServiceEntity.getEntity();
                    final PublishedService oldService = serviceManager.findByPrimaryKey(Goid.parseGoid(id));

                    if (oldService == null) {
                        throw new ResourceFactory.ResourceNotFoundException("Existing service not found. ID: " + id);
                    }

                    rbacAccessService.validatePermitted(oldService, OperationType.UPDATE);
                    RestResourceFactoryUtils.checkMovePermitted(rbacAccessService, oldService.getFolder(), newService.getFolder());
                    policyHelper.checkPolicyAssertionAccess(newService.getPolicy());

                    newService.setGoid(Goid.parseGoid(id));
                    //needs to be done this way to properly copy over the policy id.
                    oldService.getPolicy().setXml(newService.getPolicy().getXml());
                    newService.setPolicy(oldService.getPolicy());
                    //need to update the service version as wsman does not set it. SSG-8476
                    if(resource.getVersion() != null) {
                        newService.setVersion(resource.getVersion());
                    }

                    RestResourceFactoryUtils.validate(newService, Collections.<String, String>emptyMap());
                    serviceManager.update(newService);
                    policyVersionManager.checkpointPolicy(newService.getPolicy(), active, comment, false);
                    saveServiceDocuments(Goid.parseGoid(id), newServiceEntity.getServiceDocuments());
                } catch (ObjectModelException ome) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Unable to update service: " + ome.getMessage());
                }
            }
        });
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