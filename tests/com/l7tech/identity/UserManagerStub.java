package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.EntityHeaderImp;

import java.util.*;

/**
 * Test stub for user  manager. A <code>Map</code> backed user
 * manager for easier testing.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class UserManagerStub implements UserManager {

    public UserManagerStub() {
        initialize();
    }

    public User findByPrimaryKey(long oid) throws FindException {
        return (User) users.get(new Long(oid));
    }

    public void delete(User user) throws DeleteException {
        if (users.remove(new Long(user.getOid())) == null) {
            throw new DeleteException("Could not find user oid= " + user.getOid());
        }
    }

    public long save(User user) throws SaveException {
        long oid = nextSequence();
        user.setOid(oid);
        Long key = new Long(oid);
        if (users.get(key) != null) {
            throw new SaveException("Record exists, user oid= " + user.getOid());
        }
        users.put(key, user);
        return oid;

    }

    public void update(User user) throws UpdateException {
        Long key = new Long(user.getOid());
        if (users.get(key) == null) {
            throw new UpdateException("Record missing, user oid= " + user.getOid());
        }
        users.remove(key);
        users.put(key, user);

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
                users.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromUser((User) users.get(key)));
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
                users.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromUser((User) users.get(key)));
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
                users.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(users.get(key));
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
                users.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(users.get(key));
                count++;
            }
        }
        return list;
    }


    private void initialize() {
    }

    private EntityHeader fromUser(User u) {
        return
                new EntityHeaderImp(u.getOid(), User.class, u.getName());
    }

    /**
     * @return the next sequence
     */
    private long nextSequence() {
        return ++sequenceId;
    }

    private Map users = new HashMap();
    private long sequenceId = 0;
}
