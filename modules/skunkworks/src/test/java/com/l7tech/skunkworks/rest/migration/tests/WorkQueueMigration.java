package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.workqueue.WorkQueue;
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
import static org.junit.Assert.assertTrue;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class WorkQueueMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(WorkQueueMigration.class.getName());
    private Item<WorkQueueMO> workQueueItem;

    @Before
    public void before() throws Exception {
        WorkQueueMO workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setName("Test work queue created");
        workQueueMO.setMaxQueueSize(5);
        workQueueMO.setThreadPoolMax(2);
        workQueueMO.setRejectPolicy(WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM);

        RestResponse response = getSourceEnvironment().processRequest("workQueues", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(workQueueMO)));
        assertOkCreatedResponse(response);

        workQueueItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        workQueueItem.setContent(workQueueMO);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("workQueues/" + workQueueItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?workQueue=" + workQueueItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items.", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items.", 1, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreWorkQueueDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?workQueue=" + workQueueItem.getId() + "&requireWorkQueue=" + workQueueItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A workQueue", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A workQueue", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }
}
