package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.TestCertificateGenerator;
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
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class TrustedCertificateMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(TrustedCertificateMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<TrustedCertificateMO> trustedCertItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        //create trusted cert
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=source").generate();
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Source Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));
        RestResponse response = getSourceEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);

        trustedCertItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertItem.setContent(trustedCertificateMO);


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
                "        <L7p:WsSecurity>\n" +
                "            <L7p:RecipientTrustedCertificateGoid goidValue=\""+trustedCertItem.getId()+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "        </L7p:WsSecurity>" +
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

        response = getSourceEnvironment().processRequest("trustedCertificates/" + trustedCertItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy, and trusted cert", 2, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(certMapping.getSrcId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // verify dependencies
        response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO certDependency = policyDependencies.get(0);
        Assert.assertNotNull(certDependency);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certDependency.getDependentObject().getType());
        Assert.assertEquals(trustedCertItem.getName(), certDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertItem.getId(), certDependency.getDependentObject().getId());


        validate(mappings);

    }


    @Test
    public void testNoMappingExistingCertSameGoid() throws Exception {
        //create the cert on the target
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=target").generate();
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setId(trustedCertItem.getId());
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));

        RestResponse response = getTargetEnvironment().processRequest("trustedCertificates/"+trustedCertItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());

        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO certDependency = policyDependencies.get(0);
        Assert.assertNotNull(certDependency);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certDependency.getDependentObject().getType());
        Assert.assertEquals(trustedCertificateMO.getName(), certDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertificateMO.getId(), certDependency.getDependentObject().getId());

        validate(mappings);
    }

    @Test
    public void testMapToExistingDifferentCert() throws Exception {
        //create the cert on the target
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=target").generate();
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));

        RestResponse response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the cert to the existing one
        bundleItem.getContent().getMappings().get(0).setTargetId(trustedCertificateMO.getId());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertNotSame(certMapping.getSrcId(), certMapping.getTargetId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO certDependency = policyDependencies.get(0);
        Assert.assertNotNull(certDependency);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certDependency.getDependentObject().getType());
        Assert.assertEquals(trustedCertificateMO.getName(), certDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertificateMO.getId(), certDependency.getDependentObject().getId());

        validate(mappings);
    }

    @Test
    public void testMqAlwaysCreateNewWithCertConflict() throws Exception{

        //create the cert on the target
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(trustedCertItem.getContent().getCertificateData());
        RestResponse response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        // create cleanup mapping info
        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setTargetUri("/restman/1.0/trustedCertificates/"+trustedCertificateMO.getId());
        mapping.setTargetId(trustedCertificateMO.getId());
        Mappings mappings = ManagedObjectFactory.createMappings(CollectionUtils.list(mapping));
        mappingsToClean = new ItemBuilder<Mappings>("Bundle mappings", "BUNDLE MAPPINGS")
                .setContent(mappings)
                .build()
        ;
        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the cert to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.AlwaysCreateNew);

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
    }

    @Test
    public void testAlwaysCreateNew() throws Exception{

        //get the bundle
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the cert to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.AlwaysCreateNew);
        bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnExisting", true).map());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertNotSame(certMapping.getSrcId(), certMapping.getTargetId());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO mqDependency = policyDependencies.get(0);
        Assert.assertNotNull(mqDependency);
        Assert.assertEquals(trustedCertItem.getName(), mqDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertItem.getId(), mqDependency.getDependentObject().getId());

        validate(mappings);
    }

    @Test
    public void testUpdateExistingSameGoid() throws Exception{
        //create the cert on the target
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setId(trustedCertItem.getId());
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(new TestCertificateGenerator().subject("cn=target").generate()));

        RestResponse response = getTargetEnvironment().processRequest("trustedCertificates/"+trustedCertItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

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
        Assert.assertEquals("There should be 3 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO mqDependency = policyDependencies.get(0);
        Assert.assertNotNull(mqDependency);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), mqDependency.getDependentObject().getType());
        Assert.assertEquals(trustedCertItem.getName(), mqDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertItem.getId(), mqDependency.getDependentObject().getId());

        validate(mappings);
    }

    @Test
    public void testMapToUpdateExisting() throws Exception{
        //create the cert on the target
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(new TestCertificateGenerator().subject("cn=target").generate()));
        RestResponse response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the cert to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrUpdate);
        bundleItem.getContent().getMappings().get(0).setTargetId(trustedCertificateMO.getId());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO mqDependency = policyDependencies.get(0);
        Assert.assertNotNull(mqDependency);
        Assert.assertEquals(trustedCertItem.getName(), mqDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertificateMO.getId(), mqDependency.getDependentObject().getId());

        validate(mappings);
    }

    @Test
    public void testMqMapByName() throws Exception {
        //create the cert on the target
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(new TestCertificateGenerator().subject("cn=target").generate()));
        RestResponse response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the cert to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
        bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).put("MapBy", "name").put("MapTo", trustedCertificateMO.getName()).map());
        bundleItem.getContent().getMappings().get(0).setTargetId(trustedCertificateMO.getId());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO mqDependency = policyDependencies.get(0);
        Assert.assertNotNull(mqDependency);
        Assert.assertEquals(trustedCertificateMO.getName(), mqDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertificateMO.getId(), mqDependency.getDependentObject().getId());

        validate(mappings);
    }

    @Test
    public void testMapCertForNonSoapVerifyElementAssertion() throws Exception {
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
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:NonSoapVerifyElement>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:VariablePrefix stringValueNull=\"null\"/>\n" +
                "            <L7p:VerifyCertificateGoid goidValue=\"" + trustedCertItem.getId() + "\"/>\n" +
                "        </L7p:NonSoapVerifyElement>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>");
        response = getSourceEnvironment().processRequest("policies/"+policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);
        policyItem.setContent(policyMO);

        //create the cert on the target
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(new TestCertificateGenerator().subject("cn=target").generate()));
        response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the password to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
        bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
        bundleItem.getContent().getMappings().get(0).setTargetId(trustedCertificateMO.getId());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO certDependency = policyDependencies.get(0);
        Assert.assertNotNull(certDependency);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certDependency.getDependentObject().getType());
        Assert.assertEquals(trustedCertificateMO.getName(), certDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertificateMO.getId(), certDependency.getDependentObject().getId());

        validate(mappings);
    }


    @Test
    public void testMapCertForHttpRoutingAssertion() throws Exception {
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HttpRoutingAssertion>\n" +
                "            <L7p:ProtectedServiceUrl stringValue=\"http://blah\"/>\n" +
                "            <L7p:ProxyHost stringValue=\"http://blah\"/>\n" +
                "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
                "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
                "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:RequestHeaderRules>\n" +
                "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestParamRules>\n" +
                "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:ResponseHeaderRules>\n" +
                "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
                "            <L7p:TlsTrustedCertGoids goidArrayValue=\"included\">\n" +
                "                <L7p:item goidValue=\""+trustedCertItem.getId()+"\"/>\n" +
                "            </L7p:TlsTrustedCertGoids>\n" +
                "            <L7p:TlsTrustedCertNames stringArrayValue=\"included\">\n" +
                "                <L7p:item stringValue=\"user\"/>\n" +
                "            </L7p:TlsTrustedCertNames>\n" +
                "        </L7p:HttpRoutingAssertion>" +
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
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(new TestCertificateGenerator().subject("cn=target").generate()));
        response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the password to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
        bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
        bundleItem.getContent().getMappings().get(0).setTargetId(trustedCertificateMO.getId());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO certDependency = policyDependencies.get(0);
        Assert.assertNotNull(certDependency);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certDependency.getDependentObject().getType());
        Assert.assertEquals(trustedCertificateMO.getName(), certDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertificateMO.getId(), certDependency.getDependentObject().getId());

        validate(mappings);
    }

    @Test
    public void testMapCertForBridgeRoutingAssertion() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:BridgeRoutingAssertion>\n" +
                "            <L7p:Login stringValue=\"blah\"/>\n" +
                "            <L7p:NtlmHost stringValue=\"ga\"/>\n" +
                "            <L7p:Password stringValue=\"blah\"/>\n" +
                "            <L7p:ProtectedServiceUrl stringValue=\"https://blah\"/>\n" +
                "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
                "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
                "            <L7p:Realm stringValue=\"age\"/>\n" +
                "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:RequestHeaderRules>\n" +
                "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestParamRules>\n" +
                "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:ResponseHeaderRules>\n" +
                "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
                "            <L7p:ServerCertificateGoid goidValue=\""+trustedCertItem.getId()+"\"/>\n" +
                "        </L7p:BridgeRoutingAssertion>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";


        // update policy to use BridgeRoutingAssertion
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
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(new TestCertificateGenerator().subject("cn=target").generate()));
        response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);
        Item<TrustedCertificateMO> certCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertificateMO.setId(certCreated.getId());

        //get the bundle
        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A policy and trusted certificate", 2, bundleItem.getContent().getReferences().size());

        //update the bundle mapping to map the password to the existing one
        bundleItem.getContent().getMappings().get(0).setAction(Mapping.Action.NewOrExisting);
        bundleItem.getContent().getMappings().get(0).setProperties(CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).map());
        bundleItem.getContent().getMappings().get(0).setTargetId(trustedCertificateMO.getId());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundleItem.getContent()));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping certMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, certMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), certMapping.getSrcId());
        Assert.assertEquals(trustedCertificateMO.getId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(1);
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

        Item<DependencyTreeMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> policyDependencies = policyCreatedDependencies.getContent().getDependencies();

        Assert.assertNotNull(policyDependencies);
        Assert.assertEquals(1, policyDependencies.size());

        DependencyMO certDependency = policyDependencies.get(0);
        Assert.assertNotNull(certDependency);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), certDependency.getDependentObject().getType());
        Assert.assertEquals(trustedCertificateMO.getName(), certDependency.getDependentObject().getName());
        Assert.assertEquals(trustedCertificateMO.getId(), certDependency.getDependentObject().getId());

        validate(mappings);
    }


}
