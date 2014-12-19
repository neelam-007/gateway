package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class RevocationCheckMigrationTest extends MigrationTestBase {
    private static final Logger logger = Logger.getLogger(RevocationCheckMigrationTest.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<TrustedCertificateMO> trustedCertItem;
    private Item<RevocationCheckingPolicyMO> revCheckItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {

        // create revocation check policy
        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName("Source Rev Check Policy");
        checkPolicyMO.setDefaultPolicy(true);
        RevocationCheckingPolicyItemMO checkItem = ManagedObjectFactory.createRevocationCheckingPolicyItem();
        checkItem.setType(RevocationCheckingPolicyItemMO.Type.CRL_FROM_CERTIFICATE);
        checkItem.setUrl(".*");
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        RestResponse response = getSourceEnvironment().processRequest("revocationCheckingPolicies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(checkPolicyMO)));

        assertOkCreatedResponse(response);

        revCheckItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        revCheckItem.setContent(checkPolicyMO);

        // create trusted cert using revocation check policy
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=revcheck").generate();
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Source Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));
        trustedCertificateMO.setRevocationCheckingPolicyId(revCheckItem.getId());
        trustedCertificateMO.setProperties(new HashMap<String, Object>());
        trustedCertificateMO.getProperties().put(
                "revocationCheckingEnabled", true);
        response = getSourceEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);

        trustedCertItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        trustedCertItem.setContent(trustedCertificateMO);

        //create policy
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
                "            <L7p:RecipientTrustedCertificateGoid goidValue=\"" + trustedCertItem.getId() + "\"/>\n" +
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
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("trustedCertificates/" + trustedCertItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("revocationCheckingPolicies/" + revCheckItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, trusted cert and revocation policy", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 items. A folder, a policy, trusted cert and revocation policy", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping passwordMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), passwordMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
        Assert.assertEquals(revCheckItem.getId(), passwordMapping.getSrcId());
        Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

        Mapping jdbcMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), jdbcMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
        Assert.assertEquals(trustedCertItem.getId(), jdbcMapping.getSrcId());
        Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

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
    public void testMapById() throws Exception {
        // create revocation check policy
        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName("Target Rev Check Policy");
        checkPolicyMO.setDefaultPolicy(true);
        RevocationCheckingPolicyItemMO checkItem = ManagedObjectFactory.createRevocationCheckingPolicyItem();
        checkItem.setType(RevocationCheckingPolicyItemMO.Type.OCSP_FROM_CERTIFICATE);
        checkItem.setUrl(".*");
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        RestResponse response = getTargetEnvironment().processRequest("revocationCheckingPolicies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(checkPolicyMO)));

        assertOkCreatedResponse(response);

        Item<RevocationCheckingPolicyMO> targetRevCheckItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetRevCheckItem.setContent(checkPolicyMO);

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, trusted cert and revocation policy", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. A folder, a policy, trusted cert and revocation policy", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(0).setTargetId(targetRevCheckItem.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(revCheckItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(targetRevCheckItem.getId(), passwordMapping.getTargetId());

            Mapping jdbcMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), jdbcMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
            Assert.assertEquals(trustedCertItem.getId(), jdbcMapping.getSrcId());
            Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

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
        } finally {

            response = getTargetEnvironment().processRequest("trustedCertificates/" + trustedCertItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("revocationCheckingPolicies/" + targetRevCheckItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testUsedExisting() throws Exception {
        // create revocation check policy
        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName("Target Rev Check Policy");
        checkPolicyMO.setId(revCheckItem.getId());
        checkPolicyMO.setDefaultPolicy(true);
        RevocationCheckingPolicyItemMO checkItem = ManagedObjectFactory.createRevocationCheckingPolicyItem();
        checkItem.setType(RevocationCheckingPolicyItemMO.Type.OCSP_FROM_CERTIFICATE);
        checkItem.setUrl(".*");
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        RestResponse response = getTargetEnvironment().processRequest("revocationCheckingPolicies/" + revCheckItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(checkPolicyMO)));

        assertOkCreatedResponse(response);

        Item<RevocationCheckingPolicyMO> targetRevCheckItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetRevCheckItem.setContent(checkPolicyMO);

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, trusted cert and revocation policy", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. A folder, a policy, trusted cert and revocation policy", 4, bundleItem.getContent().getMappings().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, passwordMapping.getActionTaken());
            Assert.assertEquals(revCheckItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping jdbcMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), jdbcMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
            Assert.assertEquals(trustedCertItem.getId(), jdbcMapping.getSrcId());
            Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

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
        } finally {

            response = getTargetEnvironment().processRequest("trustedCertificates/" + trustedCertItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("revocationCheckingPolicies/" + targetRevCheckItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testMigrateRevocationCheckPolicyWithCertificateReferences() throws Exception {
        // create signer cert
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=revsigner").generate();
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Source Signer Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));
        RestResponse response = getSourceEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);

        Item<TrustedCertificateMO> srcTrustedCertSignerItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        srcTrustedCertSignerItem.setContent(trustedCertificateMO);

        // update revocation check policy
        RevocationCheckingPolicyMO checkPolicyMO = revCheckItem.getContent();
        RevocationCheckingPolicyItemMO checkItem = checkPolicyMO.getRevocationCheckItems().get(0);
        checkItem.setTrustedSigners(CollectionUtils.list(srcTrustedCertSignerItem.getId()));
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        response = getSourceEnvironment().processRequest("revocationCheckingPolicies/" + revCheckItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(checkPolicyMO)));

        assertOkResponse(response);

        revCheckItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        revCheckItem.setContent(checkPolicyMO);

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 4 items. A policy, 2 trusted certs and revocation policy", 4, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 5 items. A folder, a policy, 2 trusted cert and revocation policy", 5, bundleItem.getContent().getMappings().size());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());

            Mapping signerCertMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), signerCertMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, signerCertMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, signerCertMapping.getActionTaken());
            Assert.assertEquals(srcTrustedCertSignerItem.getId(), signerCertMapping.getSrcId());
            Assert.assertEquals(signerCertMapping.getSrcId(), signerCertMapping.getTargetId());

            Mapping passwordMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(revCheckItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping jdbcMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), jdbcMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
            Assert.assertEquals(trustedCertItem.getId(), jdbcMapping.getSrcId());
            Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);
        } finally {

            response = getSourceEnvironment().processRequest("trustedCertificates/" + srcTrustedCertSignerItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMigrateRevocationCheckPolicyMapCertificateReference() throws Exception {
        // create signer cert
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=revsigner").generate();
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Source Signer Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));
        RestResponse response = getSourceEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);

        Item<TrustedCertificateMO> srcTrustedCertSignerItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        srcTrustedCertSignerItem.setContent(trustedCertificateMO);

        // update revocation check policy
        RevocationCheckingPolicyMO checkPolicyMO = revCheckItem.getContent();
        RevocationCheckingPolicyItemMO checkItem = checkPolicyMO.getRevocationCheckItems().get(0);
        checkItem.setTrustedSigners(CollectionUtils.list(srcTrustedCertSignerItem.getId()));
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        response = getSourceEnvironment().processRequest("revocationCheckingPolicies/" + revCheckItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(checkPolicyMO)));

        assertOkResponse(response);

        revCheckItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        revCheckItem.setContent(checkPolicyMO);

        // create target signer cert
        certificate = new TestCertificateGenerator().subject("cn=targetrevsigner").generate();
        trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Signer Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));
        response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);

        Item<TrustedCertificateMO> targetTrustedCertSignerItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetTrustedCertSignerItem.setContent(trustedCertificateMO);

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 4 items. A policy, 2 trusted certs and revocation policy", 4, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 5 items. A folder, a policy, 2 trusted cert and revocation policy", 5, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(0).setTargetId(targetTrustedCertSignerItem.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());

            Mapping signerCertMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), signerCertMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, signerCertMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, signerCertMapping.getActionTaken());
            Assert.assertEquals(srcTrustedCertSignerItem.getId(), signerCertMapping.getSrcId());
            Assert.assertEquals(targetTrustedCertSignerItem.getId(), signerCertMapping.getTargetId());

            Mapping passwordMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(revCheckItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping jdbcMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), jdbcMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, jdbcMapping.getActionTaken());
            Assert.assertEquals(trustedCertItem.getId(), jdbcMapping.getSrcId());
            Assert.assertEquals(jdbcMapping.getSrcId(), jdbcMapping.getTargetId());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(4);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);

            // validate dependency
            response = getTargetEnvironment().processRequest("policies/"+policyItem.getId()+"/dependencies", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
            assertOkResponse(response);
            Item<DependencyListMO> dependencyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(3, dependencyItem.getContent().getDependencies().size());

        } finally {

            response = getTargetEnvironment().processRequest("trustedCertificates/" + targetTrustedCertSignerItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

            response = getSourceEnvironment().processRequest("trustedCertificates/" + srcTrustedCertSignerItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testMigrateRevocationCheckPolicyMapCircularCertificateReference() throws Exception {

        // update revocation check policy to be circular
        RevocationCheckingPolicyMO checkPolicyMO = revCheckItem.getContent();
        RevocationCheckingPolicyItemMO checkItem = checkPolicyMO.getRevocationCheckItems().get(0);
        checkItem.setTrustedSigners(CollectionUtils.list(trustedCertItem.getId()));
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        RestResponse response = getSourceEnvironment().processRequest("revocationCheckingPolicies/" + revCheckItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(checkPolicyMO)));

        assertOkResponse(response);

        revCheckItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        revCheckItem.setContent(checkPolicyMO);

        // create target signer cert
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=targetrevsigner").generate();
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Target Signer Cert");
        trustedCertificateMO.setCertificateData(ManagedObjectFactory.createCertificateData(certificate));
        response = getTargetEnvironment().processRequest("trustedCertificates", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(trustedCertificateMO)));

        assertOkCreatedResponse(response);

        Item<TrustedCertificateMO> targetTrustedCertSignerItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetTrustedCertSignerItem.setContent(trustedCertificateMO);

        try {
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, trusted cert and revocation policy", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 items. A folder, a policy, trusted cert and revocation policy", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).setTargetId(targetTrustedCertSignerItem.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

            Mapping passwordMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), passwordMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, passwordMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, passwordMapping.getActionTaken());
            Assert.assertEquals(revCheckItem.getId(), passwordMapping.getSrcId());
            Assert.assertEquals(passwordMapping.getSrcId(), passwordMapping.getTargetId());

            Mapping jdbcMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.TRUSTED_CERT.toString(), jdbcMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, jdbcMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, jdbcMapping.getActionTaken());
            Assert.assertEquals(trustedCertItem.getId(), jdbcMapping.getSrcId());
            Assert.assertEquals(targetTrustedCertSignerItem.getId(), jdbcMapping.getTargetId());

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

            // check circular dependency
            response = getTargetEnvironment().processRequest("revocationCheckingPolicies/" + revCheckItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
            assertOkResponse(response);
            Item<RevocationCheckingPolicyMO> migratedTargetRevocationCheckPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(1,migratedTargetRevocationCheckPolicy.getContent().getRevocationCheckItems().size());
            RevocationCheckingPolicyItemMO item = migratedTargetRevocationCheckPolicy.getContent().getRevocationCheckItems().get(0);
            Assert.assertEquals(1,item.getTrustedSigners().size());
            Assert.assertEquals(targetTrustedCertSignerItem.getId(),item.getTrustedSigners().get(0));


        } finally {

            response = getTargetEnvironment().processRequest("trustedCertificates/" + targetTrustedCertSignerItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
            assertOkEmptyResponse(response);

        }
    }


    protected String objectToString(Object object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }
}
