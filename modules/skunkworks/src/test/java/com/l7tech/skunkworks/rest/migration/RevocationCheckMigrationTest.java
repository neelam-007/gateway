package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.*;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class RevocationCheckMigrationTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(RevocationCheckMigrationTest.class.getName());

    private TrustedCert trustedCert = new TrustedCert();
    private RevocationCheckPolicy revocationCheckPolicy = new RevocationCheckPolicy();
    private Policy policy = new Policy(PolicyType.INTERNAL, "Policy", "", false);
    private TrustedCertManager trustedCertManager;
    private RevocationCheckPolicyManager revocationCheckPolicyManager;
    private PolicyManager policyManager;

    @Before
    public void before() throws Exception {

        trustedCertManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("trustedCertManager", TrustedCertManager.class);
        revocationCheckPolicyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("revocationCheckPolicyManager", RevocationCheckPolicyManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);

        // create revocation check policy
        revocationCheckPolicy.setName("Test Revocation check policy");
        revocationCheckPolicy.setGoid(revocationCheckPolicyManager.save(revocationCheckPolicy));

        // create trusted cert using revocation check policy
        X509Certificate newCertificate = new TestCertificateGenerator().subject("cn=revcheck").generate();
        trustedCert.setName(CertUtils.extractFirstCommonNameFromCertificate(newCertificate));
        trustedCert.setCertificate(newCertificate);
        trustedCert.setTrustAnchor(false);
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.SPECIFIED);
        trustedCert.setRevocationCheckPolicyOid(revocationCheckPolicy.getGoid());
        trustedCert.setGoid(trustedCertManager.save(trustedCert));

        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:WsSecurity>\n" +
                        "            <L7p:RecipientTrustedCertificateGoid goidValue=\""+trustedCert.getId()+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:WsSecurity>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";
        policy.setXml(policyXml);
        policy.setGuid(UUID.randomUUID().toString());
        policy.setGoid(policyManager.save(policy));
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        cleanDatabase();
    }

    private void cleanDatabase() throws Exception  {
        for(TrustedCert cert: trustedCertManager.findAll()){
            trustedCertManager.delete(cert);
        }

        for(RevocationCheckPolicy rev: revocationCheckPolicyManager.findAll()){
            revocationCheckPolicyManager.delete(rev);
        }

        for(Policy policy: policyManager.findAll()){
            if(!policy.getType().isServicePolicy())
                policyManager.delete(policy);
        }
    }


    @Test
    public void migrationMapTest() throws Exception {
        RestResponse response =
                getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET,null,"");
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals("The bundle should have 2 items. A policy, a trusted certificate", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 4 items. Root folder, a policy, a trusted certificate, a revocation check policy", 4, bundleItem.getContent().getMappings().size());

        cleanDatabase();

        // create new revocation check policy
        RevocationCheckPolicy newRev = new RevocationCheckPolicy();
        newRev.setName("Target Revocation check policy");
        newRev.setGoid(revocationCheckPolicyManager.save(newRev));

        // update bundle
        bundleItem.getContent().getMappings().get(0).setTargetId(newRev.getId());

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());
        
        // check mapping

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        
        //verify the mappings
        assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping revMapping = mappings.getContent().getMappings().get(0);
        assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), revMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, revMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, revMapping.getActionTaken());
        assertNotSame(revMapping.getSrcId(), revMapping.getTargetId());
        assertEquals(revocationCheckPolicy.getId(), revMapping.getSrcId());
        assertEquals(newRev.getId(), revMapping.getTargetId());

        Mapping certMapping = mappings.getContent().getMappings().get(1);
        assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        assertEquals(Mapping.ActionTaken.CreatedNew, certMapping.getActionTaken());
        assertEquals(trustedCert.getId(), certMapping.getSrcId());
        assertEquals(certMapping.getSrcId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        assertEquals(policy.getId(), policyMapping.getSrcId());
        assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());
        
        // check dependency
        response = getDatabaseBasedRestManagementEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());
        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();

        assertNotNull(dependencies);
        assertEquals(2, dependencies.size());

        assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(),dependencies.get(0).getType());
        assertEquals(EntityType.TRUSTED_CERT.toString(),dependencies.get(1).getType());
    }

    @Test
    public void migrationUseExistingTest() throws Exception {
        RestResponse response =
                getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET,null,"");
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals("The bundle should have 2 items. A policy, a trusted certificate", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 4 items. Root folder, a policy, a trusted certificate, a revocation check policy", 4, bundleItem.getContent().getMappings().size());

        cleanDatabase();

        // create new revocation check policy
        revocationCheckPolicyManager.save(revocationCheckPolicy.getGoid(),revocationCheckPolicy);

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        // check mapping

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping revMapping = mappings.getContent().getMappings().get(0);
        assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), revMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, revMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, revMapping.getActionTaken());
        assertEquals(revocationCheckPolicy.getId(), revMapping.getSrcId());
        assertEquals(revMapping.getSrcId(), revMapping.getTargetId());

        Mapping certMapping = mappings.getContent().getMappings().get(1);
        assertEquals(EntityType.TRUSTED_CERT.toString(), certMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, certMapping.getAction());
        assertEquals(Mapping.ActionTaken.CreatedNew, certMapping.getActionTaken());
        assertEquals(trustedCert.getId(), certMapping.getSrcId());
        assertEquals(certMapping.getSrcId(), certMapping.getTargetId());

        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        assertEquals(policy.getId(), policyMapping.getSrcId());
        assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        // check dependency
        response = getDatabaseBasedRestManagementEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());
        Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();

        assertNotNull(dependencies);
        assertEquals(2, dependencies.size());

        assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(),dependencies.get(0).getType());
        assertEquals(EntityType.TRUSTED_CERT.toString(),dependencies.get(1).getType());
    }

    protected String objectToString(Object object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }
}
