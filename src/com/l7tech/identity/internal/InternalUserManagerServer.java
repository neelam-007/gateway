package com.l7tech.identity.internal;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.hibernate.Session;
import net.sf.hibernate.HibernateException;

/**
 * SSG-side implementation of the UserManager for the internal identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003<br/>
 *
 * @version $Revision$
 *
 */
public class InternalUserManagerServer extends HibernateEntityManager implements UserManager {
    public static final String IMPCLASSNAME = InternalUser.class.getName();

    public InternalUserManagerServer( InternalIdentityProviderServer provider ) {
        super();
        _provider = provider;
    }

    public User findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            InternalUser out = (InternalUser)PersistenceManager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid));
            if (out == null) return null;
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException( se.toString(), se );
        } catch ( NumberFormatException nfe ) {
            logger.log(Level.SEVERE, null, nfe);
            throw new FindException( nfe.toString(), nfe );
        }
    }

    public User findByLogin( String login ) throws FindException {
        try {
            List users = PersistenceManager.find( getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".login = ?", login, String.class );
            switch ( users.size() ) {
            case 0:
                return null;
            case 1:
                InternalUser u = (InternalUser)users.get(0);
                u.setProviderId( IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID );
                return u;
            default:
                String err = "Found more than one user with the login " + login;
                logger.log(Level.SEVERE, err);
                throw new FindException( err );
            }
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException( se.toString(), se );
        }
    }

    /**
     * Search for the user headers using the given search string.
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
                                                   getAllHeadersQuery() + " where " + getTableName() + ".login like ?",
                                                   searchString, String.class);
            List headers = new ArrayList();
            for (Iterator i = results.iterator(); i.hasNext();) {
                Object[] row = (Object[])i.next();
                Object oid = row[0];
                Object name = row[1];
                if ( oid != null && name != null ) {
                    final long id = ((Long)oid).longValue();
                    headers.add(new EntityHeader(id, EntityType.fromInterface(getInterfaceClass()), name.toString(), EMPTY_STRING));
                }
            }
            return Collections.unmodifiableList(headers);
        } catch (SQLException e) {
            final String msg = "Error while searching for "+getInterfaceClass() + " instances.";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
    }

    protected String getAllHeadersQuery() {
        return allHeadersQuery;
    }

    /** Must be called in a transaction! */
    public void delete(User user) throws DeleteException, ObjectNotFoundException {
        InternalUser userImp = cast(user);
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)getContext();
            InternalUser originalUser = (InternalUser)findByPrimaryKey( userImp.getUniqueIdentifier() );

            if (originalUser == null) {
                throw new ObjectNotFoundException("User "+user.getName());
            }

            if (isLastAdmin(originalUser)) {
                String msg = "An attempt was made to nuke the last standing adminstrator";
                logger.severe(msg);
                throw new DeleteException(msg);
            }

            Session s = context.getSession();
            GroupManager groupManager = _provider.getGroupManager();
            Set groupHeaders = groupManager.getGroupHeaders(userImp);
            for ( Iterator i = groupHeaders.iterator(); i.hasNext(); ) {
                EntityHeader groupHeader = (EntityHeader) i.next();
                s.delete(new GroupMembership(userImp.getOid(), groupHeader.getOid()));
            }
            s.delete( userImp );
            revokeCert( userImp );
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new DeleteException( se.toString(), se );
        } catch ( FindException e ) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException( e.toString(), e );
        } catch ( HibernateException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new DeleteException( e.toString(), e );
        }
    }

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        InternalUser imp = new InternalUser();
        imp.setOid( Long.valueOf( identifier ).longValue() );
        delete(imp);
    }

    public String save( User user ) throws SaveException {
        return save( user, null );
    }

    public String save(User user, Set groupHeaders ) throws SaveException {
        InternalUser imp = cast(user);

        try {
            // check to see if an existing user with same login exist
            User existingDude = null;
            try {
                existingDude = findByLogin(user.getLogin());
            } catch (FindException e) {
                existingDude = null;
            }
            if (existingDude != null) {
                throw new SaveException("Cannot save this user. Existing user with login \'"
                                        + user.getLogin() + "\' present.");
            }

            String oid = Long.toString( PersistenceManager.save( getContext(), imp ) );

            if ( groupHeaders != null ) {
                try {
                    _provider.getGroupManager().setGroupHeaders(user, groupHeaders);
                } catch (FindException e) {
                    logger.log( Level.SEVERE, e.getMessage(), e );
                    throw new SaveException( e.getMessage(), e );
                } catch (UpdateException e) {
                    logger.log( Level.SEVERE, e.getMessage(), e );
                    throw new SaveException( e.getMessage(), e );
                }
            }

            return oid;
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new SaveException( se.toString(), se );
        }
    }

    static InternalUser cast(User user) {
        InternalUser imp;
        if ( user instanceof UserBean ) {
            imp = new InternalUser( (UserBean)user );
        } else {
            imp = (InternalUser)user;
        }
        return imp;
    }


    public void update( User user ) throws UpdateException , ObjectNotFoundException{
        update( user, null );
    }

    /**
     * checks that passwd was changed. if so, also revokes the existing cert
     * checks if the user is the last standing admin account, throws if so
     * @param user existing user
     */
    public void update( User user, Set groupHeaders ) throws UpdateException , ObjectNotFoundException {
        InternalUser imp = cast( user );

        try {
            InternalUser originalUser = (InternalUser)findByPrimaryKey(user.getUniqueIdentifier());
            if (originalUser == null) {
                throw new ObjectNotFoundException("User "+user.getName());
            }

            // check for version conflict
            if (originalUser.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            // checks whether the user changed his password
            String originalPasswd = originalUser.getPassword();
            String newPasswd = user.getPassword();

            // if password has changed, any cert should be revoked
            if (!originalPasswd.equals(newPasswd)) {
                logger.info("Revoking cert for user " + originalUser.getLogin() + " because he is changing his passwd.");
                // must revoke the cert

                revokeCert( originalUser );
            }

            if ( groupHeaders != null )
                _provider.getGroupManager().setGroupHeaders( user.getUniqueIdentifier(), groupHeaders );

            // update user
            originalUser.copyFrom(imp);
            // update from existing user
            PersistenceManager.update( getContext(), originalUser );
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new UpdateException( se.toString(), se );
        } catch ( FindException e ) {
            logger.log(Level.SEVERE, null, e);
            throw new UpdateException( e.toString(), e );
        }
    }

    private void revokeCert( InternalUser originalUser ) throws ObjectNotFoundException {
        ClientCertManager man = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
        try {
            man.revokeUserCert(originalUser);
        } catch (UpdateException e) {
            logger.log(Level.FINE, "could not revoke cert for user " + originalUser.getLogin() +
                    " perhaps this user had no existing cert", e);
        }
    }

    public EntityHeader userToHeader(User user) {
        InternalUser imp = (InternalUser)user;
        return new EntityHeader(imp.getUniqueIdentifier(), EntityType.USER, user.getName(), null);
    }

    public User headerToUser(EntityHeader header) {
        try {
            return findByPrimaryKey(header.getStrId());
        } catch (FindException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public String getTableName() {
        return "internal_user";
    }

    public Class getImpClass() {
        return InternalUser.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }

    /**
     * check whether this user is the last standing administrator
     * this is used at update time to prevent the last administrator to nuke his admin accout membership
     * @param currentUser
     * @return true is this user is an administrator and no other users are
     */
    private boolean isLastAdmin(User currentUser) throws FindException {
        InternalUser imp = (InternalUser)currentUser;
        GroupManager gman = _provider.getGroupManager();
        Iterator i = gman.getGroupHeaders(imp).iterator();
        while (i.hasNext()) {
            EntityHeader grp = (EntityHeader)i.next();
            // is he an administrator?
            if (Group.ADMIN_GROUP_NAME.equals(grp.getName())) {
                // is he the last one ?
                Set adminUserHeaders = gman.getUserHeaders( grp.getStrId() );
                if ( adminUserHeaders.size() <= 1 ) return true;
                return false;
            }
        }
        return false;
    }

    private String alias = getTableName();

    private final String allHeadersQuery = "select " + alias + ".oid, " +
                                             alias + ".login from " + alias + " in class "+
                                             getImpClass().getName();

    public static final String F_LOGIN = "login";
    public static final String F_NAME = "name";
    private final Logger logger = Logger.getLogger(getClass().getName());
    private InternalIdentityProviderServer _provider;

}
