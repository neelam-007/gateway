/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.*;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

/**
 * Abstract manager functionality for {@link PersistentGroup} instances
 * @author alex
 */
public abstract class PersistentGroupManager extends HibernateEntityManager<PersistentGroup, IdentityHeader> implements GroupManager {
    private final String HQL_DELETE_BY_PROVIDEROID =
            "FROM grp IN CLASS " + getImpClass().getName() +
                    " WHERE grp.providerId = ?";

    private final String HQL_DELETE_MEMBERSHIPS_BY_PROVIDEROID =
            "FROM mem IN CLASS " + getMembershipClass().getName() +
                    " WHERE mem.thisGroupProviderOid = ?";

    private static final String HQL_DELETE_VIRTUAL_BY_PROVIDEROID =
            "FROM vg IN CLASS " + VirtualGroup.class.getName() + " WHERE vg.providerId = ?";

    private final String HQL_GETGROUPS =
            "select grp from " +
                    "grp in class " + getImpClass().getName() + ", " +
                    "mem in class " + getMembershipClass().getName() +
                " where mem.thisGroupId = grp.oid " +
                "and mem.memberUserId = ?";

    private final String HQL_ISMEMBER = "from membership in class "+ getMembershipClass().getName() +
          " where membership.memberUserId = ? " +
          "and membership.thisGroupId = ?";

    /**
     * Can't be defined statically; it needs information from the identity provider
     */
    private static String HQL_GETMEMBERS;

    public PersistentGroupManager(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;

        StringBuffer queryString = new StringBuffer("select usr from usr in class ");
        queryString.append(getUserManager().getImpClass().getName()).append(", ");
        queryString.append("membership in class ").append(getMembershipClass().getName());
        queryString.append(" where membership.memberUserId = usr.oid ");
        queryString.append("and membership.thisGroupId = ?");
        HQL_GETMEMBERS = queryString.toString();
    }

    /**
     * empty subclassing constructor (required for class proxying)
     */
    protected PersistentGroupManager() {
    }

    public PersistentGroup findByUniqueName(String name) throws FindException {
        PersistentGroup pg = super.findByUniqueName(name);
        if (pg != null) pg.setProviderId(identityProvider.getConfig().getOid());
        return pg;
    }

