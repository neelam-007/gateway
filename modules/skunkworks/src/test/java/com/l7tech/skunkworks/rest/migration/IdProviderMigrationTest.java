package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class IdProviderMigrationTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(IdProviderMigrationTest.class.getName());

    private Policy policy = new Policy(PolicyType.INTERNAL, "Policy", "", false);
    private LdapIdentityProviderConfig ldap;
    private IdentityProviderConfigManager identityProviderConfigManager;
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    private PolicyManager policyManager;

    @Before
    public void before() throws Exception {

        identityProviderConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);

        //create ldap provider
        ldap = LdapIdentityProviderConfig.newLdapIdentityProviderConfig();
        ldap.setName("Test Ldap");
        ldap.setUserLookupByCertMode(LdapIdentityProviderConfig.UserLookupByCertMode.LOGIN);
        ldap.setUserCertificateUseType(LdapIdentityProviderConfig.UserCertificateUseType.NONE);
        ldap.setLdapUrl(new String[]{"ldap://smldap.l7tech.com:389"});
        ldap.setClientAuthEnabled(false);
        ldap.setSearchBase("dc=l7tech,dc=com");
        ldap.setBindDN("dc=l7tech,dc=com");
        ldap.setBindPasswd("password");
        identityProviderConfigManager.save(ldap);

        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Authentication>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+ internalProviderId +"\"/>\n" +
                        "        </L7p:Authentication>\n" +
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
        cleanPolicies();
        identityProviderConfigManager.delete(ldap);
    }

    private void cleanPolicies() throws Exception  {
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
        assertEquals("The bundle should have 2 items A policy", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 3 mapping. Root folder, a policy, an id provider", 3, bundleItem.getContent().getMappings().size());

        cleanPolicies();

        // update bundle
        bundleItem.getContent().getMappings().get(0).setTargetId(ldap.getId());

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());
        
        // check mapping

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        
        //verify the mappings
        assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping idMapping = mappings.getContent().getMappings().get(0);
        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, idMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, idMapping.getActionTaken());
        assertNotSame(idMapping.getSrcId(), idMapping.getTargetId());
        assertEquals(internalProviderId, idMapping.getSrcId());
        assertEquals(ldap.getId(), idMapping.getTargetId());

        Mapping rootFolderMapping = mappings.getContent().getMappings().get(1);
        assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
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
        assertEquals(1, dependencies.size());

        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(),dependencies.get(0).getType());
        assertEquals(ldap.getId(),dependencies.get(0).getId());
    }

    @Test
    public void migrationUseExistingTest() throws Exception {
        RestResponse response =
                getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET,null,"");
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals("The bundle should have 2 items A policy", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 3 mapping. Root folder, a policy, an id provider", 3, bundleItem.getContent().getMappings().size());

        cleanPolicies();

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        // check mapping

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        Mapping idMapping = mappings.getContent().getMappings().get(0);
        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, idMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, idMapping.getActionTaken());
        assertEquals(idMapping.getSrcId(), idMapping.getTargetId());
        assertEquals(internalProviderId, idMapping.getSrcId());

        Mapping rootFolderMapping = mappings.getContent().getMappings().get(1);
        assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());

        Mapping policyMapping = mappings.getContent().getMappings().get(2);
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
        assertEquals(1, dependencies.size());

        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(),dependencies.get(0).getType());
        assertEquals(internalProviderId,dependencies.get(0).getId());
    }

    protected String objectToString(Object object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }
}
