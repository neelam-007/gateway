package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ResourceFactory.ResourceType(type=GenericEntityMO.class)
public class GenericEntityResourceFactory extends EntityManagerResourceFactory<GenericEntityMO, GenericEntity, GenericEntityHeader> {

    //- PUBLIC

    public GenericEntityResourceFactory(final RbacServices services,
                                        final SecurityFilter securityFilter,
                                        final PlatformTransactionManager transactionManager,
                                        final GenericEntityManager genericEntityManager) {
        // we do not allow look up of Generic Entities by name, there is no guid for a generic entity yet.
        super(false, false, services, securityFilter, transactionManager, genericEntityManager);
        this.genericEntityManager = genericEntityManager;
    }

    //- PROTECTED

    @Override
    public GenericEntityMO asResource(GenericEntity entity) {
        final GenericEntityMO genericEntityResource = ManagedObjectFactory.createGenericEntity();

        genericEntityResource.setName(entity.getName());
        genericEntityResource.setDescription(entity.getDescription());
        genericEntityResource.setEnabled(entity.isEnabled());
        genericEntityResource.setEntityClassName(entity.getEntityClassName());
        genericEntityResource.setValueXml(entity.getValueXml());

        return genericEntityResource;
    }

    @Override
    public GenericEntity fromResource(Object resource, boolean strict) throws InvalidResourceException {
        if (!(resource instanceof GenericEntityMO)) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected generic entity");
        }

        final GenericEntityMO resourceEntity = (GenericEntityMO) resource;

        final GenericEntity genericEntity = new GenericEntity();
        genericEntity.setName(resourceEntity.getName());
        genericEntity.setDescription(resourceEntity.getDescription());
        genericEntity.setEnabled(resourceEntity.getEnabled());
        genericEntity.setEntityClassName(resourceEntity.getEntityClassName());
        genericEntity.setValueXml(resourceEntity.getValueXml());

        return genericEntity;
    }

    @Override
    protected void updateEntity(GenericEntity oldEntity, GenericEntity newEntity) throws InvalidResourceException {
        // id and entity class name are read only, do not update them
        oldEntity.setName(newEntity.getName());
        oldEntity.setDescription(newEntity.getDescription());
        oldEntity.setEnabled(newEntity.isEnabled());
        oldEntity.setValueXml(newEntity.getValueXml());
    }

    @Override
    protected Set<String> getCustomSelectors() {
        return new HashSet<String>(Arrays.asList(NAME_SELECTOR, ENTITY_TYPE_SELECTOR));
    }

    @Override
    protected GenericEntity selectEntityCustom(final Map<String, String> selectorMap) throws ResourceAccessException, InvalidResourceSelectors {
        final String name = selectorMap.get(NAME_SELECTOR);
        final String entityClass = selectorMap.get(ENTITY_TYPE_SELECTOR);

        if (name != null && entityClass == null) {
            // if name is provided and no entity class name, then this is an invalid selector combination, we will
            // never find a unique generic entity based on name alone. See constructor configuration.
            throw new InvalidResourceSelectors();
        }

        final boolean selectorsFound = name != null || entityClass != null;

        GenericEntity entity = null;
        if (selectorsFound && genericEntityManager.isRegistered(entityClass)) {
            try {
                entity = genericEntityManager.findByUniqueName(entityClass, name);
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        return entity;
    }

    @Override
    protected Goid doSaveEntity( GenericEntity entity) throws SaveException {
        if(!genericEntityManager.isRegistered(entity.getEntityClassName())){
            throw new SaveException("No generic entity class named " + entity.getEntityClassName() + " is registered");
        }
        return genericEntityManager.save(entity);
    }

    //- PRIVATE
    final GenericEntityManager genericEntityManager;
    private static final String ENTITY_TYPE_SELECTOR = "genericEntityType";
}
