package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
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

import static org.junit.Assert.assertEquals;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GenericEntityMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(GenericEntityMigration.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<PolicyMO> policyItem2;
    private Item<GenericEntityMO> genericEntityItem;
    private Item<GenericEntityMO> genericEntityItem2;

    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        //create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Source Generic Entity 1");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Source Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        RestResponse response = getSourceEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));

        assertOkCreatedResponse(response);

        genericEntityItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        genericEntityItem.setContent(genericEntityMO);

        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Source Generic Entity 2");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Source Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"demoGenericEntityId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + genericEntityItem.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getSourceEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));

        assertOkCreatedResponse(response);

        genericEntityItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        genericEntityItem2.setContent(genericEntityMO);


        //create policy;
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
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:GenericEntityManagerDemo>\n" +
                "            <L7p:GenericEntityId goidValue=\"" + genericEntityItem.getId() + "\"/>\n" +
                "            <L7p:GenericEntityClass stringValue=\"" + genericEntityMO.getEntityClassName() + "\"/>\n" +
                "        </L7p:GenericEntityManagerDemo>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);

        policyMO = ManagedObjectFactory.createPolicy();
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicy2");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:GenericEntityManagerDemo>\n" +
                "            <L7p:GenericEntityId goidValue=\"" + genericEntityItem2.getId() + "\"/>\n" +
                "            <L7p:GenericEntityClass stringValue=\"" + genericEntityMO.getEntityClassName() + "\"/>\n" +
                "        </L7p:GenericEntityManagerDemo>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem2.setContent(policyMO);
    }

    @After
    public void after() throws Exception {
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem2.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem2.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?genericEntity=" + genericEntityItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A genericEntityItem", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items. A genericEntityItem", 1, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy, and generic entity", 2, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping entityMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entityMapping.getActionTaken());
        Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
        Assert.assertEquals(entityMapping.getSrcId(), entityMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // verify dependencies
        response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO entityDependency = getDependency(policyDependencies, genericEntityItem.getId());
        Assert.assertNotNull(entityDependency);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
        Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
        Assert.assertEquals(genericEntityItem.getId(), entityDependency.getId());

        validate(mappings);
    }

    @Test
    public void testMapToExisting() throws Exception {
        RestResponse response;
        // create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {

            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy, and generic entity", 2, bundleItem.getContent().getReferences().size());

            // update mapping
            bundleItem.getContent().getMappings().get(0).setTargetId(createdGenericEntity.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(createdGenericEntity.getId(), entityMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, createdGenericEntity.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(createdGenericEntity.getName(), entityDependency.getName());
            Assert.assertEquals(createdGenericEntity.getId(), entityDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapByName() throws Exception {
        RestResponse response;
        // create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {

            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy, and generic entity", 2, bundleItem.getContent().getReferences().size());

            // update mapping
            bundleItem.getContent().getMappings().get(0).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder()
                            .put("MapBy", "name")
                            .put("MapTo", createdGenericEntity.getName())
                            .map());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(createdGenericEntity.getId(), entityMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, createdGenericEntity.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(createdGenericEntity.getName(), entityDependency.getName());
            Assert.assertEquals(createdGenericEntity.getId(), entityDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testUpdateExisting() throws Exception {
        RestResponse response;
        // create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<xml>target value</xml>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {

            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy, and generic entity", 2, bundleItem.getContent().getReferences().size());

            // update mapping
            bundleItem.getContent().getMappings().get(0).setTargetId(createdGenericEntity.getId());
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(createdGenericEntity.getId(), entityMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, createdGenericEntity.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
            Assert.assertEquals(createdGenericEntity.getId(), entityDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void genericEntityItemServiceDependencyTest() throws Exception {
        //create a service
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMappingHttp = ManagedObjectFactory.createHttpMapping();
        serviceMappingHttp.setUrlPattern("/srcService");
        serviceMappingHttp.setVerbs(Arrays.asList("POST"));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);

        try {
            //update generic entity
            genericEntityItem.setName("Service Dependency");
            genericEntityItem.getContent().setName("Service Dependency");
            genericEntityItem.getContent().setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"id\"> <string>44b76dc332468759d5707768b58f65ea</string> </void> <void property=\"name\"> <string>Service Dependency</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"serviceId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + serviceItem.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
            response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityItem.getContent())));

            assertOkResponse(response);

            //migrate
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, and generic entity, and a service", 3, bundleItem.getContent().getReferences().size());

            //export the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping entityMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(entityMapping.getSrcId(), entityMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, genericEntityItem.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
            Assert.assertEquals(genericEntityItem.getId(), entityDependency.getId());
            Assert.assertEquals(1, entityDependency.getDependencies().size());
            Assert.assertEquals(EntityType.SERVICE.toString(), entityDependency.getDependencies().get(0).getType());
            Assert.assertEquals(serviceItem.getId(), entityDependency.getDependencies().get(0).getId());

            DependencyMO serviceDependency = getDependency(policyDependencies, serviceItem.getId());
            Assert.assertNotNull(serviceDependency);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceDependency.getType());
            Assert.assertEquals(serviceItem.getName(), serviceDependency.getName());
            Assert.assertEquals(serviceItem.getId(), serviceDependency.getId());

            validate(mappings);

        } finally {
            response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void genericEntityItemServiceDependencyTestMapService() throws Exception {
        //create a service
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMappingHttp = ManagedObjectFactory.createHttpMapping();
        serviceMappingHttp.setUrlPattern("/srcService");
        serviceMappingHttp.setVerbs(Arrays.asList("POST"));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);

        serviceDetail.setName("targetService");
        response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> targetServiceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetServiceItem.setContent(serviceMO);

        try {
            //update generic entity
            genericEntityItem.setName("Service Dependency");
            genericEntityItem.getContent().setName("Service Dependency");
            genericEntityItem.getContent().setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"id\"> <string>44b76dc332468759d5707768b58f65ea</string> </void> <void property=\"name\"> <string>Service Dependency</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"serviceId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + serviceItem.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
            response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityItem.getContent())));

            assertOkResponse(response);

            //migrate
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //update the mappings
            bundleItem.getContent().getMappings().get(1).setTargetId(targetServiceItem.getId());

            Assert.assertEquals("The bundle should have 3 items. A policy, and generic entity, and a service", 3, bundleItem.getContent().getReferences().size());

            //export the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(targetServiceItem.getId(), serviceMapping.getTargetId());

            Mapping entityMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(entityMapping.getSrcId(), entityMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, genericEntityItem.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
            Assert.assertEquals(genericEntityItem.getId(), entityDependency.getId());
            Assert.assertEquals(1, entityDependency.getDependencies().size());
            Assert.assertEquals(EntityType.SERVICE.toString(), entityDependency.getDependencies().get(0).getType());
            Assert.assertEquals(targetServiceItem.getId(), entityDependency.getDependencies().get(0).getId());

            DependencyMO serviceDependency = getDependency(policyDependencies, targetServiceItem.getId());
            Assert.assertNotNull(serviceDependency);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceDependency.getType());
            Assert.assertEquals(targetServiceItem.getName(), serviceDependency.getName());
            Assert.assertEquals(targetServiceItem.getId(), serviceDependency.getId());

            validate(mappings);

        } finally {
            response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void genericEntityItemSelfServiceDependencyTest() throws Exception {
        //create a service
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMappingHttp = ManagedObjectFactory.createHttpMapping();
        serviceMappingHttp.setUrlPattern("/srcService");
        serviceMappingHttp.setVerbs(Arrays.asList("POST"));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);

        try {
            //update generic entity
            genericEntityItem.setName("Service Dependency");
            genericEntityItem.getContent().setName("Service Dependency");
            genericEntityItem.getContent().setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"id\"> <string>44b76dc332468759d5707768b58f65ea</string> </void> <void property=\"name\"> <string>Service Dependency</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"serviceId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + serviceItem.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
            response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityItem.getContent())));

            assertOkResponse(response);

            //update service to contain generic entity reference
            policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <wsp:All wsp:Usage=\"Required\">\n" +
                    "        <L7p:GenericEntityManagerDemo>\n" +
                    "            <L7p:GenericEntityId goidValue=\"" + genericEntityItem.getId() + "\"/>\n" +
                    "            <L7p:GenericEntityClass stringValue=\"" + genericEntityItem.getContent().getEntityClassName() + "\"/>\n" +
                    "        </L7p:GenericEntityManagerDemo>\n" +
                    "    </wsp:All>\n" +
                    "</wsp:Policy>");

            response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

            assertOkResponse(response);

            //migrate
            response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A service, and generic entity", 2, bundleItem.getContent().getReferences().size());

            //export the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(entityMapping.getSrcId(), entityMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("services/" + serviceMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> serviceCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> serviceDependencies = serviceCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(serviceDependencies);
            Assert.assertEquals(2, serviceDependencies.size());

            DependencyMO entityDependency = getDependency(serviceDependencies, genericEntityItem.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
            Assert.assertEquals(genericEntityItem.getId(), entityDependency.getId());
            Assert.assertEquals(1, entityDependency.getDependencies().size());
            Assert.assertEquals(EntityType.SERVICE.toString(), entityDependency.getDependencies().get(0).getType());
            Assert.assertEquals(serviceItem.getId(), entityDependency.getDependencies().get(0).getId());

            DependencyMO serviceDependency = getDependency(serviceDependencies, serviceItem.getId());
            Assert.assertNotNull(serviceDependency);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceDependency.getType());
            Assert.assertEquals(serviceItem.getName(), serviceDependency.getName());
            Assert.assertEquals(serviceItem.getId(), serviceDependency.getId());
            Assert.assertEquals(1, serviceDependency.getDependencies().size());
            Assert.assertEquals(EntityType.GENERIC.toString(), serviceDependency.getDependencies().get(0).getType());
            Assert.assertEquals(genericEntityItem.getId(), serviceDependency.getDependencies().get(0).getId());

            validate(mappings);

        } finally {
            response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void genericEntityItemSelfServiceDependencyTestMapAndUpdateService() throws Exception {
        //create a service
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMappingHttp = ManagedObjectFactory.createHttpMapping();
        serviceMappingHttp.setUrlPattern("/srcService");
        serviceMappingHttp.setVerbs(Arrays.asList("POST"));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);

        serviceDetail.setName("targetService");
        response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> targetServiceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetServiceItem.setContent(serviceMO);

        try {
            //update generic entity
            genericEntityItem.setName("Service Dependency");
            genericEntityItem.getContent().setName("Service Dependency");
            genericEntityItem.getContent().setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"id\"> <string>44b76dc332468759d5707768b58f65ea</string> </void> <void property=\"name\"> <string>Service Dependency</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"serviceId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + serviceItem.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
            response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityItem.getContent())));

            assertOkResponse(response);

            //update service to contain generic entity reference
            policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <wsp:All wsp:Usage=\"Required\">\n" +
                    "        <L7p:GenericEntityManagerDemo>\n" +
                    "            <L7p:GenericEntityId goidValue=\"" + genericEntityItem.getId() + "\"/>\n" +
                    "            <L7p:GenericEntityClass stringValue=\"" + genericEntityItem.getContent().getEntityClassName() + "\"/>\n" +
                    "        </L7p:GenericEntityManagerDemo>\n" +
                    "    </wsp:All>\n" +
                    "</wsp:Policy>");

            response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

            assertOkResponse(response);

            //migrate
            response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A service, and generic entity", 2, bundleItem.getContent().getReferences().size());

            //update the mappings
            bundleItem.getContent().getMappings().get(2).setTargetId(targetServiceItem.getId());
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.NewOrUpdate);

            //export the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping serviceMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(targetServiceItem.getId(), serviceMapping.getTargetId());

            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(entityMapping.getSrcId(), entityMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("services/" + serviceMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> serviceCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> serviceDependencies = serviceCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(serviceDependencies);
            Assert.assertEquals(2, serviceDependencies.size());

            DependencyMO entityDependency = getDependency(serviceDependencies, genericEntityItem.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
            Assert.assertEquals(genericEntityItem.getId(), entityDependency.getId());
            Assert.assertEquals(1, entityDependency.getDependencies().size());
            Assert.assertEquals(EntityType.SERVICE.toString(), entityDependency.getDependencies().get(0).getType());
            Assert.assertEquals(targetServiceItem.getId(), entityDependency.getDependencies().get(0).getId());

            DependencyMO serviceDependency = getDependency(serviceDependencies, targetServiceItem.getId());
            Assert.assertNotNull(serviceDependency);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceDependency.getType());
            Assert.assertEquals(targetServiceItem.getName(), serviceDependency.getName());
            Assert.assertEquals(targetServiceItem.getId(), serviceDependency.getId());
            Assert.assertEquals(1, serviceDependency.getDependencies().size());
            Assert.assertEquals(EntityType.GENERIC.toString(), serviceDependency.getDependencies().get(0).getType());
            Assert.assertEquals(genericEntityItem.getId(), serviceDependency.getDependencies().get(0).getId());

            validate(mappings);

        } finally {
            response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void genericEntityItemGenericEntityDependencyTest() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem2.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, and 2 generic entity", 3, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping entityMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entityMapping.getActionTaken());
        Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
        Assert.assertEquals(entityMapping.getSrcId(), entityMapping.getTargetId());

        Mapping entity2Mapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.GENERIC.toString(), entity2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, entity2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entity2Mapping.getActionTaken());
        Assert.assertEquals(genericEntityItem2.getId(), entity2Mapping.getSrcId());
        Assert.assertEquals(entity2Mapping.getSrcId(), entity2Mapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem2.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // verify dependencies
        response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(2, policyDependencies.size());

        DependencyMO entityDependency = getDependency(policyDependencies, genericEntityItem.getId());
        Assert.assertNotNull(entityDependency);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
        Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
        Assert.assertEquals(genericEntityItem.getId(), entityDependency.getId());
        Assert.assertNull(entityDependency.getDependencies());

        DependencyMO entityDependency2 = getDependency(policyDependencies, genericEntityItem2.getId());
        Assert.assertNotNull(entityDependency2);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getType());
        Assert.assertEquals(genericEntityItem2.getName(), entityDependency2.getName());
        Assert.assertEquals(genericEntityItem2.getId(), entityDependency2.getId());
        Assert.assertNotNull(entityDependency2.getDependencies());
        Assert.assertEquals(1, entityDependency2.getDependencies().size());
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getDependencies().get(0).getType());
        Assert.assertEquals(genericEntityItem.getId(), entityDependency2.getDependencies().get(0).getId());

        validate(mappings);
    }

    @Test
    public void genericEntityItemGenericEntityDependencyTestMap() throws Exception {
        RestResponse response;
        // create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem2.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, and 2 generic entity", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).setTargetId(createdGenericEntity.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(createdGenericEntity.getId(), entityMapping.getTargetId());

            Mapping entity2Mapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.GENERIC.toString(), entity2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, entity2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entity2Mapping.getActionTaken());
            Assert.assertEquals(genericEntityItem2.getId(), entity2Mapping.getSrcId());
            Assert.assertEquals(entity2Mapping.getSrcId(), entity2Mapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem2.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, createdGenericEntity.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(createdGenericEntity.getName(), entityDependency.getName());
            Assert.assertEquals(createdGenericEntity.getId(), entityDependency.getId());
            Assert.assertNull(entityDependency.getDependencies());

            DependencyMO entityDependency2 = getDependency(policyDependencies, genericEntityItem2.getId());
            Assert.assertNotNull(entityDependency2);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getType());
            Assert.assertEquals(genericEntityItem2.getName(), entityDependency2.getName());
            Assert.assertEquals(genericEntityItem2.getId(), entityDependency2.getId());
            Assert.assertNotNull(entityDependency2.getDependencies());
            Assert.assertEquals(1, entityDependency2.getDependencies().size());
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getDependencies().get(0).getType());
            Assert.assertEquals(createdGenericEntity.getId(), entityDependency2.getDependencies().get(0).getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void genericEntityItemGenericEntityDependencyTestCycle() throws Exception {
        //update the generic entity to refer to the other one so that a cylce is made
        genericEntityItem.getContent().setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Source Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"demoGenericEntityId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + genericEntityItem2.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        RestResponse response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityItem.getContent())));

        assertOkResponse(response);

        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem2.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, and 2 generic entity", 3, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping entityMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, entityMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entityMapping.getActionTaken());
        Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
        Assert.assertEquals(entityMapping.getSrcId(), entityMapping.getTargetId());

        Mapping entity2Mapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.GENERIC.toString(), entity2Mapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, entity2Mapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, entity2Mapping.getActionTaken());
        Assert.assertEquals(genericEntityItem2.getId(), entity2Mapping.getSrcId());
        Assert.assertEquals(entity2Mapping.getSrcId(), entity2Mapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem2.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // verify dependencies
        response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(2, policyDependencies.size());

        DependencyMO entityDependency = getDependency(policyDependencies, genericEntityItem.getId());
        Assert.assertNotNull(entityDependency);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
        Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
        Assert.assertEquals(genericEntityItem.getId(), entityDependency.getId());
        Assert.assertNotNull(entityDependency.getDependencies());
        Assert.assertEquals(1, entityDependency.getDependencies().size());
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getDependencies().get(0).getType());
        Assert.assertEquals(genericEntityItem2.getId(), entityDependency.getDependencies().get(0).getId());

        DependencyMO entityDependency2 = getDependency(policyDependencies, genericEntityItem2.getId());
        Assert.assertNotNull(entityDependency2);
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getType());
        Assert.assertEquals(genericEntityItem2.getName(), entityDependency2.getName());
        Assert.assertEquals(genericEntityItem2.getId(), entityDependency2.getId());
        Assert.assertNotNull(entityDependency2.getDependencies());
        Assert.assertEquals(1, entityDependency2.getDependencies().size());
        Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getDependencies().get(0).getType());
        Assert.assertEquals(genericEntityItem.getId(), entityDependency2.getDependencies().get(0).getId());

        validate(mappings);
    }

    @Test
    public void genericEntityItemGenericEntityDependencyTestCycleMapAndUpdate() throws Exception {
        RestResponse response;
        // create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity 1");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity 2");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {
            //update the generic entity to refer to the other one so that a cylce is made
            genericEntityItem.getContent().setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Source Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"demoGenericEntityId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + genericEntityItem2.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
            response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityItem.getContent())));

            assertOkResponse(response);

            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem2.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, and 2 generic entity", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).setTargetId(createdGenericEntity1.getId());
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).setTargetId(createdGenericEntity2.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(createdGenericEntity1.getId(), entityMapping.getTargetId());

            Mapping entity2Mapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.GENERIC.toString(), entity2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, entity2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, entity2Mapping.getActionTaken());
            Assert.assertEquals(genericEntityItem2.getId(), entity2Mapping.getSrcId());
            Assert.assertEquals(createdGenericEntity2.getId(), entity2Mapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem2.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, createdGenericEntity1.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(genericEntityItem.getName(), entityDependency.getName());
            Assert.assertEquals(createdGenericEntity1.getId(), entityDependency.getId());
            Assert.assertNotNull(entityDependency.getDependencies());
            Assert.assertEquals(1, entityDependency.getDependencies().size());
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getDependencies().get(0).getType());
            Assert.assertEquals(createdGenericEntity2.getId(), entityDependency.getDependencies().get(0).getId());

            DependencyMO entityDependency2 = getDependency(policyDependencies, createdGenericEntity2.getId());
            Assert.assertNotNull(entityDependency2);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getType());
            Assert.assertEquals(genericEntityItem2.getName(), entityDependency2.getName());
            Assert.assertEquals(createdGenericEntity2.getId(), entityDependency2.getId());
            Assert.assertNotNull(entityDependency2.getDependencies());
            Assert.assertEquals(1, entityDependency2.getDependencies().size());
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getDependencies().get(0).getType());
            Assert.assertEquals(createdGenericEntity1.getId(), entityDependency2.getDependencies().get(0).getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity2.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void genericEntityItemGenericEntityDependencyTestCycleMapByNameAndUpdate() throws Exception {
        RestResponse response;
        // create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity 1");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity 2");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {
            //update the generic entity to refer to the other one so that a cylce is made
            genericEntityItem.getContent().setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Source Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"demoGenericEntityId\"> <object class=\"com.l7tech.objectmodel.Goid\"> <string>" + genericEntityItem2.getId() + "</string> </object> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
            response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityItem.getContent())));

            assertOkResponse(response);

            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem2.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, and 2 generic entity", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).addProperty("MapBy", "name");
            bundleItem.getContent().getMappings().get(0).addProperty("MapTo", createdGenericEntity1.getName());
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).addProperty("MapBy", "name");
            bundleItem.getContent().getMappings().get(1).addProperty("MapTo", createdGenericEntity2.getName());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping entityMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, entityMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, entityMapping.getActionTaken());
            Assert.assertEquals(genericEntityItem.getId(), entityMapping.getSrcId());
            Assert.assertEquals(createdGenericEntity1.getId(), entityMapping.getTargetId());

            Mapping entity2Mapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.GENERIC.toString(), entity2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, entity2Mapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, entity2Mapping.getActionTaken());
            Assert.assertEquals(genericEntityItem2.getId(), entity2Mapping.getSrcId());
            Assert.assertEquals(createdGenericEntity2.getId(), entity2Mapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem2.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO entityDependency = getDependency(policyDependencies, createdGenericEntity1.getId());
            Assert.assertNotNull(entityDependency);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getType());
            Assert.assertEquals(createdGenericEntity1.getName(), entityDependency.getName());
            Assert.assertEquals(createdGenericEntity1.getId(), entityDependency.getId());
            Assert.assertNotNull(entityDependency.getDependencies());
            Assert.assertEquals(1, entityDependency.getDependencies().size());
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency.getDependencies().get(0).getType());
            Assert.assertEquals(createdGenericEntity2.getId(), entityDependency.getDependencies().get(0).getId());

            DependencyMO entityDependency2 = getDependency(policyDependencies, createdGenericEntity2.getId());
            Assert.assertNotNull(entityDependency2);
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getType());
            Assert.assertEquals(createdGenericEntity2.getName(), entityDependency2.getName());
            Assert.assertEquals(createdGenericEntity2.getId(), entityDependency2.getId());
            Assert.assertNotNull(entityDependency2.getDependencies());
            Assert.assertEquals(1, entityDependency2.getDependencies().size());
            Assert.assertEquals(EntityType.GENERIC.toString(), entityDependency2.getDependencies().get(0).getType());
            Assert.assertEquals(createdGenericEntity1.getId(), entityDependency2.getDependencies().get(0).getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("genericEntities/" + createdGenericEntity2.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        RestResponse response;
        // create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Target Entity");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <java version=\"1.7.0_60\" class=\"java.beans.XMLDecoder\"> <object class=\"com.l7tech.external.assertions.whichmodule.DemoGenericEntity\"> <void property=\"age\"> <int>24</int> </void> <void property=\"name\"> <string>Target Entity</string> </void> <void property=\"playsTrombone\"> <boolean>true</boolean> </void> <void property=\"valueXml\"> <string></string> </void> </object> </java>");
        genericEntityMO.setEnabled(true);
        response = getTargetEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));
        assertOkCreatedResponse(response);
        Item<GenericEntityMO> createdGenericEntity = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdGenericEntity.setContent(genericEntityMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(createdGenericEntity.getId());
        mapping.setSrcId(Goid.DEFAULT_GOID.toString());
        mapping.setType(createdGenericEntity.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(createdGenericEntity.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Arrays.<Item>asList(createdGenericEntity));

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
        Assert.assertEquals(EntityType.GENERIC.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(createdGenericEntity.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.GENERIC.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("genericEntities/"+createdGenericEntity.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }
}
