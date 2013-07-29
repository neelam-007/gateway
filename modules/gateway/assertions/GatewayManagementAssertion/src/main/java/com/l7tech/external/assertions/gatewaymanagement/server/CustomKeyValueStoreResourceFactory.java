package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

@ResourceFactory.ResourceType(type=CustomKeyValueStoreMO.class)
public class CustomKeyValueStoreResourceFactory extends GoidEntityManagerResourceFactory<CustomKeyValueStoreMO, CustomKeyValueStore,EntityHeader>{

    //- PUBLIC

    public CustomKeyValueStoreResourceFactory(final RbacServices services,
                                              final SecurityFilter securityFilter,
                                              final PlatformTransactionManager transactionManager,
                                              final CustomKeyValueStoreManager customKeyValueStoreManager) {
        super(false, true, false, services, securityFilter, transactionManager, customKeyValueStoreManager);
        this.customKeyValueStoreManager = customKeyValueStoreManager;
    }

    //- PROTECTED

    @Override
    protected CustomKeyValueStore fromResource(Object resource) throws InvalidResourceException {
        if (!(resource instanceof CustomKeyValueStoreMO )) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected custom key value store");
        }

        final CustomKeyValueStoreMO customKeyValueStoreResource = (CustomKeyValueStoreMO) resource;

        final CustomKeyValueStore customKeyValueStoreEntity = new CustomKeyValueStore();
        customKeyValueStoreEntity.setName(customKeyValueStoreResource.getKey());
        customKeyValueStoreEntity.setValue(customKeyValueStoreResource.getValue());

        return customKeyValueStoreEntity;
    }

    @Override
    protected CustomKeyValueStoreMO asResource(CustomKeyValueStore entity) {
        final CustomKeyValueStoreMO customKeyValueStoreResource = ManagedObjectFactory.createCustomKeyValueStore();

        customKeyValueStoreResource.setKey(entity.getName());
        customKeyValueStoreResource.setValue(entity.getValue());

        return customKeyValueStoreResource;
    }

    @Override
    protected void updateEntity(final CustomKeyValueStore oldEntity, final CustomKeyValueStore newEntity) throws InvalidResourceException {
        oldEntity.setName(newEntity.getName());
        oldEntity.setValue(newEntity.getValue());
    }

    //- PRIVATE
    final CustomKeyValueStoreManager customKeyValueStoreManager;
}