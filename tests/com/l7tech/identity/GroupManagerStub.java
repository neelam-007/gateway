package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import java.util.*;

/**
 * Test stub for group manager. A <code>Map</code> backed group
 * manager for easier testing.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class GroupManagerStub implements GroupManager {
    private StubDataStore dataStore;

    public GroupManagerStub(StubDataStore dataStore) {
            this.dataStore = dataStore;
    }

    public Group findByPrimaryKey(long oid) throws FindException {
        return (Group) dataStore.getGroups().get(new Long(oid));
    }

    public void delete(Group group) throws DeleteException {
        if (dataStore.getGroups().remove(new Long(group.getOid())) == null) {
            throw new DeleteException("Could not find group oid= " + group.getOid());
        }
    }

    public long save(Group group) throws SaveException {
        long oid = dataStore.nextObjectId();
        group.setOid(oid);
        Long key = new Long(oid);
        if (dataStore.getGroups().get(key) != null) {
            throw new SaveException("Record exists, group oid= " + group.getOid());
        }
        dataStore.getGroups().put(key, group);
        return oid;
    }

    public void update(Group group) throws UpdateException {
        Long key = new Long(group.getOid());
        if (dataStore.getGroups().get(key) == null) {
            throw new UpdateException("Record missing, group oid= " + group.getOid());
        }
        dataStore.getGroups().remove(key);
        dataStore.getGroups().put(key, group);
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
                dataStore.getGroups().keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(fromGroup((Group) dataStore.getGroups().get(key)));
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
                dataStore.getGroups().keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromGroup((Group) dataStore.getGroups().get(key)));
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
                dataStore.getGroups().keySet().iterator(); i.hasNext();) {
            Long key = (Long) i.next();
            list.add(dataStore.getGroups().get(key));
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
                dataStore.getGroups().keySet().iterator(); i.hasNext(); index++) {
            Long key = (Long) i.next();

            if (index >= offset && count <= windowSize) {
                list.add(dataStore.getGroups().get(key));
                count++;
            }
        }
        return list;
    }


    private EntityHeader fromGroup(Group g) {
        return
                new EntityHeader(g.getOid(), EntityType.GROUP, g.getName(), null);
    }
}
