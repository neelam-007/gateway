package com.l7tech.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.hibernate.Session;
import net.sf.hibernate.Query;
import net.sf.hibernate.HibernateException;

/**
 * GroupManager implementation for the internal identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
public class InternalGroupManagerServer extends HibernateEntityManager implements GroupManager {
    public InternalGroupManagerServer( InternalIdentityProviderServer provider ) {
        super();
        _provider = provider;
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
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            InternalGroup out = (InternalGroup)_manager.findByPrimaryKey(getContext(), getImpClass(), Long.parseLong(oid));
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

    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
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

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        InternalGroup imp = new InternalGroup();
        imp.setOid( Long.valueOf( identifier ).longValue() );
        delete( imp );
    }

    public String save( Group group ) throws SaveException {
        return save( group, null );
    }

    public String save(Group group, Set userHeaders) throws SaveException {
        try {
            InternalGroup imp = cast(group);
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
            String oid = Long.toString( _manager.save(getContext(), imp) );

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

    static InternalGroup cast(Group group) {
        InternalGroup imp;
        if ( group instanceof GroupBean ) {
            imp = new InternalGroup( (GroupBean)group );
        } else {
            imp = (InternalGroup)group;
        }
        return imp;
    }

    public void update( Group group ) throws UpdateException, ObjectNotFoundException {
        update( group, null );
    }

    public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
        InternalGroup imp = cast( group );

        try {
            // if this is the admin group, make sure that we are not removing all memberships
            if (Group.ADMIN_GROUP_NAME.equals(group.getName())) {
                Set oldAdminUserHeaders = getUserHeaders( group );
                if (oldAdminUserHeaders.size() < 1) {
                    logger.severe("Blocked update on admin group because all members were deleted.");
                    throw new UpdateException("Cannot update admin group with no memberships!");
                }
            }
            InternalGroup originalGroup = (InternalGroup)findByPrimaryKey( group.getUniqueIdentifier() );
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
            _manager.update(getContext(), originalGroup);
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
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
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
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
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
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
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
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
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
        } catch (SQLException e) {
            throw new FindException( e.getMessage(), e );
        } catch (HibernateException e) {
            throw new FindException( e.getMessage(), e );
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
                    gm = new GroupMembership( uoid, new Long( newGid ).longValue() );
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
                    gm = new GroupMembership( uoid, new Long( existingGid ).longValue() );
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
            InternalUser user = (InternalUser)i.next();
            headers.add( new EntityHeader( user.getOid(), EntityType.USER, user.getLogin(), null ) );
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
        } catch (SQLException se ) {
            throw new UpdateException( se.toString(), se );
        } catch ( HibernateException he ) {
            throw new UpdateException( he.toString(), he );
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

    private final Logger logger = Logger.getLogger(getClass().getName());
    private InternalIdentityProviderServer _provider;

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
