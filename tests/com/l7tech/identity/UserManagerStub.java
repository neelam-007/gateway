package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.internal.InternalUser;

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
         for (Iterator i = dataStore.getUsers().values().iterator(); i.hasNext();) {
             User u = (User)i.next();
             if (login.equals(u.getLogin())) {
                 return u;
             }
        }
        return null;
    }

    public void delete(User user) throws DeleteException {
        InternalUser imp = (InternalUser)user;
        if (dataStore.getUsers().remove(new Long(imp.getOid())) == null) {
            throw new DeleteException("Could not find user oid= " + imp.getOid());
        }
    }

    public void delete(String identifier) throws DeleteException {
        InternalUser imp = new InternalUser();
        imp.setOid( Long.valueOf( identifier ).longValue() );
        delete( imp );
    }

    public String save(User user) throws SaveException {
        InternalUser imp = (InternalUser)user;
        long oid = dataStore.nextObjectId();
        imp.setOid(oid);
        Long key = new Long(oid);
        if (dataStore.getUsers().get(key) != null) {
            throw new SaveException("Record exists, user oid= " + imp.getOid());
        }
        dataStore.getUsers().put(key, user);
        return new Long( oid ).toString();

    }

    public void update(User user) throws UpdateException {
        InternalUser imp = (InternalUser)user;
        Long key = new Long(imp.getOid());
        if (dataStore.getUsers().get(key) == null) {
            throw new UpdateException("Record missing, user oid= " + imp.getOid());
        }
        dataStore.getUsers().remove(key);
        dataStore.getUsers().put(key, user);

    }

    public String save(User user, Set groupHeaders) throws SaveException {
        return save( user, null );
    }

    public void update(User user, Set groupHeaders) throws UpdateException {
        update( user, null );
    }

    public EntityHeader userToHeader(User user) {
        return null;
    }

    public User headerToUser(EntityHeader header) {
        return null;
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
        InternalUser imp = (InternalUser)u;
        return
                new EntityHeader(imp.getOid(), EntityType.USER, u.getLogin(), null);
    }

    private StubDataStore dataStore;

}
