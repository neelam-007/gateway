package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.identity.IdentityProviderFactory;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GroupsMigrationTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(GroupsMigrationTest.class.getName());

    private Policy policy = new Policy(PolicyType.INTERNAL, "Policy", "", false);
    private IdentityProviderConfigManager identityProviderConfigManager;
    private IdentityProviderFactory identityProviderFactory;
    private GroupManager internalGroupManager;
    private UserManager internalUserManager;
    private InternalGroup internalGroup =  new InternalGroup("Source Group");
    private final Goid internalIdProvider = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;

    private PolicyManager policyManager;

    @Before
    public void before() throws Exception {

        identityProviderConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);

        // create internal user

        internalGroupManager = identityProviderFactory.getProvider(internalIdProvider).getGroupManager();
        internalUserManager = identityProviderFactory.getProvider(internalIdProvider).getUserManager();
        internalGroupManager.saveGroup(internalGroup);
        internalGroupManager.addUser(internalUserManager.findByLogin("admin"),internalGroup);

        // create policy
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:MemberOfGroup>\n" +
                        "            <L7p:GroupId stringValue=\""+internalGroup.getId()+"\"/>\n" +
                        "            <L7p:GroupName stringValue=\""+internalGroup.getName()+"\"/>\n" +
                        "            <L7p:IdentityProviderOid goidValue=\""+internalIdProvider.toString()+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:MemberOfGroup>" +
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

        Collection<IdentityHeader> headers = internalGroupManager.findAllHeaders();
        for (IdentityHeader header : headers) {
            internalGroupManager.delete(header.getStrId());
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
        assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 4 mappings. Root folder, a policy, a group, a id provider", 4, bundleItem.getContent().getMappings().size());

        cleanDatabase();

        // create new group
        InternalGroup targetGroup =  new InternalGroup("Target Group");
        internalGroupManager.saveGroup(targetGroup);
        internalGroupManager.addUser(internalUserManager.findByLogin("admin"), targetGroup);

        // update bundle
        assertEquals(EntityType.GROUP.toString(), bundleItem.getContent().getMappings().get(1).getType());
        bundleItem.getContent().getMappings().get(1).setTargetId(targetGroup.getId());

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        // check mapping

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
        assertEquals(internalIdProvider.toString(), idProviderMapping.getSrcId());
        assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

        Mapping groupMapping = mappings.getContent().getMappings().get(1);
        assertEquals(EntityType.GROUP.toString(), groupMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, groupMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, groupMapping.getActionTaken());
        assertNotSame(groupMapping.getSrcId(), groupMapping.getTargetId());
        assertEquals(internalGroup.getId(), groupMapping.getSrcId());
        assertEquals(targetGroup.getId(), groupMapping.getTargetId());


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
        assertEquals(3, dependencies.size());

        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(),dependencies.get(0).getType());
        assertEquals(EntityType.GROUP.toString(),dependencies.get(1).getType());
        assertEquals(targetGroup.getId(),dependencies.get(1).getId());
        assertEquals(EntityType.POLICY.toString(),dependencies.get(2).getType());
    }

    @Test
    public void migrationMapByNameTest() throws Exception {
        RestResponse response =
                getDatabaseBasedRestManagementEnvironment().processRequest("bundle/policy/" + policy.getId(), HttpMethod.GET,null,"");
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        assertEquals("The bundle should have 1 item. A policy", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 4 mappings. Root folder, a policy, a group, a id provider", 4, bundleItem.getContent().getMappings().size());

        cleanDatabase();

        // create new group
        InternalGroup targetGroup =  new InternalGroup("Target Group");
        internalGroupManager.saveGroup(targetGroup);
        internalGroupManager.addUser(internalUserManager.findByLogin("admin"), targetGroup);

        // update bundle
        assertEquals(EntityType.GROUP.toString(), bundleItem.getContent().getMappings().get(1).getType());
        bundleItem.getContent().getMappings().get(1).getProperties().put("MapBy", "name");
        bundleItem.getContent().getMappings().get(1).getProperties().put("MapTo", targetGroup.getName());

        // import bundle
        response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals(200, response.getStatus());

        // check mapping

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping idProviderMapping = mappings.getContent().getMappings().get(0);
        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(), idProviderMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, idProviderMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, idProviderMapping.getActionTaken());
        assertEquals(internalIdProvider.toString(), idProviderMapping.getSrcId());
        assertEquals(idProviderMapping.getSrcId(), idProviderMapping.getTargetId());

        Mapping groupMapping = mappings.getContent().getMappings().get(1);
        assertEquals(EntityType.GROUP.toString(), groupMapping.getType());
        assertEquals(Mapping.Action.NewOrExisting, groupMapping.getAction());
        assertEquals(Mapping.ActionTaken.UsedExisting, groupMapping.getActionTaken());
        assertNotSame(groupMapping.getSrcId(), groupMapping.getTargetId());
        assertEquals(internalGroup.getId(), groupMapping.getSrcId());
        assertEquals(targetGroup.getId(), groupMapping.getTargetId());


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
        assertEquals(3, dependencies.size());

        assertEquals(EntityType.ID_PROVIDER_CONFIG.toString(),dependencies.get(0).getType());
        assertEquals(EntityType.GROUP.toString(),dependencies.get(1).getType());
        assertEquals(targetGroup.getId(),dependencies.get(1).getId());
        assertEquals(EntityType.POLICY.toString(),dependencies.get(2).getType());
    }

    protected String objectToString(Object object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }
}
