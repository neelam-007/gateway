package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ListenPortMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(ListenPortMigration.class.getName());
    private Item<ListenPortMO> identityProviderItem;

    @Before
    public void before() throws Exception {
        ListenPortMO listenPortMO = ManagedObjectFactory.createListenPort();
        listenPortMO.setName("ConnectorWithHardwiredServiceId");
        listenPortMO.setEnabled(false);
        listenPortMO.setProtocol("http");
        listenPortMO.setInterface("ConnectorWithHardwiredServiceId");
        listenPortMO.setPort(5555);
        listenPortMO.setEnabledFeatures(Arrays.asList("Published service message input"));

        RestResponse response = getSourceEnvironment().processRequest("listenPorts", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(listenPortMO)));
        assertOkCreatedResponse(response);

        identityProviderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        identityProviderItem.setContent(listenPortMO);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("listenPorts/" + identityProviderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?listenPort=" + identityProviderItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A listenPort", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items. A listenPort", 1, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreListenPortDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?listenPort=" + identityProviderItem.getId() + "&requireListenPort=" + identityProviderItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A listenPort", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A listenPort", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }
}
