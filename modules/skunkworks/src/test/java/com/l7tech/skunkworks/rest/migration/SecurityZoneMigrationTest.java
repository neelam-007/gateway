package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class SecurityZoneMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(SecurityZoneMigrationTest.class.getName());
    private Item<SecurityZoneMO> securityZoneItem;

    private Item<Mappings> mappingsToClean;
    private Item<FolderMO> folderItem;

    @Before
    public void before() throws Exception {
        //create securityZone;
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("MySecurityZone");
        securityZoneMO.setDescription("MySecurityZone description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        RestResponse response = getSourceEnvironment().processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);

        securityZoneItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securityZoneItem.setContent(securityZoneMO);

        //create a folder with a security zone
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("MyFolder");
        folderMO.setSecurityZone(securityZoneItem.getContent().getName());
        folderMO.setSecurityZoneId(securityZoneItem.getId());
        response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));

        assertOkCreatedResponse(response);
        folderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderItem.setContent(folderMO);
    }

    @After
    public void after() throws Exception {
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testFolderNewSecurityZone() throws Exception {
        //get the bundle
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + folderItem.getId(), "includeRequestFolder=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A security zone and folder", 2, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
        Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
        Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testFolderMapSecurityZone() throws Exception {
        //create securityZone;
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("MySecurityZoneTarget");
        securityZoneMO.setDescription("MySecurityZone description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        RestResponse response = getTargetEnvironment().processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);

        Item securityZoneItemTarget = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/folder/" + folderItem.getId(), "includeRequestFolder=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //map the securityZone to the one created above.
            bundleItem.getContent().getMappings().get(0).setTargetId(securityZoneItemTarget.getId());

            Assert.assertEquals("The bundle should have 2 items. A security zone and folder", 2, bundleItem.getContent().getReferences().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, securityZoneMapping.getActionTaken());
            Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
            Assert.assertEquals(securityZoneItemTarget.getId(), securityZoneMapping.getTargetId());

            Mapping folderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
            Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("folders/" + folderMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> folderCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> folderDependencies = folderCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(folderDependencies);
            Assert.assertEquals(1, folderDependencies.size());

            DependencyMO securityZoneDependency = getDependency(folderDependencies,securityZoneItemTarget.getId());
            Assert.assertNotNull(securityZoneDependency);
            Assert.assertEquals(securityZoneMO.getName(), securityZoneDependency.getName());
            Assert.assertEquals(securityZoneItemTarget.getId(), securityZoneDependency.getId());
        }finally{
            response = getTargetEnvironment().processRequest("securityZones/" + securityZoneItemTarget.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testPolicyMapSecurityZone() throws Exception {
        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setSecurityZoneId(securityZoneItem.getId());
        policyMO.setSecurityZone(securityZoneItem.getName());
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
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
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        Item<PolicyMO> policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);

        //create securityZone;
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("MySecurityZoneTarget");
        securityZoneMO.setDescription("MySecurityZone description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        response = getTargetEnvironment().processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);

        Item securityZoneItemTarget = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //map the securityZone to the one created above.
            bundleItem.getContent().getMappings().get(0).setTargetId(securityZoneItemTarget.getId());

            Assert.assertEquals("The bundle should have 2 items. A security zone and folder", 2, bundleItem.getContent().getReferences().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, securityZoneMapping.getActionTaken());
            Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
            Assert.assertEquals(securityZoneItemTarget.getId(), securityZoneMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO securityZoneDependency = getDependency(policyDependencies,securityZoneItemTarget.getId());
            Assert.assertNotNull(securityZoneDependency);
            Assert.assertEquals(securityZoneMO.getName(), securityZoneDependency.getName());
            Assert.assertEquals(securityZoneItemTarget.getId(), securityZoneDependency.getId());

        } finally {
            response = getTargetEnvironment().processRequest("securityZones/" + securityZoneItemTarget.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

        }
    }
}
