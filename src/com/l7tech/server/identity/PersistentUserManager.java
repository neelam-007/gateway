/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;
import net.sf.hibernate.Criteria;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.expression.Expression;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentUserManager extends HibernateEntityManager implements UserManager {
    protected PersistentUserManager(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    /**
     * empty subclassing constructor (required for class proxying)
     */
    protected PersistentUserManager() {
    }

    public User findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            PersistentUser out = (PersistentUser)PersistenceManager.findByPrimaryKey(getContext(), getImpClass(), Long.parseLong(oid));
            if (out == null) return null;
            out.setProviderId(identityProvider.getConfig().getOid());
            return out;
        } catch (SQLException se) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException(se.toString(), se);
        } catch (NumberFormatException nfe) {
            logger.log(Level.SEVERE, null, nfe);
            throw new FindException(nfe.toString(), nfe);
        }
    }

    public User findByLogin(String login) throws FindException {
        try {
            Criteria findByLogin = getContext().getSession().createCriteria(getImpClass());
            findByLogin.add(Expression.eq("login", login));
            addFindAllCriteria(findByLogin);
            List users = findByLogin.list();
            switch (users.size()) {
                case 0:
                    return null;
                case 1:
                    PersistentUser u = (PersistentUser)users.get(0);
                    u.setProviderId(identityProvider.getConfig().getOid());
                    return u;
                default:
                    String err = "Found more than one user with the login " + login;
                    logger.log(Level.SEVERE, err);
                    throw new FindException(err);
            }
        } catch (SQLException se) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException(se.toString(), se);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, null, e);
            throw new FindException(e.toString(), e);
        }
    }

    /**
     * Search for the user headers using the given search string.
     *
     * @param searchString the search string (supports '*' wildcards)
     * @return the never <b>null</b> collection of entitites
     * @throws com.l7tech.objectmodel.FindException
     *          thrown if an SQL error is encountered
     * @see com.l7tech.server.identity.PersistentGroupManager
     */
    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            Criteria search = getContext().getSession().createCriteria(getImpClass());
            search.add(Expression.ilike(getNameFieldname(), searchString));
            addFindAllCriteria(search);
            List entities = search.list();
            List headers = new ArrayList();
            for (Iterator i = entities.iterator(); i.hasNext();) {
                PersistentUser user = (PersistentUser)i.next();
                headers.add(userToHeader(user));
            }
            return Collections.unmodifiableList(headers);
        } catch (SQLException e) {
            final String msg = "Error while searching for " + getInterfaceClass() + " instances.";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
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
    public void delete(User user) throws DeleteException, ObjectNotFoundException {
        PersistentUser userImp = cast(user);
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)getContext();
            PersistentUser originalUser = (PersistentUser)findByPrimaryKey(userImp.getUniqueIdentifier());

            if (originalUser == null) {
                throw new ObjectNotFoundException("User " + user.getName());
            }

            preDelete(originalUser);

            Session s = context.getSession();
            PersistentGroupManager groupManager = (PersistentGroupManager)identityProvider.getGroupManager();
            Set groupHeaders = groupManager.getGroupHeaders(userImp);
            for (Iterator i = groupHeaders.iterator(); i.hasNext();) {
                EntityHeader groupHeader = (EntityHeader)i.next();
                s.delete(groupManager.newMembership(userImp.getOid(), groupHeader.getOid()));
            }
            s.delete(userImp);
            revokeCert(userImp);
        } catch (SQLException se) {
            logger.log(Level.SEVERE, null, se);
            throw new DeleteException(se.toString(), se);
        } catch (FindException e) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException(e.toString(), e);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.toString(), e);
        }
    }

    /**
     * Delete all users of the identity provider given the identity provider Id
     * <p/>
     * Must be called in a transaction!
     *
     * @param ipoid The identity provider id
     * @throws DeleteException
     * @throws ObjectNotFoundException
     */
    public void deleteAll(long ipoid) throws DeleteException, ObjectNotFoundException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass());
        hql.append(" WHERE provider_oid = ?");

        try {
            getContext().getSession().delete(hql.toString(), new Long(ipoid), Hibernate.LONG);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass());
        hql.append(" WHERE oid = ?");

        try {
            getContext().getSession().delete(hql.toString(), identifier, Hibernate.STRING);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public String save(User user) throws SaveException {
        return save(user, null);
    }

    public String save(User user, Set groupHeaders) throws SaveException {
        PersistentUser imp = cast(user);

        try {
            preSave(imp);

            String oid = Long.toString(PersistenceManager.save(getContext(), imp));

            if (groupHeaders != null) {
                try {
                    identityProvider.getGroupManager().setGroupHeaders(user, groupHeaders);
                } catch (FindException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    throw new SaveException(e.getMessage(), e);
                } catch (UpdateException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    throw new SaveException(e.getMessage(), e);
                }
            }

            return oid;
        } catch (SQLException se) {
            logger.log(Level.SEVERE, null, se);
            throw new SaveException(se.toString(), se);
        }
    }

    public void update(User user) throws UpdateException, ObjectNotFoundException {
        update(user, null);
    }

    /**
     * checks that passwd was changed. if so, also revokes the existing cert
     * checks if the user is the last standing admin account, throws if so
     *
     * @param user existing user
     */
    public void update(User user, Set groupHeaders) throws UpdateException, ObjectNotFoundException {
        PersistentUser imp = cast(user);

        try {
            PersistentUser originalUser = (PersistentUser)findByPrimaryKey(user.getUniqueIdentifier());
            if (originalUser == null) {
                logger.warning("The user " + user.getName() + " is not found.");
                throw new ObjectNotFoundException("User " + user.getName());
            }

            // check for version conflict
            if (originalUser.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            checkUpdate(originalUser, imp);

            if (groupHeaders != null)
                identityProvider.getGroupManager().setGroupHeaders(user.getUniqueIdentifier(), groupHeaders);

            // update user
            originalUser.copyFrom(imp);
            // update from existing user
            PersistenceManager.update(getContext(), originalUser);
        } catch (SQLException se) {
            logger.log(Level.SEVERE, null, se);
            throw new UpdateException(se.toString(), se);
        } catch (ObjectModelException e) {
            logger.log(Level.SEVERE, null, e);
            throw new UpdateException(e.toString(), e);
        }
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }


    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    protected void initDao() throws Exception {
        super.initDao();
        if (clientCertManager == null) {
            throw new IllegalArgumentException("The Client Certificate Manager is required");
        }
    }

    /**
     * Override this method to check something before a user is saved
     *
     * @throws SaveException to veto the save
     */
    protected void preSave(PersistentUser user) throws SaveException {
    }

    /**
     * Override this method to verify changes to a user before it's updated
     *
     * @throws ObjectModelException to veto the update
     */
    protected void checkUpdate(PersistentUser originalUser, PersistentUser updatedUser) throws ObjectModelException {
    }

    /**
     * Override this method to check whether a user can be deleted
     *
     * @throws DeleteException to veto the deletion
     */
    protected void preDelete(PersistentUser user) throws DeleteException {
    }

    protected void revokeCert(PersistentUser originalUser) throws ObjectNotFoundException {
        try {
            clientCertManager.revokeUserCert(originalUser);
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

    protected IdentityProvider identityProvider;
    protected ClientCertManager clientCertManager;
}
