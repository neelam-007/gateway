package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.CustomKeyValueStoreAdmin;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomKeyValueStoreTest {

    private static final String PREFIX = "com.l7tech.server.store.prefix.";
    private static final String KEY = PREFIX + "key";
    private static final String VALUE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<java version=\"1.7.0_03\" class=\"java.beans.XMLDecoder\">\n" +
        "<object class=\"com.l7tech.custom.salesforce.partner.v26.assertion.SalesForceConnection\">\n" +
        "<void property=\"description\">\n" +
        "<string>sfdc1</string>\n" +
        "</void>\n" +
        "<void property=\"passwordOid\">\n" +
        "<long>9601024</long>\n" +
        "</void>\n" +
        "<void property=\"securityTokenOid\">\n" +
        "<long>9601025</long>\n" +
        "</void>\n" +
        "<void property=\"username\">\n" +
        "<string>user@salesforce.com</string>\n" +
        "</void>\n" +
        "</object>\n" +
        "</java>";

    private CustomKeyValueStore customKeyValue;
    private Collection<CustomKeyValueStore> customKeyValues;

    @Mock
    private Registry registry;

    @Mock
    private CustomKeyValueStoreAdmin customKeyValueStoreAdmin;

    private KeyValueStoreServicesImpl keyValueStoreServices;
    private CustomKeyValueStoreImpl customKeyValueStore;

    @Before
    public void setup() throws Exception {
        keyValueStoreServices = new KeyValueStoreServicesImpl();
        customKeyValueStore = new CustomKeyValueStoreImpl();
        Registry.setDefault(registry);
        when(registry.getCustomKeyValueStoreAdmin()).thenReturn(customKeyValueStoreAdmin);

        customKeyValue = new CustomKeyValueStore();
        customKeyValue.setName(KEY);
        customKeyValue.setValue(VALUE.getBytes("UTF-8"));
        customKeyValues = new ArrayList<>();
        customKeyValues.add(customKeyValue);
    }

    @Test
    public void testGetKeyValueStore() throws Exception {
        // Test get default key value store.
        assertNotNull(keyValueStoreServices.getKeyValueStore());

        // Test get known key value store.
        assertNotNull(keyValueStoreServices.getKeyValueStore(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME));
    }

    @Test(expected = KeyValueStoreException.class)
    public void testGetKeyValueStoreNotFound() throws Exception {
        keyValueStoreServices.getKeyValueStore("unknown_key_store");
    }

    @Test
    public void testFindByKeyPrefix() throws Exception {
        when(customKeyValueStoreAdmin.findByKeyPrefix(PREFIX)).thenReturn(customKeyValues);
        Map<String, byte[]> actual = customKeyValueStore.findAllWithKeyPrefix(PREFIX);
        assertNotNull(actual);

        for (CustomKeyValueStore customKeyValue : customKeyValues) {
            byte[] actualValue = actual.get(customKeyValue.getName());
            assertNotNull(actualValue);
            assertEquals(customKeyValue.getValue(), actualValue);
        }
    }

    @Test
    public void testGet() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(customKeyValue);
        byte[] actual = customKeyValueStore.get(KEY);
        assertNotNull(actual);
        assertTrue(Arrays.equals(VALUE.getBytes("UTF-8"), actual));
    }

    @Test
    public void testGetNotFound() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(null);
        byte[] actual = customKeyValueStore.get(KEY);
        assertNull(actual);
    }

    @Test
    public void testContain() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(customKeyValue);
        assertTrue(customKeyValueStore.contains(KEY));
    }

    @Test
    public void testContainNotFound() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(null);
        assertFalse(customKeyValueStore.contains(KEY));
    }

    @Test
    public void testSave() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(null);
        customKeyValueStore.save(KEY, VALUE.getBytes("UTF-8"));
        // Nothing to check. Just make sure that method does not throw any exceptions.
    }

    @Test (expected = KeyValueStoreException.class)
    public void testSaveKeyAlreadyExist() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(customKeyValue);
        customKeyValueStore.save(KEY, VALUE.getBytes("UTF-8"));
    }

    @Test
    public void testUpdate() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(customKeyValue);
        customKeyValueStore.update(KEY, VALUE.getBytes("UTF-8"));
        // Nothing to check. Just make sure that method does not throw any exceptions.
    }

    @Test (expected = KeyValueStoreException.class)
    public void testUpdateKeyDoesNotExist() throws Exception {
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(null);
        customKeyValueStore.update(KEY, VALUE.getBytes("UTF-8"));
    }

    @Test
    public void testSaveOrUpdate() throws Exception {
        // Key does not exist
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(null);
        customKeyValueStore.saveOrUpdate(KEY, VALUE.getBytes("UTF-8"));

        // key already exist
        when(customKeyValueStoreAdmin.findByUniqueKey(KEY)).thenReturn(customKeyValue);
        customKeyValueStore.saveOrUpdate(KEY, VALUE.getBytes("UTF-8"));

        // Nothing to check. Just make sure that method does not throw any exceptions.
    }

    @Test
    public void testDelete() throws Exception {
        customKeyValueStore.delete(KEY);
        // Nothing to check. Just make sure that method does not throw any exceptions.
    }
}