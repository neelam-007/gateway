package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
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


/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyAliasMigrationTest extends MigrationTestBase {
    private static final Logger logger = Logger.getLogger(PolicyAliasMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<FolderMO> folderItem;
    private Item<PolicyAliasMO> policyAliasItem;
    private Item<Mappings> mappingsToClean;

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
        policyItem.setContent(policyMO);

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
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, folder, and policy alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a policy, and a policy alias", 4, bundleItem.getContent().getMappings().size());

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
    public void testImportExisting() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, folder, and policy alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a policy, and a policy alias", 4, bundleItem.getContent().getMappings().size());

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

        //import the bundle again
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        policyAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, policyAliasMapping.getActionTaken());
        Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
        Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportNewMapAliasToBeInFolderWithAliasForSamePolicy() throws Exception {

        Item<FolderMO> folderItem2 = null;
        Item<PolicyAliasMO> policyAliasItem2 = null;
        RestResponse response;
        try {
            //create folder item
            FolderMO parentFolderMO = ManagedObjectFactory.createFolder();
            parentFolderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
            parentFolderMO.setName("Source folder 2");
            response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(parentFolderMO)));

            assertOkCreatedResponse(response);

            folderItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            folderItem2.setContent(parentFolderMO);

            // create policy alias
            PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
            policyAliasMO.setFolderId(folderItem2.getId());
            policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyItem.getId()));
            response = getSourceEnvironment().processRequest("policyAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                    XmlUtil.nodeToString(ManagedObjectFactory.write(policyAliasMO)));
            assertOkCreatedResponse(response);
            policyAliasItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            policyAliasItem2.setContent(policyAliasMO);

            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 5 item. A policy, 2 folders, and 2 policy policy", 5, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 6 mappings. 3 folders, a policy, and 2 policy policy", 6, bundleItem.getContent().getMappings().size());

            //map the alias folder to the other alias folder containing the alias policy
            Mapping aliasFolderMapping = getMapping(bundleItem.getContent().getMappings(), folderItem.getId());
            aliasFolderMapping.setTargetId(folderItem2.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertConflictResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //verify the mappings
            Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping folderMapping = getMapping(mappings.getContent().getMappings(), folderItem.getId());
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            //either created new or used existing. it depends on the order that the mapping is exported, it could so wither way.
            Assert.assertTrue(Mapping.ActionTaken.UsedExisting.equals(folderMapping.getActionTaken()) || Mapping.ActionTaken.CreatedNew.equals(folderMapping.getActionTaken()));
            Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals(folderItem2.getId(), folderMapping.getTargetId());

            Mapping folder2Mapping = getMapping(mappings.getContent().getMappings(), folderItem2.getId());
            Assert.assertEquals(EntityType.FOLDER.toString(), folder2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folder2Mapping.getAction());
            //either created new or used existing. it depends on the order that the mapping is exported, it could so wither way.
            Assert.assertTrue(Mapping.ActionTaken.UsedExisting.equals(folder2Mapping.getActionTaken()) || Mapping.ActionTaken.CreatedNew.equals(folder2Mapping.getActionTaken()));
            Assert.assertEquals(folderItem2.getId(), folder2Mapping.getSrcId());
            Assert.assertEquals(folderItem2.getId(), folder2Mapping.getTargetId());

            Mapping policyMapping = getMapping(mappings.getContent().getMappings(), policyItem.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            Mapping policyAliasMapping = getMapping(mappings.getContent().getMappings(), policyAliasItem.getId());
            Mapping policyAlias2Mapping = getMapping(mappings.getContent().getMappings(), policyAliasItem2.getId());

            Assert.assertTrue("at least one of the policy aliases need to have errored because it was attempted to be created in a folder with the other (the second one should error)", policyAliasMapping.getErrorType() != null || policyAlias2Mapping.getErrorType() != null);

        } finally {
            mappingsToClean = null;

            if(policyAliasItem2 != null) {
                response = getSourceEnvironment().processRequest("policyAliases/" + policyAliasItem2.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
            }
            if(folderItem2 != null) {
                response = getSourceEnvironment().processRequest("folders/" + folderItem2.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
            }
        }
    }

    @Test
    public void testImportNewMapAliasToBeInFolderWithPolicy() throws Exception {
        mappingsToClean = null;

        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, folder, and policy alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a policy, and a policy alias", 4, bundleItem.getContent().getMappings().size());


        //map the alias folder to the root folder containing the alias policy
        Mapping aliasFolderMapping = getMapping(bundleItem.getContent().getMappings(), folderItem.getId());
        aliasFolderMapping.setTargetId(Folder.ROOT_FOLDER_ID.toString());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

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
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        Mapping policyAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.UniqueKeyConflict, policyAliasMapping.getErrorType());
        Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());

    }
}
