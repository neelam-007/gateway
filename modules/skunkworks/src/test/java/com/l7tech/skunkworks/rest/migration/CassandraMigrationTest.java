package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
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

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class CassandraMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(CassandraMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<StoredPasswordMO> securePasswordItem;
    private Item<CassandraConnectionMO> cassandraConnectionItem;
    private Item<Mappings> mappingsToClean;

    private static ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator configuredGOIDGenerator = new ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator();

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

        //create Cassandra connection;
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName("MyCassandraConnection");
        //cassandraConnectionMO.setId(getGoid().toString());
        cassandraConnectionMO.setCompression(CassandraConnection.COMPRESS_NONE);
        cassandraConnectionMO.setSsl(true);
        cassandraConnectionMO.setKeyspace("test");
        cassandraConnectionMO.setPort("9042");
        cassandraConnectionMO.setContactPoint("localhost");
        cassandraConnectionMO.setEnabled(true);
        cassandraConnectionMO.setUsername(securePasswordItem.getName());
        cassandraConnectionMO.setPasswordId(securePasswordItem.getId());
        cassandraConnectionMO.setTlsciphers("SOME_RSA_CIPHER,SOME_EC_CIPHER");
        cassandraConnectionMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("test", "test").map());
        response = getSourceEnvironment().processRequest("cassandraConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);

        cassandraConnectionItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionItem.setContent(cassandraConnectionMO);

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
                "        <L7p:CassandraQuery>\n" +
                "            <L7p:ConnectionName stringValue=\"MyCassandraConnection\"/>\n" +
                "            <L7p:QueryDocument stringValue=\"select * from users;\"/>\n" +
                "            <L7p:QueryTimeout stringValue=\"10\"/>\n" +
                "        </L7p:CassandraQuery>\n" +
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
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("cassandraConnections/" + cassandraConnectionItem.getId(), HttpMethod.DELETE, null, "");
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

        Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

        //change the secure password MO to contain a password.
        ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
        getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

        Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
        Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
        Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        validate(mappings);

    }

    @Test
    public void testMapToExistingPasswordSameGoidSameName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName());
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        storedPasswordMO.setId(securePasswordItem.getId());
        RestResponse response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testUpdateExistingPasswordSameGoidDifferentName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName());
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        storedPasswordMO.setId(securePasswordItem.getId());
        RestResponse response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //change the bundle to update the password
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

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
            Assert.assertEquals(Mapping.Action.NewOrUpdate, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testAlwaysCreateNewExistingPasswordSameGoidDifferentName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName() + "Target");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        storedPasswordMO.setId(securePasswordItem.getId());
        RestResponse response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //change the bundle to update the password
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.AlwaysCreateNew);
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

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
            Assert.assertEquals(Mapping.Action.AlwaysCreateNew, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertNotSame(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

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
            List<DependencyMO> cassandraDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(cassandraDependencies);
            Assert.assertEquals(2, cassandraDependencies.size());

            DependencyMO passwordDependency = getDependency(cassandraDependencies, EntityType.SECURE_PASSWORD);
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(securePasswordItem.getContent().getName(), passwordDependency.getName());
            Assert.assertNotSame(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + storedPasswordMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingPasswordDifferentGoidSameName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName());
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + passwordCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingPasswordDifferentGoidDifferentName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName() + "Target");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + storedPasswordMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingPasswordSameGoidDifferentName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePasswordItem.getId());
        storedPasswordMO.setName(securePasswordItem.getContent().getName() + "Target");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + storedPasswordMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingPasswordByNameSameGoidSameName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName());
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        storedPasswordMO.setId(securePasswordItem.getId());
        RestResponse response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder().put("MapBy", "name").map());

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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

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

            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> cassandraDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(cassandraDependencies);
            Assert.assertEquals(2, cassandraDependencies.size());

            DependencyMO passwordDependency = getDependency(cassandraDependencies, storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingPasswordByNameSameGoidDifferentName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName() + "Target");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        storedPasswordMO.setId(securePasswordItem.getId());
        RestResponse response = getTargetEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder()
                            .put("MapBy", "name")
                            .put("MapTo", storedPasswordMO.getName())
                            .map());

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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + storedPasswordMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingPasswordByNameDifferentGoidSameName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName());
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder().put("MapBy", "name").map());

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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

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
            List<DependencyMO> cassandraDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(cassandraDependencies);
            Assert.assertEquals(2, cassandraDependencies.size());

            DependencyMO passwordDependency = getDependency(cassandraDependencies, storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + storedPasswordMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingPasswordByNameDifferentGoidDifferentName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName() + "Target");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder().put("MapBy", "name").put("MapTo", storedPasswordMO.getName()).map());

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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + storedPasswordMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testUpdateExistingPasswordByNameDifferentGoidDifferentName() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getContent().getName() + "Target");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(securePasswordItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);

        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());


        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            bundleItem.getContent().getMappings().get(0).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder().put("MapBy", "name").put("MapTo", storedPasswordMO.getName()).map());
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

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
            Assert.assertEquals(Mapping.Action.NewOrUpdate, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + storedPasswordMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingCassandraDifferentGoidSameName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName());
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setTargetId(cassandraConnectionMO.getId());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraMapping.getTargetId());

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
            List<DependencyMO> cassandraDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(cassandraDependencies);
            Assert.assertEquals(2, cassandraDependencies.size());

            DependencyMO passwordDependency = getDependency(cassandraDependencies, securePasswordItem.getId());
            Assert.assertNotNull(passwordDependency);

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingCassandraDifferentGoidDifferentName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName() + "Updated");
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setTargetId(cassandraConnectionMO.getId());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraMapping.getTargetId());

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

            DependencyMO cassandraDependency = getDependency(policyDependencies, cassandraConnectionMO.getId());
            Assert.assertNotNull(cassandraDependency);
            Assert.assertEquals(cassandraConnectionMO.getName(), cassandraDependency.getName());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingCassandraSameGoidDifferentName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(cassandraConnectionItem.getId());
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName() + "Updated");
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setTargetId(cassandraConnectionMO.getId());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraMapping.getSrcId(), cassandraMapping.getTargetId());

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

            DependencyMO cassandraDependency = getDependency(policyDependencies, cassandraConnectionItem.getId());
            Assert.assertNotNull(cassandraDependency);
            Assert.assertEquals(cassandraConnectionMO.getName(), cassandraDependency.getName());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingCassandraByNameDifferentGoidDifferentName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName() + "Updated");
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder()
                            .put("MapBy", "name")
                            .put("MapTo", cassandraConnectionMO.getName()).map());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraMapping.getTargetId());

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

            DependencyMO cassandraDependency = getDependency(policyDependencies, cassandraConnectionMO.getId());
            Assert.assertNotNull(cassandraDependency);
            Assert.assertEquals(cassandraConnectionMO.getName(), cassandraDependency.getName());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testUpdateExistingCassandraByNameDifferentGoidDifferentName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName() + "Updated");
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder()
                            .put("MapBy", "name")
                            .put("MapTo", cassandraConnectionMO.getName()).map());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraMapping.getTargetId());

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

            DependencyMO cassandraDependency = getDependency(policyDependencies, cassandraConnectionMO.getId());
            Assert.assertNotNull(cassandraDependency);
            Assert.assertEquals(cassandraConnectionMO.getName(), cassandraDependency.getName());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingCassandraByNameSameGoidDifferentName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(cassandraConnectionItem.getId());
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName() + "Updated");
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder()
                            .put("MapBy", "name")
                            .put("MapTo", cassandraConnectionMO.getName()).map());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraMapping.getTargetId());

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

            DependencyMO cassandraDependency = getDependency(policyDependencies, cassandraConnectionMO.getId());
            Assert.assertNotNull(cassandraDependency);
            Assert.assertEquals(cassandraConnectionMO.getName(), cassandraDependency.getName());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingCassandraByNameDifferentGoidSameName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName());
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder()
                            .put("MapBy", "name").map());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraMapping.getTargetId());

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

            DependencyMO cassandraDependency = getDependency(policyDependencies, cassandraConnectionMO.getId());
            Assert.assertNotNull(cassandraDependency);
            Assert.assertEquals(cassandraConnectionMO.getName(), cassandraDependency.getName());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingCassandraByNameSameGoidSameName() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(cassandraConnectionItem.getId());
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName());
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());

        try {
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, cassandraConnection and secure password", 3, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the Cassandra connection to the existing one
            bundleItem.getContent().getMappings().get(1).setProperties(
                    CollectionUtils.MapBuilder.<String, Object>builder()
                            .put("MapBy", "name").map());
            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

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

            Mapping cassandraMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), cassandraMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, cassandraMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, cassandraMapping.getActionTaken());
            Assert.assertEquals(cassandraConnectionItem.getId(), cassandraMapping.getSrcId());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraMapping.getTargetId());

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

            DependencyMO cassandraDependency = getDependency(policyDependencies, cassandraConnectionItem.getId());
            Assert.assertNotNull(cassandraDependency);
            Assert.assertEquals(cassandraConnectionMO.getName(), cassandraDependency.getName());
            Assert.assertEquals(cassandraConnectionMO.getId(), cassandraDependency.getId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionMO.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        //create the Cassandra on the target
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setName(cassandraConnectionItem.getContent().getName() + "Updated");
        cassandraConnectionMO.setCompression(cassandraConnectionItem.getContent().getCompression());
        cassandraConnectionMO.setSsl(cassandraConnectionItem.getContent().isSsl());
        cassandraConnectionMO.setKeyspace(cassandraConnectionItem.getContent().getKeyspace());
        cassandraConnectionMO.setPort(cassandraConnectionItem.getContent().getPort());
        cassandraConnectionMO.setContactPoint(cassandraConnectionItem.getContent().getContactPoint());
        cassandraConnectionMO.setEnabled(false);
        cassandraConnectionMO.setUsername(cassandraConnectionItem.getContent().getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnectionItem.getContent().getPasswordId());
        cassandraConnectionMO.setTlsciphers(cassandraConnectionItem.getContent().getTlsciphers());
        cassandraConnectionMO.setProperties(cassandraConnectionItem.getContent().getProperties());
        RestResponse response = getTargetEnvironment().processRequest("cassandraConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(cassandraConnectionMO)));

        assertOkCreatedResponse(response);
        Item<CassandraConnectionMO> cassandraConnectionCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        cassandraConnectionMO.setId(cassandraConnectionCreated.getId());
        cassandraConnectionCreated.setContent(cassandraConnectionMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(cassandraConnectionCreated.getId());
        mapping.setSrcId(Goid.DEFAULT_GOID.toString());
        mapping.setType(cassandraConnectionCreated.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(cassandraConnectionCreated.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Arrays.<Item>asList(cassandraConnectionCreated));

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
        Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(cassandraConnectionCreated.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("cassandraConnections/" + cassandraConnectionCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }

    protected Goid getGoid() {
        return (Goid) configuredGOIDGenerator.generate(null, null);
    }
}