    public PersistentGroup findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            PersistentGroup out = findByPrimaryKey(getImpClass(), Long.parseLong(oid));
            if (out == null) return null;
            out.setProviderId(getProviderOid());
            return out;
        } catch (NumberFormatException nfe) {
            throw new FindException("Can't find groups with non-numeric OIDs!", nfe);
        }
    }

    /**
     * Search for the group headers using the given search string.
     *
     * @param searchString the search string (supports '*' wildcards)
     * @return the never <b>null</b> collection of entitites
     * @throws com.l7tech.objectmodel.FindException
     *          thrown if an SQL error is encountered
     * @see com.l7tech.server.identity.PersistentGroupManager
     * @see com.l7tech.server.identity.PersistentUserManager
     */
    public Collection<IdentityHeader> search(final String searchString) throws FindException {
        try {
            //noinspection unchecked
            return (Collection<IdentityHeader>) getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Criteria searchCriteria = session.createCriteria(getImpClass());
                    // replace wildcards to match stuff understood by mysql
                    // replace * with % and ? with _
                    // note. is this portable?
                    String s = searchString.replace('*', '%').replace('?', '_');
                    searchCriteria.add(Restrictions.ilike("name", s));
                    addFindAllCriteria(searchCriteria);
                    List results = searchCriteria.list();
                    List<IdentityHeader> headers = new ArrayList<IdentityHeader>();
                    for (Object result : results) {
                        PersistentGroup group = (PersistentGroup) result;
                        headers.add(new IdentityHeader(
                                group.getProviderId(),
                                group.getUniqueIdentifier(),
                                EntityType.GROUP,
                                group.getName(),
                                group.getDescription()));
                    }
                    return Collections.unmodifiableList(headers);
                }
            });
        } catch (HibernateException e) {
            final String msg = "Error while searching for " + getInterfaceClass().getName() + " instances.";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
    }

    protected EntityHeader newHeader(long id, String name) {
        return new IdentityHeader(getProviderOid(), Long.toString(id), EntityType.GROUP, name, null);
    }

    protected long getProviderOid() {
        return identityProvider.getConfig().getOid();
    }

    /**
     * Must be called in a transaction!
     */
    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
        try {
            // it is not allowed to delete the admin group
            PersistentGroup pgroup = cast(group);
            preDelete(pgroup);
            Set<IdentityHeader> userHeaders = getUserHeaders(pgroup);
            for (Object userHeader : userHeaders) {
                User u = getUserManager().headerToUser((IdentityHeader) userHeader);
                deleteMembership(pgroup, u);
            }
            getHibernateTemplate().delete(group);
        } catch (ObjectModelException e) {
            throw new DeleteException(e.toString(), e);
        } catch (HibernateException e) {
            throw new DeleteException(e.toString(), e);
        }
    }

    public abstract GroupMembership newMembership(Group group, User user);

    protected abstract Class getMembershipClass();

    protected void preSave(PersistentGroup persistentGroup) throws SaveException { }

    protected void preDelete(PersistentGroup group) throws DeleteException { }

    protected void preUpdate(PersistentGroup group) throws FindException, UpdateException { }

    protected abstract PersistentGroup cast(Group group);

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        final String msg = "Couldn't find group to be deleted";
        try {
            Group g = findByPrimaryKey(identifier);
            if (g == null) {
                throw new ObjectNotFoundException(msg);
            }
            delete(g);
        } catch (FindException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new ObjectNotFoundException(msg, e);
        }
    }

    /**
     * Delete all groups of the identity provider given the identity provider Id
     * <p/>
     * Only apply to Federated Identity Provider
     * <p/>
     * Must be called in a transaction!
     *
     * @param ipoid The identity provider id
     * @throws DeleteException
     * @throws ObjectNotFoundException
     */
    public void deleteAll(final long ipoid) throws DeleteException, ObjectNotFoundException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    // Delete all group members
                    Query q = session.createQuery(HQL_DELETE_MEMBERSHIPS_BY_PROVIDEROID);
                    q.setLong(0, ipoid);

                    for (Iterator i = q.iterate(); i.hasNext();) {
                        session.delete(i.next());
                    }

                    // Delete all groups
                    q = session.createQuery(HQL_DELETE_BY_PROVIDEROID);
                    q.setLong(0, ipoid);
                    for (Iterator i = q.iterate(); i.hasNext();) {
                        session.delete(i.next());
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        }
    }

    /**
     * Delete all virutal groups of the identity provider given the identity provider Id
     * <p/>
     * Only apply to Federated Identity Provider
     * <p/>
     * Must be called in a transaction!
     *
     * @param ipoid The identity provider id
     * @throws DeleteException
     * @throws ObjectNotFoundException
     */
    public void deleteAllVirtual(final long ipoid) throws DeleteException, ObjectNotFoundException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE_VIRTUAL_BY_PROVIDEROID);
                    q.setLong(0, ipoid);
                    for (Iterator i = q.iterate(); i.hasNext();) {
                        session.delete(i.next());
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public String save(Group group) throws SaveException {
        return save(group, null);
    }

    public String save(Group group, Set<IdentityHeader> userHeaders) throws SaveException {
        try {
            // check that no existing group have same name
            Group existingGrp;
            try {
                existingGrp = findByUniqueName(group.getName());
            } catch (FindException e) {
                existingGrp = null;
            }
            if (existingGrp != null) {
                throw new DuplicateObjectException("This group cannot be saved because an existing group " +
                  "already uses the name '" + group.getName() + "'");
            }

            PersistentGroup imp = cast(group);
            preSave(imp);
            String oid = getHibernateTemplate().save(imp).toString();

            if (userHeaders != null) {
                try {
                    setUserHeaders(oid, userHeaders);
                } catch (FindException e) {
                    logger.log(Level.SEVERE, e.getMessage());
                    throw new SaveException(e.getMessage(), e);
                } catch (UpdateException e) {
                    logger.log(Level.SEVERE, e.getMessage());
                    throw new SaveException(e.getMessage(), e);
                }
            }

            return oid;
        } catch (DataAccessException se) {
            throw new SaveException(se.toString(), se);
        }
    }

    public void update(Group group) throws UpdateException, ObjectNotFoundException {
        update(group, null);
    }

    public void update(Group group, Set<IdentityHeader> userHeaders) throws UpdateException, ObjectNotFoundException {
        PersistentGroup imp = cast(group);

        try {
            // if this is the admin group, make sure that we are not removing all memberships
            preUpdate(imp);
            PersistentGroup originalGroup = findByPrimaryKey(group.getUniqueIdentifier());
            if (originalGroup == null) {
                throw new ObjectNotFoundException("Group " + group.getName());
            }
            // check for version conflict
            if (originalGroup.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            setUserHeaders(group.getUniqueIdentifier(), userHeaders);

            originalGroup.copyFrom(imp);
            getHibernateTemplate().update(originalGroup);
        } catch (FindException e) {
            throw new UpdateException("Update called on group that does not already exist", e);
        } catch (DataAccessException se) {
            throw new UpdateException(se.toString(), se);
        }
    }

    public boolean isMember(final User user, final Group group) throws FindException {
        if (!checkProvider(user)) return false;
        try {
            return (Boolean)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query query = session.createQuery(HQL_ISMEMBER);
                    query.setString(0, user.getUniqueIdentifier());
                    query.setString(1, group.getUniqueIdentifier());
                    return (query.iterate().hasNext());
                }
            });
        } catch (Exception e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    protected boolean checkProvider(User user) {
        String msg = null;
        Level level = null;
        if (user.getProviderId() == PersistentUser.DEFAULT_OID) {
            msg = "User was authenticated by an unknown identity provider";
            level = Level.WARNING;
        } else if (user.getProviderId() != identityProvider.getConfig().getOid()) {
            msg = "User was authenticated by a different identity provider";
            level = Level.FINE;
        }

        if (msg != null) {
            logger.log(level, msg);
            return false;
        }

        return true;
    }

    public void addUsers(Group group, Set<User> users) throws FindException, UpdateException {
        PersistentGroup pgroup = cast(group);
        try {
            for (User user1 : users) {
                PersistentUser user = (PersistentUser) user1;
                getHibernateTemplate().save(newMembership(pgroup, user));
            }
        } catch (Exception e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public void removeUsers(Group group, Set<User> users) throws FindException, UpdateException {
        PersistentGroup pgroup = cast(group);
        try {
            for (User user1 : users) {
                PersistentUser user = (PersistentUser) user1;
                deleteMembership(pgroup, user);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    void deleteMembership(final Group group, final User user) throws HibernateException, FindException, UpdateException {
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(getMembershipClass());
                addMembershipCriteria(crit, group, user);
                crit.add(Restrictions.eq("memberUserId", user.getUniqueIdentifier()));
                crit.add(Restrictions.eq("thisGroupId", group.getUniqueIdentifier()));
                addMembershipCriteria(crit, group, user);
                List toBeDeleted = crit.list();
                if (toBeDeleted == null || toBeDeleted.size() == 0) {
                    throw new RuntimeException("Couldn't find membership to be deleted; user " + user.getUniqueIdentifier() + ", group " + group.getUniqueIdentifier());
                } else if (toBeDeleted.size() > 1) {
                    throw new RuntimeException("Found more than one membership to be deleted; user " + user.getUniqueIdentifier() + ", group " + group.getUniqueIdentifier());
                }
                session.delete(toBeDeleted.get(0));
                return null;
            }
        });
    }

    /**
     * Allows subclasses to extend the {@link Criteria} used to query membership records
     *
     * @param crit the Criteria to be updated
     * @param group the Group for which memberships are sought
     * @param identity the Identity whose membership is sought
     */
    protected abstract void addMembershipCriteria(Criteria crit, Group group, Identity identity);

    public void addUser(User user, Set<Group> groups) throws FindException, UpdateException {
        PersistentUser puser = getUserManager().cast(user);
        try {
            for (Group group1 : groups) {
                PersistentGroup group = (PersistentGroup) group1;
                getHibernateTemplate().save(newMembership(group, puser));
            }
        } catch (Exception e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    protected PersistentUserManager getUserManager() {
        return (PersistentUserManager)identityProvider.getUserManager();
    }

    public void removeUser(User user, Set<Group> groups) throws FindException, UpdateException {
        PersistentUser puser = getUserManager().cast(user);
        try {
            for (Group group1 : groups) {
                PersistentGroup group = (PersistentGroup) group1;
                deleteMembership(group, puser);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public void addUser(User user, Group group) throws FindException, UpdateException {
        PersistentUser userImp = getUserManager().cast(user);
        PersistentGroup groupImp = cast(group);
        try {
            getHibernateTemplate().save(newMembership(groupImp, userImp));
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public void removeUser(User user, Group group) throws FindException, UpdateException {
        PersistentUser userImp = getUserManager().cast(user);
        PersistentGroup groupImp = cast(group);
        try {
            deleteMembership(groupImp, userImp);
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public Set<IdentityHeader> getGroupHeaders(User user) throws FindException {
        return getGroupHeaders(user.getUniqueIdentifier());
    }

    public Set<IdentityHeader> getGroupHeaders(String userId) throws FindException {
        try {
            return doGetGroupHeaders(userId);
        } catch (HibernateException he) {
            throw new FindException(he.toString(), he);
        }

    }

    public void setGroupHeaders(User user, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        setGroupHeaders(user.getUniqueIdentifier(), groupHeaders);
    }

    public void setGroupHeaders(String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        if (groupHeaders == null) return;
        try {
            Set<String> newGids = headersToIds(groupHeaders);
            Set<String> existingGids = headersToIds(doGetGroupHeaders(userId));

            GroupMembership gm;
            User thisUser = getUserManager().findByPrimaryKey(userId);

            // Check for new memberships
            for (String newGid : newGids) {
                if (!existingGids.contains(newGid)) {
                    PersistentGroup newGroup = cast(findByPrimaryKey(newGid));
                    gm = newMembership(newGroup, thisUser);
                    getHibernateTemplate().save(gm);
                }
            }

            // Check for removed memberships
            for (String existingGid : existingGids) {
                if (!newGids.contains(existingGid)) {
                    Group oldGroup = findByPrimaryKey(existingGid);
                    if (Group.ADMIN_GROUP_NAME.equals(oldGroup.getName())) {
                        Set adminUserHeaders = getUserHeaders(oldGroup);
                        if (adminUserHeaders.size() < 2) {
                            String msg = "Can't remove last administrator membership!";
                            logger.info(msg);
                            throw new UpdateException(msg);
                        }
                    }
                    deleteMembership(oldGroup, thisUser);
                }
            }
        } catch (HibernateException he) {
            throw new UpdateException(he.toString(), he);
        }

    }

    public Set<IdentityHeader> getUserHeaders(Group group) throws FindException {
        return getUserHeaders(group.getUniqueIdentifier());
    }

    public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
        try {
            return doGetUserHeaders(groupId);
        } catch (HibernateException he) {
            throw new FindException(he.toString(), he);
        }
    }

    private Set<IdentityHeader> doGetUserHeaders(final String groupId) throws HibernateException {
        //noinspection unchecked
        return (Set<IdentityHeader>)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Set<IdentityHeader> headers = new HashSet<IdentityHeader>();
                Query query = session.createQuery(HQL_GETMEMBERS);
                query.setString(0, groupId);
                for (Iterator i = query.iterate(); i.hasNext();) {
                    PersistentUser user = (PersistentUser)i.next();
                    headers.add(identityProvider.getUserManager().userToHeader(user));
                }
                return headers;
            }
        });
    }

    private Set<IdentityHeader> doGetGroupHeaders(final String userId) throws HibernateException {
        //noinspection unchecked
        return (Set<IdentityHeader>)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Set<IdentityHeader> headers = new HashSet<IdentityHeader>();
                Query query = session.createQuery(HQL_GETGROUPS);
                query.setString(0, userId);
                for (Iterator i = query.iterate(); i.hasNext();) {
                    PersistentGroup group = (PersistentGroup)i.next();
                    headers.add(new IdentityHeader(group.getProviderId(), group.getUniqueIdentifier(), EntityType.GROUP, group.getName(), null));
                }
                return headers;
            }
        });
    }

    public void setUserHeaders(Group group, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        setUserHeaders(group.getUniqueIdentifier(), groupHeaders);
    }

    private Set<String> headersToIds(Set<IdentityHeader> headers) {
        Set<String> uids = new HashSet<String>();
        for (EntityHeader header : headers) {
            uids.add(header.getStrId());
        }
        return uids;
    }

    public void setUserHeaders(String groupId, Set<IdentityHeader> userHeaders) throws FindException, UpdateException {
        if (userHeaders == null) return;
        try {
            Set<String> newUids = headersToIds(userHeaders);
            Set<String> existingUids = headersToIds(doGetUserHeaders(groupId));

            Group thisGroup = findByPrimaryKey(groupId);
            if (Group.ADMIN_GROUP_NAME.equals(thisGroup.getName()) &&
              userHeaders.size() == 0) {
                String msg = "Can't delete last administrator";
                logger.info(msg);
                throw new UpdateException(msg);
            }

            // Check for new memberships
            for (String newUid : newUids) {
                if (!existingUids.contains(newUid)) {
                    User newUser = getUserManager().findByPrimaryKey(newUid);
                    getHibernateTemplate().save(newMembership(thisGroup, newUser));
                }
            }

            // Check for removed memberships
            for (String existingUid : existingUids) {
                if (!newUids.contains(existingUid)) {
                    User oldUser = getUserManager().findByPrimaryKey(existingUid);
                    deleteMembership(thisGroup, oldUser);
                }
            }
        } catch (HibernateException he) {
            throw new UpdateException(he.toString(), he);
        }

    }

    public EntityType getEntityType() {
        return EntityType.GROUP;
    }

    protected IdentityProvider identityProvider;

    // Not static on purpose; we want to pretend to be the subclass
    private final Logger logger = Logger.getLogger(getClass().getName());

}
