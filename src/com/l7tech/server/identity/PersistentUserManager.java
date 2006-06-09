/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;
import org.springframework.dao.DataAccessException;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentUserManager extends HibernateEntityManager implements UserManager
{
    private static final Logger logger = Logger.getLogger(PersistentUserManager.class.getName());

    private final String HQL_DELETE_BY_PROVIDEROID =
            "FROM user IN CLASS " + getImpClass().getName() +
                    " WHERE user.providerId = ?";

    private final String HQL_DELETE =
            "FROM user IN CLASS " + getImpClass().getName() +
                    " WHERE user.oid = ?";

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
            PersistentUser out = (PersistentUser)findByPrimaryKey(getImpClass(), Long.parseLong(oid));
            if (out == null) return null;
            out.setProviderId(identityProvider.getConfig().getOid());
            return out;
        } catch (NumberFormatException nfe) {
            logger.log(Level.SEVERE, null, nfe);
            throw new FindException(nfe.toString(), nfe);
        }
    }

    public User findByLogin(String login) throws FindException {
        try {
            Criteria findByLogin = getSession().createCriteria(getImpClass());
            findByLogin.add(Restrictions.eq("login", login));
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
    public Collection<EntityHeader> search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            Criteria search = getSession().createCriteria(getImpClass());
            search.add(Restrictions.ilike(getNameFieldname(), searchString));
            addFindAllCriteria(search);
            List entities = search.list();
            List<EntityHeader> headers = new ArrayList<EntityHeader>();
            for (Object entity : entities) {
                PersistentUser user = (PersistentUser) entity;
                headers.add(userToHeader(user));
            }
            return Collections.unmodifiableList(headers);
        } catch (HibernateException e) {
            final String msg = "Error while searching for " + getInterfaceClass().getName() + " instances.";
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
        try {
            PersistentUser originalUser = (PersistentUser)findByPrimaryKey(userImp.getUniqueIdentifier());

            if (originalUser == null) {
                throw new ObjectNotFoundException("User " + user.getName());
            }

            preDelete(originalUser);

            Session s = getSession();
            PersistentGroupManager groupManager = (PersistentGroupManager)identityProvider.getGroupManager();
            Set<EntityHeader> groupHeaders = groupManager.getGroupHeaders(userImp);
            for (EntityHeader groupHeader : groupHeaders) {
                Group group = groupManager.findByPrimaryKey(groupHeader.getStrId());
                groupManager.deleteMembership(s, group, user);
            }
            s.delete(userImp);
            revokeCert(userImp);
        } catch (ObjectModelException e) {
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
        try {
            Query q = getSession().createQuery(HQL_DELETE_BY_PROVIDEROID);
            q.setLong(0, ipoid);
            for (Iterator i = q.iterate(); i.hasNext();) {
                getSession().delete(i.next());
            }
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        try {
            Query q = getSession().createQuery(HQL_DELETE);
            q.setString(0, identifier);
            for (Iterator i = q.iterate(); i.hasNext();) {
                getSession().delete(i.next());
            }
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public String save(User user) throws SaveException {
        return save(user, null);
    }

    public String save(User user, Set<EntityHeader> groupHeaders) throws SaveException {
        PersistentUser imp = cast(user);

        try {
            preSave(imp);

            String oid = getHibernateTemplate().save(imp).toString();

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
        } catch (DataAccessException se) {
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
    public void update(User user, Set<EntityHeader> groupHeaders) throws UpdateException, ObjectNotFoundException {
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
            getHibernateTemplate().update(originalUser);
        } catch (DataAccessException se) {
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

    public EntityType getEntityType() {
        return EntityType.USER;
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
