package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class CustomKeyValueMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(CustomKeyValueMigration.class.getName());
    private Item<CustomKeyValueStoreMO> customKeyValueStoreItem;

    @Before
    public void before() throws Exception {
        CustomKeyValueStoreMO customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setKey("My Key");
        customKeyValueStoreMO.setStoreName(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME);
        customKeyValueStoreMO.setValue("My Value".getBytes());

        RestResponse response = getSourceEnvironment().processRequest("customKeyValues", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(customKeyValueStoreMO)));
        assertOkCreatedResponse(response);

        customKeyValueStoreItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        customKeyValueStoreItem.setContent(customKeyValueStoreMO);

    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("customKeyValues/" + customKeyValueStoreItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?customKeyValue=" + customKeyValueStoreItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A CustomKeyValue", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items. A CustomKeyValue", 1, bundleItem.getContent().getMappings().size());
    }
}
