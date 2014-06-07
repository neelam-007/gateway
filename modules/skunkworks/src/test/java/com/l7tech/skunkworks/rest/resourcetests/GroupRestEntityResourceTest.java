package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.GroupMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GroupRestEntityResourceTest extends RestEntityTests<Group, GroupMO> {

    private static final Logger logger = Logger.getLogger(RestEntityTests.class.getName());

    private GroupManager internalGroupManager;
    private IdentityProvider internalIdentityProvider;
    private IdentityProviderFactory identityProviderFactory;
    private IdentityProviderConfigManager idConfigManager;
    private String otherIdentityProviderId;
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();
    private List<Group> groups = new ArrayList<>();

    @Before
    public void before() throws Exception {
        identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);
        idConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        internalIdentityProvider = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        internalGroupManager = internalIdentityProvider.getGroupManager();

        //get admin user
        final User adminUser = internalIdentityProvider.getUserManager().findByLogin("admin");

        //Create groups
        InternalGroup group = new InternalGroup("Group1");
        group.setDescription("Description1");
        internalGroupManager.saveGroup(group);
        internalGroupManager.addUser(adminUser,group);
        groups.add(group);

        group = new InternalGroup("Group2");
        group.setDescription("Description2");
        internalGroupManager.saveGroup(group);
        internalGroupManager.addUser(adminUser,group);
        groups.add(group);

        group = new InternalGroup("Group3");
        group.setDescription("Description3");
        internalGroupManager.saveGroup(group);
        internalGroupManager.addUser(adminUser,group);
        groups.add(group);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<IdentityHeader> all = internalGroupManager.findAllHeaders();
        for (IdentityHeader user : all) {
            internalGroupManager.delete(user.getStrId());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(groups, new Functions.Unary<String, Group>() {
            @Override
            public String call(Group group) {
                return group.getId();
            }
        });
    }

    @Override
    public List<GroupMO> getCreatableManagedObjects() {
        return Collections.emptyList();
    }

    @Override
    public List<GroupMO> getUpdateableManagedObjects() {
        return Collections.emptyList();
    }

    @Override
    public Map<GroupMO, Functions.BinaryVoid<GroupMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<GroupMO, Functions.BinaryVoid<GroupMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setName("Doomed Group");

        builder.put(groupMO, new Functions.BinaryVoid<GroupMO, RestResponse>() {
            @Override
            public void call(GroupMO groupMOMO, RestResponse restResponse) {
                Assert.assertEquals(405, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<GroupMO, Functions.BinaryVoid<GroupMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<GroupMO, Functions.BinaryVoid<GroupMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setId(groups.get(0).getId());
        groupMO.setName(groups.get(1).getName());

        builder.put(groupMO, new Functions.BinaryVoid<GroupMO, RestResponse>() {
            @Override
            public void call(GroupMO groupMOMO, RestResponse restResponse) {
                Assert.assertEquals(405, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("asdf"+getGoid().toString(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals("Expected successful response", 404, restResponse.getStatus());
            }
        });
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Collections.emptyList();
    }

    @Override
    public void testDeleteNoExistingEntity() throws Exception {
        Goid anyID = getGoid();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + anyID.toString(), HttpMethod.DELETE, null, "");
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected method not allowed", 405, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
    }

    @Override
    public String getResourceUri() {
        return "identityProviders/"+internalProviderId+"/groups";
    }

    @Override
    public String getType() {
        return EntityType.GROUP.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        Group entity = internalGroupManager.findByPrimaryKey(id);
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        Group entity = internalGroupManager.findByPrimaryKey(id);
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, GroupMO managedObject) throws FindException {
        Group entity = internalGroupManager.findByPrimaryKey(id);
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getDescription(), managedObject.getDescription());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(groups, new Functions.Unary<String, Group>() {
                    @Override
                    public String call(Group group) {
                        return group.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(groups.get(0).getName()), Arrays.asList(groups.get(0).getId()))
                .put("name=" + URLEncoder.encode(groups.get(0).getName()) + "&name=" + URLEncoder.encode(groups.get(1).getName()), Functions.map(groups.subList(0, 2), new Functions.Unary<String, Group>() {
                    @Override
                    public String call(Group group) {
                        return group.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("name=" + URLEncoder.encode(groups.get(0).getName()) + "&name=" + URLEncoder.encode(groups.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(groups.get(1).getId(), groups.get(0).getId()))
                .put("sort=id&order=desc", Arrays.asList(groups.get(2).getId(),groups.get(1).getId(), groups.get(0).getId()))
                .map();
    }

    @Override
    protected boolean getDefaultListIsOrdered() {
        return false;
    }
}
