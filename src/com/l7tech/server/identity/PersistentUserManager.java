/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentUserManager extends HibernateEntityManager implements UserManager {
    public abstract Class getImpClass();

    public abstract Class getInterfaceClass();

    public abstract String getTableName();

    public User findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            PersistentUser out = (PersistentUser)PersistenceManager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid));
            if (out == null) return null;
            out.setProviderId(provider.getConfig().getOid());
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
                PersistentUser u = (PersistentUser)users.get(0);
                u.setProviderId(provider.getConfig().getOid());
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
     * @throws com.l7tech.objectmodel.FindException thrown if an SQL error is encountered
     * @see com.l7tech.server.identity.PersistentGroupManager
     */
    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            List results = PersistenceManager.find(getContext(),
                                                   getAllHeadersQuery() + " where " + getTableName() + "." + getNameFieldname() + " like ?",
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


    /** Must be called in a transaction! */
    public void delete(User user) throws DeleteException, ObjectNotFoundException {
        PersistentUser userImp = cast(user);
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)getContext();
            PersistentUser originalUser = (PersistentUser)findByPrimaryKey( userImp.getUniqueIdentifier() );

            if (originalUser == null) {
                throw new ObjectNotFoundException("User "+user.getName());
            }

            preDelete( originalUser );

            Session s = context.getSession();
            PersistentGroupManager groupManager = (PersistentGroupManager)provider.getGroupManager();
            Set groupHeaders = groupManager.getGroupHeaders(userImp);
            for ( Iterator i = groupHeaders.iterator(); i.hasNext(); ) {
                EntityHeader groupHeader = (EntityHeader) i.next();
                s.delete(groupManager.newMembership(userImp.getOid(), groupHeader.getOid()));
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
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass());
        hql.append(" WHERE oid = ?");

        try {
            getContext().getSession().delete(hql.toString(), identifier, Hibernate.STRING);
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new DeleteException(e.getMessage(), e);
        } catch ( HibernateException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public String save( User user ) throws SaveException {
        return save( user, null );
    }

    public String save(User user, Set groupHeaders ) throws SaveException {
        PersistentUser imp = cast(user);

        try {
            preSave( imp );

            String oid = Long.toString( PersistenceManager.save( getContext(), imp ) );

            if ( groupHeaders != null ) {
                try {
                    provider.getGroupManager().setGroupHeaders(user, groupHeaders);
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

    public void update( User user ) throws UpdateException , ObjectNotFoundException{
        update( user, null );
    }

    /**
     * checks that passwd was changed. if so, also revokes the existing cert
     * checks if the user is the last standing admin account, throws if so
     * @param user existing user
     */
    public void update( User user, Set groupHeaders ) throws UpdateException , ObjectNotFoundException {
        PersistentUser imp = cast( user );

        try {
            PersistentUser originalUser = (PersistentUser)findByPrimaryKey(user.getUniqueIdentifier());
            if (originalUser == null) {
                logger.warning("The user " + user.getName() + " is not found.");
                throw new ObjectNotFoundException("User "+user.getName());
            }

            // check for version conflict
            if (originalUser.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            checkUpdate( originalUser, imp );

            if ( groupHeaders != null )
                provider.getGroupManager().setGroupHeaders( user.getUniqueIdentifier(), groupHeaders );

            // update user
            originalUser.copyFrom(imp);
            // update from existing user
            PersistenceManager.update( getContext(), originalUser );
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new UpdateException( se.toString(), se );
        } catch ( ObjectModelException e ) {
            logger.log(Level.SEVERE, null, e);
            throw new UpdateException( e.toString(), e );
        }
    }

    protected String getAllHeadersQuery() {
        return allHeadersQuery;
    }

    /**
     * Override this method to check something before a user is saved
     * @throws SaveException to veto the save
     */
    protected void preSave( PersistentUser user ) throws SaveException { }

    /**
     * Override this method to verify changes to a user before it's updated
     * @throws ObjectModelException to veto the update
     */
    protected void checkUpdate( PersistentUser originalUser, PersistentUser updatedUser ) throws ObjectModelException { }

    /**
     * Override this method to check whether a user can be deleted
     * @throws DeleteException to veto the deletion
     */
    protected void preDelete( PersistentUser user ) throws DeleteException { }

    protected void revokeCert( PersistentUser originalUser ) throws ObjectNotFoundException {
        ClientCertManager man = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
        try {
            man.revokeUserCert(originalUser);
        } catch (UpdateException e) {
            logger.log(Level.FINE, "could not revoke cert for user " + originalUser.getLogin() +
                    " perhaps this user had no existing cert", e);
        }
    }

    protected abstract PersistentUser cast(User user);

    /**
     * @return the name of the field to be used as the "name" in EntityHeaders
     */
    protected abstract String getNameFieldname();

    protected IdentityProvider provider;

    private final String allHeadersQuery = "select " + getTableName() + ".oid, " +
                                           getTableName() + "." + getNameFieldname() + " from " + getTableName() +
                                           " in class "+ getImpClass().getName();
}
