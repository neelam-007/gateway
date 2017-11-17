package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InsufficientPermissionsException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.AuditConfigurationTransformer;
import com.l7tech.gateway.api.AuditConfigurationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.audit.AuditConfigurationManager;
import com.l7tech.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class AuditConfigurationAPIResourceFactory implements APIResourceFactory<AuditConfigurationMO> {

    @Inject
    AuditConfigurationTransformer transformer;

    @Inject
    private AuditConfigurationManager auditConfigurationManager;

    @Inject
    protected RbacAccessService rbacAccessService;


    @Override
    public String createResource(@NotNull AuditConfigurationMO resource) throws ResourceFactory.InvalidResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createResource(@NotNull String id, @NotNull AuditConfigurationMO resource) throws ResourceFactory.ResourceFactoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateResource(@NotNull String id, @NotNull AuditConfigurationMO resource) throws ResourceFactory.ResourceFactoryException {
        try {
            rbacAccessService.validatePermitted(EntityType.CLUSTER_PROPERTY, OperationType.UPDATE);
            auditConfigurationManager.update(transformer.convertFromMO(resource, null).getEntity());
        } catch (UpdateException e) {
            throw new ResourceFactory.ResourceAccessException("Unable to update entity.", e);
        }
    }

 

    @Override
    public AuditConfigurationMO getResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException {
        try {
            rbacAccessService.validatePermitted(EntityType.CLUSTER_PROPERTY, OperationType.READ);
            AuditConfiguration entity = auditConfigurationManager.findByPrimaryKey(Goid.parseGoid(id));
            if (entity == null) {
                throw new ResourceFactory.ResourceNotFoundException("Resource not found " + id);
            }
            return transformer.convertToMO(entity);
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException("Unable to find entity.", e);
        }
    }

    @Override
    public boolean resourceExists(@NotNull String id) {
        return true;
    }

    @Override
    public List<AuditConfigurationMO> listResources(@Nullable String sortKey, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> filters) {
        try {
            return CollectionUtils.list(getResource(AuditConfiguration.ENTITY_ID.toString()));
        } catch (ResourceFactory.ResourceNotFoundException | InsufficientPermissionsException e) {
            return CollectionUtils.list();
        }
    }

    @Override
    public void deleteResource(@NotNull String id) throws ResourceFactory.ResourceNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mapping buildMapping(@NotNull AuditConfigurationMO resource, @Nullable Mapping.Action defaultAction, @Nullable String defaultMapBy) {
        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setType(getResourceType());
        mapping.setSrcId(AuditConfiguration.ENTITY_ID.toString());
        mapping.setAction(Mapping.Action.NewOrExisting);
        mapping.addProperty("FailOnNew", true);
        mapping.addProperty("MapBy", "id");
        return mapping;
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.AUDIT_CONFIG.toString();
    }
}
