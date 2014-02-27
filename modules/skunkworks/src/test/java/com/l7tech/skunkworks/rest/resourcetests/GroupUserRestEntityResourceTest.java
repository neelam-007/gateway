package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Charsets;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GroupUserRestEntityResourceTest extends RestEntityTestBase{
    private static final Logger logger = Logger.getLogger(GroupUserRestEntityResourceTest.class.getName());

    private GroupManager internalGroupManager;
    private UserManager internalUserManager;
    private IdentityProviderFactory identityProviderFactory;
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();

    private List<String> usersToCleanup = new ArrayList<>();
    private List<String> groupsToCleanup = new ArrayList<>();

    @Before
    public void before() throws Exception {
        identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);

        internalGroupManager = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID).getGroupManager();
        internalUserManager = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID).getUserManager();
        PasswordHasher passwordHasher = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("passwordHasher", PasswordHasher.class);

        // add users
        InternalUser user1 = new InternalUser("user1");
        user1.setHashedPassword(passwordHasher.hashPassword("password".getBytes(Charsets.UTF8)));
        InternalUser user2 = new InternalUser("user2");
        user2.setHashedPassword(passwordHasher.hashPassword("password2".getBytes(Charsets.UTF8)));
        internalUserManager.save(user1,null);
        internalUserManager.save(user2,null);
        usersToCleanup.add(user1.getId());
        usersToCleanup.add(user2.getId());

        // add internal group
        InternalGroup group1 =  new InternalGroup("group1");
        InternalGroup group2 =  new InternalGroup("group2");
        internalGroupManager.saveGroup(group1);
        internalGroupManager.saveGroup(group2);
        internalGroupManager.addUser(user1, group1);
        internalGroupManager.addUser(user2, group2);
        groupsToCleanup.add(group1.getId());
        groupsToCleanup.add(group2.getId());

    }

    @After
    public void after() throws FindException, DeleteException {
        for (String group : groupsToCleanup) {
            internalGroupManager.delete(group);
        }

        for (String user : usersToCleanup) {
            internalUserManager.delete(internalUserManager.findByPrimaryKey(user));
        }
    }

    protected String writeMOToString(ManagedObject mo) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ManagedObjectFactory.write(mo, bout);
        return bout.toString();
    }

    @Test
    public void UserCreateTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        userMO.setPassword("12!@qwQW");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");

        String userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(201, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", userMO.getLogin(), item.getName());
        assertEquals(EntityType.USER.toString(), item.getType());

        String userId = item.getId();
        usersToCleanup.add(userId);

        User user = internalUserManager.findByPrimaryKey(userId);

        assertNotNull(user);
        assertEquals("User Name:", userMO.getLogin(), user.getName());
        assertEquals("User Login:", userMO.getLogin(), user.getLogin());
        assertEquals("User First name:", userMO.getFirstName(), user.getFirstName());
        assertEquals("User last name:", userMO.getLastName(), user.getLastName());
    }

    @Test
    public void UserCreatePasswordFailTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        userMO.setPassword("12");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");

        String userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(403, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceAccess",error.getType());
        assertEquals("Unable to create user. Caused by: Password must be at least 8 characters in length", error.getDetail());
    }

    @Test
    public void UserUpdateTest() throws Exception {

        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(usersToCleanup.get(0));
        userMO.setProviderId(internalProviderId);
        userMO.setLogin("login");
        userMO.setFirstName("first name");
        userMO.setLastName("last name");

        String userMOString = writeMOToString(userMO);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userMO.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), userMOString);
        assertEquals(200, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", userMO.getLogin(), item.getName());
        assertEquals("User id:", userMO.getId(), item.getId());
        assertEquals(EntityType.USER.toString(), item.getType());

        User user = internalUserManager.findByPrimaryKey(userMO.getId());

        assertNotNull(user);
        assertEquals("User Name:", userMO.getLogin(), user.getName());
        assertEquals("User Login:", userMO.getLogin(), user.getLogin());
        assertEquals("User First name:", userMO.getFirstName(), user.getFirstName());
        assertEquals("User last name:", userMO.getLastName(), user.getLastName());
    }

    @Test
    public void UserChangePasswordFailTest() throws Exception {

        String userId = usersToCleanup.get(0);
        String simplePassword = "12";

        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/changePassword", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), simplePassword);
        assertEquals(403, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceAccess",error.getType());
        assertEquals("Unable to change user password. Caused by: Password must be at least 8 characters in length", error.getDetail());
    }


    @Test
    public void UserChangePasswordTest() throws Exception {

        String userId = usersToCleanup.get(0);
        String password = "34#$erER";

        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId + "/changePassword", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), password);
        assertEquals(200, response.getStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<UserMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertEquals("User Name:", "user1", item.getName());
        assertEquals("User id:", userId, item.getId());
        assertEquals(EntityType.USER.toString(), item.getType());
    }

    @Test
    public void UserDeleteTest() throws Exception {

        String userId = usersToCleanup.get(0);
        RestResponse response = processRequest("identityProviders/" + internalProviderId + "/users/" + userId, HttpMethod.DELETE, null, "");
        assertEquals(204, response.getStatus());

        usersToCleanup.remove(userId);

        // check entity
        Assert.assertNull(internalUserManager.findByPrimaryKey(userId));
    }

    @Test
    public void UserSearchTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/users?login=user1", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<UserMO> userList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(userList.getContent());
        assertEquals(1, userList.getContent().size());
        assertEquals(usersToCleanup.get(0),userList.getContent().get(0).getId());

    }

    @Test
    public void UserListTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/users", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<UserMO> userList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(userList.getContent());
        assertEquals(3, userList.getContent().size());

    }


    @Test
    public void GroupSearchTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/groups?name=group1", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<GroupMO> groupList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(groupList.getContent());
        assertEquals(1, groupList.getContent().size());
        assertEquals(groupsToCleanup.get(0),groupList.getContent().get(0).getId());

    }

    @Test
    public void GroupListTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/groups", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<GroupMO> groupList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(groupList.getContent());
        assertEquals(2, groupList.getContent().size());

    }
}
