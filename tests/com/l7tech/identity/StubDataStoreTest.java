package com.l7tech.identity;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityHeader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashSet;
import java.util.Iterator;
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
        UserManager um = registry.getInternalUserManager();
        GroupManager gm = registry.getInternalGroupManager();
        IdentityProvider ip = registry.getInternalProvider();
        final long providerConfigOid = ip.getConfig().getOid();

        Iterator it = um.findAll().iterator();
        for (; it.hasNext();) {
            User u = (User)it.next();
            assertTrue("Expected provider " + providerConfigOid +
              " received " + u.getProviderId(), u.getProviderId() == providerConfigOid);
        }

        it = gm.findAll().iterator();
        for (; it.hasNext();) {
            Group g = (Group)it.next();
            assertTrue("Expected provider " + providerConfigOid +
              " received " + g.getProviderId(), g.getProviderId() == providerConfigOid);
        }
    }

    public void testAddAndUpdateUser() throws Exception {
        UserManager um = registry.getInternalUserManager();
        InternalUser user = new InternalUser();
        user.setLogin("mgreen");
        user.setName(user.getLogin());
        user.setFirstName("Mary");
        user.setLastName("Green");
        user.setEmail("mgreen@one.com");
        String uid = um.save(user);
        User found = um.findByPrimaryKey(uid);
        assertTrue("Expected user could not be found " + uid, found != null);

        found.getUserBean().setLastName("Red");
        um.update(found);
        User updated = um.findByPrimaryKey(uid);
        assertTrue("Expected user could not be found " + uid, updated != null);

        assertTrue("Expected updated user " + uid, "Red".equals(updated.getLastName()));

        User bylogin = um.findByLogin("mgreen");
        assertTrue("Expected user " + uid, bylogin !=null);

    }

    public void testAddAndUpdateGroup() throws Exception {
        GroupManager gm = registry.getInternalGroupManager();
        InternalGroup group = new InternalGroup();
        group.setName("26-floor");
        group.setDescription("people at 26th floor");
        String gid = gm.save(group);
        Group found = gm.findByPrimaryKey(gid);
        assertTrue("Expected group could not be found " + gid, found != null);

        found.getGroupBean().setDescription("none");
        gm.update(found);
        Group updated = gm.findByPrimaryKey(gid);
        assertTrue("Expected group could not be found " + gid, updated != null);
        assertTrue("Expected updated group " + gid, "none".equals(updated.getDescription()));
    }

    public void testAddAndUpdateUserGroups() throws Exception {
        UserManager um = registry.getInternalUserManager();
        GroupManager gm = registry.getInternalGroupManager();

        testAddAndUpdateUser();
        User user = um.findByLogin("mgreen");
        assertTrue("Expected non null user ", user !=null);

        Set newGroupHeaders = new HashSet();
        newGroupHeaders.addAll(gm.findAllHeaders());
        int allGroupsSize = newGroupHeaders.size();

        gm.setGroupHeaders(user, newGroupHeaders);
        Set headers = gm.getGroupHeaders(user);
        assertTrue("Expected number of groups "+allGroupsSize, headers.size() == allGroupsSize);

        if (allGroupsSize == 0) return;

        EntityHeader eh = (EntityHeader)gm.findAllHeaders().iterator().next();
        newGroupHeaders = new HashSet();
        newGroupHeaders.add(eh);
        gm.setGroupHeaders(user, newGroupHeaders);

        headers = gm.getGroupHeaders(user);
        assertTrue("Expected number of groups is 1", headers.size() == 1);
    }


    /**
     * Test <code>StubDataStoreTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
