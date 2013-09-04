/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract manager functionality for {@link PersistentGroup} instances
 * @author alex
 */
public abstract class PersistentGroupManagerImpl<UT extends PersistentUser, GT extends PersistentGroup, UMT extends PersistentUserManager<UT>, GMT extends PersistentGroupManager<UT, GT>>
        extends HibernateEntityManager<GT, IdentityHeader>
        implements PersistentGroupManager<UT, GT>
{
    private final String HQL_DELETE_BY_PROVIDEROID =
            "FROM grp IN CLASS " + getImpClass().getName() +
                    " WHERE grp.providerId = ?";

    private final String HQL_DELETE_MEMBERSHIPS_BY_PROVIDEROID =
            "FROM mem IN CLASS " + getMembershipClass().getName() +
                    " WHERE mem.thisGroupProviderGoid = ?";

    private static final String HQL_DELETE_VIRTUAL_BY_PROVIDEROID =
            "FROM vg IN CLASS " + VirtualGroup.class.getName() + " WHERE vg.providerId = ?";

    private final String HQL_GETGROUPS =
            "select grp from " +
                    "grp in class " + getImpClass().getName() + ", " +
                    "mem in class " + getMembershipClass().getName() +
                " where mem.thisGroupId = grp.goid " +
                "and mem.memberUserId = ?";

    private final String HQL_ISMEMBER = "from membership in class "+ getMembershipClass().getName() +
          " where membership.memberUserId = ? " +
          "and membership.thisGroupId = ?";

    private String getMembersQuery;

    protected PersistentGroupManagerImpl() {
    }

    public synchronized void setIdentityProvider(final PersistentIdentityProvider<UT, GT, UMT, GMT> identityProvider) {
        if ( this.identityProvider != null ) throw new IllegalStateException("identityProvider is already set");
        this.identityProvider = identityProvider;
    }

    private synchronized String getMemberQueryString() {
        if (getMembersQuery == null) {
            StringBuilder queryString = new StringBuilder("select usr from usr in class ");
            queryString.append(identityProvider.getUserManager().getImpClass().getName()).append(", ");
            queryString.append("membership in class ").append(getMembershipClass().getName());
            queryString.append(" where membership.memberUserId = usr.goid ");
            queryString.append("and membership.thisGroupId = ?");
            getMembersQuery = queryString.toString();
        }
        return getMembersQuery;
    }

    @Override
    public GT findByName(String name) throws FindException {
        GT pg = super.findByUniqueName(name);
        if (pg != null) pg.setProviderId(identityProvider.getConfig().getGoid());
        return pg;
    }

    @Override
    public GT findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            GT out = findByPrimaryKey(getImpClass(), Goid.parseGoid(oid));
            if (out == null) return null;
            out.setProviderId(getProviderGoid());
            return out;
        } catch (IllegalArgumentException iae) {
            throw new FindException("Can't find groups with bad IDs: " + oid, iae);
        }
    }

    /**
     * Search for the group headers using the given search string.
     *
     * @param searchString the search string (supports '*' wildcards)
     * @return the never <b>null</b> collection of entitites
     * @throws com.l7tech.objectmodel.FindException
     *          thrown if an SQL error is encountered
     * @see PersistentGroupManager
     * @see PersistentUserManager
     */
    @Override
    public Collection<IdentityHeader> search(final String searchString) throws FindException {
        try {
            //noinspection unchecked
            return (Collection<IdentityHeader>) getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
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
                        //noinspection unchecked
                        GT group = (GT)result;
                        headers.add( newHeader(group) );
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

    @Override
    protected IdentityHeader newHeader(GT entity) {
        return new IdentityHeader(getProviderGoid(), entity.getGoid(), EntityType.GROUP, entity.getName(), entity.getDescription(), null, entity.getVersion());
    }

    protected Goid getProviderGoid() {
        return identityProvider.getConfig().getGoid();
    }

    /**
     * Must be called in a transaction!
     */
    @Override
    public void delete(Goid oid) throws DeleteException, FindException {
        findAndDelete( oid );        
    }

    /**
     * Must be called in a transaction!
     */
    @Override
    public void delete(GT group) throws DeleteException {
        try {
            GT pgroup = cast(group);
            preDelete(pgroup);
            Set<IdentityHeader> userHeaders = getUserHeaders(pgroup);
            for (Object userHeader : userHeaders) {
                UT u = identityProvider.getUserManager().headerToUser((IdentityHeader) userHeader);
                deleteMembership(pgroup, u);
            }
            getHibernateTemplate().delete(pgroup);
            postDelete( pgroup );
        } catch (ObjectModelException e) {
            throw new DeleteException(e.getMessage(), e);
        } catch (HibernateException e) {
            throw new DeleteException(e.toString(), e);
        }
    }

    protected abstract Class getMembershipClass();

    protected void preSave(GT persistentGroup) throws SaveException { }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void preDelete(GT group) throws DeleteException { }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void postDelete(GT group) throws DeleteException { }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void preUpdate(GT group) throws FindException, UpdateException { }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void postUpdate(GT group) throws UpdateException { }

    protected abstract GT cast(Group group);

    @Override
    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        final String msg = "Couldn't find group to be deleted";
        try {
            GT g = findByPrimaryKey(identifier);
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
    @Override
    public void deleteAll(final Goid ipoid) throws DeleteException, ObjectNotFoundException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    // Delete all group members
                    Query q = session.createQuery(HQL_DELETE_MEMBERSHIPS_BY_PROVIDEROID);
                    q.setBinary(0, ipoid.getBytes());

                    for (Iterator i = q.iterate(); i.hasNext();) {
                        session.delete(i.next());
                    }

                    // Delete all groups
                    q = session.createQuery(HQL_DELETE_BY_PROVIDEROID);
                    q.setBinary(0, ipoid.getBytes());
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
        
        postDelete( null );
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
    @Override
    public void deleteAllVirtual(final Goid ipoid) throws DeleteException, ObjectNotFoundException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE_VIRTUAL_BY_PROVIDEROID);
                    q.setBinary(0, ipoid.getBytes());
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

    @Override
    public String saveGroup(GT group) throws SaveException {
        return save(group, null);
    }

    @Override
    public String save(GT group, Set<IdentityHeader> userHeaders) throws SaveException {
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

            GT imp = cast(group);
            imp.setProviderId(identityProvider.getConfig().getGoid());
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

    @Override
    public void update(GT group) throws UpdateException {
        update(group, null);
    }

    @Override
    public void update(GT group, Set<IdentityHeader> userHeaders) throws UpdateException {
        GT imp = cast(group);

        try {
            // if this is the admin group, make sure that we are not removing all memberships
            preUpdate(imp);
            GT originalGroup = findByPrimaryKey(group.getId());
            if (originalGroup == null) {
                throw new FindException("Couldn't find original version of Group " + group.getName());
            }
            // check for version conflict
            if (originalGroup.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            if (!group.getProviderId().equals(identityProvider.getConfig().getGoid())) throw new IllegalArgumentException("Can't update a Group from a different provider");
            setUserHeaders(group.getId(), userHeaders);

            originalGroup.copyFrom(imp);
            getHibernateTemplate().update(originalGroup);
            postUpdate(originalGroup);
        } catch (FindException e) {
            throw new UpdateException("Update called on group that does not already exist", e);
        } catch (DataAccessException se) {
            throw new UpdateException(se.toString(), se);
        }
    }

    @Override
    public boolean isMember(final User user, final GT group) throws FindException {
        if (!checkProvider(user)) return false;
        try {
            return (Boolean)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query query = session.createQuery(HQL_ISMEMBER);
                    query.setBinary(0, Goid.parseGoid(user.getId()).getBytes());
                    query.setBinary(1, Goid.parseGoid(group.getId()).getBytes());
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
        if (Goid.isDefault(user.getProviderId())) {
            msg = "User was authenticated by an unknown identity provider";
            level = Level.WARNING;
        } else if (!user.getProviderId().equals(identityProvider.getConfig().getGoid())) {
            msg = "User was authenticated by a different identity provider";
            level = Level.FINE;
        }

        if (msg != null) {
            logger.log(level, msg);
            return false;
        }

        return true;
    }

    @Override
    public void addUsers(GT group, Set<UT> users) throws FindException, UpdateException {
        GT pgroup = cast(group);
        try {
            for (UT user : users) {
                getHibernateTemplate().save(newMembership(pgroup, user));
            }
        } catch (Exception e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    @Override
    public void removeUsers(GT group, Set<UT> users) throws FindException, UpdateException {
        GT pgroup = cast(group);
        try {
            for (UT user : users) {
                deleteMembership(pgroup, user);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }

        try {
            postDelete( null );
        } catch ( DeleteException de ) {
            throw new UpdateException( ExceptionUtils.getMessage(de), de );
        }
    }

    @Override
    public void deleteMembership(final GT group, final UT user) throws HibernateException {
        getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(getMembershipClass());
                addMembershipCriteria(crit, group, user);
                crit.add(Restrictions.eq("memberUserId", user.getGoid()));
                crit.add(Restrictions.eq("thisGroupId", group.getGoid()));
                addMembershipCriteria(crit, group, user);
                List toBeDeleted = crit.list();
                if (toBeDeleted == null || toBeDeleted.size() == 0) {
                    throw new RuntimeException("Couldn't find membership to be deleted; user " + user.getGoid() + ", group " + group.getGoid());
                } else if (toBeDeleted.size() > 1) {
                    throw new RuntimeException("Found more than one membership to be deleted; user " + user.getGoid() + ", group " + group.getGoid());
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

    @Override
    public void addUser(UT user, Set<GT> groups) throws FindException, UpdateException {
        UT puser = identityProvider.getUserManager().cast(user);
        try {
            for (GT group : groups) {
                getHibernateTemplate().save(newMembership(group, puser));
            }
        } catch (Exception e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    @Override
    public void removeUser(UT user, Set<GT> groups) throws FindException, UpdateException {
        UT puser = identityProvider.getUserManager().cast(user);
        try {
            for (GT group : groups) {
                deleteMembership(group, puser);
            }
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }

        try {
            postDelete( null );
        } catch ( DeleteException de ) {
            throw new UpdateException( ExceptionUtils.getMessage(de), de );
        }
    }

    @Override
    public void addUser(UT user, GT group) throws FindException, UpdateException {
        UT userImp = identityProvider.getUserManager().cast(user);
        GT groupImp = cast(group);
        try {
            getHibernateTemplate().save(newMembership(groupImp, userImp));
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    @Override
    public void removeUser(UT user, GT group) throws FindException, UpdateException {
        UT userImp = identityProvider.getUserManager().cast(user);
        GT groupImp = cast(group);
        try {
            deleteMembership(groupImp, userImp);
        } catch (HibernateException e) {
            throw new FindException(e.getMessage(), e);
        }

        try {
            postDelete( null );
        } catch ( DeleteException de ) {
            throw new UpdateException( ExceptionUtils.getMessage(de), de );
        }
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(UT user) throws FindException {
        return getGroupHeaders(user.getId());
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(String userId) throws FindException {
        try {
            return doGetGroupHeaders(userId);
        } catch (HibernateException he) {
            throw new FindException(he.toString(), he);
        }

    }

    @Override
    public Set<IdentityHeader> getGroupHeadersForNestedGroup(String groupId) throws FindException {
        // nested groups are not supported
        return Collections.emptySet();
    }

    @Override
    public void setGroupHeaders(UT user, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        setGroupHeaders(user.getId(), groupHeaders);
    }

    @Override
    public void setGroupHeaders(String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        if (groupHeaders == null) return;
        try {
            Set<String> newGids = headersToIds(groupHeaders);
            Set<String> existingGids = headersToIds(doGetGroupHeaders(userId));

            GroupMembership gm;
            UT thisUser = identityProvider.getUserManager().findByPrimaryKey(userId);

            // Check for new memberships
            for (String newGid : newGids) {
                if (!existingGids.contains(newGid)) {
                    GT newGroup = cast(findByPrimaryKey(newGid));
                    gm = newMembership(newGroup, thisUser);
                    getHibernateTemplate().save(gm);
                }
            }

            // Check for removed memberships
            for (String existingGid : existingGids) {
                if (!newGids.contains(existingGid)) {
                    GT oldGroup = findByPrimaryKey(existingGid);
                    deleteMembership(oldGroup, thisUser);
                }
            }
        } catch (HibernateException he) {
            throw new UpdateException(he.toString(), he);
        }

    }

    @Override
    public Set<IdentityHeader> getUserHeaders(GT group) throws FindException {
        return getUserHeaders(group.getId());
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
        try {
            return doGetUserHeaders(Goid.parseGoid(groupId));
        } catch (HibernateException he) {
            throw new FindException(he.toString(), he);
        }
    }

    private Set<IdentityHeader> doGetUserHeaders(final Goid groupId) throws HibernateException {
        //noinspection unchecked
        return (Set<IdentityHeader>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Set<IdentityHeader> headers = new HashSet<IdentityHeader>();
                Query query = session.createQuery(getMemberQueryString());
                query.setBinary(0, groupId.getBytes());
                for (Iterator i = query.iterate(); i.hasNext();) {
                    //noinspection unchecked
                    UT user = (UT) i.next();
                    headers.add(identityProvider.getUserManager().userToHeader(user));
                }
                return headers;
            }
        });
    }

    private Set<IdentityHeader> doGetGroupHeaders(final String userId) throws HibernateException {
        //noinspection unchecked
        return (Set<IdentityHeader>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Set<IdentityHeader> headers = new HashSet<IdentityHeader>();
                Query query = session.createQuery(HQL_GETGROUPS);
                Goid userGoid;
                try{
                    userGoid =  Goid.parseGoid(userId);
                }catch(IllegalArgumentException e){
                    throw new SQLException(e);
                }
                query.setBinary(0,userGoid.getBytes());
                for (Iterator i = query.iterate(); i.hasNext();) {
                    //noinspection unchecked
                    GT group = (GT) i.next();
                    headers.add( newHeader( group ) );
                }
                return headers;
            }
        });
    }

    @Override
    public void setUserHeaders(GT group, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        setUserHeaders(group.getId(), groupHeaders);
    }

    private Set<String> headersToIds(Set<IdentityHeader> headers) {
        Set<String> uids = new HashSet<String>();
        for (EntityHeader header : headers) {
            uids.add(header.getStrId());
        }
        return uids;
    }

    @Override
    public void setUserHeaders(String groupId, Set<IdentityHeader> userHeaders) throws FindException, UpdateException {
        if (userHeaders == null) return;
        try {
            Set<String> newUids = headersToIds(userHeaders);
            Set<String> existingUids = headersToIds(doGetUserHeaders(Goid.parseGoid(groupId)));

            GT thisGroup = findByPrimaryKey(groupId);

            // Check for new memberships
            final UMT uman = identityProvider.getUserManager();
            for (String newUid : newUids) {
                if (!existingUids.contains(newUid)) {
                    UT newUser = uman.findByPrimaryKey(newUid);
                    getHibernateTemplate().save(newMembership(thisGroup, newUser));
                }
            }

            // Check for removed memberships
            for (String existingUid : existingUids) {
                if (!newUids.contains(existingUid)) {
                    UT oldUser = uman.findByPrimaryKey(existingUid);
                    deleteMembership(thisGroup, oldUser);
                }
            }
        } catch (HibernateException he) {
            throw new UpdateException(he.toString(), he);
        }

    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(GT entity) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("providerId", entity.getProviderId());
        attrs.put("name", entity.getName());
        return Arrays.asList(attrs);
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.GROUP;
    }

    protected PersistentIdentityProvider<UT, GT, UMT, GMT> identityProvider;

    // Not static on purpose; we want to pretend to be the subclass
    private final Logger logger = Logger.getLogger(getClass().getName());

}
