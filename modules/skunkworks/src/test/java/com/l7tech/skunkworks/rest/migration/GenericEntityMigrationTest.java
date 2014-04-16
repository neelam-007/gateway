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


/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GenericEntityMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(GenericEntityMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<GenericEntityMO> genericEntityItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        //create generic entity
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Source Entity");
        genericEntityMO.setEntityClassName("com.l7tech.external.assertions.whichmodule.DemoGenericEntity");
        genericEntityMO.setValueXml("<xml>source value</xml>");
        genericEntityMO.setEnabled(true);
        RestResponse response = getSourceEnvironment().processRequest("genericEntities", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(genericEntityMO)));

        assertOkCreatedResponse(response);

        genericEntityItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        genericEntityItem.setContent(genericEntityMO);


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
                "            <L7p:GenericEntityId goidValue=\""+ genericEntityItem.getId() +"\"/>\n" +
                "            <L7p:GenericEntityClass stringValue=\""+ genericEntityMO.getEntityClassName() +"\"/>\n" +
                "        </L7p:GenericEntityManagerDemo>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);
    }

    @After
    public void after() throws Exception {
        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("genericEntities/" + genericEntityItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);
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
        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
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
    public void testMapToExisting() throws Exception{
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

        try{

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
            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
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
        }finally{
            response = getTargetEnvironment().processRequest("genericEntities/"+createdGenericEntity.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);
        }
    }

    @Test
    public void testMapByName() throws Exception{
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

        try{

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
            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
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
        }finally{
            response = getTargetEnvironment().processRequest("genericEntities/"+createdGenericEntity.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);
        }
    }

    @Test
    public void testUpdateExisting() throws Exception{
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

        try{

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
            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
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
        }finally{
            response = getTargetEnvironment().processRequest("genericEntities/"+createdGenericEntity.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);
        }
    }
}
