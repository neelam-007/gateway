package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.skunkworks.rest.tools.JVMDatabaseBasedRestManagementEnvironment;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
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
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class UsersMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(UsersMigration.class.getName());

    private Item<UserMO> userItem;
    private Item<UserMO> adminUserItem;
    private Item<IdentityProviderMO> idProviderItem;
    private Item<PolicyMO> policyItem;
    private Item<ServiceMO> serviceItem;
    private Item<Mappings> mappingsToClean;
    private Item<IdentityProviderMO> federatedIdentityProvider;

    @Before
    public void before() throws Exception {
        // get internal provider item
        RestResponse response = getSourceEnvironment().processRequest("identityProviders/"+ IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString(), HttpMethod.GET, null,"");
        assertOkResponse(response);
        idProviderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("My FederatedId Provider");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.FEDERATED);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .put("enableCredentialType.x509", true)
                .put("enableCredentialType.saml", true)
                .map());
        IdentityProviderMO.FederatedIdentityProviderDetail federatedDetails = identityProviderMO.getFederatedIdentityProviderDetail();
        response = getSourceEnvironment().processRequest("identityProviders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(identityProviderMO)));
        assertOkCreatedResponse(response);
        federatedIdentityProvider = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getSourceEnvironment().processRequest("identityProviders/"+ federatedIdentityProvider.getId(), HttpMethod.GET, null,"");
        assertOkResponse(response);
        federatedIdentityProvider = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // get admin user
        response = getSourceEnvironment().processRequest("identityProviders/"+ IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString() + "/users/"+ new Goid(0,3).toString(), HttpMethod.GET, null,"");
        assertOkResponse(response);
        adminUserItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("SrcUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getSourceEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        userItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        userItem.setContent(userMO);

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
        resource.setContent( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SpecificUser>\n" +
                "            <L7p:IdentityProviderOid goidValue=\""+idProviderItem.getId()+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:UserLogin stringValue=\""+adminUserItem.getName()+"\"/>\n" +
                "            <L7p:UserName stringValue=\""+adminUserItem.getName()+"\"/>\n" +
                "            <L7p:UserUid stringValue=\""+adminUserItem.getId()+"\"/>\n" +
                "        </L7p:SpecificUser>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);

        //create policy;
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("MyService");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        resourceSet = ManagedObjectFactory.createResourceSet();
        serviceMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SpecificUser>\n" +
                "            <L7p:IdentityProviderOid goidValue=\""+idProviderItem.getId()+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:UserLogin stringValue=\""+adminUserItem.getName()+"\"/>\n" +
                "            <L7p:UserName stringValue=\""+adminUserItem.getName()+"\"/>\n" +
                "            <L7p:UserUid stringValue=\""+adminUserItem.getId()+"\"/>\n" +
                "        </L7p:SpecificUser>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);
    }

    @After
    public void after() throws Exception {
        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("identityProviders/"+federatedIdentityProvider.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
        Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
        Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

        Mapping userMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, userMapping.getActionTaken());
        Assert.assertEquals(adminUserItem.getId(), userMapping.getSrcId());
        Assert.assertEquals(userMapping.getSrcId(), userMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
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
        Assert.assertEquals(2, policyDependencies.size());

        Assert.assertNotNull(getDependency(policyDependencies, adminUserItem.getId()));
        Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

        validate(mappings);
    }

    @Test
    public void testMapToExisting() throws Exception{
        RestResponse response;
        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("targetUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> createdUser = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).setTargetId(createdUser.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, userMapping.getActionTaken());
            Assert.assertEquals(adminUserItem.getId(), userMapping.getSrcId());
            Assert.assertEquals(createdUser.getId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
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
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, createdUser.getId()));
            Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users/"+createdUser.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);

        }

    }

    @Test
    public void testMapByName() throws Exception{
        RestResponse response;
        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("targetUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> createdUser = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).getProperties().put("MapBy", "name");
            bundleItem.getContent().getMappings().get(1).getProperties().put("MapTo", createdUser.getName());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, userMapping.getActionTaken());
            Assert.assertEquals(adminUserItem.getId(), userMapping.getSrcId());
            Assert.assertEquals(createdUser.getId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
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
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, createdUser.getId()));
            Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users/"+createdUser.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);

        }

    }

    @Test
    public void testMapByNameInService() throws Exception{
        RestResponse response;
        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("targetUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> createdUser = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A service, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A service, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).getProperties().put("MapBy", "name");
            bundleItem.getContent().getMappings().get(1).getProperties().put("MapTo", createdUser.getName());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, userMapping.getActionTaken());
            Assert.assertEquals(adminUserItem.getId(), userMapping.getSrcId());
            Assert.assertEquals(createdUser.getId(), userMapping.getTargetId());

            Mapping serviceMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("services/"+serviceMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> serviceCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> serviceDependencies = serviceCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(serviceDependencies);
            Assert.assertEquals(2, serviceDependencies.size());

            Assert.assertNotNull(getDependency(serviceDependencies, createdUser.getId()));
            Assert.assertNotNull(getDependency(serviceDependencies, idProviderItem.getId()));

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users/"+createdUser.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);

        }

    }

    @Test
    public void testBuildRstrSoapResponseAssertion() throws Exception{
        RestResponse response;
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:BuildRstrSoapResponse>\n" +
                        "            <L7p:IdentityTarget IdentityTarget=\"included\">\n" +
                        "                <L7p:IdentityId stringValue=\""+userItem.getId()+"\"/>\n" +
                        "                <L7p:IdentityInfo stringValue=\""+ userItem.getName()+"\"/>\n" +
                        "                <L7p:IdentityProviderOid goidValue=\""+ idProviderItem.getId()+"\"/>\n" +
                        "                <L7p:TargetIdentityType identityType=\"USER\"/>\n" +
                        "            </L7p:IdentityTarget>\n" +
                        "            <L7p:ResponseForIssuance booleanValue=\"false\"/>\n" +
                        "        </L7p:BuildRstrSoapResponse>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";


        // update policy to use BuildRstrSoapResponseAssertion
        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        PolicyMO policyMO = policyItem.getContent();
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent(assXml);
        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);
        policyItem.setContent(policyMO);

        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("targetUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> createdUser = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).setTargetId(createdUser.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, userMapping.getActionTaken());
            Assert.assertEquals(userItem.getId(), userMapping.getSrcId());
            Assert.assertEquals(createdUser.getId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
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
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, createdUser.getId()));
            Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users/"+createdUser.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);

        }

    }

    @Test
    public void deleteTest() throws Exception {
        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("targetUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        RestResponse response = getTargetEnvironment().processRequest("identityProviders/" + idProviderItem.getId() + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> createdUser = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdUser.setContent(userMO);
        userMO.setId(createdUser.getId());

        UserMO userMissingMO = ManagedObjectFactory.createUserMO();
        userMissingMO.setLogin("MissingUser");
        userMissingMO.setId(Goid.DEFAULT_GOID.toString());
        userMissingMO.setProviderId(idProviderItem.getId());
        Item<UserMO> userMissingItem = new ItemBuilder<UserMO>("temp", EntityType.USER.toString()).setContent(userMissingMO).build();
        userMissingItem.setId(Goid.DEFAULT_GOID.toString());

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setSrcId(createdUser.getId());
        mapping.setType(EntityType.USER.name());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(EntityType.USER.name());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Arrays.<Item>asList(createdUser, userMissingItem));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertEquals(200, response.getStatus());

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
        Mapping privateKeyMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.USER.toString(), privateKeyMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, privateKeyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, privateKeyMapping.getActionTaken());
        Assert.assertEquals(createdUser.getId(), privateKeyMapping.getTargetId());

        Mapping privateKeyMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.USER.toString(), privateKeyMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, privateKeyMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, privateKeyMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, privateKeyMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("identityProviders/" + idProviderItem.getId() + "/users/"+createdUser.getId(), HttpMethod.GET, null, "");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testEncryptSecretsImportNewUser() throws Exception{
        RestResponse response;
        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicy - EncryptSecrets");
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
        resource.setContent( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SpecificUser>\n" +
                "            <L7p:IdentityProviderOid goidValue=\""+idProviderItem.getId()+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:UserLogin stringValue=\""+userItem.getName()+"\"/>\n" +
                "            <L7p:UserName stringValue=\""+userItem.getName()+"\"/>\n" +
                "            <L7p:UserUid stringValue=\""+userItem.getId()+"\"/>\n" +
                "        </L7p:SpecificUser>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        Item<PolicyMO> newPolicyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        newPolicyItem.setContent(policyMO);

        try {

            response = getSourceEnvironment().processRequest("bundle/policy/" + newPolicyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, userMapping.getActionTaken());
            Assert.assertEquals(userItem.getId(), userMapping.getSrcId());
            //Assert.assertEquals(userItem.getId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(newPolicyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, userMapping.getTargetId()));
            Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

            validate(mappings);
        }finally{
            response = getSourceEnvironment().processRequest("policies/"+newPolicyItem.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testEncryptSecretsImportUpdateUser() throws Exception{
        RestResponse response;
        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicy - EncryptSecrets");
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
        resource.setContent( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SpecificUser>\n" +
                "            <L7p:IdentityProviderOid goidValue=\""+idProviderItem.getId()+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:UserLogin stringValue=\""+userItem.getName()+"\"/>\n" +
                "            <L7p:UserName stringValue=\""+userItem.getName()+"\"/>\n" +
                "            <L7p:UserUid stringValue=\""+userItem.getId()+"\"/>\n" +
                "        </L7p:SpecificUser>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        Item<PolicyMO> newPolicyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        newPolicyItem.setContent(policyMO);

        // create user to update
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(userItem.getContent().getProviderId());
        userMO.setLogin("targetUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> createdUserItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdUserItem.setContent(userMO);

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + newPolicyItem.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true&defaultAction=NewOrUpdate", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            // remove fail on new
            Assert.assertEquals(Mapping.Action.NewOrUpdate,bundleItem.getContent().getMappings().get(1).getAction());
            Assert.assertEquals("USER",bundleItem.getContent().getMappings().get(1).getType());
            bundleItem.getContent().getMappings().get(1).setTargetId(createdUserItem.getId());


            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, userMapping.getActionTaken());
            Assert.assertEquals(userItem.getId(), userMapping.getSrcId());
            Assert.assertEquals(createdUserItem.getId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(newPolicyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, userMapping.getTargetId()));
            Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

            validate(mappings);

        }finally{

            response = getSourceEnvironment().processRequest("policies/"+newPolicyItem.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users/"+createdUserItem.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testImportNewUserWithCertificate() throws Exception {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("UserWithCertificate");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        X509Certificate certificate = new TestCertificateGenerator().subject("cn="+userMO.getLogin()).generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        userMO.setCertificateData(certData);

        Item<UserMO> userItemCreated = createUser(getSourceEnvironment(), userMO);
        Item<PolicyMO> policyItemCreated = createPolicyUsingUser(getSourceEnvironment(), userItemCreated.getContent(), "testImportNewUserWithCertificate");

        try {
            RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItemCreated.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, userMapping.getActionTaken());
            Assert.assertEquals(userItemCreated.getId(), userMapping.getSrcId());
            Assert.assertEquals(userMapping.getSrcId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItemCreated.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, userItemCreated.getId()));
            Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

            //verify that the user cert is updated
            response = getTargetEnvironment().processRequest("identityProviders/" + userItemCreated.getContent().getProviderId() + "/users/" + userItemCreated.getId() + "/certificate", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<CertificateData> targetUserCertData = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertArrayEquals(userItemCreated.getContent().getCertificateData().getEncoded(), targetUserCertData.getContent().getEncoded());

            validate(mappings);
        } finally {
            deleteUser(getSourceEnvironment(), userItemCreated);
            deletePolicy(getSourceEnvironment(), policyItemCreated);
        }
    }

    @Test
    public void testImportUpdateUserWithCertificate() throws Exception {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("UserWithCertificate");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        X509Certificate certificate = new TestCertificateGenerator().subject("cn="+userMO.getLogin()).generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        userMO.setCertificateData(certData);

        Item<UserMO> userItemCreated = createUser(getSourceEnvironment(), userMO);
        Item<PolicyMO> policyItemCreated = createPolicyUsingUser(getSourceEnvironment(), userItemCreated.getContent(), "testImportNewUserWithCertificate");

        userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(idProviderItem.getId());
        userMO.setLogin("UserWithCertificateTarget");
        password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        Item<UserMO> userItemCreatedTarget = createUser(getTargetEnvironment(), userMO);

        try {
            RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItemCreated.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            bundleItem.getContent().getMappings().get(1).setTargetId(userItemCreatedTarget.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(idProviderItem.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, userMapping.getActionTaken());
            Assert.assertEquals(userItemCreated.getId(), userMapping.getSrcId());
            Assert.assertEquals(userItemCreatedTarget.getId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItemCreated.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, userItemCreatedTarget.getId()));
            Assert.assertNotNull(getDependency(policyDependencies, idProviderItem.getId()));

            //verify that the user cert is updated
            response = getTargetEnvironment().processRequest("identityProviders/" + userItemCreatedTarget.getContent().getProviderId() + "/users/" + userItemCreatedTarget.getId() + "/certificate", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<CertificateData> targetUserCertData = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertArrayEquals(userItemCreated.getContent().getCertificateData().getEncoded(), targetUserCertData.getContent().getEncoded());

            validate(mappings);
        } finally {
            deleteUser(getSourceEnvironment(), userItemCreated);
            deleteUser(getTargetEnvironment(), userItemCreatedTarget);
            deletePolicy(getSourceEnvironment(), policyItemCreated);
        }
    }

    @Test
    public void testImportNewUserWithCertificateFIP() throws Exception {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(federatedIdentityProvider.getId());
        userMO.setName("UserWithCertificate");
        userMO.setLogin("UserWithCertificate");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        X509Certificate certificate = new TestCertificateGenerator().subject("cn="+userMO.getLogin()).generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        userMO.setCertificateData(certData);

        Item<UserMO> userItemCreated = createUser(getSourceEnvironment(), userMO);
        Item<PolicyMO> policyItemCreated = createPolicyUsingUser(getSourceEnvironment(), userItemCreated.getContent(), "testImportNewUserWithCertificate");

        try {
            RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItemCreated.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, idProviderMapping.getActionTaken());
            Assert.assertEquals(federatedIdentityProvider.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, userMapping.getActionTaken());
            Assert.assertEquals(userItemCreated.getId(), userMapping.getSrcId());
            Assert.assertEquals(userMapping.getSrcId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItemCreated.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, userItemCreated.getId()));
            Assert.assertNotNull(getDependency(policyDependencies, federatedIdentityProvider.getId()));

            //verify that the user cert is updated
            response = getTargetEnvironment().processRequest("identityProviders/" + userItemCreated.getContent().getProviderId() + "/users/" + userItemCreated.getId() + "/certificate", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<CertificateData> targetUserCertData = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertArrayEquals(userItemCreated.getContent().getCertificateData().getEncoded(), targetUserCertData.getContent().getEncoded());

            validate(mappings);
        } finally {
            deleteUser(getSourceEnvironment(), userItemCreated);
            deletePolicy(getSourceEnvironment(), policyItemCreated);
        }
    }

    @Test
    public void testImportUpdateUserWithCertificateFIP() throws Exception {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(federatedIdentityProvider.getId());
        userMO.setName("UserWithCertificate");
        userMO.setLogin("UserWithCertificate");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        X509Certificate certificate = new TestCertificateGenerator().subject("cn="+userMO.getLogin()).generate();
        CertificateData certData = ManagedObjectFactory.createCertificateData(certificate);
        userMO.setCertificateData(certData);

        Item<UserMO> userItemCreated = createUser(getSourceEnvironment(), userMO);
        Item<PolicyMO> policyItemCreated = createPolicyUsingUser(getSourceEnvironment(), userItemCreated.getContent(), "testImportNewUserWithCertificate");

        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("My FederatedId Provider");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.FEDERATED);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .put("enableCredentialType.x509", true)
                .put("enableCredentialType.saml", true)
                .map());
        IdentityProviderMO.FederatedIdentityProviderDetail federatedDetails = identityProviderMO.getFederatedIdentityProviderDetail();
        RestResponse response = getTargetEnvironment().processRequest("identityProviders/"+federatedIdentityProvider.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(identityProviderMO)));
        assertOkCreatedResponse(response);

        userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(federatedIdentityProvider.getId());
        userMO.setLogin("UserWithCertificateTarget");
        userMO.setName("UserWithCertificateTarget");
        password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        Item<UserMO> userItemCreatedTarget = createUser(getTargetEnvironment(), userMO);

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItemCreated.getId(), "encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A policy, an identity provider, a user", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A policy, root folder, an identity provider, a user", 4, bundleItem.getContent().getMappings().size());

            bundleItem.getContent().getMappings().get(1).setTargetId(userItemCreatedTarget.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
            Assert.assertEquals(federatedIdentityProvider.getId(), idProviderMapping.getSrcId());
            Assert.assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

            Mapping userMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, userMapping.getActionTaken());
            Assert.assertEquals(userItemCreated.getId(), userMapping.getSrcId());
            Assert.assertEquals(userItemCreatedTarget.getId(), userMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItemCreated.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/" + policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            Assert.assertNotNull(getDependency(policyDependencies, userItemCreatedTarget.getId()));
            Assert.assertNotNull(getDependency(policyDependencies, federatedIdentityProvider.getId()));

            //verify that the user cert is updated
            response = getTargetEnvironment().processRequest("identityProviders/" + userItemCreatedTarget.getContent().getProviderId() + "/users/" + userItemCreatedTarget.getId() + "/certificate", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<CertificateData> targetUserCertData = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertArrayEquals(userItemCreated.getContent().getCertificateData().getEncoded(), targetUserCertData.getContent().getEncoded());

            validate(mappings);
        } finally {
            deleteUser(getSourceEnvironment(), userItemCreated);
            deleteUser(getTargetEnvironment(), userItemCreatedTarget);
            deletePolicy(getSourceEnvironment(), policyItemCreated);

            response = getTargetEnvironment().processRequest("identityProviders/"+federatedIdentityProvider.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    private void deleteUser(JVMDatabaseBasedRestManagementEnvironment environment, Item<UserMO> user) throws Exception {
        RestResponse response = environment.processRequest("identityProviders/" + user.getContent().getProviderId() + "/users/" + user.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    private void deletePolicy(JVMDatabaseBasedRestManagementEnvironment environment, Item<PolicyMO> policy) throws Exception {
        RestResponse response = environment.processRequest("policies/" + policy.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    private Item<UserMO> createUser(JVMDatabaseBasedRestManagementEnvironment environment, UserMO userMO) throws Exception {
        //create user
        RestResponse response = environment.processRequest("identityProviders/" + userMO.getProviderId() + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> userItemCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        userItemCreated.setContent(userMO);
        userMO.setId(userItemCreated.getId());

        if(userMO.getCertificateData() != null){
            response = environment.processRequest("identityProviders/" + userMO.getProviderId() + "/users/" + userItemCreated.getId() + "/certificate", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    writeMOToString(userMO.getCertificateData()));
            assertOkResponse(response);
        }

        return userItemCreated;
    }

    protected String writeMOToString(Object  mo) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult( bout );
        MarshallingUtils.marshal( mo, result, false );
        return bout.toString();
    }

    private Item<PolicyMO> createPolicyUsingUser(JVMDatabaseBasedRestManagementEnvironment environment, UserMO userMO, String policyName) throws Exception {

        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName(policyName);
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
        resource.setContent( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SpecificUser>\n" +
                "            <L7p:IdentityProviderOid goidValue=\""+userMO.getProviderId()+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:UserLogin stringValue=\""+userMO.getLogin()+"\"/>\n" +
                "            <L7p:UserName stringValue=\""+userMO.getLogin()+"\"/>\n" +
                "            <L7p:UserUid stringValue=\""+userMO.getId()+"\"/>\n" +
                "        </L7p:SpecificUser>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");

        RestResponse response = environment.processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        Item<PolicyMO> policyItemCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItemCreated.setContent(policyMO);
        policyMO.setId(policyItemCreated.getId());
        return policyItemCreated;
    }
}
