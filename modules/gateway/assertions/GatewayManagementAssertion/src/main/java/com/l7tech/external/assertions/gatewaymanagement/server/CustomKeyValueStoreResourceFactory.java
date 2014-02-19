package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.CustomKeyValueStoreMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

@ResourceFactory.ResourceType(type=CustomKeyValueStoreMO.class)
public class CustomKeyValueStoreResourceFactory extends EntityManagerResourceFactory<CustomKeyValueStoreMO, CustomKeyValueStore,EntityHeader> {

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
    public CustomKeyValueStore fromResource(Object resource, boolean strict) throws InvalidResourceException {
        if (!(resource instanceof CustomKeyValueStoreMO )) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected custom key value");
        }

        final CustomKeyValueStoreMO customKeyValueStoreResource = (CustomKeyValueStoreMO) resource;
        if (!KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME.equals(customKeyValueStoreResource.getStoreName())) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid store name");
        }

        final CustomKeyValueStore customKeyValueStoreEntity = new CustomKeyValueStore();
        customKeyValueStoreEntity.setName(customKeyValueStoreResource.getKey());
        customKeyValueStoreEntity.setValue(customKeyValueStoreResource.getValue());

        return customKeyValueStoreEntity;
    }

    @Override
    public CustomKeyValueStoreMO asResource(CustomKeyValueStore entity) {
        final CustomKeyValueStoreMO customKeyValueStoreResource = ManagedObjectFactory.createCustomKeyValueStore();

        customKeyValueStoreResource.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
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