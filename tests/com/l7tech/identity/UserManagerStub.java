package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.*;

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
     * @param dataStore the datastore to use
     */
    public UserManagerStub(StubDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public User findByPrimaryKey( String oid) throws FindException {
        return (User)dataStore.getUsers().get(new Long(oid));
    }

    public User findByLogin(String login) throws FindException {
        return null;
    }

    public void delete(User user) throws DeleteException {
        if (dataStore.getUsers().remove(new Long(user.getOid())) == null) {
            throw new DeleteException("Could not find user oid= " + user.getOid());
        }
    }

    public long save(User user) throws SaveException {
        long oid = dataStore.nextObjectId();
        user.setOid(oid);
        Long key = new Long(oid);
        if (dataStore.getUsers().get(key) != null) {
            throw new SaveException("Record exists, user oid= " + user.getOid());
        }
        dataStore.getUsers().put(key, user);
        return oid;

    }

    public void update(User user) throws UpdateException {
        Long key = new Long(user.getOid());
        if (dataStore.getUsers().get(key) == null) {
            throw new UpdateException("Record missing, user oid= " + user.getOid());
        }
        dataStore.getUsers().remove(key);
        dataStore.getUsers().put(key, user);

    }

    public void setIdentityProviderOid(long oid) {
        // not implementes
    }


    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
                dataStore.getUsers().keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromUser((User) dataStore.getUsers().get(key)));
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
                dataStore.getUsers().keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromUser((User) dataStore.getUsers().get(key)));
                count++;
            }
        }
        return list;

    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of Entity objects.
     */
    public Collection findAll() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
                dataStore.getUsers().keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(dataStore.getUsers().get(key));
        }
        return list;
    }

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection list = new ArrayList();
        int index = 0;
        int count = 0;
        for (Iterator i =
                dataStore.getUsers().keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(dataStore.getUsers().get(key));
                count++;
            }
        }
        return list;
    }

    private EntityHeader fromUser(User u) {
        return
                new EntityHeader(u.getOid(), EntityType.USER, u.getName(), null);
    }

    private StubDataStore dataStore;

}
