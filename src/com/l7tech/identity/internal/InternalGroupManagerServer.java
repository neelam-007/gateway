package com.l7tech.identity.internal;

import cirrus.hibernate.*;
import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.TransactionException;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 */
public class InternalGroupManagerServer extends HibernateEntityManager implements GroupManager {
    public InternalGroupManagerServer() {
        super();
        logger = LogManager.getInstance().getSystemLogger();
    }

    public Group findByName(String name) throws FindException {
        try {
            List groups = _manager.find(getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".name = ?", name, String.class);
            switch (groups.size()) {
                case 0:
                    return null;
                case 1:
                    InternalGroup g = (InternalGroup)groups.get(0);
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
            InternalGroup out = (InternalGroup)_manager.findByPrimaryKey(getContext(), getImpClass(), Long.parseLong(oid));
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
     * @throws FindException thrown if an SQL error is encountered
     * @see InternalGroupManagerServer
     * @see InternalUserManagerServer
     */
    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            List results = PersistenceManager.find(getContext(),
              allHeadersQuery + " where " + getTableName() + ".name like ?",
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

    public void delete(Group group) throws DeleteException {
        try {
            InternalGroup imp = cast( group );
            // it is not allowed to delete the admin group
            if ( Group.ADMIN_GROUP_NAME.equals( imp.getName() ) ) {
                logger.severe("an attempt to delete the admin group was made.");
                throw new DeleteException("Cannot delete administrator group.");
            }
            _manager.delete(getContext(), imp);
        } catch (SQLException se) {
            throw new DeleteException(se.toString(), se);
        }
    }

    public void delete(String identifier) throws DeleteException {
        InternalGroup imp = new InternalGroup();
        imp.setOid( Long.valueOf( identifier ).longValue() );
        delete( imp );
    }

    public String save(Group group) throws SaveException {
        try {
            InternalGroup imp = cast(group);
            // check that no existing group have same name
            Group existingGrp = null;
            try {
                existingGrp = findByPrimaryKey( group.getUniqueIdentifier() );
            } catch (FindException e) {
                existingGrp = null;
            }
            if (existingGrp != null) {
                throw new SaveException("This group cannot be saved because an existing group already uses the name '" + group.getName() + "'");
            }
            return new Long( _manager.save(getContext(), imp) ).toString();
        } catch (SQLException se) {
            throw new SaveException(se.toString(), se);
        }
    }

    static InternalGroup cast(Group group) {
        InternalGroup imp;
        if ( group instanceof GroupBean ) {
            imp = new InternalGroup( (GroupBean)group );
        } else {
            imp = (InternalGroup)group;
        }
        return imp;
    }

    public void update(Group group) throws UpdateException {
        InternalGroup imp = cast( group );

        try {
            // if this is the admin group, make sure that we are not removing all memberships
            if (Group.ADMIN_GROUP_NAME.equals(group.getName())) {
                if (group.getMembers().size() < 1) {
                    logger.severe("Blocked update on admin group because all members were deleted.");
                    throw new UpdateException("Cannot update admin group with no memberships!");
                }
            }
            InternalGroup originalGroup = (InternalGroup)findByPrimaryKey( group.getUniqueIdentifier() );

            // check for version conflict
            if (originalGroup.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            originalGroup.copyFrom(imp);
            _manager.update(getContext(), originalGroup);
        } catch (FindException e) {
            throw new UpdateException("Update called on group that does not already exist", e);
        } catch (SQLException se) {
            throw new UpdateException(se.toString(), se);
        }
    }

    public EntityHeader groupToHeader(Group group) {
        return new EntityHeader(group.getUniqueIdentifier(), EntityType.GROUP, group.getName(), group.getDescription());
    }

    public Group headerToGroup(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getStrId());
    }

    public boolean isMember( User user, Group group ) throws FindException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            Query query = s.createQuery( HQL_ISMEMBER );
            return ( query.iterate().hasNext() );
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    public void addUsers(Group group, Set users) throws FindException, UpdateException {
        InternalGroup imp = cast( group );
        GroupMembership membership = new GroupMembership( -1, imp.getOid() );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = users.iterator(); i.hasNext();) {
                InternalUser user = (InternalUser)i.next();
                membership.setUserOid( user.getOid() );
                s.save( membership );
            }
            s.flush();
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    public void removeUsers( Group group, Set users ) throws FindException, UpdateException {
        InternalGroup imp = cast(group);
        GroupMembership membership = new GroupMembership( -1, imp.getOid() );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = users.iterator(); i.hasNext();) {
                InternalUser user = (InternalUser)i.next();
                membership.setUserOid( user.getOid() );
                s.delete( membership );
            }
            s.flush();
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    public void addUser(User user, Set groups) throws FindException, UpdateException {
        InternalUser imp = InternalUserManagerServer.cast( user );
        GroupMembership membership = new GroupMembership( imp.getOid(), -1 );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = groups.iterator(); i.hasNext();) {
                InternalGroup group = (InternalGroup)i.next();
                membership.setGroupOid( group.getOid() );
                s.save( membership );
            }
            s.flush();
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    public void removeUser(User user, Set groups) throws FindException, UpdateException {
        InternalUser imp = InternalUserManagerServer.cast( user );
        GroupMembership membership = new GroupMembership( imp.getOid(), -1 );
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            for (Iterator i = groups.iterator(); i.hasNext();) {
                InternalGroup group = (InternalGroup)i.next();
                membership.setGroupOid( group.getOid() );
                s.delete( membership );
            }
            s.flush();
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    public void addUser( User user, Group group ) throws FindException, UpdateException {
        InternalUser userImp = InternalUserManagerServer.cast(user);
        InternalGroup groupImp = cast(group);
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();

            s.save( new GroupMembership( userImp.getOid(), groupImp.getOid() ) );

            s.flush();
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    public void removeUser( User user, Group group ) throws FindException, UpdateException {
        InternalUser userImp = InternalUserManagerServer.cast(user);
        InternalGroup groupImp = cast(group);
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            Session s = hpc.getSession();
            s.delete( new GroupMembership( userImp.getOid(), groupImp.getOid() ) );
            s.flush();
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
        } finally {
            if ( hpc != null ) hpc.close();
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
        } finally {
            if ( hpc != null ) hpc.close();
        }

    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        setGroupHeaders( user.getUniqueIdentifier(), groupHeaders );
    }

    public void setGroupHeaders( String userId, Set groupHeaders ) throws FindException, UpdateException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = context();
            hpc.beginTransaction();
            Session s = hpc.getSession();

            Set newGids = headersToIds( groupHeaders );
            Set existingGids = headersToIds( doGetGroupHeaders( hpc, userId ) );

            GroupMembership gm;
            long uoid = new Long( userId ).longValue();

            // Check for new memberships
            for (Iterator j = newGids.iterator(); j.hasNext();) {
                String newGid = (String)j.next();
                if ( !existingGids.contains( newGid ) ) {
                    gm = new GroupMembership( uoid, new Long( newGid ).longValue() );
                    s.save( gm );
                }
            }

            // Check for removed memberships
            for (Iterator i = existingGids.iterator(); i.hasNext();) {
                String existingGid = (String) i.next();
                if ( !newGids.contains( existingGid ) ) {
                    gm = new GroupMembership( uoid, new Long( existingGid ).longValue() );
                    s.delete( gm );
                }
            }

            hpc.commitTransaction();
        } catch (SQLException se ) {
            throw new UpdateException( se.toString(), se );
        } catch ( TransactionException te ) {
            throw new UpdateException( te.toString(), te );
        } catch ( HibernateException he ) {
            throw new UpdateException( he.toString(), he );
        } finally {
            if ( hpc != null ) hpc.close();
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
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    private Set doGetUserHeaders( HibernatePersistenceContext hpc, String groupId) throws SQLException, HibernateException {
        Set headers = new HashSet();
        Session s = hpc.getSession();
        String hql = HQL_GETUSERS;
        Query query = s.createQuery( hql );
        query.setString( 0, groupId );
        for (Iterator i = query.iterate(); i.hasNext();) {
            InternalUser user = (InternalUser)i.next();
            headers.add( new EntityHeader( user.getOid(), EntityType.USER, user.getName(), null ) );
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
            InternalGroup group = (InternalGroup)i.next();
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
            hpc.beginTransaction();

            Session s = hpc.getSession();

            Set newUids = headersToIds( userHeaders );
            Set existingUids = headersToIds( doGetUserHeaders( hpc, groupId ) );

            GroupMembership gm;
            long goid = new Long( groupId ).longValue();

            // Check for new memberships
            for (Iterator j = newUids.iterator(); j.hasNext();) {
                String newUid = (String)j.next();
                if ( !existingUids.contains( newUid ) ) {
                    gm = new GroupMembership( new Long( newUid ).longValue(), goid );
                    s.save( gm );
                }
            }

            // Check for removed memberships
            for (Iterator i = existingUids.iterator(); i.hasNext();) {
                String existingUid = (String) i.next();
                if ( !newUids.contains( existingUid ) ) {
                    gm = new GroupMembership( new Long( existingUid ).longValue(), goid );
                    s.delete( gm );
                }
            }

            hpc.commitTransaction();
        } catch (SQLException se ) {
            throw new UpdateException( se.toString(), se );
        } catch ( TransactionException te ) {
            throw new UpdateException( te.toString(), te );
        } catch ( HibernateException he ) {
            throw new UpdateException( he.toString(), he );
        } finally {
            if ( hpc != null ) hpc.close();
        }

    }

    public String getTableName() {
        return "internal_group";
    }

    public Class getImpClass() {
        return InternalGroup.class;
    }

    public static final String IMPCLASSNAME = InternalGroup.class.getName();
    public static final String GMCLASSNAME = GroupMembership.class.getName();

    public Class getInterfaceClass() {
        return Group.class;
    }

    private Logger logger = null;

    public static final String HQL_GETGROUPS = "select grp from grp in class " + IMPCLASSNAME + ", " +
             "membership in class " + GMCLASSNAME + " " +
             "where membership.groupOid = grp.oid " +
             "and membership.userOid = ?";

    public static final String HQL_GETUSERS = "select usr from usr in class " + InternalUserManagerServer.IMPCLASSNAME + ", " +
                         "membership in class " + GMCLASSNAME + " " +
                         "where membership.userOid = usr.oid " +
                         "and membership.groupOid = ?";

    public static final String HQL_ISMEMBER = "from membership in class " + GMCLASSNAME + " " +
                             "where membership.userOid = ? " +
                             "and membership.groupOid = ?";
}
