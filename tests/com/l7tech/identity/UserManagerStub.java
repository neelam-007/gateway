package com.l7tech.identity;

import com.l7tech.console.util.Registry;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Test stub for user  manager. A <code>Map</code> backed user
 * manager for easier testing.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class UserManagerStub implements UserManager {
    /**
     * initialize the user manager with the data store
     * 
     * @param dataStore the datastore to use
     */
    public UserManagerStub(StubDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public User findByPrimaryKey(String oid) throws FindException {
        return (User)dataStore.getUsers().get(oid);
    }

    public User findByLogin(String login) throws FindException {
        for (Iterator i = dataStore.getUsers().values().iterator(); i.hasNext();) {
            User u = (User)i.next();
            if (login.equals(u.getLogin())) {
                return u;
            }
        }
        return null;
    }

    public void delete(User user) throws DeleteException, ObjectNotFoundException {
        InternalUser imp = (InternalUser)user;
        if (dataStore.getUsers().remove(imp.getUniqueIdentifier()) == null) {
            throw new ObjectNotFoundException("Could not find user oid= " + imp.getOid());
        }
    }

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        InternalUser imp = new InternalUser();
        imp.setOid(Long.valueOf(identifier).longValue());
        delete(imp);
    }

    public String save(User user) throws SaveException {
        if (!(user instanceof UserBean)) {
            throw new IllegalArgumentException("Expected " + UserBean.class);
        }
        InternalUser imp = new InternalUser((UserBean)user);
        long oid = dataStore.nextObjectId();
        imp.setOid(oid);
        final String uniqueIdentifier = imp.getUniqueIdentifier();
        if (dataStore.getUsers().get(uniqueIdentifier) != null) {
            throw new SaveException("Record exists, user oid= " + uniqueIdentifier);
        }
        dataStore.getUsers().put(uniqueIdentifier, imp);
        return uniqueIdentifier;

    }

    public void update(User user) throws UpdateException, ObjectNotFoundException {
        String key = user.getUniqueIdentifier();
        InternalUser iu = (InternalUser)dataStore.getUsers().get(key);
        if (iu == null) {
            throw new ObjectNotFoundException("Record missing, user oid= " + key);
        }
        iu.copyFrom(user);
    }

    public String save(User user, Set groupHeaders) throws SaveException {
        IdentityProvider ip = Registry.getDefault().getIdentityProvider(user.getProviderId());
        if (ip == null) {
            throw new SaveException("Could not obtain provider " + user.getProviderId());
        }
        GroupManager gman = ip.getGroupManager();
        if (gman == null) {
            throw new RuntimeException("Could not obtain the group manager service");
        }
        String uid = save(user);
        try {
            gman.setGroupHeaders(uid, groupHeaders);
            return uid;
        } catch (FindException e) {
            throw new SaveException("Error saving groups for uid " + uid, e);
        } catch (UpdateException e) {
            throw new SaveException("Error saving groups for uid " + uid, e);
        }
    }

    public void update(User user, Set groupHeaders) throws UpdateException, ObjectNotFoundException {
        IdentityProvider ip = Registry.getDefault().getIdentityProvider(user.getProviderId());
        if (ip == null) {
            throw new ObjectNotFoundException("Could not obtain provider " + user.getProviderId());
        }
        update(user);
        GroupManager gman = ip.getGroupManager();
        if (gman == null) {
            throw new RuntimeException("Could not obtain the group manager service");
        }
        final String uid = user.getUniqueIdentifier();
        try {
            gman.setGroupHeaders(uid, groupHeaders);
        } catch (FindException e) {
            throw new UpdateException("Error saving groups for uid " + uid, e);
        }
    }

    public Class getImpClass() {
        return InternalUser.class;
    }


    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
     * 
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
          dataStore.getUsers().keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            list.add(fromUser((User)dataStore.getUsers().get(key)));
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * 
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
          dataStore.getUsers().keySet().iterator(); i.hasNext(); index++) {
            Object key = i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromUser((User)dataStore.getUsers().get(key)));
                count++;
            }
        }
        return list;

    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for all instances of the entity class corresponding to this Manager.
     * 
     * @return A <code>Collection</code> of Entity objects.
     */
    public Collection findAll() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
          dataStore.getUsers().keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            list.add(dataStore.getUsers().get(key));
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * 
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
          dataStore.getUsers().keySet().iterator(); i.hasNext(); index++) {
            Object key = i.next();

            if (index >= offset && count <= windowSize) {
                list.add(dataStore.getUsers().get(key));
                count++;
            }
        }
        return list;
    }

    private EntityHeader fromUser(User u) {
        InternalUser imp = (InternalUser)u;
        return new EntityHeader(imp.getOid(), EntityType.USER, u.getLogin(), null);
    }

    private StubDataStore dataStore;

}
