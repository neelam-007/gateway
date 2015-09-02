package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.JVMDatabaseBasedRestManagementEnvironment;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import junit.framework.Assert;
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
public class PolicyMigration extends MigrationTestBase {
    private static final Logger logger = Logger.getLogger(PolicyMigration.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<FolderMO> folderItem;
    private Item<PolicyAliasMO> policyAliasItem;
    private Item<Mappings> mappingsToClean;

    final String Default_Ass_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <wsp:All wsp:Usage=\"Required\">\n" +
                    "        <L7p:AuditDetailAssertion>\n" +
                    "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                    "        </L7p:AuditDetailAssertion>\n" +
                    "    </wsp:All>\n" +
                    "</wsp:Policy>";

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
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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

        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create folder item
        FolderMO parentFolderMO = ManagedObjectFactory.createFolder();
        parentFolderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        parentFolderMO.setName("Source folder");
        response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(parentFolderMO)));

        assertOkCreatedResponse(response);

        folderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderItem.setContent(parentFolderMO);

        // create policy alias
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId(folderItem.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyItem.getId()));
        response = getSourceEnvironment().processRequest("policyAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyAliasMO)));
        assertOkCreatedResponse(response);
        policyAliasItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyAliasItem.setContent(policyAliasMO);
    }

    @After
    public void after() throws Exception {

        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response;

        response = getSourceEnvironment().processRequest("policyAliases/" + policyAliasItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?policy=" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items.", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 2 items.", 2, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnorePolicyDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?policy=" + policyItem.getId() + "&requirePolicy=" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A policy", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A policy", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 2 mappings. A folder, a policy", 2, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        validate(mappings);

        //validate that the policy GUID's are preserved
        response = getTargetEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        Item<PolicyMO> policyTargetItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(policyItem.getContent().getGuid(), policyTargetItem.getContent().getGuid());
    }

    @Test
    public void testImportNewActivateWithComment() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 2 mappings. A folder, a policy", 2, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle?activate=true&versionComment=Comment", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());

        validate(mappings);

        // verify that new policy is activated
        response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId() + "/versions", "", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<PolicyVersionMO> policyVersionList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(1,policyVersionList.getContent().size());
        PolicyVersionMO version = policyVersionList.getContent().get(0).getContent();
        Assert.assertEquals(true,version.isActive());
        Assert.assertEquals("Comment",version.getComment());
    }

    @Test
    public void testImportNewDefaultSettings() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 2 mappings. A folder, a policy", 2, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());

        validate(mappings);

        // verify that new policy is activated
        response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId() + "/versions", "", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<PolicyVersionMO> policyVersionList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(1,policyVersionList.getContent().size());
        PolicyVersionMO version = policyVersionList.getContent().get(0).getContent();
        Assert.assertEquals(true,version.isActive());
    }

    @Test
    public void testImportNewFolder() throws Exception {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("New Target folder");
        RestResponse response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());
        folderCreated.setContent(folderMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 2 mappings. A folder, a policy", 2, bundleItem.getContent().getMappings().size());

            // update mapping
            bundleItem.getContent().getMappings().get(0).setTargetId(folderCreated.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(folderCreated.getId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            //verify policy
            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.GET, null,"");
            Item<PolicyMO> targetPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(folderCreated.getId(), targetPolicy.getContent().getPolicyDetail().getFolderId());
            mappingsToClean = mappings;

            validate(mappings);
        }finally{

            response = getTargetEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("folders/"+folderCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testImportPolicyAliasNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, a folder, a policy alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, 2 folders, a policy alias", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        Mapping policyAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyAliasMapping.getActionTaken());
        Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
        Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testMappedPolicy() throws Exception{
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.setGuid(null);
        policyMO.getPolicyDetail().setName("Target Policy");
        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());
        policyCreated.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, a folder, a policy alias", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, 2 folders, a policy alias", 4, bundleItem.getContent().getMappings().size());

            // map the policy
            bundleItem.getContent().getMappings().get(2).setTargetId(policyCreated.getId());
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping folderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
            Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyCreated.getId(), policyMapping.getTargetId());

            Mapping policyAliasMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyAliasMapping.getActionTaken());
            Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
            Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

            // validate policy alias reference
            response = getTargetEnvironment().processRequest("policyAliases/" + policyAliasItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<PolicyAliasMO> newPolicyAlias = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(policyCreated.getId(), newPolicyAlias.getContent().getPolicyReference().getId());


            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("policyAliases/" + policyAliasItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testPolicyDuplicateName() throws Exception{
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setName(policyItem.getName());
        policyMO.getPolicyDetail().setId(null);
        policyMO.setResourceSets(Arrays.asList(policyMO.getResourceSets().get(0)));
        logger.info( XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());
        policyCreated.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 2 mappings. A policy, a folder", 2, bundleItem.getContent().getMappings().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertConflictResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Mapping policyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ErrorType.UniqueKeyConflict, policyMapping.getErrorType());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        }finally{
            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testUpdatePolicy() throws Exception{
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.setGuid(null);
        policyMO.getPolicyDetail().setName("Target Policy");
        policyMO.getPolicyDetail().setId(null);
        policyMO.setResourceSets(Arrays.asList(policyMO.getResourceSets().get(0)));
        logger.info( XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());
        policyCreated.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 2 mappings. A policy, a folder", 2, bundleItem.getContent().getMappings().size());

            // map and update policy
            bundleItem.getContent().getMappings().get(1).setTargetId(policyCreated.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).addProperty("FailOnNew", true);
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyCreated.getId(), policyMapping.getTargetId());

            // validate updated policy
            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<PolicyMO> updatedPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(policyItem.getName(), updatedPolicy.getContent().getPolicyDetail().getName());
            Assert.assertEquals(1, updatedPolicy.getContent().getResourceSets().size());

            validate(mappings);

        }finally{
            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapByNameNoNameButSameIdPolicy() throws Exception{
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.setGuid(null);
        policyMO.getPolicyDetail().setName("Target Policy");
        policyMO.getPolicyDetail().setId(null);
        policyMO.setResourceSets(Arrays.asList(policyMO.getResourceSets().get(0)));
        logger.info( XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        RestResponse response = getTargetEnvironment().processRequest("policies/" +policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());
        policyCreated.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 2 mappings. A policy, a folder", 2, bundleItem.getContent().getMappings().size());

            // map and update policy
            bundleItem.getContent().getMappings().get(1).setTargetId(policyCreated.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).addProperty("FailOnExisting", true);
            bundleItem.getContent().getMappings().get(1).addProperty("MapBy", "name");
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertConflictResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, policyMapping.getAction());
            Assert.assertEquals(Mapping.ErrorType.UniqueKeyConflict, policyMapping.getErrorType());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());

        }finally{
            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @BugId("SSG-8716")
    @Test
    public void testGuidConflictPolicy() throws Exception{
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.setGuid(null);
        policyMO.getPolicyDetail().setName("Target Policy 1");
        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated1.getId());
        policyCreated1.setContent(policyMO);

        policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setGuid(policyItem.getContent().getGuid());
        policyMO.setGuid(policyItem.getContent().getGuid());
        policyMO.getPolicyDetail().setName("Target Policy 2");
        response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated2.getId());
        policyCreated2.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, a folder, a policy alias", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, 2 folders, a policy alias", 4, bundleItem.getContent().getMappings().size());

            // map the policy
            bundleItem.getContent().getMappings().get(2).addProperty("MapBy", "name");
            bundleItem.getContent().getMappings().get(2).addProperty("MapTo", "Target Policy");
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertConflictResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping folderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
            Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ErrorType.InvalidResource, policyMapping.getErrorType());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());

            Mapping policyAliasMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
            Assert.assertEquals(Mapping.ErrorType.InvalidResource, policyMapping.getErrorType());
            Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
        }finally{
            mappingsToClean = null;
        }
    }

    /**
     * Source:
     * - Root
     *   - Policy A
     *   - Folder A
     *     - PolicyAlias A (for Policy A)
     *
     * Target:
     * - Root
     *   - PolicyAlias B (for Policy B)
     *   - Folder B
     *     - Policy B
     *
     * Map the following:
     * Policy A -> Policy B
     * PolicyAlias A -> PolicyAlias B
     *
     * Expected Result:
     * - Root
     *   - PolicyAlias A (for Policy A)
     *   - Folder B
     *     - Policy A
     *   - Folder A
     *
     */
    @BugId("SSG-8792")
    @Test
    public void testUpdatePolicyInDifferentFolder() throws Exception{
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setName("MyTargetPolicyFolder");
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        RestResponse response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());
        folderCreated.setContent(folderMO);

        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()), PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.setGuid(null);
        policyMO.getPolicyDetail().setName("Target Policy");
        policyMO.getPolicyDetail().setId(null);
        policyMO.getPolicyDetail().setFolderId(folderCreated.getId());
        policyMO.setResourceSets(Arrays.asList(policyMO.getResourceSets().get(0)));
        logger.info(XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());
        policyCreated.setContent(policyMO);

        // create policy alias
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyCreated.getId()));
        response = getTargetEnvironment().processRequest("policyAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyAliasMO)));
        assertOkCreatedResponse(response);
        Item<PolicyAliasMO> policyAliasCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyAliasMO.setId(policyAliasCreated.getId());
        policyAliasCreated.setContent(policyAliasMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, folder and policy alias", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. 2 folder, a policy, and policy alias", 4, bundleItem.getContent().getMappings().size());

            // map and update policy
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), policyItem.getId()).setTargetId(policyCreated.getId());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), policyItem.getId()).setAction(Mapping.Action.NewOrUpdate);
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), policyItem.getId()).addProperty("FailOnNew", true);

            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), policyAliasItem.getId()).setTargetId(policyAliasCreated.getId());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), policyAliasItem.getId()).setAction(Mapping.Action.NewOrUpdate);
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), policyAliasItem.getId()).addProperty("FailOnNew", true);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), Folder.ROOT_FOLDER_ID.toString());
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyItem.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyCreated.getId(), policyMapping.getTargetId());

            Mapping policyAliasMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyAliasItem.getId());
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, policyAliasMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, policyAliasMapping.getActionTaken());
            Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
            Assert.assertEquals(policyAliasCreated.getId(), policyAliasMapping.getTargetId());

            // validate updated policy
            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<PolicyMO> updatedPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(policyItem.getName(), updatedPolicy.getContent().getPolicyDetail().getName());
            Assert.assertEquals("The policy folder was updated but it shouldn't have been.", folderCreated.getId(), updatedPolicy.getContent().getPolicyDetail().getFolderId());
            Assert.assertEquals(1, updatedPolicy.getContent().getResourceSets().size());

            validate(mappings);

            //get the created folder dependencies. It should be empty
            response = getTargetEnvironment().processRequest("folders/" + folderItem.getId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(0, dependencies.getContent().getDependencies().size());

        }finally{

            response = getTargetEnvironment().processRequest("policyAliases/" + policyAliasCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("folders/" + folderCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

        }
    }

    @Test
    public void testAlwaysCreateNewGuidConflict() throws Exception{
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(policyItem.getId());
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.setGuid(policyItem.getContent().getGuid());
        policyMO.getPolicyDetail().setName("Target Policy");
        RestResponse response = getTargetEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());
        policyCreated.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, a folder, a policy alias", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, 2 folders, a policy alias", 4, bundleItem.getContent().getMappings().size());

            // map the policy
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.AlwaysCreateNew);
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping folderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
            Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.AlwaysCreateNew, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertFalse(policyCreated.getId().equals(policyMapping.getTargetId()));

            Mapping policyAliasMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyAliasMapping.getActionTaken());
            Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
            Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

            // validate policy alias reference
            response = getTargetEnvironment().processRequest("policyAliases/" + policyAliasItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<PolicyAliasMO> newPolicyAlias = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(policyMapping.getTargetId(), newPolicyAlias.getContent().getPolicyReference().getId());


            validate(mappings);

            mappingsToClean = mappings;
        }finally{

            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(policyItem.getContent()),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.setGuid(null);
        policyMO.getPolicyDetail().setName("Target Policy");
        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());
        policyCreated.setContent(policyMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(policyCreated.getId());
        mapping.setSrcId(Goid.DEFAULT_GOID.toString());
        mapping.setType(policyCreated.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(policyCreated.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Arrays.<Item>asList(policyCreated));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
        Mapping activeConnectorMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.POLICY.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(policyCreated.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("policies/"+policyCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);

        //check that all auto created roles where deleted
        response = getTargetEnvironment().processRequest("roles", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<RbacRoleMO> roles = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        for(Item<RbacRoleMO> role : roles.getContent()) {
            Assert.assertNotSame("Found the auto created role for the deleted entity: " + objectToString(role), policyCreated.getId(), role.getContent().getEntityID());
        }
    }

    @BugId("SSG-10712")
    @Test
    public void deletePolicyUsedByServiceTest() throws Exception {
        Item<PolicyMO> policyCreated = createPolicy(getTargetEnvironment(), "MyUsedPolicy", Default_Ass_xml);
        Item<ServiceMO> serviceCreated = createService(getTargetEnvironment(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Include>\n" +
                "            <L7p:PolicyGuid stringValue=\""+policyCreated.getContent().getGuid()+"\"/>\n" +
                "        </L7p:Include>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");


        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping serviceMapping = ManagedObjectFactory.createMapping();
        serviceMapping.setAction(Mapping.Action.Delete);
        serviceMapping.setSrcId(serviceCreated.getId());
        serviceMapping.setType(serviceCreated.getType());

        Mapping policyMapping = ManagedObjectFactory.createMapping();
        policyMapping.setAction(Mapping.Action.Delete);
        policyMapping.setSrcId(policyCreated.getId());
        policyMapping.setType(policyCreated.getType());

        bundle.setMappings(Arrays.asList(serviceMapping, policyMapping));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        RestResponse response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
        Mapping serviceMappingReturned = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMappingReturned.getType());
        Assert.assertEquals(Mapping.Action.Delete, serviceMappingReturned.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, serviceMappingReturned.getActionTaken());
        Assert.assertEquals(serviceCreated.getId(), serviceMappingReturned.getTargetId());

        Mapping policyMappingReturned = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMappingReturned.getType());
        Assert.assertEquals(Mapping.Action.Delete, policyMappingReturned.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, policyMappingReturned.getActionTaken());
        Assert.assertEquals(policyCreated.getId(), policyMappingReturned.getTargetId());

        response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);

        response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);

        //check that all auto created roles where deleted
        response = getTargetEnvironment().processRequest("roles", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<RbacRoleMO> roles = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        for (Item<RbacRoleMO> role : roles.getContent()) {
            Assert.assertNotSame("Found the auto created role for the deleted entity: " + objectToString(role), policyCreated.getId(), role.getContent().getEntityID());
            Assert.assertNotSame("Found the auto created role for the deleted entity: " + objectToString(role), serviceCreated.getId(), role.getContent().getEntityID());
        }
    }

    @BugId("SSG-10712")
    @Test
    public void deletePolicyUsedByPolicyTest() throws Exception {
        Item<PolicyMO> policyCreated = createPolicy(getTargetEnvironment(), "MyUsedPolicy", Default_Ass_xml);
        Item<PolicyMO> policyUsing = createPolicy(getTargetEnvironment(), "MyUsingPolicy", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Include>\n" +
                "            <L7p:PolicyGuid stringValue=\"" + policyCreated.getContent().getGuid() + "\"/>\n" +
                "        </L7p:Include>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");


        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping policyUsingMapping = ManagedObjectFactory.createMapping();
        policyUsingMapping.setAction(Mapping.Action.Delete);
        policyUsingMapping.setSrcId(policyUsing.getId());
        policyUsingMapping.setType(policyUsing.getType());

        Mapping policyMapping = ManagedObjectFactory.createMapping();
        policyMapping.setAction(Mapping.Action.Delete);
        policyMapping.setSrcId(policyCreated.getId());
        policyMapping.setType(policyCreated.getType());

        bundle.setMappings(Arrays.asList(policyUsingMapping, policyMapping));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        RestResponse response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
        Mapping serviceMappingReturned = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.POLICY.toString(), serviceMappingReturned.getType());
        Assert.assertEquals(Mapping.Action.Delete, serviceMappingReturned.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, serviceMappingReturned.getActionTaken());
        Assert.assertEquals(policyUsing.getId(), serviceMappingReturned.getTargetId());

        Mapping policyMappingReturned = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMappingReturned.getType());
        Assert.assertEquals(Mapping.Action.Delete, policyMappingReturned.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, policyMappingReturned.getActionTaken());
        Assert.assertEquals(policyCreated.getId(), policyMappingReturned.getTargetId());

        response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);

        response = getTargetEnvironment().processRequest("policies/" + policyUsing.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);

        //check that all auto created roles where deleted
        response = getTargetEnvironment().processRequest("roles", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<RbacRoleMO> roles = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        for (Item<RbacRoleMO> role : roles.getContent()) {
            Assert.assertNotSame("Found the auto created role for the deleted entity: " + objectToString(role), policyCreated.getId(), role.getContent().getEntityID());
            Assert.assertNotSame("Found the auto created role for the deleted entity: " + objectToString(role), policyUsing.getId(), role.getContent().getEntityID());
        }
    }

    @BugId("SSG-10712")
    @Test
    public void deletePolicyUsedByPolicyFailTest() throws Exception {

        Item<PolicyMO> policyCreated = createPolicy(getTargetEnvironment(), "MyUsedPolicy", Default_Ass_xml);
        Item<PolicyMO> policyUsing = createPolicy(getTargetEnvironment(), "MyUsingPolicy", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Include>\n" +
                "            <L7p:PolicyGuid stringValue=\"" + policyCreated.getContent().getGuid() + "\"/>\n" +
                "        </L7p:Include>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        try {
            Bundle bundle = ManagedObjectFactory.createBundle();

            Mapping policyUsingMapping = ManagedObjectFactory.createMapping();
            policyUsingMapping.setAction(Mapping.Action.Ignore);
            policyUsingMapping.setSrcId(policyUsing.getId());
            policyUsingMapping.setType(policyUsing.getType());

            Mapping policyMapping = ManagedObjectFactory.createMapping();
            policyMapping.setAction(Mapping.Action.Delete);
            policyMapping.setSrcId(policyCreated.getId());
            policyMapping.setType(policyCreated.getType());

            bundle.setMappings(Arrays.asList(policyUsingMapping, policyMapping));

            //import the bundle
            logger.log(Level.INFO, objectToString(bundle));
            RestResponse response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundle));
            assertConflictResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
            Mapping usingPolicyReturnedMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.POLICY.toString(), usingPolicyReturnedMapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, usingPolicyReturnedMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, usingPolicyReturnedMapping.getActionTaken());

            Mapping policyMappingReturned = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMappingReturned.getType());
            Assert.assertEquals(Mapping.Action.Delete, policyMappingReturned.getAction());
            Assert.assertEquals(Mapping.ErrorType.ImproperMapping, policyMappingReturned.getErrorType());
            Assert.assertEquals(policyCreated.getId(), policyMappingReturned.getSrcId());
        } finally {
            RestResponse response = getTargetEnvironment().processRequest("policies/" + policyUsing.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/" + policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }

    }

    private Item<PolicyMO> createPolicy(JVMDatabaseBasedRestManagementEnvironment environment, String policyName, String assXml) throws Exception {
        //create policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName(policyName);
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml);
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = environment.processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        Item<PolicyMO> policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = environment.processRequest("policies/" + policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }

    private Item<ServiceMO> createService(JVMDatabaseBasedRestManagementEnvironment environment, String assXml) throws Exception {
        //create service
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMapping = ManagedObjectFactory.createHttpMapping();
        serviceMapping.setUrlPattern("/srcService");
        serviceMapping.setVerbs(Arrays.asList("POST"));
        ServiceDetail.SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
        soapMapping.setLax(false);
        serviceDetail.setServiceMappings(Arrays.asList(serviceMapping,soapMapping));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", true)
                .put("soapVersion", "1.2")
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml );
        ResourceSet wsdlResourceSet = ManagedObjectFactory.createResourceSet();
        wsdlResourceSet.setTag("wsdl");
        wsdlResourceSet.setRootUrl("http://localhost:8080/test.wsdl");
        Resource wsdlResource = ManagedObjectFactory.createResource();
        wsdlResourceSet.setResources(Arrays.asList(wsdlResource));
        wsdlResource.setType("wsdl");
        wsdlResource.setSourceUrl("http://localhost:8080/test.wsdl");
        wsdlResource.setContent("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>" );
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet,wsdlResourceSet));

        RestResponse response = environment.processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);
        return serviceItem;
    }
}
