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
public class SecurePasswordMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(SecurePasswordMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<StoredPasswordMO> securePasswordItem;
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

        securePasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securePasswordItem.setContent(storedPasswordMO);

        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("SourcePolicy");
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
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:FtpRoutingAssertion>\n" +
                        "            <L7p:Arguments stringValue=\"Args\"/>\n" +
                        "            <L7p:CredentialsSource credentialsSource=\"specified\"/>\n" +
                        "            <L7p:Directory stringValue=\"Dir\"/>\n" +
                        "            <L7p:DownloadedContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
                        "            <L7p:FileNameSource fileNameSource=\"argument\"/>\n" +
                        "            <L7p:FtpMethod ftpCommand=\"RETR\"/>\n" +
                        "            <L7p:HostName stringValue=\"myHost\"/>\n" +
                        "            <L7p:PasswordGoid goidValue=\""+securePasswordItem.getId()+"\"/>\n" +
                        "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                        "                <L7p:Target target=\"RESPONSE\"/>\n" +
                        "            </L7p:ResponseTarget>\n" +
                        "            <L7p:Security security=\"ftp\"/>\n" +
                        "            <L7p:UserName stringValue=\"myUser\"/>\n" +
                        "        </L7p:FtpRoutingAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        resource.setContent(policyXml);

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

        response = getSourceEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getTargetEnvironment().processRequest("policies/" , HttpMethod.GET, null, "");
        response = getTargetEnvironment().processRequest("passwords/" , HttpMethod.GET, null, "");
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy, and secure password", 2, bundleItem.getContent().getReferences().size());

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
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

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

        DependencyMO passwordDependency = getDependency(policyDependencies,securePasswordItem.getId());
        Assert.assertNotNull(passwordDependency);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
        Assert.assertEquals(securePasswordItem.getName(), passwordDependency.getName());
        Assert.assertEquals(securePasswordItem.getId(), passwordDependency.getId());

        validate(mappings);
    }


    @Test
    public void testNoMappingExistingCertSameGoid() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePasswordItem.getId());
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("passwords/"+securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());

            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally {
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToExistingDifferentCert() throws Exception {
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());

        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the cert to the existing one
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

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
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertNotSame(passwordMapping.getSrcId(), passwordMapping.getTargetId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally {
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testAlwaysCreateNewWithNameConflict() throws Exception{

        //create the cert on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName(securePasswordItem.getName());
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());
        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //update the bundle mapping to map the cert to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.AlwaysCreateNew);

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");
            getMapping(bundleItem.getContent().getMappings(), securePasswordItem.getId()).setProperties(Collections.<String, Object>emptyMap());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            logger.info(response.toString());

            // import fail
            assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
            assertEquals(409, response.getStatus());
            Item<Mappings> mappingsReturned = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            assertEquals(Mapping.ErrorType.UniqueKeyConflict, mappingsReturned.getContent().getMappings().get(0).getErrorType());
            assertTrue("Error message:",mappingsReturned.getContent().getMappings().get(0).<String>getProperty("ErrorMessage").contains("must be unique"));
            
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testAlwaysCreateNew() throws Exception{

        //get the bundle
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the cert to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.AlwaysCreateNew);
        bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnExisting", true).map());

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
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
        Assert.assertNotSame(passwordMapping.getSrcId(), passwordMapping.getTargetId());
        Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

        DependencyMO passwordDependency = getDependency(policyDependencies,securePasswordItem.getId());
        Assert.assertNotNull(passwordDependency);
        Assert.assertEquals(securePasswordItem.getName(), passwordDependency.getName());
        Assert.assertEquals(securePasswordItem.getId(), passwordDependency.getId());

        validate(mappings);
    }

    @Test
    public void testUpdateExistingSameGoid() throws Exception{
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setId(securePasswordItem.getId());
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("passwords/"+securePasswordItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //update the bundle mapping to map the cert to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,securePasswordItem.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(securePasswordItem.getId(), passwordDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapToUpdateExisting() throws Exception{
        //create the password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //update the bundle mapping to map the cert to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMqMapByName() throws Exception {
        //create the secure password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //update the bundle mapping to map the cert to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).put("MapBy", "name").put("MapTo", storedPasswordMO.getName()).map());
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapForSshRoutingAssertion() throws Exception {
        // update policy to use NonSoapVerifyElementAssertion
        RestResponse response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        PolicyMO policyMO = policyItem.getContent();
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:SshRouteAssertion>\n" +
                        "            <L7p:CommandTypeVariableName stringValue=\"\"/>\n" +
                        "            <L7p:CredentialsSourceSpecified booleanValue=\"true\"/>\n" +
                        "            <L7p:Directory stringValue=\"wage\"/>\n" +
                        "            <L7p:DownloadContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
                        "            <L7p:FileName stringValue=\"eawg\"/>\n" +
                        "            <L7p:Host stringValue=\"agwe\"/>\n" +
                        "            <L7p:NewFileName stringValue=\"\"/>\n" +
                        "            <L7p:PasswordGoid goidValue=\""+securePasswordItem.getId()+"\"/>\n" +
                        "            <L7p:Username stringValue=\"awge\"/>\n" +
                        "        </L7p:SshRouteAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";
        resource.setContent(assXml);
        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);
        policyItem.setContent(policyMO);

        //create the secure password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }


    @Test
    public void testMapForKerberosAuthenticationAssertion() throws Exception {
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:KerberosAuthentication>\n" +
                        "            <L7p:KrbConfiguredAccount stringValue=\"aweg\"/>\n" +
                        "            <L7p:KrbSecurePasswordReference goidValue=\""+ securePasswordItem.getId()+"\"/>\n" +
                        "            <L7p:KrbUseGatewayKeytab booleanValue=\"true\"/>\n" +
                        "            <L7p:LastAuthenticatedUser booleanValue=\"true\"/>\n" +
                        "            <L7p:Realm stringValue=\"hfd.com\"/>\n" +
                        "            <L7p:S4U2Self booleanValue=\"true\"/>\n" +
                        "            <L7p:ServicePrincipalName stringValue=\"http/service@DOMAIN.COM\"/>\n" +
                        "            <L7p:UserRealm stringValue=\"\"/>\n" +
                        "        </L7p:KerberosAuthentication>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        // update policy to use HttpRoutingAssertion
        RestResponse response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
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

        //create the cert on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapForFtpsRoutingAssertion() throws Exception {
        // update policy to use FtpsRoutingAssertion
        RestResponse response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        PolicyMO policyMO = policyItem.getContent();
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:FtpRoutingAssertion>\n" +
                "            <L7p:Arguments stringValue=\"args\"/>\n" +
                "            <L7p:CredentialsSource credentialsSource=\"specified\"/>\n" +
                "            <L7p:Directory stringValue=\"dir\"/>\n" +
                "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                "            <L7p:HostName stringValue=\"hostname\"/>\n" +
                "            <L7p:PasswordGoid goidValue=\""+securePasswordItem.getId()+"\"/>\n" +
                "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                "                <L7p:Target target=\"RESPONSE\"/>\n" +
                "            </L7p:ResponseTarget>\n" +
                "            <L7p:Security security=\"ftpsExplicit\"/>\n" +
                "            <L7p:UserName stringValue=\"username\"/>\n" +
                "        </L7p:FtpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        resource.setContent(assXml);
        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);
        policyItem.setContent(policyMO);

        //create the secure password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMapForRadiusAuthenticateAssertion() throws Exception {
        // update policy to use RadiusAuthenticateAssertion
        RestResponse response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        PolicyMO policyMO = policyItem.getContent();
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:RadiusAuthenticate>\n" +
                        "            <L7p:AcctPort stringValue=\"1813\"/>\n" +
                        "            <L7p:AuthPort stringValue=\"1812\"/>\n" +
                        "            <L7p:Authenticator stringValue=\"pap\"/>\n" +
                        "            <L7p:Host stringValue=\"asdf\"/>\n" +
                        "            <L7p:Prefix stringValue=\"radius\"/>\n" +
                        "            <L7p:SecretGoid goidValue=\""+securePasswordItem.getId()+"\"/>\n" +
                        "            <L7p:Timeout stringValue=\"5\"/>\n" +
                        "        </L7p:RadiusAuthenticate>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";
        resource.setContent(assXml);
        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);
        policyItem.setContent(policyMO);

        //create the secure password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());

        try{
            //get the bundle
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 2 items. A policy and secure password", 2, bundleItem.getContent().getReferences().size());

            //change the secure password MO to contain a password.
            ((StoredPasswordMO) bundleItem.getContent().getReferences().get(0).getContent()).setPassword("password");

            //update the bundle mapping to map the password to the existing one
            bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
            bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
            bundleItem.getContent().getMappings().get(0).setTargetId(storedPasswordMO.getId());

            //import the bundle
            logger.log(Level.INFO, objectToString(bundleItem.getContent()));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(securePasswordItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(storedPasswordMO.getId(), passwordMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(2);
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

            DependencyMO passwordDependency = getDependency(policyDependencies,storedPasswordMO.getId());
            Assert.assertNotNull(passwordDependency);
            Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDependency.getType());
            Assert.assertEquals(storedPasswordMO.getName(), passwordDependency.getName());
            Assert.assertEquals(storedPasswordMO.getId(), passwordDependency.getId());

            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.DELETE, null,"");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        //create the secure password on the target
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("TargetPassword");
        storedPasswordMO.setPassword("targetpassword");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", false)
                .put("type", "Password")
                .map());
        RestResponse response = getTargetEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));

        assertOkCreatedResponse(response);
        Item<StoredPasswordMO> passwordCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        storedPasswordMO.setId(passwordCreated.getId());
        passwordCreated.setContent(storedPasswordMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(passwordCreated.getId());
        mapping.setSrcId(Goid.DEFAULT_GOID.toString());
        mapping.setType(passwordCreated.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(passwordCreated.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Arrays.<Item>asList(passwordCreated));

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
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(passwordCreated.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SECURE_PASSWORD.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("passwords/"+passwordCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }
}
