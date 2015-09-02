package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.Goid;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ActiveConnectorMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(ActiveConnectorMigration.class.getName());
    private Item<ActiveConnectorMO> activeConnectorItem;

    @Before
    public void before() throws Exception {
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("Test MQ Config created");
        activeConnectorMO.setHardwiredId(new Goid(123, 567).toString());
        activeConnectorMO.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager")
                .put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "false")
                .map());

        RestResponse response = getSourceEnvironment().processRequest("activeConnectors", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));
        assertOkCreatedResponse(response);

        activeConnectorItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        activeConnectorItem.setContent(activeConnectorMO);

    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("activeConnectors/" + activeConnectorItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?activeConnector=" + activeConnectorItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. An Active Connector", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 2 mappings. An Active Connector and a service", 2, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreActiveConnectorDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?activeConnector=" + activeConnectorItem.getId() + "&requireActiveConnector=" + activeConnectorItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. An Active Connector", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. An Active Connector", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }
}
