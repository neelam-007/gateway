package com.l7tech.server.store;

import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomKeyValueStoreAdminImplTest {

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

    private CustomKeyValueStoreAdminImpl admin;
    private CustomKeyValueStore customKeyValue;
    private Collection<CustomKeyValueStore> customKeyValues;

    @Mock
    private CustomKeyValueStoreManager manager;

    @Before
    public void setup() throws Exception {
        admin = new CustomKeyValueStoreAdminImpl();
        admin.setCustomKeyValueStoreManager(manager);

        customKeyValue = new CustomKeyValueStore();
        customKeyValue.setName(KEY);
        customKeyValue.setValue(VALUE.getBytes("UTF-8"));
        customKeyValues = new ArrayList<>();
        customKeyValues.add(customKeyValue);
    }

    @Test
    public void testFindByKeyPrefix() throws Exception {
        when(manager.findByKeyPrefix(PREFIX)).thenReturn(customKeyValues);
        Collection<CustomKeyValueStore> actual = admin.findByKeyPrefix(PREFIX);
        assertNotNull(actual);
        assertArrayEquals(customKeyValues.toArray(), actual.toArray());
    }

    @Test
    public void testFindByUniqueKey() throws Exception {
        when(manager.findByUniqueName(KEY)).thenReturn(customKeyValue);
        CustomKeyValueStore actual = admin.findByUniqueKey(KEY);
        assertNotNull(actual);
        assertEquals(customKeyValue, actual);
    }

    @Test
    public void findByPrimaryKeyNotFound() throws Exception {
        when(manager.findByUniqueName(anyString())).thenReturn(null);
        CustomKeyValueStore actual = admin.findByUniqueKey(KEY);
        assertNull(actual);
    }

    @Test
    public void testSaveCustomKeyValue() throws Exception {
        admin.saveCustomKeyValue(customKeyValue);
        // Nothing to check. Just make sure that method does not throw any exceptions.
    }

    @Test
    public void testUpdateCustomKeyValue() throws Exception {
        admin.updateCustomKeyValue(customKeyValue);
        // Nothing to check. Just make sure that method does not throw any exceptions.
    }

    @Test
    public void testDeleteCustomKeyValue() throws Exception {
        admin.deleteCustomKeyValue(KEY);
        // Nothing to check. Just make sure that method does not throw any exceptions.
    }
}