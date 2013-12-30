package com.l7tech.server.store;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

public class CustomKeyValueStoreTest {

    private static final String PREFIX = "com.l7tech.server.store.prefix.";

    private static final String EXISTING_KEY = PREFIX + "existing_key";
    private static final String EXISTING_VALUE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<java version=\"1.7.0_03\" class=\"java.beans.XMLDecoder\">" +
        "<object class=\"com.l7tech.custom.salesforce.partner.v26.assertion.SalesForceConnection\">" +
        "<void property=\"description\">" +
        "<string>sfdc1</string>" +
        "</void>" +
        "<void property=\"passwordOid\">" +
        "<long>9601024</long>" +
        "</void>" +
        "<void property=\"securityTokenOid\">" +
        "<long>9601025</long>" +
        "</void>" +
        "<void property=\"username\">" +
        "<string>user@salesforce.com</string>" +
        "</void>" +
        "</object>" +
        "</java>";

    private static final String NEW_KEY = PREFIX + "new_key";
    private static final String NEW_VALUE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<java version=\"1.7.0_03\" class=\"java.beans.XMLDecoder\">" +
        "<object class=\"com.l7tech.custom.salesforce.partner.v26.assertion.SalesForceConnection\">" +
        "<void property=\"description\">" +
        "<string>sfdc2</string>" +
        "</void>" +
        "<void property=\"passwordOid\">" +
        "<long>9601024</long>" +
        "</void>" +
        "<void property=\"securityTokenOid\">" +
        "<long>9601025</long>" +
        "</void>" +
        "<void property=\"username\">" +
        "<string>user@salesforce.com</string>" +
        "</void>" +
        "</object>" +
        "</java>";

    private Collection<CustomKeyValueStore> customKeyValues;

    private KeyValueStoreServicesImpl keyValueStoreServices;
    private CustomKeyValueStoreImpl customKeyValueStore;

    @Before
    public void setup() throws Exception {
        CustomKeyValueStore customKeyValue = new CustomKeyValueStore();
        customKeyValue.setGoid(new Goid(0,1));
        customKeyValue.setName(EXISTING_KEY);
        customKeyValue.setValue(EXISTING_VALUE.getBytes("UTF-8"));
        customKeyValues = new ArrayList<>();
        customKeyValues.add(customKeyValue);

        CustomKeyValueStoreManager manager = new CustomKeyValueStoreManagerStub(customKeyValue);
        keyValueStoreServices = new KeyValueStoreServicesImpl(manager);
        customKeyValueStore = new CustomKeyValueStoreImpl(manager);
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
        byte[] actual = customKeyValueStore.get(EXISTING_KEY);
        assertNotNull(actual);
        assertTrue(Arrays.equals(EXISTING_VALUE.getBytes("UTF-8"), actual));
    }

    @Test
    public void testGetNotFound() throws Exception {
        byte[] actual = customKeyValueStore.get(NEW_KEY);
        assertNull(actual);
    }

    @Test
    public void testContain() throws Exception {
        assertTrue(customKeyValueStore.contains(EXISTING_KEY));
    }

    @Test
    public void testContainNotFound() throws Exception {
        assertFalse(customKeyValueStore.contains(NEW_KEY));
    }

    @Test
    public void testSave() throws Exception {
        assertFalse(customKeyValueStore.contains(NEW_KEY));
        customKeyValueStore.save(NEW_KEY, NEW_KEY.getBytes("UTF-8"));
        assertTrue(customKeyValueStore.contains(NEW_KEY));
    }

    @Test (expected = KeyValueStoreException.class)
    public void testSaveKeyAlreadyExist() throws Exception {
        customKeyValueStore.save(EXISTING_KEY, EXISTING_VALUE.getBytes("UTF-8"));
    }

    @Test
    public void testUpdate() throws Exception {
        customKeyValueStore.update(EXISTING_KEY, NEW_VALUE.getBytes("UTF-8"));
        byte[] actual = customKeyValueStore.get(EXISTING_KEY);
        assertNotNull(actual);
        assertTrue(Arrays.equals(NEW_VALUE.getBytes("UTF-8"), actual));
    }

    @Test (expected = KeyValueStoreException.class)
    public void testUpdateKeyDoesNotExist() throws Exception {
        customKeyValueStore.update(NEW_KEY, NEW_VALUE.getBytes("UTF-8"));
    }

    @Test
    public void testSaveOrUpdate() throws Exception {
        // Key does not exist
        assertFalse(customKeyValueStore.contains(NEW_KEY));
        customKeyValueStore.saveOrUpdate(NEW_KEY, NEW_VALUE.getBytes("UTF-8"));
        byte[] actual = customKeyValueStore.get(NEW_KEY);
        assertNotNull(actual);
        assertTrue(Arrays.equals(NEW_VALUE.getBytes("UTF-8"), actual));

        // key already exist
        assertTrue(customKeyValueStore.contains(NEW_KEY));
        customKeyValueStore.saveOrUpdate(NEW_KEY, EXISTING_VALUE.getBytes("UTF-8"));
        actual = customKeyValueStore.get(NEW_KEY);
        assertNotNull(actual);
        assertTrue(Arrays.equals(EXISTING_VALUE.getBytes("UTF-8"), actual));
    }

    @Test
    public void testDelete() throws Exception {
        assertTrue(customKeyValueStore.contains(EXISTING_KEY));
        customKeyValueStore.delete(EXISTING_KEY);
        assertFalse(customKeyValueStore.contains(EXISTING_KEY));
    }

    @Test
    public void testDeleteDoesNotExist() throws Exception {
        assertFalse(customKeyValueStore.contains(NEW_KEY));
        customKeyValueStore.delete(NEW_KEY);
        assertFalse(customKeyValueStore.contains(NEW_KEY));
    }

    @Test
    public void testKeyValueStoreChangeEventListener() throws Exception {
        // Cannot fully test event notification mechanism because CustomKeyValueStoreManagerStub is
        // used, and not the actual CustomKeyValueStoreManagerImpl in this unit test.
        //
        KeyValueStoreChangeEventListener listener = new KeyValueStoreChangeEventListenerImpl();
        customKeyValueStore.addListener(PREFIX, listener);
        customKeyValueStore.removeListener(PREFIX, listener);
    }

    private class KeyValueStoreChangeEventListenerImpl implements KeyValueStoreChangeEventListener {
        public KeyValueStoreChangeEventListenerImpl() {
        }

        @Override
        public void onEvent(List<Event> events) {
            // Do something.
        }
    }
}