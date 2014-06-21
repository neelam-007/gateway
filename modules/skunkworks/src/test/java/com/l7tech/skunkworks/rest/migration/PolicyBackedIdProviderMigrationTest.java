package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
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
public class PolicyBackedIdProviderMigrationTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(PolicyBackedIdProviderMigrationTest.class.getName());

    private Policy policy = new Policy(PolicyType.INTERNAL, "Policy", "", false);
    private IdentityProviderConfigManager identityProviderConfigManager;
    private PolicyManager policyManager;
    private RoleManager roleManager;
    private Policy policyBackedIdentityProviderPolicy;
    private Role defaultPolicyBackedIdentityProviderRole;
    private PolicyBackedIdentityProviderConfig policyBackedIdentityProviderConfig;
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();


    @Before
    public void before() throws Exception {

        identityProviderConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        roleManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("roleManager", RoleManager.class);

        //create a policy for policyBack id provider.
        policyBackedIdentityProviderPolicy = new Policy(PolicyType.IDENTITY_PROVIDER_POLICY, "PolicyBackedIdProviderPolicy", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>", false);
        policyBackedIdentityProviderPolicy.setGuid(UUID.randomUUID().toString());
        policyManager.save(policyBackedIdentityProviderPolicy);

        //create role
        defaultPolicyBackedIdentityProviderRole = new Role();
        defaultPolicyBackedIdentityProviderRole.setName("IdProviderDefaultRole");
        roleManager.save(defaultPolicyBackedIdentityProviderRole);

        //create policy backed id provider provider
        policyBackedIdentityProviderConfig = new PolicyBackedIdentityProviderConfig();
        policyBackedIdentityProviderConfig.setName("PolicyBackedIdentityProvider");
        policyBackedIdentityProviderConfig.setDefaultRoleId(defaultPolicyBackedIdentityProviderRole.getGoid());
        policyBackedIdentityProviderConfig.setPolicyId(policyBackedIdentityProviderPolicy.getGoid());

        identityProviderConfigManager.save(policyBackedIdentityProviderConfig);

        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Authentication>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+ policyBackedIdentityProviderConfig.getId() +"\"/>\n" +
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
        identityProviderConfigManager.delete(policyBackedIdentityProviderConfig);
        cleanPolicies();
        roleManager.delete(defaultPolicyBackedIdentityProviderRole);
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
        assertEquals("The bundle should have 4 items two policies, identity provider, and role", 4, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 5 mapping. Root folder, 2 policies, an id provider, and a role", 5, bundleItem.getContent().getMappings().size());

        cleanPolicies();

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());
        
        // check mapping
        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        
        //verify the mappings
        assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), Folder.ROOT_FOLDER_ID.toString());
        assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());

        Mapping idPolicyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyBackedIdentityProviderPolicy.getId());
        assertEquals(EntityType.POLICY.toString(), idPolicyMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, idPolicyMapping.getAction());
        assertEquals(Mapping.ActionTaken.CreatedNew, idPolicyMapping.getActionTaken());
        assertEquals(policyBackedIdentityProviderPolicy.getId(), idPolicyMapping.getSrcId());
        assertEquals(idPolicyMapping.getSrcId(), idPolicyMapping.getTargetId());

        Mapping roleMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), defaultPolicyBackedIdentityProviderRole.getId());
        assertEquals(EntityType.RBAC_ROLE.toString(), roleMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, roleMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, roleMapping.getActionTaken());
        assertEquals(defaultPolicyBackedIdentityProviderRole.getId(), roleMapping.getSrcId());
        assertEquals(roleMapping.getSrcId(), roleMapping.getTargetId());

        Mapping idMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyBackedIdentityProviderConfig.getId());
        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, idMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, idMapping.getActionTaken());
        assertNotSame(idMapping.getSrcId(), idMapping.getTargetId());
        assertEquals(policyBackedIdentityProviderConfig.getId(), idMapping.getSrcId());
        assertEquals(policyBackedIdentityProviderConfig.getId(), idMapping.getTargetId());

        Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policy.getId());
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
        assertEquals(3, dependencies.size());

        assertEquals(EntityType.POLICY.toString(),dependencies.get(0).getType());
        assertEquals(policyBackedIdentityProviderPolicy.getId(),dependencies.get(0).getId());

        assertEquals(EntityType.RBAC_ROLE.toString(),dependencies.get(1).getType());
        assertEquals(defaultPolicyBackedIdentityProviderRole.getId(),dependencies.get(1).getId());

        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(),dependencies.get(2).getType());
        assertEquals(policyBackedIdentityProviderConfig.getId(),dependencies.get(2).getId());
    }

    @Test
    public void migrationMapToInternalIgnoreRoleTest() throws Exception {
        RestResponse response =
                getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET,null,"");
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals("The bundle should have 4 items two policies, identity provider, and role", 4, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 5 mapping. Root folder, 2 policies, an id provider, and a role", 5, bundleItem.getContent().getMappings().size());

        cleanPolicies();

        //map to internal, ignore role and policy
        bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.Ignore);
        bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.Ignore);
        bundleItem.getContent().getMappings().get(3).setTargetId(internalProviderId);

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        // check mapping

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        assertEquals("There should be 5 mappings after the import", 5, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), Folder.ROOT_FOLDER_ID.toString());
        assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());

        Mapping idPolicyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyBackedIdentityProviderPolicy.getId());
        assertEquals(EntityType.POLICY.toString(), idPolicyMapping.getType());
        assertEquals(Mapping.Action.Ignore, idPolicyMapping.getAction());
        assertEquals(Mapping.ActionTaken.Ignored, idPolicyMapping.getActionTaken());
        assertEquals(policyBackedIdentityProviderPolicy.getId(), idPolicyMapping.getSrcId());

        Mapping roleMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), defaultPolicyBackedIdentityProviderRole.getId());
        assertEquals(EntityType.RBAC_ROLE.toString(), roleMapping.getType());
        assertEquals(Mapping.Action.Ignore, roleMapping.getAction());
        assertEquals(Mapping.ActionTaken.Ignored, roleMapping.getActionTaken());
        assertEquals(defaultPolicyBackedIdentityProviderRole.getId(), roleMapping.getSrcId());

        Mapping idMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policyBackedIdentityProviderConfig.getId());
        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, idMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, idMapping.getActionTaken());
        assertNotSame(idMapping.getSrcId(), idMapping.getTargetId());
        assertEquals(policyBackedIdentityProviderConfig.getId(), idMapping.getSrcId());
        assertEquals(internalProviderId, idMapping.getTargetId());

        Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), policy.getId());
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
