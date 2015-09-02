package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.transport.email.EmailListener;
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
public class EmailListenerMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(EmailListenerMigration.class.getName());
    private Item<EmailListenerMO> emailListenerItem;

    @Before
    public void before() throws Exception {
        EmailListenerMO emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName("Test Email listener created");
        emailListenerMO.setActive(true);
        emailListenerMO.setHostname("remoteHost");
        emailListenerMO.setPort(123);
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.POP3);
        emailListenerMO.setDeleteOnReceive(false);
        emailListenerMO.setUsername("AUser");
        emailListenerMO.setPassword("UserPass");
        emailListenerMO.setFolder("MyFolder");
        emailListenerMO.setPollInterval(5000);
        emailListenerMO.setUseSsl(false);
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(EmailListener.PROP_IS_HARDWIRED_SERVICE, (Boolean.TRUE).toString())
                .map());

        RestResponse response = getSourceEnvironment().processRequest("emailListeners", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(emailListenerMO)));
        assertOkCreatedResponse(response);

        emailListenerItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        emailListenerItem.setContent(emailListenerMO);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("emailListeners/" + emailListenerItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?emailListener=" + emailListenerItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. An emailListener", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items. An emailListener", 1, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreEmailListenerDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?emailListener=" + emailListenerItem.getId() + "&requireEmailListener=" + emailListenerItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A emailListener", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A emailListener", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }
}
