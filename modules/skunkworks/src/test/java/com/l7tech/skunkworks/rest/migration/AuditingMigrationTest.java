package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks audits created by the migration importing action
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class AuditingMigrationTest extends LocalMigrationTestCases {

    private static final Logger logger = Logger.getLogger(AuditingMigrationTest.class.getName());

    private PolicyManager policyManager;
    private FolderManager folderManager;
    private AuditRecordManager auditRecordManager;

    private final Policy policy = new Policy(PolicyType.INTERNAL, "Policy", "", false);
    protected Folder rootFolder;


    @Before
    public void before() throws Exception {
        super.before();

        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        auditRecordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("auditRecordManager", AuditRecordManager.class);

        rootFolder = folderManager.findRootFolder();

        // create policy
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        policy.setXml(policyXml);
        policy.setGuid(UUID.randomUUID().toString());
        policy.setFolder(rootFolder);
        policyManager.save(policy);
    }

    @After
    public void after() throws Exception {
        for(EntityHeader header : policyManager.findAllHeaders()) {
            policyManager.delete(header.getGoid());
        }

    }

    @Test
    public void testImportSuccessful() throws Exception{

        // get bundle
        RestResponse response;

        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // make all update
        for(int i = 0 ; i < bundleItem.getContent().getMappings().size(); ++i ) {
            if(! bundleItem.getContent().getMappings().get(i).getType().equals(EntityType.FOLDER.toString())) {
                bundleItem.getContent().getMappings().get(i).setAction(Mapping.Action.NewOrUpdate);
            }
        }

        // import bundle
        long before = System.currentTimeMillis();
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle" , HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);
        final Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));


        List<AuditRecordHeader> adminAudits = auditRecordManager.findHeaders(new AuditSearchCriteria.Builder().auditType(AuditType.ADMIN).fromTime(new Date(before)).build());
        Assert.assertTrue(adminAudits.size() > 0);
        logger.log(Level.INFO, "Admin audits recorded:"+ adminAudits.size());

        for(int i = 0 ; i < mappings.getContent().getMappings().size(); ++i ) {
            final String targetId = mappings.getContent().getMappings().get(i).getTargetId();
            if(mappings.getContent().getMappings().get(i).getActionTaken().equals(Mapping.ActionTaken.UpdatedExisting)) {
                Assert.assertTrue(Functions.exists(adminAudits, new Functions.Unary<Boolean, AuditRecordHeader>() {
                            @Override
                            public Boolean call(AuditRecordHeader auditRecordHeader) {
                                return auditRecordHeader.getDescription().matches(".*"+targetId+".*"+"updated"+".*");
                            }
                        })
                );
            }
        }
    }

    @Test
    public void testImportTest() throws Exception{

        // get bundle
        RestResponse response;

        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // make all update
        for(int i = 0 ; i < bundleItem.getContent().getMappings().size(); ++i ) {
            if(! bundleItem.getContent().getMappings().get(i).getType().equals(EntityType.FOLDER.toString())) {
                bundleItem.getContent().getMappings().get(i).setAction(Mapping.Action.NewOrUpdate);
            }
        }

        long before = System.currentTimeMillis();

        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle" ,"test=true", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        List<AuditRecordHeader> adminAudits = auditRecordManager.findHeaders(new AuditSearchCriteria.Builder().auditType(AuditType.ADMIN).fromTime(new Date(before)).build());
        Assert.assertEquals("No admin audits recorded", 0, adminAudits.size());
    }

    @Test
    public void testImportFailedTest() throws Exception{

        // get bundle
        RestResponse response;

        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // make all create new
        for(int i = 0 ; i < bundleItem.getContent().getMappings().size(); ++i ) {
            if(! bundleItem.getContent().getMappings().get(i).getType().equals(EntityType.FOLDER.toString())) {
                bundleItem.getContent().getMappings().get(i).setAction(Mapping.Action.AlwaysCreateNew);
            }
        }

        long before = System.currentTimeMillis();

        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        logger.log(Level.INFO, response.toString());
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertTrue("Import error", 400 <= response.getStatus());

        List<AuditRecordHeader> adminAudits = auditRecordManager.findHeaders(new AuditSearchCriteria.Builder().auditType(AuditType.ADMIN).fromTime(new Date(before)).build());
        Assert.assertEquals("No admin audits recorded", 0, adminAudits.size());
    }


    protected String objectToString(Object object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }

}
