package com.l7tech.server.store;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.imp.GoidEntityImp;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.server.GoidEntityManagerStub;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomKeyValueStoreManagerStub extends GoidEntityManagerStub<CustomKeyValueStore, EntityHeader> implements CustomKeyValueStoreManager {

    public CustomKeyValueStoreManagerStub(CustomKeyValueStore... customKeyValueStoresIn) {
        super(customKeyValueStoresIn);
    }

    @NotNull
    @Override
    public Collection<CustomKeyValueStore> findByKeyPrefix(@NotNull String keyPrefix) throws FindException {
        Collection<CustomKeyValueStore> got = findAll();
        List<CustomKeyValueStore> ret = new ArrayList<>();
        for (CustomKeyValueStore keyValue : got) {
            if (keyValue.getName().startsWith(keyPrefix)) {
                ret.add(keyValue);
            }
        }
        return ret;
    }

    @Override
    public void deleteByKey(@NotNull String key) throws DeleteException {
        try {
            Collection<CustomKeyValueStore> got = findAll();
            for (CustomKeyValueStore keyValue : got) {
                if (keyValue.getName().equals(key)) {
                    entities.remove(keyValue.getGoid());
                }
            }
        } catch (FindException e) {
            throw new DeleteException("Unable to delete using key: " + key, e);
        }
    }

    @Override
    public Class<? extends GoidEntityImp> getImpClass() {
        return CustomKeyValueStore.class;
    }
}