package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ScheduledTaskMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(ScheduledTaskMigration.class.getName());
    private Item<ScheduledTaskMO> scheduledTaskItem;
    private Item<PolicyMO> policyItem;

    @Before
    public void before() throws Exception {
        //create policy
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Source Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.SERVICE_OPERATION);
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .put("tag", "com.l7tech.objectmodel.polback.BackgroundTask")
                .put("subtag", "run")
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml );
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        ScheduledTaskMO scheduledTask = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTask.setName("Test Scheduled Task created");
        scheduledTask.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyItem.getId()));
        scheduledTask.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
        scheduledTask.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.DISABLED);
        scheduledTask.setCronExpression("* * */5 * ?");
        scheduledTask.setUseOneNode(false);
        scheduledTask.setProperties(CollectionUtils.<String, String>mapBuilder().put("idProvider", new Goid(0, -3).toString()).put("userId", new Goid(1, 2).toString()).map());

        response = getSourceEnvironment().processRequest("scheduledTasks", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(scheduledTask)));
        assertOkCreatedResponse(response);

        scheduledTaskItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        scheduledTaskItem.setContent(scheduledTask);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("scheduledTasks/" + scheduledTaskItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?scheduledTask=" + scheduledTaskItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 2 items.", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 4 items.", 4, bundleItem.getContent().getMappings().size());
    }
}
