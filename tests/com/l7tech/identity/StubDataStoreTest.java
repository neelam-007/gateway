package com.l7tech.identity;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.EntityHeader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class <code>StubDataStoreTest</code> tests the stub data store and
 * stub managers.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class StubDataStoreTest extends TestCase {
    private Registry registry;

    /**
     * test <code>StubDataStoreTest</code> constructor
     */
    public StubDataStoreTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * StubDataStoreTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(StubDataStoreTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        StubDataStore.recycle();
        registry = new RegistryStub();
    }

    public void tearDown() throws Exception {
        StubDataStore.recycle();
        registry = null;
    }

    public void testIntegrity() throws Exception {
        IdentityAdmin admin = registry.getIdentityAdmin();
        IdentityProviderConfig ipc = registry.getInternalProviderConfig();
        final long providerConfigOid = ipc.getOid();

        EntityHeader[] headers = admin.findAllUsers(providerConfigOid);
        for ( int i = 0; i < headers.length; i++ ) {
            EntityHeader header = headers[i];
            User u = admin.findUserByID(providerConfigOid, header.getStrId());
            assertTrue("Expected provider " + providerConfigOid +
              " received " + u.getProviderId(), u.getProviderId() == providerConfigOid);
        }

        headers = admin.findAllGroups(providerConfigOid);
        for ( int j = 0; j < headers.length; j++ ) {
            EntityHeader header = headers[j];
            Group g = admin.findGroupByID(providerConfigOid, header.getStrId());
            assertTrue("Expected provider " + providerConfigOid +
              " received " + g.getProviderId(), g.getProviderId() == providerConfigOid);
        }
    }

    public void testAddAndUpdateUser() throws Exception {
        long provider = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
        IdentityAdmin admin = registry.getIdentityAdmin();
        UserBean user = new UserBean();
        user.setLogin("mgreen");
        user.setName(user.getLogin());
        user.setFirstName("Mary");
        user.setLastName("Green");
        user.setEmail("mgreen@one.com");
        String uid = admin.saveUser(provider, user, null);
        User found = admin.findUserByID(provider, uid);
        assertTrue("Expected user could not be found " + uid, found != null);

        found.getUserBean().setLastName("Red");
        admin.saveUser(provider, found, null);
        User updated = admin.findUserByID(provider, uid);
        assertTrue("Expected user could not be found " + uid, updated != null);

        assertTrue("Expected updated user " + uid, "Red".equals(updated.getLastName()));

        User bylogin = admin.findUserByLogin(provider, "mgreen");
        assertTrue("Expected user " + uid, bylogin !=null);

    }

    public void testAddAndUpdateGroup() throws Exception {
        long provider = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
        IdentityAdmin admin = registry.getIdentityAdmin();
        InternalGroup group = new InternalGroup();
        group.setName("26-floor");
        group.setDescription("people at 26th floor");
        String gid = admin.saveGroup(provider, group, null);
        Group found = admin.findGroupByID(provider, gid);
        assertTrue("Expected group could not be found " + gid, found != null);

        found.getGroupBean().setDescription("none");
        admin.saveGroup(provider, found, null);
        Group updated = admin.findGroupByID(provider, gid);
        assertTrue("Expected group could not be found " + gid, updated != null);
        assertTrue("Expected updated group " + gid, "none".equals(updated.getDescription()));
    }

    public void testAddAndUpdateUserGroups() throws Exception {
        long provider = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
        IdentityAdmin admin = registry.getIdentityAdmin();

        testAddAndUpdateUser();
        User user = admin.findUserByLogin(provider, "mgreen");
        assertTrue("Expected non null user ", user !=null);

        Set newGroupHeaders = new HashSet();
        newGroupHeaders.addAll(Arrays.asList(admin.findAllGroups(provider)));
        int allGroupsSize = newGroupHeaders.size();

        admin.saveUser(provider, user, newGroupHeaders);
        Set headers = admin.getGroupHeaders(provider, user.getUniqueIdentifier());
        assertTrue("Expected number of groups "+allGroupsSize, headers.size() == allGroupsSize);

        if (allGroupsSize == 0) return;

        EntityHeader eh = (EntityHeader)admin.findAllGroups(provider)[0];
        newGroupHeaders = new HashSet();
        newGroupHeaders.add(eh);
        admin.saveUser(provider, user, newGroupHeaders);

        headers = admin.getGroupHeaders(provider, user.getUniqueIdentifier());
        assertTrue("Expected number of groups is 1", headers.size() == 1);
    }


    /**
     * Test <code>StubDataStoreTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
