package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
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

import static org.junit.Assert.*;


/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class SiteminderConfigurationMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(SiteminderConfigurationMigration.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<SiteMinderConfigurationMO> siteminderItem;
    private Item<StoredPasswordMO> passwordItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        //create secure password;
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("SourcePassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        RestResponse response = getSourceEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);

        passwordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        passwordItem.setContent(storedPasswordMO);

        // create siteminder config
        SiteMinderConfigurationMO siteMinderConfigurationMO = ManagedObjectFactory.createSiteMinderConfiguration();
        siteMinderConfigurationMO.setName("Source Siteminder Config");
        siteMinderConfigurationMO.setAddress("0.0.0.0");
        siteMinderConfigurationMO.setPasswordId(passwordItem.getId());
        siteMinderConfigurationMO.setHostname("srchost");
        siteMinderConfigurationMO.setEnabled(false);
        siteMinderConfigurationMO.setNonClusterFailover(false);
        siteMinderConfigurationMO.setIpCheck(false);
        siteMinderConfigurationMO.setUpdateSsoToken(false);
        siteMinderConfigurationMO.setFipsMode(2);
        siteMinderConfigurationMO.setSecret("srcSecret");
        response = getSourceEnvironment().processRequest("siteMinderConfigurations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(siteMinderConfigurationMO)));
        assertOkCreatedResponse(response);
        siteminderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        siteminderItem.setContent(siteMinderConfigurationMO);

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
                "        <L7p:SiteMinderCheckProtected>\n" +
                "            <L7p:AgentGoid goidValue=\""+siteminderItem.getId()+"\"/>\n" +
                "            <L7p:AgentId stringValue=\""+siteminderItem.getName()+"\"/>\n" +
                "            <L7p:ProtectedResource stringValue=\"protected resource\"/>\n" +
                "            <L7p:Action stringValue=\"GET\"/>\n" +
                "            <L7p:Prefix stringValue=\"prefix\"/>\n" +
                "        </L7p:SiteMinderCheckProtected>\n" +
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

        response = getSourceEnvironment().processRequest("siteMinderConfigurations/" + siteminderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("passwords/" + passwordItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?siteMinderConfiguration=" + siteminderItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 2 items.", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 2 items.", 2, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreSiteMinderConfigurationDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?siteMinderConfiguration=" + siteminderItem.getId() + "&requireSiteMinderConfiguration=" + siteminderItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A siteMinderConfiguration", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A siteMinderConfiguration", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, a siteminder configuration and a secure password", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. Root folder, a policy, a siteminder configuration and a secure password", 4, bundleItem.getContent().getMappings().size());

        //change the secure password MO to contain a password.
        ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
        getMapping(bundleItem.getContent().getMappings(), passwordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

        //change the siteminder MO to contain a secret.
        ((SiteMinderConfigurationMO) bundleItem.getContent().getReferences().get(1).getContent()).setSecret("secret");

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
        Assert.assertEquals(passwordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

        Mapping siteminderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, siteminderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, siteminderMapping.getActionTaken());
        Assert.assertEquals(siteminderItem.getId(), siteminderMapping.getSrcId());
        Assert.assertEquals(siteminderMapping.getSrcId(), siteminderMapping.getTargetId());

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

        assertNotNull(getDependency(policyDependencies, siteminderItem.getId()));
        assertNotNull(getDependency(policyDependencies, passwordItem.getId()));

        validate(mappings);
    }


    @Test
         public void testNoMappingExistingSameGoid() throws Exception {
        RestResponse response;

        // create passwrod on target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create the cert on the target
        SiteMinderConfigurationMO siteMinderConfigurationMO = ManagedObjectFactory.createSiteMinderConfiguration();
        siteMinderConfigurationMO.setName("Target Siteminder Config");
        siteMinderConfigurationMO.setAddress("0.0.0.0");
        siteMinderConfigurationMO.setPasswordId(passwordCreated.getId());
        siteMinderConfigurationMO.setHostname("targethost");
        siteMinderConfigurationMO.setEnabled(false);
        siteMinderConfigurationMO.setNonClusterFailover(false);
        siteMinderConfigurationMO.setIpCheck(false);
        siteMinderConfigurationMO.setUpdateSsoToken(false);
        siteMinderConfigurationMO.setFipsMode(1);
        siteMinderConfigurationMO.setSecret("targetSecret");
        response = getTargetEnvironment().processRequest("siteMinderConfigurations/"+siteminderItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(siteMinderConfigurationMO)));
        assertOkCreatedResponse(response);
        Item<SiteMinderConfigurationMO> siteminderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, a siteminder configuration and a secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder, a policy, a siteminder configuration and a secure password", 4, bundleItem.getContent().getMappings().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), passwordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

            //change the siteminder MO to contain a secret.
            ((SiteMinderConfigurationMO) bundleItem.getContent().getReferences().get(1).getContent()).setSecret("secret");

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
            Assert.assertEquals(passwordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping siteminderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, siteminderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, siteminderMapping.getActionTaken());
            Assert.assertEquals(siteminderItem.getId(), siteminderMapping.getSrcId());
            Assert.assertEquals(siteminderMapping.getSrcId(), siteminderMapping.getTargetId());

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

            assertNotNull(getDependency(policyDependencies, siteminderItem.getId()));
            assertNull(getDependency(policyDependencies, passwordItem.getId()));
            assertNotNull(getDependency(policyDependencies, passwordCreated.getId()));

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("siteMinderConfigurations/" + siteminderCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("passwords/" + passwordCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testNoMappingUpdateExistingSameGoid() throws Exception {
        RestResponse response;

        // create passwrod on target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create the cert on the target
        SiteMinderConfigurationMO siteMinderConfigurationMO = ManagedObjectFactory.createSiteMinderConfiguration();
        siteMinderConfigurationMO.setName("Target Siteminder Config");
        siteMinderConfigurationMO.setAddress("0.0.0.0");
        siteMinderConfigurationMO.setPasswordId(passwordCreated.getId());
        siteMinderConfigurationMO.setHostname("targethost");
        siteMinderConfigurationMO.setEnabled(false);
        siteMinderConfigurationMO.setNonClusterFailover(false);
        siteMinderConfigurationMO.setIpCheck(false);
        siteMinderConfigurationMO.setUpdateSsoToken(false);
        siteMinderConfigurationMO.setFipsMode(1);
        siteMinderConfigurationMO.setSecret("targetSecret");
        response = getTargetEnvironment().processRequest("siteMinderConfigurations/"+siteminderItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(siteMinderConfigurationMO)));
        assertOkCreatedResponse(response);
        Item<SiteMinderConfigurationMO> siteminderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, a siteminder configuration and a secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder, a policy, a siteminder configuration and a secure password", 4, bundleItem.getContent().getMappings().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), passwordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

            //change the siteminder MO to contain a secret.
            ((SiteMinderConfigurationMO) bundleItem.getContent().getReferences().get(1).getContent()).setSecret("secret");

            // set mappings
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);

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
            Assert.assertEquals(passwordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping siteminderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, siteminderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, siteminderMapping.getActionTaken());
            Assert.assertEquals(siteminderItem.getId(), siteminderMapping.getSrcId());
            Assert.assertEquals(siteminderMapping.getSrcId(), siteminderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify updated
            response = getTargetEnvironment().processRequest("siteMinderConfigurations/"+siteminderMapping.getSrcId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<SiteMinderConfigurationMO> updatedSiteminder = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertEquals(updatedSiteminder.getContent().getName(),siteminderItem.getName());
            assertEquals(updatedSiteminder.getContent().getFipsMode(), siteminderItem.getContent().getFipsMode());
            assertEquals(updatedSiteminder.getContent().getHostname(),siteminderItem.getContent().getHostname());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(2, policyDependencies.size());

            assertNotNull(getDependency(policyDependencies, siteminderItem.getId()));
            assertNotNull(getDependency(policyDependencies, passwordItem.getId()));
            assertNull(getDependency(policyDependencies, passwordCreated.getId()));

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("siteMinderConfigurations/" + siteminderCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("passwords/" + passwordCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMappingExisting() throws Exception {
        RestResponse response;

        // create passwrod on target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create the cert on the target
        SiteMinderConfigurationMO siteMinderConfigurationMO = ManagedObjectFactory.createSiteMinderConfiguration();
        siteMinderConfigurationMO.setName("Target Siteminder Config");
        siteMinderConfigurationMO.setAddress("0.0.0.0");
        siteMinderConfigurationMO.setPasswordId(passwordCreated.getId());
        siteMinderConfigurationMO.setHostname("targethost");
        siteMinderConfigurationMO.setEnabled(false);
        siteMinderConfigurationMO.setNonClusterFailover(false);
        siteMinderConfigurationMO.setIpCheck(false);
        siteMinderConfigurationMO.setUpdateSsoToken(false);
        siteMinderConfigurationMO.setFipsMode(1);
        siteMinderConfigurationMO.setSecret("targetSecret");
        response = getTargetEnvironment().processRequest("siteMinderConfigurations/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(siteMinderConfigurationMO)));
        assertOkCreatedResponse(response);
        Item<SiteMinderConfigurationMO> siteminderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, a siteminder configuration and a secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder, a policy, a siteminder configuration and a secure password", 4, bundleItem.getContent().getMappings().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), passwordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

            //change the siteminder MO to contain a secret.
            ((SiteMinderConfigurationMO) bundleItem.getContent().getReferences().get(1).getContent()).setSecret("secret");

            // set mappings
            bundleItem.getContent().getMappings().get(1).setTargetId(siteminderCreated.getId());

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
            Assert.assertEquals(passwordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping siteminderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, siteminderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, siteminderMapping.getActionTaken());
            Assert.assertEquals(siteminderItem.getId(), siteminderMapping.getSrcId());
            Assert.assertEquals(siteminderCreated.getId(), siteminderMapping.getTargetId());

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

            assertNotNull(getDependency(policyDependencies, siteminderCreated.getId()));
            assertNotNull(getDependency(policyDependencies, passwordCreated.getId()));

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("siteMinderConfigurations/" + siteminderCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("passwords/" + passwordCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMappingPassword() throws Exception {
        RestResponse response;

        // create passwrod on target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, a siteminder configuration and a secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder, a policy, a siteminder configuration and a secure password", 4, bundleItem.getContent().getMappings().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //change the siteminder MO to contain a secret.
            ((SiteMinderConfigurationMO) bundleItem.getContent().getReferences().get(1).getContent()).setSecret("secret");

            // set mappings
            bundleItem.getContent().getMappings().get(0).setTargetId(passwordCreated.getId());

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
            Assert.assertEquals(passwordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordCreated.getId(), passwordMapping.getTargetId());

            Mapping siteminderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, siteminderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, siteminderMapping.getActionTaken());
            Assert.assertEquals(siteminderItem.getId(), siteminderMapping.getSrcId());
            Assert.assertEquals(siteminderMapping.getSrcId(), siteminderMapping.getTargetId());

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

            assertNotNull(getDependency(policyDependencies, siteminderItem.getId()));
            assertNotNull(getDependency(policyDependencies, passwordCreated.getId()));

            validate(mappings);
        } finally {
            //need to delete the siteminder configuration here before we can delete the password for avoid foreign constraint errors
            response = getTargetEnvironment().processRequest("siteMinderConfigurations/" + siteminderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
            mappingsToClean.getContent().getMappings().remove(1);
            response = getTargetEnvironment().processRequest("passwords/" + passwordCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        RestResponse response;

        // create passwrod on target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        try {
            //create the cert on the target
            SiteMinderConfigurationMO siteMinderConfigurationMO = ManagedObjectFactory.createSiteMinderConfiguration();
            siteMinderConfigurationMO.setName("Target Siteminder Config");
            siteMinderConfigurationMO.setAddress("0.0.0.0");
            siteMinderConfigurationMO.setPasswordId(passwordCreated.getId());
            siteMinderConfigurationMO.setHostname("targethost");
            siteMinderConfigurationMO.setEnabled(false);
            siteMinderConfigurationMO.setNonClusterFailover(false);
            siteMinderConfigurationMO.setIpCheck(false);
            siteMinderConfigurationMO.setUpdateSsoToken(false);
            siteMinderConfigurationMO.setFipsMode(1);
            siteMinderConfigurationMO.setSecret("targetSecret");
            response = getTargetEnvironment().processRequest("siteMinderConfigurations/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                    XmlUtil.nodeToString(ManagedObjectFactory.write(siteMinderConfigurationMO)));
            assertOkCreatedResponse(response);
            Item<SiteMinderConfigurationMO> siteminderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            siteminderCreated.setContent(siteMinderConfigurationMO);

            Bundle bundle = ManagedObjectFactory.createBundle();

            Mapping mapping = ManagedObjectFactory.createMapping();
            mapping.setAction(Mapping.Action.Delete);
            mapping.setTargetId(siteminderCreated.getId());
            mapping.setSrcId(Goid.DEFAULT_GOID.toString());
            mapping.setType(siteminderCreated.getType());

            Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
            mappingNotExisting.setAction(Mapping.Action.Delete);
            mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
            mappingNotExisting.setType(siteminderCreated.getType());

            bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
            bundle.setReferences(Arrays.<Item>asList(siteminderCreated));

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
            Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), activeConnectorMapping.getType());
            Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
            Assert.assertEquals(siteminderCreated.getId(), activeConnectorMapping.getTargetId());

            Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), activeConnectorMappingNotExisting.getType());
            Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
            Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

            response = getTargetEnvironment().processRequest("siteMinderConfigurations/" + siteminderCreated.getId(), HttpMethod.GET, null, "");
            assertNotFoundResponse(response);
        } finally {
            response = getTargetEnvironment().processRequest("passwords/" + passwordCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testImportNewWithSecrets() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId() + "?encryptSecrets=true&encryptUsingClusterPassphrase=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, a siteminder configuration and a secure password", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. Root folder, a policy, a siteminder configuration and a secure password", 4, bundleItem.getContent().getMappings().size());

        //should have encrypted password
        final StoredPasswordMO storedPassMO = (StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent();
        assertTrue(storedPassMO.getPassword().startsWith("$L7C2"));
        assertTrue(storedPassMO.getPasswordBundleKey().startsWith("$L7C2"));

        //should have encrypted secret
        final SiteMinderConfigurationMO siteminderConfigMO = (SiteMinderConfigurationMO) bundleItem.getContent().getReferences().get(1).getContent();
        assertTrue(siteminderConfigMO.getSecret().startsWith("$L7C2"));
        assertTrue(siteminderConfigMO.getSecretBundleKey().startsWith("$L7C2"));

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
        Assert.assertEquals(passwordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

        Mapping siteminderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, siteminderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, siteminderMapping.getActionTaken());
        Assert.assertEquals(siteminderItem.getId(), siteminderMapping.getSrcId());
        Assert.assertEquals(siteminderMapping.getSrcId(), siteminderMapping.getTargetId());

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

        assertNotNull(getDependency(policyDependencies, siteminderItem.getId()));
        assertNotNull(getDependency(policyDependencies, passwordItem.getId()));

        validate(mappings);
    }

    @BugId("SSG-10514")
    @Test
    public void testMinimalConfigImportNew() throws Exception {
        RestResponse response;
        //create the cert on the target
        SiteMinderConfigurationMO siteMinderConfigurationMO = ManagedObjectFactory.createSiteMinderConfiguration();
        siteMinderConfigurationMO.setName("Source Siteminder Config Minimal");
        siteMinderConfigurationMO.setAddress("");
        siteMinderConfigurationMO.setHostname("srchost");
        siteMinderConfigurationMO.setEnabled(false);
        siteMinderConfigurationMO.setNonClusterFailover(false);
        siteMinderConfigurationMO.setIpCheck(false);
        siteMinderConfigurationMO.setUpdateSsoToken(false);
        siteMinderConfigurationMO.setFipsMode(2);
        siteMinderConfigurationMO.setSecret("srcSecret");
        response = getSourceEnvironment().processRequest("siteMinderConfigurations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(siteMinderConfigurationMO)));
        assertOkCreatedResponse(response);
        Item<SiteMinderConfigurationMO> siteminderMinimal = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        siteminderMinimal.setContent(siteMinderConfigurationMO);


        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicyMinimal");
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
                "        <L7p:SiteMinderCheckProtected>\n" +
                "            <L7p:AgentGoid goidValue=\""+siteminderMinimal.getId()+"\"/>\n" +
                "            <L7p:AgentId stringValue=\""+siteminderMinimal.getName()+"\"/>\n" +
                "            <L7p:ProtectedResource stringValue=\"protected resource\"/>\n" +
                "            <L7p:Action stringValue=\"GET\"/>\n" +
                "            <L7p:Prefix stringValue=\"prefix\"/>\n" +
                "        </L7p:SiteMinderCheckProtected>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        Item<PolicyMO> policyItemMinimal = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItemMinimal.setContent(policyMO);

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItemMinimal.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy, a siteminder configuration", 2, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 3 mappings. Root folder, a policy, a siteminder configuration", 3, bundleItem.getContent().getMappings().size());

            //change the siteminder MO to contain a secret.
            ((SiteMinderConfigurationMO) bundleItem.getContent().getReferences().get(0).getContent()).setSecret("secret");

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());

            Mapping siteminderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, siteminderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, siteminderMapping.getActionTaken());
            Assert.assertEquals(siteminderMinimal.getId(), siteminderMapping.getSrcId());
            Assert.assertEquals(siteminderMapping.getSrcId(), siteminderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItemMinimal.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            // verify dependencies
            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

            Assert.assertNotNull(policyDependencies);
            Assert.assertEquals(1, policyDependencies.size());

            assertNotNull(getDependency(policyDependencies, siteminderMinimal.getId()));

            validate(mappings);
        }finally{
            response = getSourceEnvironment().processRequest("siteMinderConfigurations/" + siteminderMinimal.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getSourceEnvironment().processRequest("policies/" + policyItemMinimal.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }
}
