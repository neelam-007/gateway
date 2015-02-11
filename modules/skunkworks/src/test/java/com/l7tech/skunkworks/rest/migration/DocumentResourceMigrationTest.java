package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DocumentResourceMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(DocumentResourceMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<SecurityZoneMO> securityZoneItem;
    private Item<ResourceDocumentMO> resourceDocItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {

        //create security zone
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("SourceZone");
        securityZoneMO.setDescription("Source Description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        RestResponse response = getSourceEnvironment().processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);

        securityZoneItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securityZoneItem.setContent(securityZoneMO);

        //create document resource
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("sourceBooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "source resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        resourceDocumentMO.setSecurityZoneId(securityZoneItem.getId());
        response = getSourceEnvironment().processRequest("resources", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);

        resourceDocItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocItem.setContent(resourceDocumentMO);

        //create policy
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:SchemaValidation>\n" +
                        "            <L7p:ResourceInfo globalResourceInfo=\"included\">\n" +
                        "                <L7p:Id stringValue=\"" + resourceDocItem.getContent().getResource().getSourceUrl() + "\"/>\n" +
                        "            </L7p:ResourceInfo>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:SchemaValidation>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
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
        resource.setContent(assXml);

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);
    }

    @After
    public void after() throws Exception {
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("resources/" + resourceDocItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
        Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
        Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

        Mapping docMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), docMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, docMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, docMapping.getActionTaken());
        Assert.assertEquals(resourceDocItem.getId(), docMapping.getSrcId());
        Assert.assertEquals(docMapping.getSrcId(), docMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testNoMappingExistingSameGoid() throws Exception {
        //create the resource on the target
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("targetbooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setId(resourceDocItem.getId());
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "target resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getTargetEnvironment().processRequest("resources/" + resourceDocItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
            Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
            Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

            Mapping docMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), docMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, docMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, docMapping.getActionTaken());
            Assert.assertEquals(resourceDocItem.getId(), docMapping.getSrcId());
            Assert.assertEquals(resourceDocumentMO.getId(), docMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO docDependency = getDependency(policyDependencies, resourceDocumentMO.getId());
            Assert.assertNotNull(docDependency);
            Assert.assertEquals(resourceDocumentMO.getResource().getSourceUrl(), docDependency.getName());
            Assert.assertEquals(resourceDocumentMO.getId(), docDependency.getId());
            Assert.assertNull(docDependency.getDependencies());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("resources/" + docCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingDifferentResource() throws Exception {

        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("targetbooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "target resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getTargetEnvironment().processRequest("resources", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the resource to the existing one
            bundleItem.getContent().getMappings().get(1).setTargetId(resourceDocumentMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
            Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
            Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

            Mapping docMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), docMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, docMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, docMapping.getActionTaken());
            Assert.assertEquals(resourceDocItem.getId(), docMapping.getSrcId());
            Assert.assertEquals(resourceDocumentMO.getId(), docMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO docDependency = getDependency(policyDependencies, resourceDocumentMO.getId());
            Assert.assertNotNull(docDependency);
            Assert.assertEquals(resourceDocumentMO.getResource().getSourceUrl(), docDependency.getName());
            Assert.assertEquals(resourceDocumentMO.getId(), docDependency.getId());
            Assert.assertNull(docDependency.getDependencies());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("resources/" + docCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testAlwaysCreateNewWithNameConflict() throws Exception {

        //create the resource on the target
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl(resourceDocItem.getContent().getResource().getSourceUrl());
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "target resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getTargetEnvironment().processRequest("resources", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the resource to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.AlwaysCreateNew);

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            logger.info(response.toString());

            // import fail
            assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
            assertEquals(409, response.getStatus());
            Item<Mappings> mappingsReturned = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertEquals(Mapping.ErrorType.UniqueKeyConflict, mappingsReturned.getContent().getMappings().get(1).getErrorType());
            assertTrue("Error message:", mappingsReturned.getContent().getMappings().get(1).<String>getProperty("ErrorMessage").contains("must be unique"));
        } finally {
            response = getTargetEnvironment().processRequest("resources/" + docCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testAlwaysCreateNew() throws Exception {

        //get the bundle
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the resource to the existing one
        bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.AlwaysCreateNew);
        bundleItem.getContent().getMappings().get(1).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnExisting", true).map());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
        Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
        Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

        Mapping docMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), docMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, docMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, docMapping.getActionTaken());
        Assert.assertEquals(resourceDocItem.getId(), docMapping.getSrcId());
        Assert.assertEquals(resourceDocItem.getId(), docMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

        logger.log(Level.INFO, policyXml);

        response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(2, policyDependencies.size());

        DependencyMO docDependency = getDependency(policyDependencies, resourceDocItem.getId());
        Assert.assertNotNull(docDependency);
        Assert.assertEquals(resourceDocItem.getName(), docDependency.getName());
        Assert.assertEquals(resourceDocItem.getId(), docDependency.getId());

        validate(mappings);
    }

    @Test
    public void testUpdateExistingSameGoid() throws Exception {
        //create the resource document on the target
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("targetbooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setId(resourceDocItem.getId());
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "target resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getTargetEnvironment().processRequest("resources/" + resourceDocItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the resource to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
            Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
            Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

            Mapping docMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), docMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, docMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, docMapping.getActionTaken());
            Assert.assertEquals(resourceDocItem.getId(), docMapping.getSrcId());
            Assert.assertEquals(resourceDocumentMO.getId(), docMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO docDependency = getDependency(policyDependencies, resourceDocItem.getId());
            Assert.assertNotNull(docDependency);
            Assert.assertEquals(resourceDocItem.getName(), docDependency.getName());
            Assert.assertEquals(resourceDocItem.getId(), docDependency.getId());
            Assert.assertEquals(1, docDependency.getDependencies().size());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("resources/" + docCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToUpdateExisting() throws Exception {
        //create the resource document on the target
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("targetbooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "target resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getTargetEnvironment().processRequest("resources", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the resource to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).setTargetId(resourceDocumentMO.getId());
            bundleItem.getContent().getMappings().get(1).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, securityZoneMapping.getActionTaken());
            Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
            Assert.assertEquals(securityZoneMapping.getSrcId(), securityZoneMapping.getTargetId());

            Mapping docMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), docMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, docMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, docMapping.getActionTaken());
            Assert.assertEquals(resourceDocItem.getId(), docMapping.getSrcId());
            Assert.assertEquals(resourceDocumentMO.getId(), docMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO docDependency = getDependency(policyDependencies, resourceDocumentMO.getId());
            Assert.assertNotNull(docDependency);
            Assert.assertEquals(resourceDocItem.getName(), docDependency.getName());
            Assert.assertEquals(resourceDocumentMO.getId(), docDependency.getId());
            Assert.assertEquals(1, docDependency.getDependencies().size());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("resources/" + docCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapByNameFails() throws Exception {
        //create the resource document on the target
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("targetbooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "target resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getTargetEnvironment().processRequest("resources", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the resource to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(1).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).put("MapBy", "name").put("MapTo", resourceDocumentMO.getResource().getSourceUrl()).map());
            bundleItem.getContent().getMappings().get(1).setTargetId(resourceDocumentMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));

            // import fail
            assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
            assertEquals(409, response.getStatus());
            Item<Mappings> mappingsReturned = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertEquals(Mapping.ErrorType.TargetNotFound, mappingsReturned.getContent().getMappings().get(1).getErrorType());
            assertTrue("Error message:", mappingsReturned.getContent().getMappings().get(1).<String>getProperty("ErrorMessage").contains("Could not locate entity"));
        } finally {
            response = getTargetEnvironment().processRequest("resources/" + docCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testCreateNewMapSecurityZoneTest() throws Exception {
        //create the security zone on the target
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("TargetZone");
        securityZoneMO.setDescription("Target Description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        RestResponse response = getTargetEnvironment().processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);
        Item<SecurityZoneMO> securityZoneCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securityZoneMO.setId(securityZoneCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, resource document and security zone", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the security zone to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
            bundleItem.getContent().getMappings().get(0).setTargetId(securityZoneMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping securityZoneMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, securityZoneMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, securityZoneMapping.getActionTaken());
            Assert.assertEquals(securityZoneItem.getId(), securityZoneMapping.getSrcId());
            Assert.assertEquals(securityZoneMO.getId(), securityZoneMapping.getTargetId());

            Mapping docMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), docMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, docMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, docMapping.getActionTaken());
            Assert.assertEquals(resourceDocItem.getId(), docMapping.getSrcId());
            Assert.assertEquals(docMapping.getSrcId(), docMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO docDependency = getDependency(policyDependencies, resourceDocItem.getId());
            Assert.assertNotNull(docDependency);

            DependencyMO zoneDependency = getDependency(policyDependencies, securityZoneMO.getId());
            Assert.assertEquals(securityZoneMO.getName(), zoneDependency.getName());
            Assert.assertEquals(securityZoneMO.getId(), zoneDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("securityZones/" + securityZoneCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        //create the resource document on the target
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("targetbooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "target resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getTargetEnvironment().processRequest("resources", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());
        docCreated.setContent(resourceDocumentMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(resourceDocumentMO.getId());
        mapping.setSrcId(Goid.DEFAULT_GOID.toString());
        mapping.setType(docCreated.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(docCreated.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Arrays.<Item>asList(docCreated));

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
        Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(docCreated.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("resources/"+docCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }

    @Test
    public void includedInFullBundleExportTest() throws Exception {
        //create the resource document on the target
        ResourceDocumentMO resourceDocumentMO = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("sourcebooks2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        resourceDocumentMO.setResource(createResource);
        resourceDocumentMO.setProperties(new HashMap<String, Object>());
        resourceDocumentMO.getProperties().put("description", "source resource");
        resourceDocumentMO.getProperties().put("publicIdentifier", "books2");
        RestResponse response = getSourceEnvironment().processRequest("resources", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(resourceDocumentMO)));

        assertOkCreatedResponse(response);
        Item<ResourceDocumentMO> docCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        resourceDocumentMO.setId(docCreated.getId());
        docCreated.setContent(resourceDocumentMO);

        try {
            response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);
            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Mapping resourceDocumentMapping = getMapping(bundleItem.getContent().getMappings(), docCreated.getId());
            Assert.assertNotNull(resourceDocumentMapping);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            resourceDocumentMapping = getMapping(mappings.getContent().getMappings(), docCreated.getId());
            Assert.assertNotNull(resourceDocumentMapping);
            Assert.assertEquals(EntityType.RESOURCE_ENTRY.toString(), resourceDocumentMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, resourceDocumentMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, resourceDocumentMapping.getActionTaken());
            Assert.assertEquals(docCreated.getId(), resourceDocumentMapping.getSrcId());
            Assert.assertEquals(resourceDocumentMapping.getSrcId(), resourceDocumentMapping.getTargetId());

            validate(mappings);
        } finally {
            response = getSourceEnvironment().processRequest("resources/" + docCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }
}
