package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
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
import java.util.List;
import java.util.logging.Logger;


/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class FolderMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(FolderMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<PolicyAliasMO> policyAliasItem;
    private Item<FolderMO> sourceParentFolderItem;
    private Item<FolderMO> sourceChild1FolderItem;
    private Item<FolderMO> sourceChild2FolderItem;

    private Item<FolderMO> targetParentFolderItem;
    private Item<FolderMO> targetChild1FolderItem;
    private Item<FolderMO> targetChild2FolderItem;

    private Item<Mappings> mappingsToClean;

    /**
     * Source Gateway:
     * - root folder
     *     - parent folder
     *         - child 1 folder
     *             - policy
     *         - child 2 folder
     *             - policy alias
     *
     * Target Gateway:
     * - root folder
     *     - parent folder
     *         - child 1 folder
     *         - child 2 folder
     *
     */

    @Before
    public void before() throws Exception {
       //create parent folder
        FolderMO parentFolderMO = ManagedObjectFactory.createFolder();
        parentFolderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        parentFolderMO.setName("Source parent folder");
        RestResponse response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(parentFolderMO)));

        assertOkCreatedResponse(response);

        sourceParentFolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        sourceParentFolderItem.setContent(parentFolderMO);

        //create child 1 folder
        FolderMO child1FolderMO = ManagedObjectFactory.createFolder();
        child1FolderMO.setFolderId(sourceParentFolderItem.getId());
        child1FolderMO.setName("source child 1 folder");
        response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(child1FolderMO)));

        assertOkCreatedResponse(response);

        sourceChild1FolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        sourceChild1FolderItem.setContent(child1FolderMO);

        //create child 2 folder
        FolderMO child2FolderMO = ManagedObjectFactory.createFolder();
        child2FolderMO.setFolderId(sourceParentFolderItem.getId());
        child2FolderMO.setName("Source child 2 folder");
        response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(child2FolderMO)));

        assertOkCreatedResponse(response);

        sourceChild2FolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        sourceChild2FolderItem.setContent(child2FolderMO);

        //create policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Source Policy");
        policyDetail.setFolderId(sourceChild1FolderItem.getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);

        // create policy alias
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId(sourceChild2FolderItem.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class,policyItem.getId()));
        response = getSourceEnvironment().processRequest("policyAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyAliasMO)));
        assertOkCreatedResponse(response);
        policyAliasItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyAliasItem.setContent(policyAliasMO);

        // create target folders
        //create parent folder
        FolderMO targetParentFolderMO = ManagedObjectFactory.createFolder();
        targetParentFolderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        targetParentFolderMO.setName("Target parent folder");
        response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(targetParentFolderMO)));

        assertOkCreatedResponse(response);

        targetParentFolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetParentFolderItem.setContent(targetParentFolderMO);

        //create child 1 folder
        FolderMO targetChild1FolderMO = ManagedObjectFactory.createFolder();
        targetChild1FolderMO.setFolderId(targetParentFolderItem.getId());
        targetChild1FolderMO.setName("target child 1 folder");
        response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(targetChild1FolderMO)));

        assertOkCreatedResponse(response);

        targetChild1FolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetChild1FolderItem.setContent(targetChild1FolderMO);

        //create child 2 folder
        FolderMO targetChild2FolderMO = ManagedObjectFactory.createFolder();
        targetChild2FolderMO.setFolderId(targetParentFolderItem.getId());
        targetChild2FolderMO.setName("Target child 2 folder");
        response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(targetChild2FolderMO)));

        assertOkCreatedResponse(response);

        targetChild2FolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetChild2FolderItem.setContent(targetChild2FolderMO);
    }

    @After
    public void after() throws Exception {
        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response;

        response = getSourceEnvironment().processRequest("policyAliases/" + policyAliasItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("folders/" + sourceChild2FolderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("folders/" + sourceChild1FolderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("folders/" + sourceParentFolderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        // target

        assertOkDeleteResponse(response);

        response = getTargetEnvironment().processRequest("folders/" + targetChild2FolderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getTargetEnvironment().processRequest("folders/" + targetChild1FolderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getTargetEnvironment().processRequest("folders/" + targetParentFolderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);
    }

    @Test
    public void testImportNewPolicyInFolder() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + sourceChild1FolderItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(0).getType());

        Assert.assertEquals("The bundle should have 2 mappings. a policy, a folder", 2, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(1).getType());

        bundleItem.getContent().getMappings().get(0).setTargetId(targetChild1FolderItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping folderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(sourceChild1FolderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(targetChild1FolderItem.getId(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("folders/"+folderMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(1, folderDependencies.size());

        DependencyMO policyDependency = getDependency(folderDependencies,policyItem.getId());
        Assert.assertNotNull(policyDependency);
        Assert.assertNull(policyDependency.getDependencies());
        Assert.assertEquals(policyItem.getName(), policyDependency.getName());
        Assert.assertEquals(EntityType.POLICY.toString(), policyDependency.getType());

        validate(mappings);
    }

    @Test
    public void testImportNewPolicyWithFolder() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + sourceChild1FolderItem.getId() + "?includeRequestFolder=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 item. A policy, a folder", 2, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(1).getType());

        Assert.assertEquals("The bundle should have 3 mappings. a policy, 2 folder", 3, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());

        bundleItem.getContent().getMappings().get(0).setTargetId(targetChild1FolderItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping parentFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), parentFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, parentFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, parentFolderMapping.getActionTaken());
        Assert.assertEquals(sourceParentFolderItem.getId(), parentFolderMapping.getSrcId());
        Assert.assertEquals(targetChild1FolderItem.getId(), parentFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(sourceChild1FolderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals( folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("folders/"+parentFolderMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(2, folderDependencies.size());

        DependencyMO childFolderDependency = getDependency(folderDependencies,sourceChild1FolderItem.getId());
        Assert.assertNotNull(childFolderDependency);
        Assert.assertNotNull(childFolderDependency.getDependencies());

        DependencyMO policyDependency = getDependency(folderDependencies,policyItem.getId());
        Assert.assertNotNull(policyDependency);
        Assert.assertNull(policyDependency.getDependencies());
        Assert.assertEquals(policyItem.getName(), policyDependency.getName());
        Assert.assertEquals(EntityType.POLICY.toString(), policyDependency.getType());

        validate(mappings);
    }

    @Test
    public void testImportNewPolicyActivateWithComment() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + sourceChild1FolderItem.getId() + "?includeRequestFolder=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 item. A policy, a folder", 2, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(1).getType());

        Assert.assertEquals("The bundle should have 3 mappings. a policy, 2 folder", 3, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());

        bundleItem.getContent().getMappings().get(0).setTargetId(targetChild1FolderItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle?activate=true&versionComment=Comment", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());

        // verify that new policy is activated
        response = getTargetEnvironment().processRequest("policies/"+policyItem.getId() + "/versions", "", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<PolicyVersionMO> policyVersionList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(1,policyVersionList.getContent().size());
        PolicyVersionMO version = policyVersionList.getContent().get(0).getContent();
        Assert.assertEquals(true,version.isActive());
        Assert.assertEquals("Comment",version.getComment());
    }

    @Test
    public void testImportNewPolicyDefaultSettings() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + sourceChild1FolderItem.getId() + "?includeRequestFolder=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 item. A policy, a folder", 2, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(1).getType());

        Assert.assertEquals("The bundle should have 3 mappings. a policy, 2 folder", 3, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());

        bundleItem.getContent().getMappings().get(0).setTargetId(targetChild1FolderItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());

        // verify that new policy is activated
        response = getTargetEnvironment().processRequest("policies/"+policyItem.getId() + "/versions", "", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<PolicyVersionMO> policyVersionList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(1,policyVersionList.getContent().size());
        PolicyVersionMO version = policyVersionList.getContent().get(0).getContent();
        Assert.assertEquals(true,version.isActive());
    }

    @Test
    public void testImportNewFolderWith2Folders() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + sourceParentFolderItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 4 item. A policy, a policy alias, 2 folders", 4, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(1).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(2).getType());
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getReferences().get(3).getType());

        Assert.assertEquals("The bundle should have 5 mappings. a policy, a policy alias, 3 folders", 5, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(3).getType());
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getMappings().get(4).getType());

        bundleItem.getContent().getMappings().get(0).setTargetId(targetChild1FolderItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());
        Mapping parentFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), parentFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, parentFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, parentFolderMapping.getActionTaken());
        Assert.assertEquals(sourceParentFolderItem.getId(), parentFolderMapping.getSrcId());
        Assert.assertEquals(targetChild1FolderItem.getId(), parentFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(sourceChild1FolderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals( folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        Mapping folder2Mapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.FOLDER.toString(), folder2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folder2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folder2Mapping.getActionTaken());
        Assert.assertEquals(sourceChild2FolderItem.getId(), folder2Mapping.getSrcId());
        Assert.assertEquals( folder2Mapping.getSrcId(), folder2Mapping.getTargetId());

        Mapping policyAliasMapping = mappings.getContent().getMappings().get(4);
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyAliasMapping.getActionTaken());
        Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
        Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("folders/"+targetChild1FolderItem.getId() + "/dependencies?returnType=List", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(4, folderDependencies.size());

        validate(mappings);
    }

    @Test
    public void testImport2FoldersWithPolicy() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + sourceParentFolderItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 4 item. A policy, a policy alias, 2 folders", 4, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(1).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(2).getType());
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getReferences().get(3).getType());

        Assert.assertEquals("The bundle should have 5 mappings. a policy, a policy alias, 3 folders", 5, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(3).getType());
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getMappings().get(4).getType());

        bundleItem.getContent().getMappings().get(0).setTargetId(targetParentFolderItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());
        Mapping parentFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), parentFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, parentFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, parentFolderMapping.getActionTaken());
        Assert.assertEquals(sourceParentFolderItem.getId(), parentFolderMapping.getSrcId());
        Assert.assertEquals(targetParentFolderItem.getId(), parentFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(sourceChild1FolderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals( folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        Mapping folder2Mapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.FOLDER.toString(), folder2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folder2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folder2Mapping.getActionTaken());
        Assert.assertEquals(sourceChild2FolderItem.getId(), folder2Mapping.getSrcId());
        Assert.assertEquals( folder2Mapping.getSrcId(), folder2Mapping.getTargetId());

        Mapping policyAliasMapping = mappings.getContent().getMappings().get(4);
        Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyAliasMapping.getActionTaken());
        Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
        Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("folders/"+targetParentFolderItem.getId() + "/dependencies?returnType=List", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(6, folderDependencies.size());

        validate(mappings);
    }

    @Test
    public void testImport2FoldersWithPolicyToExistingFolder() throws Exception {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(targetParentFolderItem.getId());
        folderMO.setName("New Target Folder");
        folderMO.setId(sourceChild2FolderItem.getId());
        RestResponse response = getTargetEnvironment().processRequest("folders/"+sourceChild2FolderItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/folder/" + sourceParentFolderItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 4 item. A policy, a policy alias, 2 folders", 4, bundleItem.getContent().getReferences().size());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
            Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(1).getType());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(2).getType());
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getReferences().get(3).getType());

            Assert.assertEquals("The bundle should have 5 mappings. a policy, a policy alias, 3 folders", 5, bundleItem.getContent().getMappings().size());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
            Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(3).getType());
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getMappings().get(4).getType());

            bundleItem.getContent().getMappings().get(0).setTargetId(targetParentFolderItem.getId());
            bundleItem.getContent().getMappings().get(3).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());
            Mapping parentFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), parentFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, parentFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, parentFolderMapping.getActionTaken());
            Assert.assertEquals(sourceParentFolderItem.getId(), parentFolderMapping.getSrcId());
            Assert.assertEquals(targetParentFolderItem.getId(), parentFolderMapping.getTargetId());

            Mapping folderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
            Assert.assertEquals(sourceChild1FolderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals( folderMapping.getSrcId(), folderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            Mapping folder2Mapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.FOLDER.toString(), folder2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, folder2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, folder2Mapping.getActionTaken());
            Assert.assertEquals(sourceChild2FolderItem.getId(), folder2Mapping.getSrcId());
            Assert.assertEquals( folder2Mapping.getSrcId(), folder2Mapping.getTargetId());

            Mapping policyAliasMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyAliasMapping.getActionTaken());
            Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
            Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

            // verify dependencies

            response = getTargetEnvironment().processRequest("folders/"+targetParentFolderItem.getId() + "/dependencies?returnType=List", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

            Assert.assertNotNull(folderDependencies);
            Assert.assertEquals(6, folderDependencies.size());

            // verify folder updated
            response = getTargetEnvironment().processRequest("folders/"+sourceChild2FolderItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<FolderMO> folderUpdated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(sourceChild2FolderItem.getName(),folderUpdated.getName());

            validate(mappings);

        }finally{

            response = getTargetEnvironment().processRequest("policyAliases/"+policyAliasItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("folders/"+sourceChild1FolderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("folders/"+sourceChild2FolderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testImportMapPolicy() throws Exception {
        //create policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Policy");
        policyDetail.setFolderId(targetChild2FolderItem.getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");
        RestResponse response = getTargetEnvironment().processRequest("policies/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<FolderMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyMO.setId(policyCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/folder/" + sourceParentFolderItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 4 item. A policy, a policy alias, 2 folders", 4, bundleItem.getContent().getReferences().size());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
            Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getReferences().get(1).getType());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(2).getType());
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getReferences().get(3).getType());

            Assert.assertEquals("The bundle should have 5 mappings. a policy, a policy alias, 3 folders", 5, bundleItem.getContent().getMappings().size());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
            Assert.assertEquals(EntityType.POLICY.toString(), bundleItem.getContent().getMappings().get(2).getType());
            Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(3).getType());
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), bundleItem.getContent().getMappings().get(4).getType());

            bundleItem.getContent().getMappings().get(0).setTargetId(targetParentFolderItem.getId());
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(2).setTargetId(policyCreated.getId());
            bundleItem.getContent().getMappings().get(2).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());
            Mapping parentFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), parentFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, parentFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, parentFolderMapping.getActionTaken());
            Assert.assertEquals(sourceParentFolderItem.getId(), parentFolderMapping.getSrcId());
            Assert.assertEquals(targetParentFolderItem.getId(), parentFolderMapping.getTargetId());

            Mapping folderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
            Assert.assertEquals(sourceChild1FolderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals( folderMapping.getSrcId(), folderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyCreated.getId(), policyMapping.getTargetId());

            Mapping folder2Mapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.FOLDER.toString(), folder2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folder2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folder2Mapping.getActionTaken());
            Assert.assertEquals(sourceChild2FolderItem.getId(), folder2Mapping.getSrcId());
            Assert.assertEquals( folder2Mapping.getSrcId(), folder2Mapping.getTargetId());

            Mapping policyAliasMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.POLICY_ALIAS.toString(), policyAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyAliasMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyAliasMapping.getActionTaken());
            Assert.assertEquals(policyAliasItem.getId(), policyAliasMapping.getSrcId());
            Assert.assertEquals(policyAliasMapping.getSrcId(), policyAliasMapping.getTargetId());

            // verify dependencies

            response = getTargetEnvironment().processRequest("folders/"+targetParentFolderItem.getId() + "/dependencies?returnType=List", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

            Assert.assertNotNull(folderDependencies);
            Assert.assertEquals(6, folderDependencies.size());

            // verify folder updated
            response = getTargetEnvironment().processRequest("policyAliases/"+policyAliasItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<PolicyAliasMO> policyAliasUpdated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(policyCreated.getId(),policyAliasUpdated.getContent().getPolicyReference().getId());

            validate(mappings);

        }finally{

            response = getTargetEnvironment().processRequest("policyAliases/"+policyAliasItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+policyCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("folders/"+sourceChild1FolderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("folders/"+sourceChild2FolderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            mappingsToClean = null;
        }
    }
}
