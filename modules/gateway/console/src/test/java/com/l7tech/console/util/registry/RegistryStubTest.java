package com.l7tech.console.util.registry;

import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class <code>StubDataStoreTest</code> tests the stub data store and
 * stub managers.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RegistryStubTest {
    private RegistryStub registry;

    @Before
    public void setUp() throws Exception {
        //StubDataStore.recycle();
        registry = new RegistryStub();
    }

    @Test
    public void testIntegrity() throws Exception {
        IdentityAdmin admin = registry.getIdentityAdmin();
        IdentityProviderConfig ipc = registry.getInternalProviderConfig();
        final Goid providerConfigOid = ipc.getGoid();

        EntityHeaderSet<IdentityHeader> headers = admin.findAllUsers(providerConfigOid);
        for (EntityHeader header : headers) {
            User u = admin.findUserByID(providerConfigOid, header.getStrId());
            assertTrue("Expected provider " + providerConfigOid +
              " received " + u.getProviderId(), u.getProviderId().equals(providerConfigOid));
        }

        headers = admin.findAllGroups(providerConfigOid);
        for (EntityHeader header : headers) {
            Group g = admin.findGroupByID(providerConfigOid, header.getStrId());
            assertTrue("Expected provider " + providerConfigOid +
              " received " + g.getProviderId(), g.getProviderId().equals(providerConfigOid));
        }
    }

    @Test
    public void testAddAndUpdateUser() throws Exception {
        Goid provider = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;
        IdentityAdmin admin = registry.getIdentityAdmin();
        InternalUser user = new InternalUser();
        user.setLogin("mgreen");
        user.setName(user.getLogin());
        user.setFirstName("Mary");
        user.setLastName("Green");
        user.setEmail("mgreen@one.com");
        String uid = admin.saveUser(provider, user, null);
        InternalUser found = (InternalUser) admin.findUserByID(provider, uid);
        assertTrue("Expected user could not be found " + uid, found != null);

        found.setLastName("Red");
        admin.saveUser(provider, found, null);
        InternalUser updated = (InternalUser) admin.findUserByID(provider, uid);
        assertTrue("Expected user could not be found " + uid, updated != null);

        assertTrue("Expected updated user " + uid, "Red".equals(updated.getLastName()));

        User bylogin = admin.findUserByLogin(provider, "mgreen");
        assertTrue("Expected user " + uid, bylogin !=null);

    }

    @Test
    public void testAddAndUpdateGroup() throws Exception {
        Goid provider = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;
        IdentityAdmin admin = registry.getIdentityAdmin();
        InternalGroup group = new InternalGroup();
        group.setName("26-floor");
        group.setDescription("people at 26th floor");
        String gid = admin.saveGroup(provider, group, null);
        InternalGroup found = (InternalGroup) admin.findGroupByID(provider, gid);
        assertTrue("Expected group could not be found " + gid, found != null);

        found.setDescription("none");
        admin.saveGroup(provider, found, null);
        Group updated = admin.findGroupByID(provider, gid);
        assertTrue("Expected group could not be found " + gid, updated != null);
        assertTrue("Expected updated group " + gid, "none".equals(updated.getDescription()));
    }

    @Test
    public void testAddAndUpdateUserGroups() throws Exception {
        Goid provider = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;
        IdentityAdmin admin = registry.getIdentityAdmin();

        testAddAndUpdateUser();
        User user = admin.findUserByLogin(provider, "mgreen");
        assertTrue("Expected non null user ", user !=null);

        Set<IdentityHeader> newGroupHeaders = new HashSet<IdentityHeader>();
        newGroupHeaders.addAll(admin.findAllGroups(provider));
        int allGroupsSize = newGroupHeaders.size();

        admin.saveUser(provider, user, newGroupHeaders);
        Set headers = admin.getGroupHeaders(provider, user.getId());
        assertTrue("Expected number of groups "+allGroupsSize, headers.size() == allGroupsSize);

        if (allGroupsSize == 0) return;

        IdentityHeader eh = admin.findAllGroups(provider).iterator().next();
        newGroupHeaders = new HashSet<IdentityHeader>();
        newGroupHeaders.add(eh);
        admin.saveUser(provider, user, newGroupHeaders);

        headers = admin.getGroupHeaders(provider, user.getId());
        assertTrue("Expected number of groups is 1", headers.size() == 1);
    }
}
