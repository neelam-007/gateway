package com.l7tech.identity;

import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.*;

import java.util.*;

/**
 * Test stub for group manager. A <code>Map</code> backed group
 * manager for easier testing.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class GroupManagerStub extends GroupManagerAdapter {
    private StubDataStore dataStore;

    public GroupManagerStub(StubDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Group findByPrimaryKey(String oid) throws FindException {
        return (Group)dataStore.getGroups().get(oid);
    }

    public Group findByName(String name) throws FindException {
        Iterator groups = dataStore.getGroups().values().iterator();
        for (; groups.hasNext();) {
            Group g = (Group)groups.next();
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
        if (dataStore.getGroups().remove(group.getUniqueIdentifier()) == null) {
            throw new ObjectNotFoundException("Could not find group oid= " + group.getUniqueIdentifier());
        }
    }

    public String save(Group group) throws SaveException {
        InternalGroup imp = (InternalGroup)group;
        long oid = dataStore.nextObjectId();
        imp.setOid(oid);
        final String uniqueIdentifier = imp.getUniqueIdentifier();
        if (dataStore.getGroups().get(uniqueIdentifier) != null) {
            throw new SaveException("Record exists, group oid= " + imp.getOid());
        }
        dataStore.getGroups().put(imp.getUniqueIdentifier(), group);
        return imp.getUniqueIdentifier();
    }

    public void update(Group group) throws UpdateException, ObjectNotFoundException {
        InternalGroup imp = (InternalGroup)group;
        final String uniqueIdentifier = imp.getUniqueIdentifier();
        if (dataStore.getGroups().get(uniqueIdentifier) == null) {
            throw new UpdateException("Record missing, group oid= " + imp.getOid());
        }
        dataStore.getGroups().remove(imp.getUniqueIdentifier());
        dataStore.getGroups().put(imp.getUniqueIdentifier(), group);
    }

    public Class getImpClass() {
        return InternalGroup.class;
    }


    public Set getGroupHeaders(String userId) throws FindException {
        long uid = 0;
        try {
            uid = Long.parseLong(userId);
        } catch (Exception e) {
            throw new FindException("Error in parsing user id " + userId, e);
        }
        Set groupHeaders = new HashSet();
        Iterator it = dataStore.getGroupMemberships().iterator();
        for (; it.hasNext();) {
            GroupMembership gm = (GroupMembership)it.next();
            if (gm.getUserOid() == uid) {
                String gid = Long.toString(gm.getGroupOid());
                Group g = findByPrimaryKey(gid);
                if (g == null) {
                    throw new FindException("Cannot find group " + gid);
                }
                groupHeaders.add(fromGroup(g));
            }
        }
        return groupHeaders;
    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        setGroupHeaders(user.getUniqueIdentifier(), groupHeaders);
    }

    public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
        final Set groupMemberships = dataStore.getGroupMemberships();
        Iterator it = groupMemberships.iterator();
        long uid = 0;
        Collection remove = new HashSet();
        try {
            uid = Long.parseLong(userId);
        } catch (Exception e) {
            throw new FindException("Error in parsing user id " + userId, e);
        }
        for (; it.hasNext();) {
            GroupMembership gm = (GroupMembership)it.next();
            if (uid == gm.getUserOid()) {
                remove.add(gm);
            }
        }
        groupMemberships.removeAll(remove);
        if (groupHeaders != null) {
            for (Iterator iterator = groupHeaders.iterator(); iterator.hasNext();) {
                EntityHeader eh = (EntityHeader)iterator.next();
                GroupMembership gm = new GroupMembership(uid, eh.getOid());
                groupMemberships.add(gm);
            }
        }
    }

    public Set getGroupHeaders(User user) throws FindException {
        return getGroupHeaders(user.getUniqueIdentifier());
    }


    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
     * 
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException {
        Collection list = new ArrayList();
        for (Iterator i =
          dataStore.getGroups().keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            list.add(fromGroup((Group)dataStore.getGroups().get(key)));
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
          dataStore.getGroups().keySet().iterator(); i.hasNext(); index++) {
            Object key = i.next();

            if (index >= offset && count <= windowSize) {
                list.add(fromGroup((Group)dataStore.getGroups().get(key)));
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
          dataStore.getGroups().keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            list.add(dataStore.getGroups().get(key));
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
          dataStore.getGroups().keySet().iterator(); i.hasNext(); index++) {
            Object key = i.next();

            if (index >= offset && count <= windowSize) {
                list.add(dataStore.getGroups().get(key));
                count++;
            }
        }
        return list;
    }

    private EntityHeader fromGroup(Group g) {
        InternalGroup imp = (InternalGroup)g;
        return new EntityHeader(imp.getOid(), EntityType.GROUP, g.getName(), null);
    }
}
