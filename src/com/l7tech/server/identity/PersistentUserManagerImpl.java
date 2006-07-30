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
import org.springframework.orm.hibernate3.HibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentUserManagerImpl<UT extends PersistentUser, GT extends PersistentGroup, UMT extends PersistentUserManager<UT>, GMT extends PersistentGroupManager<UT, GT>>
        extends HibernateEntityManager<UT, IdentityHeader>
        implements PersistentUserManager<UT>
{
    private static final Logger logger = Logger.getLogger(PersistentUserManagerImpl.class.getName());

    private final String HQL_DELETE_BY_PROVIDEROID =
            "FROM user IN CLASS " + getImpClass().getName() +
                    " WHERE user.providerId = ?";

    private final String HQL_DELETE =
            "FROM user IN CLASS " + getImpClass().getName() +
                    " WHERE user.oid = ?";

    protected final PersistentIdentityProvider<UT, GT, UMT, GMT> identityProvider;
    private final GMT groupManager;
    protected ClientCertManager clientCertManager;

    public PersistentUserManagerImpl(PersistentIdentityProvider<UT, GT, UMT, GMT> identityProvider) {
        this.identityProvider = identityProvider;
        this.groupManager = identityProvider.getGroupManager();
    }



    public UT findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            UT out = findByPrimaryKey(getImpClass(), Long.parseLong(oid));
            if (out == null) return null;
            out.setProviderId(identityProvider.getConfig().getOid());
            return out;
        } catch (NumberFormatException nfe) {
            logger.log(Level.SEVERE, null, nfe);
            throw new FindException(nfe.toString(), nfe);
        }
    }

    public UT findByLogin(final String login) throws FindException {
        try {
            //noinspection unchecked
            UT puser = (UT)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Criteria findByLogin = session.createCriteria(getImpClass());
                    findByLogin.add(Restrictions.eq("login", login));
                    addFindAllCriteria(findByLogin);
                    return findByLogin.uniqueResult();
                }
            });
            puser.setProviderId(identityProvider.getConfig().getOid());
            return puser;
        } catch (Exception e) {
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
     * @see PersistentGroupManager
     */
    public Collection<IdentityHeader> search(final String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        try {
            //noinspection unchecked
            return (Collection<IdentityHeader>)getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Criteria search = session.createCriteria(getImpClass());
                    String s = searchString.replace('*', '%').replace('?', '_');
                    search.add(Restrictions.ilike(getNameFieldname(), s));
                    addFindAllCriteria(search);
                    List entities = search.list();
                    List<IdentityHeader> headers = new ArrayList<IdentityHeader>();
                    for (Object entity : entities) {
                        //noinspection unchecked
                        UT user = (UT) entity;
                        headers.add(userToHeader(user));
                    }
                    return Collections.unmodifiableList(headers);
                }
            });
        } catch (Exception e) {
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
    public void delete(UT user) throws DeleteException {
        UT userImp = cast(user);
        try {
            UT originalUser = findByPrimaryKey(userImp.getUniqueIdentifier());

            if (originalUser == null) {
                throw new ObjectNotFoundException("User " + user.getName());
            }

            preDelete(originalUser);

            Set<IdentityHeader> groupHeaders = groupManager.getGroupHeaders(userImp);
            for (EntityHeader groupHeader : groupHeaders) {
                GT group = groupManager.findByPrimaryKey(groupHeader.getStrId());
                groupManager.deleteMembership(group, user);
            }
            getHibernateTemplate().delete(userImp);
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
    public void deleteAll(final long ipoid) throws DeleteException, ObjectNotFoundException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE_BY_PROVIDEROID);
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

    public void delete(final String identifier) throws DeleteException, ObjectNotFoundException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE);
                    q.setString(0, identifier);
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

    public long save(UT entity) throws SaveException {
        String id = save(entity, null);
        return Long.parseLong(id);
    }

    public String saveUser(UT user) throws SaveException {
        return save(user, null);
    }

    public String save(UT user, Set<IdentityHeader> groupHeaders) throws SaveException {
        UT imp = cast(user);

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

    public void update(UT user) throws UpdateException, ObjectNotFoundException {
        update(user, null);
    }

    /**
     * checks that passwd was changed. if so, also revokes the existing cert
     * checks if the user is the last standing admin account, throws if so
     *
     * @param user existing user
     */
    public void update(UT user, Set<IdentityHeader> groupHeaders) throws UpdateException, ObjectNotFoundException {
        UT imp = cast(user);

        try {
            UT originalUser = findByPrimaryKey(user.getUniqueIdentifier());
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

    protected EntityHeader newHeader(long id, String name) {
        return new IdentityHeader(getProviderOid(), Long.toString(id), EntityType.USER, name, null);
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
    protected void preSave(UT user) throws SaveException {
    }

    /**
     * Override this method to verify changes to a user before it's updated
     *
     * @throws ObjectModelException to veto the update
     */
    protected void checkUpdate(UT originalUser, UT updatedUser) throws ObjectModelException {
    }

    /**
     * Override this method to check whether a user can be deleted
     *
     * @throws DeleteException to veto the deletion
     */
    protected void preDelete(UT user) throws DeleteException {
    }

    protected void revokeCert(UT originalUser) throws ObjectNotFoundException {
        try {
            clientCertManager.revokeUserCert(originalUser);
        } catch (UpdateException e) {
            logger.log(Level.FINE, "could not revoke cert for user " + originalUser.getLogin() +
              " perhaps this user had no existing cert", e);
        }
    }

    /**
     * @return the name of the field to be used as the "name" in EntityHeaders
     */
    protected abstract String getNameFieldname();

}
