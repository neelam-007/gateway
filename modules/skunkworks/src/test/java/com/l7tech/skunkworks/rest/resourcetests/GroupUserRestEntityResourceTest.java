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
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GroupUserRestEntityResourceTest extends RestEntityTestBase{

    private GroupManager internalGroupManager;
    private UserManager internalUserManager;
    private IdentityProviderFactory identityProviderFactory;
    private final String internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();

    private List<User> users = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();

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
        users.add(user1);
        users.add(user2);

        // add internal group
        InternalGroup group1 =  new InternalGroup("group1");
        InternalGroup group2 =  new InternalGroup("group2");
        internalGroupManager.saveGroup(group1);
        internalGroupManager.saveGroup(group2);
        internalGroupManager.addUser(user1,group1);
        internalGroupManager.addUser(user2,group2);
        groups.add(group1);
        groups.add(group2);

    }

    @After
    public void after() throws FindException, DeleteException {
        for (Group group : groups) {
            internalGroupManager.delete(group.getId());
        }

        for (User user : users) {
            internalUserManager.delete(user);
        }
    }

    @Test
    public void UserSearchTest() throws Exception {

        RestResponse response = processRequest("identityProviders/"+internalProviderId+"/users?login=user1", HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertEquals(200, response.getStatus());
        ItemsList<UserMO> userList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        assertNotNull(userList.getContent());
        assertEquals(1, userList.getContent().size());
        assertEquals(users.get(0).getId(),userList.getContent().get(0).getId());

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
        assertEquals(groups.get(0).getId(),groupList.getContent().get(0).getId());

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
