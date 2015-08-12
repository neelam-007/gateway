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
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class HttpConfigurationMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(HttpConfigurationMigration.class.getName());
    private Item<HttpConfigurationMO> httpConfigurationItem;

    @Before
    public void before() throws Exception {
        HttpConfigurationMO httpConfiguration = ManagedObjectFactory.createHttpConfiguration();
        httpConfiguration.setUsername("userNew");
        httpConfiguration.setPort(333);
        httpConfiguration.setHost("newHost");
        httpConfiguration.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfiguration.setPath("path");
        httpConfiguration.setTlsKeyUse(HttpConfigurationMO.Option.DEFAULT);

        RestResponse response = getSourceEnvironment().processRequest("httpConfigurations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(httpConfiguration)));
        assertOkCreatedResponse(response);

        httpConfigurationItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        httpConfigurationItem.setContent(httpConfiguration);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("httpConfigurations/" + httpConfigurationItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?httpConfiguration=" + httpConfigurationItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A httpConfiguration", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 2 items. A httpConfiguration and a private key", 2, bundleItem.getContent().getMappings().size());
    }
}
