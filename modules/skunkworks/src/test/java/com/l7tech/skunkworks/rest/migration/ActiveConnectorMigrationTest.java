package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ActiveConnectorMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(ActiveConnectorMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<StoredPasswordMO> securePasswordItem;
    private Item<ActiveConnectorMO> mqNativeItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        //create secure password;
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("MyPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("type", "Password")
                .map());
        RestResponse response = getSourceEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);

        securePasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securePasswordItem.setContent(storedPasswordMO);

        //create mq connector;
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("MyMQConfig");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeIsQueueCredentialRequired", "true")
                .put("MqNativeQueueManagerName", "qManager")
                .put("MqNativeSecurePasswordOid", securePasswordItem.getId())
                .put("MqNativePort", "1234")
                .put("MqNativeHostName", "host")
                .map());
        response = getSourceEnvironment().processRequest("activeConnectors", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);

        mqNativeItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mqNativeItem.setContent(activeConnectorMO);

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
                "        <L7p:MqNativeRouting>\n" +
                "            <L7p:RequestMessageAdvancedProperties mapValueNull=\"null\"/>\n" +
                "            <L7p:RequestMqNativeMessagePropertyRuleSet mappingRuleSet=\"included\"/>\n" +
                "            <L7p:ResponseMessageAdvancedProperties mapValueNull=\"null\"/>\n" +
                "            <L7p:ResponseMqNativeMessagePropertyRuleSet mappingRuleSet=\"included\"/>\n" +
                "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                "                <L7p:Target target=\"RESPONSE\"/>\n" +
                "            </L7p:ResponseTarget>\n" +
                "            <L7p:SsgActiveConnectorGoid goidValue=\""+mqNativeItem.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorId goidValue=\""+mqNativeItem.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorName stringValue=\""+mqNativeItem.getName()+"\"/>\n" +
                "        </L7p:MqNativeRouting>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

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
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("activeConnectors/" + mqNativeItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, active connection and secure password", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. Root folder,a policy, active connection and secure password", 4, bundleItem.getContent().getMappings().size());

        //change the secure password MO to contain a password.
        ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
        getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

        Mapping mqMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, mqMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, mqMapping.getActionTaken());
        Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
        Assert.assertEquals(mqMapping.getSrcId(), mqMapping.getTargetId());

        Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        validate(mappings);
    }


    @Test
    public void testNoMappingExistingMqSameGoid() throws Exception {
        //create the mq connector on the target
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setId(mqNativeItem.getId());
        activeConnectorMO.setName("TargetMQConfig");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeQueueManagerName", "qManager")
                .put("MqNativePort", "9876")
                .put("MqNativeHostName", "host")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("activeConnectors/"+mqNativeItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);
        Item<ActiveConnectorMO> mqCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        activeConnectorMO.setId(mqCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder, a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping mqMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, mqMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, mqMapping.getActionTaken());
            Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
            Assert.assertEquals(activeConnectorMO.getId(), mqMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO mqDependency = policyDependencies.get(0);
            Assert.assertNotNull(mqDependency);
            Assert.assertEquals(activeConnectorMO.getName(), mqDependency.getName());
            Assert.assertEquals(activeConnectorMO.getId(), mqDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("activeConnectors/"+ mqCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingDifferentMq() throws Exception {
        //create the mq connector on the target
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("Different MQ Config");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeQueueManagerName", "qManager")
                .put("MqNativePort", "9876")
                .put("MqNativeHostName", "otherHost")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("activeConnectors", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);
        Item<ActiveConnectorMO> mqCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        activeConnectorMO.setId(mqCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. Root folder, a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

            //update the bundle mapping to map the mq connector to the existing one
            bundleItem.getContent().getMappings().get(1).setTargetId(activeConnectorMO.getId());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping mqMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, mqMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, mqMapping.getActionTaken());
            Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
            Assert.assertEquals(activeConnectorMO.getId(), mqMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO mqDependency = policyDependencies.get(0);
            Assert.assertNotNull(mqDependency);
            Assert.assertEquals(activeConnectorMO.getName(), mqDependency.getName());
            Assert.assertEquals(activeConnectorMO.getId(), mqDependency.getId());

            validate(mappings);
        }finally {
            response = getTargetEnvironment().processRequest("activeConnectors/"+mqCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMqAlwaysCreateNewWithNameConflict() throws Exception{

        //create the mq connector on the target
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName(mqNativeItem.getName());
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeQueueManagerName", "qManager")
                .put("MqNativePort", "9876")
                .put("MqNativeHostName", "otherHost")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("activeConnectors", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);
        Item<ActiveConnectorMO> mqCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        activeConnectorMO.setId(mqCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. Root folder, a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

            //update the bundle mapping to map the mq connector to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.AlwaysCreateNew);
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

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
            assertTrue("Error message:",mappingsReturned.getContent().getMappings().get(1).<String>getProperty("ErrorMessage").contains("must be unique"));
        }finally {
            response = getTargetEnvironment().processRequest("activeConnectors/" + mqCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMqAlwaysCreateNew() throws Exception{

        //get the bundle
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 items. Root folder, a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

        //update the bundle mapping to map the mq connector to the existing one
        bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.AlwaysCreateNew);
        bundleItem.getContent().getMappings().get(1).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnExisting", true).map());

        //change the secure password MO to contain a password.
        ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
        getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

        Mapping mqMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, mqMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, mqMapping.getActionTaken());
        Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
        Assert.assertEquals(mqNativeItem.getId(), mqMapping.getTargetId());

        Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

        logger.log(Level.INFO, policyXml);

        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(2, policyDependencies.size());

        DependencyMO mqDependency = getDependency(policyDependencies,mqNativeItem.getId());
        Assert.assertNotNull(mqDependency);
        Assert.assertEquals(mqNativeItem.getName(), mqDependency.getName());
        Assert.assertEquals(mqNativeItem.getId(), mqDependency.getId());

        validate(mappings);
    }

    @Test
    public void testMqUpdateExistingSameGoid() throws Exception{
        //create the mq connector on the target
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setId(mqNativeItem.getId());
        activeConnectorMO.setName("Different MQ Config");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeQueueManagerName", "qManager")
                .put("MqNativePort", "9876")
                .put("MqNativeHostName", "otherHost")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("activeConnectors/"+mqNativeItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);
        Item<ActiveConnectorMO> mqCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        activeConnectorMO.setId(mqCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. Root folder, a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

            //update the bundle mapping to map the mq connector to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping mqMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, mqMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, mqMapping.getActionTaken());
            Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
            Assert.assertEquals(activeConnectorMO.getId(), mqMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO mqDependency = getDependency(policyDependencies, mqNativeItem.getId());
            Assert.assertNotNull(mqDependency);
            Assert.assertEquals(activeConnectorMO.getName(), mqDependency.getName());
            Assert.assertEquals(mqNativeItem.getId(), mqDependency.getId());

            validate(mappings);
        }finally {
            response = getTargetEnvironment().processRequest("activeConnectors/"+mqCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMqMapToUpdateExisting() throws Exception{
        //create the mq connector on the target
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("Different MQ Config");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeQueueManagerName", "qManager")
                .put("MqNativePort", "9876")
                .put("MqNativeHostName", "otherHost")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("activeConnectors", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);
        Item<ActiveConnectorMO> mqCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        activeConnectorMO.setId(mqCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. Root folder,a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

            //update the bundle mapping to map the mq connector to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).setTargetId(activeConnectorMO.getId());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping mqMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, mqMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, mqMapping.getActionTaken());
            Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
            Assert.assertEquals(activeConnectorMO.getId(), mqMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO mqDependency = getDependency(policyDependencies, activeConnectorMO.getId());
            Assert.assertNotNull(mqDependency);
            Assert.assertEquals(activeConnectorMO.getName(), mqDependency.getName());
            Assert.assertEquals(activeConnectorMO.getId(), mqDependency.getId());

            validate(mappings);
        }finally {
            response = getTargetEnvironment().processRequest("activeConnectors/"+mqCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMqMapByName() throws Exception {
        //create the mq connector on the target
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("Different MQ Config");
        activeConnectorMO.setEnabled(false);
        activeConnectorMO.setType("MqNative");
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put("MqNativeQueueManagerName", "qManager")
                .put("MqNativePort", "9876")
                .put("MqNativeHostName", "otherHost")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("activeConnectors", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(activeConnectorMO)));

        assertOkCreatedResponse(response);
        Item<ActiveConnectorMO> mqCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        activeConnectorMO.setId(mqCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. Root folder,a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

            //update the bundle mapping to map the mq connector to the existing one
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(1).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).put("MapBy", "name").put("MapTo", activeConnectorMO.getName()).map());
            bundleItem.getContent().getMappings().get(1).setTargetId(activeConnectorMO.getId());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String,Object>emptyMap());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping mqMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, mqMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, mqMapping.getActionTaken());
            Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
            Assert.assertEquals(activeConnectorMO.getId(), mqMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            DependencyMO mqDependency = policyDependencies.get(0);
            Assert.assertNotNull(mqDependency);
            Assert.assertEquals(activeConnectorMO.getName(), mqDependency.getName());
            Assert.assertEquals(activeConnectorMO.getId(), mqDependency.getId());

            validate(mappings);
        }finally {
            response = getTargetEnvironment().processRequest("activeConnectors/"+mqCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMqCreateNewMapPasswordTest() throws Exception {
        //create the password on the target
        StoredPasswordMO passwordMO = ManagedObjectFactory.createStoredPassword();
        passwordMO.setName("targetPassword");
        passwordMO.setPassword("targetPassword");
        passwordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("type", "Password")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(passwordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        passwordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connector and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. Root folder,a policy, active connector and secure password", 4, bundleItem.getContent().getMappings().size());

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
            bundleItem.getContent().getMappings().get(0).setTargetId(passwordMO.getId());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMO.getId(), passwordMapping.getTargetId());

            Mapping mqMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, mqMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, mqMapping.getActionTaken());
            Assert.assertEquals(mqNativeItem.getId(), mqMapping.getSrcId());
            Assert.assertEquals(mqMapping.getSrcId(), mqMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<PolicyMO> policyCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            String policyXml = policyCreated.getContent().getResourceSets().get(0).getResources().get(0).getContent();

            logger.log(Level.INFO, policyXml);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            DependencyMO mqDependency = getDependency(policyDependencies, mqNativeItem.getId());
            Assert.assertNotNull(mqDependency);

            DependencyMO passwordDependency = getDependency(policyDependencies, passwordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(passwordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(passwordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally {
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);
        }
    }
}
