package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.IdentityProviderConfigManager;
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


/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class UsersMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(UsersMigrationTest.class.getName());

    private Item<UserMO> userItem;
    private Item<UserMO> adminUserItem;
    private Item<IdentityProviderMO> idProviderItem;
    private Item<PolicyMO> policyItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        // get internal provider item
        RestResponse response = getSourceEnvironment().processRequest("identityProviders/"+ IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString(), HttpMethod.GET, null,"");
        assertOkResponse(response);
        idProviderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

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
    }

    @After
    public void after() throws Exception {
        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("identityProviders/"+idProviderItem.getId()+"/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A policy, an identity provider, a user", 1, bundleItem.getContent().getReferences().size());
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

            Assert.assertEquals("The bundle should have 1 item. A policy, an identity provider, a user", 1, bundleItem.getContent().getReferences().size());
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
            assertOkDeleteResponse(response);

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

            Assert.assertEquals("The bundle should have 1 item. A policy, an identity provider, a user", 1, bundleItem.getContent().getReferences().size());
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
            assertOkDeleteResponse(response);

        }

    }
}
