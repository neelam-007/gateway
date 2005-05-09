/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.fed.FederatedGroupMembership;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.ObjectNotFoundException;
import net.sf.hibernate.*;
import net.sf.hibernate.expression.Expression;
import org.springframework.dao.DataAccessException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentGroupManager extends HibernateEntityManager implements GroupManager {

    public PersistentGroupManager(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
        HQL_GETUSERS = "select usr from usr in class " + getUserManager().getImpClass().getName() + ", " +
          "membership in class " + getMembershipClass().getName() + " " +
          "where membership.userOid = usr.oid " +
          "and membership.groupOid = ?";

        HQL_ISMEMBER = "from membership in class " + getMembershipClass().getName() + " " +
          "where membership.userOid = ? " +
          "and membership.groupOid = ?";

        HQL_GETGROUPS = "select grp from grp in class " + getImpClass().getName() + ", " +
          "membership in class " + getMembershipClass().getName() + " " +
          "where membership.groupOid = grp.oid " +
          "and membership.userOid = ?";
    }

    /**
     * empty subclassing constructor (required for class proxying)
     */
    protected PersistentGroupManager() {
    }

    public Group findByName(String name) throws FindException {
        try {
            Criteria findByName = getSession().createCriteria(getImpClass());
            findByName.add(Expression.eq("name", name));
            addFindAllCriteria(findByName);
            List groups = findByName.list();
            switch (groups.size()) {
                case 0:
                    return null;
                case 1:
                    PersistentGroup g = (PersistentGroup)groups.get(0);
                    if (g.getProviderId() != identityProvider.getConfig().getOid()) {
                        // fla note: i did not introduce this setProviderId statement but i wrapped it in this if
                        // because it was buggy and because it used to hardcode the providerid value to -2 bugzilla 1056
                        // this is probably here due to some code being copied from the internal group manager when this
                        // was refactored. this whole thing is probably no longer needed but it wont hurt i and dont
                        // want to risk breaking something else.
                        logger.warning("Current group oid is : " + g.getProviderId() + " and provider id is " +
                          identityProvider.getConfig().getOid());
                        g.setProviderId(identityProvider.getConfig().getOid());
                    }
                    return g;
                default:
                    String err = "Found more than one group with the name " + name;
                    logger.log(Level.SEVERE, err);
                    throw new FindException(err);
            }
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, null, e);
            throw new FindException(e.toString(), e);
        }
    }

    public Group findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            PersistentGroup out = (PersistentGroup)findByPrimaryKey(getImpClass(), Long.parseLong(oid));
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
    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            Criteria searchCriteria = getSession().createCriteria(getImpClass());
            searchCriteria.add(Expression.ilike("name", searchString));
            addFindAllCriteria(searchCriteria);
            List results = searchCriteria.list();
            List headers = new ArrayList();
            for (Iterator i = results.iterator(); i.hasNext();) {
                PersistentGroup group = (PersistentGroup)i.next();
                headers.add(new EntityHeader(group.getUniqueIdentifier(),
                  EntityType.fromInterface(getInterfaceClass()),
                  group.getName(),
                  group.getDescription()));
            }
            return Collections.unmodifiableList(headers);
        } catch (HibernateException e) {
            final String msg = "Error while searching for " + getInterfaceClass() + " instances.";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
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
            PersistentGroup imp = cast(group);
            long oid = new Long(imp.getUniqueIdentifier()).longValue();
            preDelete(imp);
            Set userHeaders = getUserHeaders(imp);
            Session s = getSession();
            for (Iterator i = userHeaders.iterator(); i.hasNext();) {
                EntityHeader userHeader = (EntityHeader)i.next();
                s.delete(newMembership(userHeader.getOid(), oid));
            }
            s.delete(group);
        } catch (FindException e) {
            throw new DeleteException(e.toString(), e);
        } catch (HibernateException e) {
            throw new DeleteException(e.toString(), e);
        }
    }

    public abstract GroupMembership newMembership(long userOid, long groupOid);

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
    public void deleteAll(long ipoid) throws DeleteException, ObjectNotFoundException {
        StringBuffer hqlgroup = new StringBuffer("FROM ");
        hqlgroup.append(getTableName()).append(" IN CLASS ").append(getImpClass());
        hqlgroup.append(" WHERE provider_oid = ?");

        StringBuffer hqlgroupmember = new StringBuffer("FROM ");
        hqlgroupmember.append(getTableName()).append(" IN CLASS ").append(FederatedGroupMembership.class.getName());
        hqlgroupmember.append(" WHERE provider_oid = ?");

        try {
            // delete all group members
            final Session session = getSession();
            session.delete(hqlgroupmember.toString(), new Long(ipoid), Hibernate.LONG);

            // delete all groups
            session.delete(hqlgroup.toString(), new Long(ipoid), Hibernate.LONG);

        } catch (HibernateException e) {
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
    public void deleteAllVirtual(long ipoid) throws DeleteException, ObjectNotFoundException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(VirtualGroup.class.getName());
        hql.append(" WHERE provider_oid = ?");

        try {
            getSession().delete(hql.toString(), new Long(ipoid), Hibernate.LONG);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public String save(Group group) throws SaveException {
        return save(group, null);
    }

    public String save(Group group, Set userHeaders) throws SaveException {
        try {
            // check that no existing group have same name
            Group existingGrp = null;
            try {
                existingGrp = findByName(group.getName());
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

    public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
        PersistentGroup imp = cast(group);

        try {
            // if this is the admin group, make sure that we are not removing all memberships
            preUpdate(imp);
            PersistentGroup originalGroup = (PersistentGroup)findByPrimaryKey(group.getUniqueIdentifier());
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

    public boolean isMember(User user, Group group) throws FindException {
        if (!checkProvider(user)) return false;
        try {
            Session s = getSession();
            Query query = s.createQuery(HQL_ISMEMBER);
            query.setString(0, user.getUniqueIdentifier());
            query.setString(1, group.getUniqueIdentifier());
            return (query.iterate().hasNext());
        } catch (HibernateException e) {
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

    public void addUsers(Group group, Set users) throws FindException, UpdateException {
        PersistentGroup imp = cast(group);
        GroupMembership membership = newMembership(-1, imp.getOid());
        try {
            Session s = getSession();
            for (Iterator i = users.iterator(); i.hasNext();) {
                PersistentUser user = (PersistentUser)i.next();
                membership.setUserOid(user.getOid());
                s.save(membership);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public void removeUsers(Group group, Set users) throws FindException, UpdateException {
        PersistentGroup imp = cast(group);
        GroupMembership membership = newMembership(-1, imp.getOid());
        try {
            Session s = getSession();
            for (Iterator i = users.iterator(); i.hasNext();) {
                PersistentUser user = (PersistentUser)i.next();
                membership.setUserOid(user.getOid());
                s.delete(membership);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public void addUser(User user, Set groups) throws FindException, UpdateException {
        PersistentUser imp = getUserManager().cast(user);
        GroupMembership membership = newMembership(imp.getOid(), -1);
        try {
            Session s = getSession();
            for (Iterator i = groups.iterator(); i.hasNext();) {
                PersistentGroup group = (PersistentGroup)i.next();
                membership.setGroupOid(group.getOid());
                s.save(membership);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    private PersistentUserManager getUserManager() {
        return (PersistentUserManager)identityProvider.getUserManager();
    }

    public void removeUser(User user, Set groups) throws FindException, UpdateException {
        PersistentUser imp = getUserManager().cast(user);
        GroupMembership membership = newMembership(imp.getOid(), -1);
        try {
            Session s = getSession();
            for (Iterator i = groups.iterator(); i.hasNext();) {
                PersistentGroup group = (PersistentGroup)i.next();
                membership.setGroupOid(group.getOid());
                s.delete(membership);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public void addUser(User user, Group group) throws FindException, UpdateException {
        PersistentUser userImp = getUserManager().cast(user);
        PersistentGroup groupImp = cast(group);
        try {
            Session s = getSession();

            s.save(newMembership(userImp.getOid(), groupImp.getOid()));
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public void removeUser(User user, Group group) throws FindException, UpdateException {
        PersistentUser userImp = getUserManager().cast(user);
        PersistentGroup groupImp = cast(group);
        try {
            Session s = getSession();
            s.delete(newMembership(userImp.getOid(), groupImp.getOid()));
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    public Set getGroupHeaders(User user) throws FindException {
        return getGroupHeaders(user.getUniqueIdentifier());
    }

    public Set getGroupHeaders(String userId) throws FindException {
        try {
            return doGetGroupHeaders(userId);
        } catch (HibernateException he) {
            throw new FindException(he.toString(), he);
        }

    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        setGroupHeaders(user.getUniqueIdentifier(), groupHeaders);
    }

    public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
        if (groupHeaders == null) return;
        try {
            Session s = getSession();

            Set newGids = headersToIds(groupHeaders);
            Set existingGids = headersToIds(doGetGroupHeaders(userId));

            GroupMembership gm;
            long uoid = new Long(userId).longValue();

            // Check for new memberships
            for (Iterator j = newGids.iterator(); j.hasNext();) {
                String newGid = (String)j.next();
                if (!existingGids.contains(newGid)) {
                    gm = newMembership(uoid, new Long(newGid).longValue());
                    s.save(gm);
                }
            }

            // Check for removed memberships
            for (Iterator i = existingGids.iterator(); i.hasNext();) {
                String existingGid = (String)i.next();
                if (!newGids.contains(existingGid)) {
                    Group g = findByPrimaryKey(existingGid);
                    if (Group.ADMIN_GROUP_NAME.equals(g.getName())) {
                        Set adminUserHeaders = getUserHeaders(g);
                        if (adminUserHeaders.size() < 2) {
                            String msg = "Can't remove last administrator membership!";
                            logger.info(msg);
                            throw new UpdateException(msg);
                        }
                    }
                    gm = newMembership(uoid, new Long(existingGid).longValue());
                    s.delete(gm);
                }
            }
        } catch (HibernateException he) {
            throw new UpdateException(he.toString(), he);
        }

    }

    public Set getUserHeaders(Group group) throws FindException {
        return getUserHeaders(group.getUniqueIdentifier());
    }

    public Set getUserHeaders(String groupId) throws FindException {
        try {
            return doGetUserHeaders(groupId);
        } catch (HibernateException he) {
            throw new FindException(he.toString(), he);
        }
    }

    private Set doGetUserHeaders(String groupId) throws HibernateException {
        Set headers = new HashSet();
        Session s = getSession();
        String hql = HQL_GETUSERS;
        Query query = s.createQuery(hql);
        query.setString(0, groupId);
        for (Iterator i = query.iterate(); i.hasNext();) {
            PersistentUser user = (PersistentUser)i.next();
            headers.add(identityProvider.getUserManager().userToHeader(user));
        }
        return headers;
    }

    private Set doGetGroupHeaders(String userId) throws HibernateException {
        Set headers = new HashSet();
        Session s = getSession();
        String hql = HQL_GETGROUPS;
        Query query = s.createQuery(hql);
        query.setString(0, userId);
        for (Iterator i = query.iterate(); i.hasNext();) {
            PersistentGroup group = (PersistentGroup)i.next();
            headers.add(new EntityHeader(group.getUniqueIdentifier(), EntityType.GROUP, group.getName(), null));
        }
        return headers;
    }

    public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
        setUserHeaders(group.getUniqueIdentifier(), groupHeaders);
    }

    private Set headersToIds(Set headers) {
        Set uids = new HashSet();
        for (Iterator i = headers.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader)i.next();
            uids.add(header.getStrId());
        }
        return uids;
    }

    public void setUserHeaders(String groupId, Set userHeaders) throws FindException, UpdateException {
        if (userHeaders == null) return;
        try {
            Session s = getSession();

            Set newUids = headersToIds(userHeaders);
            Set existingUids = headersToIds(doGetUserHeaders(groupId));

            Group group = findByPrimaryKey(groupId);
            if (Group.ADMIN_GROUP_NAME.equals(group.getName()) &&
              userHeaders.size() == 0) {
                String msg = "Can't delete last administrator";
                logger.info(msg);
                throw new UpdateException(msg);
            }

            GroupMembership gm;
            long goid = new Long(groupId).longValue();

            // Check for new memberships
            for (Iterator j = newUids.iterator(); j.hasNext();) {
                String newUid = (String)j.next();
                if (!existingUids.contains(newUid)) {
                    gm = newMembership(new Long(newUid).longValue(), goid);
                    s.save(gm);
                }
            }

            // Check for removed memberships
            for (Iterator i = existingUids.iterator(); i.hasNext();) {
                String existingUid = (String)i.next();
                if (!newUids.contains(existingUid)) {
                    gm = newMembership(new Long(existingUid).longValue(), goid);
                    s.delete(gm);
                }
            }
        } catch (HibernateException he) {
            throw new UpdateException(he.toString(), he);
        }

    }

    protected IdentityProvider identityProvider;
    private final Logger logger = Logger.getLogger(getClass().getName());

    private String HQL_GETGROUPS;
    private String HQL_GETUSERS;
    private String HQL_ISMEMBER;
}
