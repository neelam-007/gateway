/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.objectmodel.*;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentGroupManager extends HibernateEntityManager implements GroupManager {
    public PersistentGroupManager(IdentityProvider provider) {
        this.provider = provider;

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

    public Group findByName(String name) throws FindException {
        try {
            List groups = PersistenceManager.find(getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".name = ?", name, String.class);
            switch (groups.size()) {
                case 0:
                    return null;
                case 1:
                    PersistentGroup g = (PersistentGroup)groups.get(0);
                    g.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
                    return g;
                default:
                    String err = "Found more than one group with the name " + name;
                    logger.log(Level.SEVERE, err);
                    throw new FindException(err);
            }
        } catch (SQLException se) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException(se.toString(), se);
        }

    }

    public Group findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            PersistentGroup out = (PersistentGroup)PersistenceManager.findByPrimaryKey(getContext(), getImpClass(), Long.parseLong(oid));
            if (out == null) return null;
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch (SQLException se) {
            throw new FindException(se.toString(), se);
        } catch (NumberFormatException nfe) {
            throw new FindException("Can't find groups with non-numeric OIDs!", nfe);
        }
    }

    /**
     * Search for the group headers using the given search string.
     *
     * @param searchString the search string (supports '*' wildcards)
     * @return the never <b>null</b> collection of entitites
     * @throws com.l7tech.objectmodel.FindException thrown if an SQL error is encountered
     * @see com.l7tech.server.identity.internal.InternalGroupManagerServer
     * @see com.l7tech.server.identity.internal.InternalUserManagerServer
     */
    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            List results = PersistenceManager.find(getContext(),
              getAllHeadersQuery() + " where " + getTableName() + ".name like ?",
              searchString, String.class);
            List headers = new ArrayList();
            for (Iterator i = results.iterator(); i.hasNext();) {
                Object[] row = (Object[])i.next();
                final long id = ((Long)row[0]).longValue();
                headers.add(new EntityHeader(id, EntityType.fromInterface(getInterfaceClass()), row[1].toString(), EMPTY_STRING));
            }
            return Collections.unmodifiableList(headers);
        } catch (SQLException e) {
            final String msg = "Error while searching for " + getInterfaceClass() + " instances.";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
    }

    /** Must be called in a transaction! */
    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)getContext();
            // it is not allowed to delete the admin group
            Group imp = cast(group);
            long oid = new Long(imp.getUniqueIdentifier()).longValue();
            preDelete(imp);
            Set userHeaders = getUserHeaders(imp);
            Session s = context.getSession();
            for ( Iterator i = userHeaders.iterator(); i.hasNext(); ) {
                EntityHeader userHeader = (EntityHeader) i.next();
                s.delete(newMembership(userHeader.getOid(), oid));
            }
            s.delete(group);
        } catch (SQLException se) {
            throw new DeleteException(se.toString(), se);
        } catch ( FindException e ) {
            throw new DeleteException(e.toString(), e);
        } catch ( HibernateException e ) {
            throw new DeleteException(e.toString(), e);
        }
    }

    protected abstract GroupMembership newMembership( long userOid, long groupOid );

    protected abstract Class getMembershipClass();

    protected void preDelete( Group group ) throws DeleteException { }

    protected void preUpdate( Group group ) throws FindException, UpdateException { }

    protected abstract PersistentGroup cast( Group group );

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        final String msg = "Couldn't find group to be deleted";
        try {
            Group g = findByPrimaryKey(identifier);
            if ( g == null ) {
                throw new ObjectNotFoundException(msg);
            }
            delete(g);
        } catch ( FindException e ) {
            logger.log( Level.WARNING, e.getMessage(), e );
            throw new ObjectNotFoundException(msg, e);
        }
    }

    public String save( Group group ) throws SaveException {
        return save( group, null );
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
                throw new SaveException("This group cannot be saved because an existing group already uses the name '" + group.getName() + "'");
            }

            Group imp = cast(group);
            String oid = Long.toString( PersistenceManager.save(getContext(), (NamedEntity)imp) );

            if ( userHeaders != null ) {
                try {
                    setUserHeaders( oid, userHeaders );
                } catch (FindException e) {
                    logger.log( Level.SEVERE, e.getMessage() );
                    throw new SaveException( e.getMessage(), e );
                } catch (UpdateException e) {
                    logger.log( Level.SEVERE, e.getMessage() );
                    throw new SaveException( e.getMessage(), e );
                }
            }

            return oid;
        } catch (SQLException se) {
            throw new SaveException(se.toString(), se);
        }
    }

    public void update( Group group ) throws UpdateException, ObjectNotFoundException {
        update( group, null );
    }

    public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
        PersistentGroup imp = cast( group );

        try {
            // if this is the admin group, make sure that we are not removing all memberships
            preUpdate( group );
            PersistentGroup originalGroup = (PersistentGroup)findByPrimaryKey( group.getUniqueIdentifier() );
            if (originalGroup == null) {
                throw new ObjectNotFoundException("Group "+group.getName());
            }
            // check for version conflict
            if (originalGroup.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            setUserHeaders( group.getUniqueIdentifier(), userHeaders );

            originalGroup.copyFrom(imp);
            PersistenceManager.update(getContext(), originalGroup);
        } catch (FindException e) {
            throw new UpdateException("Update called on group that does not already exist", e);
        } catch (SQLException se) {
            throw new UpdateException(se.toString(), se);
        }
    }

    public boolean isMember( User user, Group group ) throws FindException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            Query query = s.createQuery( HQL_ISMEMBER );
            query.setString( 0, user.getUniqueIdentifier() );
            query.setString( 1, group.getUniqueIdentifier() );
            return ( query.iterate().hasNext() );
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        }
    }

    public void addUsers(Group group, Set users) throws FindException, UpdateException {
        PersistentGroup imp = cast( group );
        GroupMembership membership = newMembership( -1, imp.getOid() );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = users.iterator(); i.hasNext();) {
                PersistentUser user = (PersistentUser)i.next();
                membership.setUserOid( user.getOid() );
                s.save( membership );
            }
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        }
    }

    public void removeUsers( Group group, Set users ) throws FindException, UpdateException {
        PersistentGroup imp = cast(group);
        GroupMembership membership = newMembership( -1, imp.getOid() );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = users.iterator(); i.hasNext();) {
                PersistentUser user = (PersistentUser)i.next();
                membership.setUserOid( user.getOid() );
                s.delete( membership );
            }
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        }
    }

    public void addUser(User user, Set groups) throws FindException, UpdateException {
        PersistentUser imp = getUserManager().cast( user );
        GroupMembership membership = newMembership( imp.getOid(), -1 );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = groups.iterator(); i.hasNext();) {
                PersistentGroup group = (PersistentGroup)i.next();
                membership.setGroupOid( group.getOid() );
                s.save( membership );
            }
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        }
    }

    private PersistentUserManager getUserManager() {
        return (PersistentUserManager)provider.getUserManager();
    }

    public void removeUser(User user, Set groups) throws FindException, UpdateException {
        PersistentUser imp = getUserManager().cast( user );
        GroupMembership membership = newMembership( imp.getOid(), -1 );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = groups.iterator(); i.hasNext();) {
                PersistentGroup group = (PersistentGroup)i.next();
                membership.setGroupOid( group.getOid() );
                s.delete( membership );
            }
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        }
    }

    public void addUser( User user, Group group ) throws FindException, UpdateException {
        PersistentUser userImp = getUserManager().cast(user);
        PersistentGroup groupImp = cast(group);
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();

            s.save(newMembership( userImp.getOid(), groupImp.getOid() ));
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        }
    }

    public void removeUser( User user, Group group ) throws FindException, UpdateException {
        PersistentUser userImp = getUserManager().cast(user);
        PersistentGroup groupImp = cast(group);
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            s.delete(newMembership( userImp.getOid(), groupImp.getOid() ));
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        }
    }

    public Set getGroupHeaders(User user) throws FindException {
        return getGroupHeaders( user.getUniqueIdentifier() );
    }

    public Set getGroupHeaders( String userId ) throws FindException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            return doGetGroupHeaders( hpc, userId );
        } catch (SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        }

    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        setGroupHeaders( user.getUniqueIdentifier(), groupHeaders );
    }

    public void setGroupHeaders( String userId, Set groupHeaders ) throws FindException, UpdateException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();

            Set newGids = headersToIds( groupHeaders );
            Set existingGids = headersToIds( doGetGroupHeaders( hpc, userId ) );

            GroupMembership gm;
            long uoid = new Long( userId ).longValue();

            // Check for new memberships
            for (Iterator j = newGids.iterator(); j.hasNext();) {
                String newGid = (String)j.next();
                if ( !existingGids.contains( newGid ) ) {
                    gm = newMembership( uoid, new Long( newGid ).longValue() );
                    s.save( gm );
                }
            }

            // Check for removed memberships
            for (Iterator i = existingGids.iterator(); i.hasNext();) {
                String existingGid = (String) i.next();
                if ( !newGids.contains( existingGid ) ) {
                    Group g = findByPrimaryKey( existingGid );
                    if ( Group.ADMIN_GROUP_NAME.equals( g.getName() ) ) {
                        Set adminUserHeaders = getUserHeaders(g);
                        if ( adminUserHeaders.size() < 2 ) {
                            String msg = "Can't remove last administrator membership!";
                            logger.info( msg );
                            throw new UpdateException( msg );
                        }
                    }
                    gm = newMembership( uoid, new Long( existingGid ).longValue() );
                    s.delete( gm );
                }
            }
        } catch (SQLException se ) {
            throw new UpdateException( se.toString(), se );
        } catch ( HibernateException he ) {
            throw new UpdateException( he.toString(), he );
        }

    }

    private HibernatePersistenceContext context() throws SQLException {
        return (HibernatePersistenceContext)PersistenceContext.getCurrent();
    }

    public Set getUserHeaders(Group group) throws FindException {
        return getUserHeaders( group.getUniqueIdentifier() );
    }

    public Set getUserHeaders(String groupId) throws FindException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            return doGetUserHeaders( hpc, groupId);
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        }
    }

    private Set doGetUserHeaders( HibernatePersistenceContext hpc, String groupId) throws SQLException, HibernateException {
        Set headers = new HashSet();
        Session s = hpc.getSession();
        String hql = HQL_GETUSERS;
        Query query = s.createQuery( hql );
        query.setString( 0, groupId );
        for (Iterator i = query.iterate(); i.hasNext();) {
            PersistentUser user = (PersistentUser)i.next();
            headers.add(provider.getUserManager().userToHeader(user));
        }
        return headers;
    }

    private Set doGetGroupHeaders( HibernatePersistenceContext hpc, String userId ) throws SQLException, HibernateException {
        Set headers = new HashSet();
        Session s = hpc.getSession();
        String hql = HQL_GETGROUPS;
        Query query = s.createQuery( hql );
        query.setString( 0, userId );
        for (Iterator i = query.iterate(); i.hasNext();) {
            PersistentGroup group = (PersistentGroup)i.next();
            headers.add( new EntityHeader( group.getOid(), EntityType.GROUP, group.getName(), null ) );
        }
        return headers;
    }

    public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
        setUserHeaders( group.getUniqueIdentifier(), groupHeaders );
    }

    private Set headersToIds( Set headers ) {
        Set uids = new HashSet();
        for (Iterator i = headers.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader) i.next();
            uids.add( header.getStrId() );
        }
        return uids;
    }

    public void setUserHeaders(String groupId, Set userHeaders) throws FindException, UpdateException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();

            Session s = hpc.getSession();

            Set newUids = headersToIds( userHeaders );
            Set existingUids = headersToIds( doGetUserHeaders( hpc, groupId ) );

            Group group = findByPrimaryKey( groupId );
            if ( Group.ADMIN_GROUP_NAME.equals( group.getName() ) &&
                userHeaders.size() == 0 ) {
                String msg = "Can't delete last administrator";
                logger.info( msg );
                throw new UpdateException( msg );
            }

            GroupMembership gm;
            long goid = new Long( groupId ).longValue();

            // Check for new memberships
            for (Iterator j = newUids.iterator(); j.hasNext();) {
                String newUid = (String)j.next();
                if ( !existingUids.contains( newUid ) ) {
                    gm = newMembership( new Long( newUid ).longValue(), goid );
                    s.save( gm );
                }
            }

            // Check for removed memberships
            for (Iterator i = existingUids.iterator(); i.hasNext();) {
                String existingUid = (String) i.next();
                if ( !newUids.contains( existingUid ) ) {
                    gm = newMembership( new Long( existingUid ).longValue(), goid );
                    s.delete( gm );
                }
            }
        } catch (SQLException se ) {
            throw new UpdateException( se.toString(), se );
        } catch ( HibernateException he ) {
            throw new UpdateException( he.toString(), he );
        }

    }

    public abstract String getTableName();

    public abstract Class getImpClass();

    public abstract Class getInterfaceClass();

    protected final IdentityProvider provider;
    private final Logger logger = Logger.getLogger(getClass().getName());

    private final String HQL_GETGROUPS;
    private final String HQL_GETUSERS;
    private final String HQL_ISMEMBER;
}
