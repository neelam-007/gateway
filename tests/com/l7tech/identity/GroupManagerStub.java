package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.EntityHeaderImp;

import java.util.*;

/**
 * Test stub for group manager. A <code>Map</code> backed group
 * manager for easier testing.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class GroupManagerStub implements GroupManager {

    public GroupManagerStub() {
        initialize();
    }

    public Group findByPrimaryKey(long oid) throws FindException {
        return (Group) groups.get(new Long(oid));
    }

    public void delete(Group group) throws DeleteException {
        if (groups.remove(new Long(group.getOid())) == null) {
            throw new DeleteException("Could not find group oid= " + group.getOid());
        }
    }

    public long save(Group group) throws SaveException {
        long oid = nextSequence();
        group.setOid(oid);
        Long key = new Long(oid);
        if (groups.get(key) != null) {
            throw new SaveException("Record exists, group oid= " + group.getOid());
        }
        groups.put(key, group);
        return oid;
    }

    public void update(Group group) throws UpdateException {
        Long key = new Long(group.getOid());
        if (groups.get(key) == null) {
            throw new UpdateException("Record missing, group oid= " + group.getOid());
        }
        groups.remove(key);
        groups.put(key, group);
    }

    public void setIdentityProviderOid(long oid) {
        // unimplented
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
                groups.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromGroup((Group) groups.get(key)));
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
                groups.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromGroup((Group) groups.get(key)));
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
                groups.keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(groups.get(key));
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
                groups.keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(groups.get(key));
                count++;
            }
        }
        return list;
    }

    private void initialize() {
    }

    private EntityHeader fromGroup(Group g) {
        return
                new EntityHeaderImp(g.getOid(), Group.class, g.getName());
    }

    /**
     * @return the next sequence
     */
    private long nextSequence() {
        return ++sequenceId;
    }

    private Map groups = new HashMap();
    private long sequenceId = 0;
}
