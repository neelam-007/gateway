package com.l7tech.server.identity;

import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.logon.LogonInfoManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PersistentUserManagerImpl<UT extends PersistentUser, GT extends PersistentGroup, UMT extends PersistentUserManager<UT>, GMT extends PersistentGroupManager<UT, GT>>
        extends HibernateEntityManager<UT, IdentityHeader>
        implements PersistentUserManager<UT>
{
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(PersistentUserManagerImpl.class.getName());

    private final String HQL_DELETE_BY_PROVIDEROID =
            "FROM user IN CLASS " + getImpClass().getName() +
                    " WHERE user.providerId = ?";

    private final String HQL_DELETE =
            "FROM user IN CLASS " + getImpClass().getName() +
                    " WHERE user.goid = ?";

    protected PersistentIdentityProvider<UT, GT, UMT, GMT> identityProvider;
    private final ClientCertManager clientCertManager;
    private LogonInfoManager logonInfoManager;

    protected PersistentUserManagerImpl( final ClientCertManager clientCertManager, LogonInfoManager logonInfoManager ) {
        this.clientCertManager = clientCertManager;
        this.logonInfoManager = logonInfoManager;
    }

    @Override
    public UT findByPrimaryKey(String oid) throws FindException {
        try {
            if (oid == null) {
                logger.fine("findByPrimaryKey called with null arg.");
                return null;
            }
            UT out = findByPrimaryKey(getImpClass(), Goid.parseGoid(oid));

            if (out == null) return null;
            out.setProviderId(getProviderGoid());
            return out;
        } catch (IllegalArgumentException iae) {
            logger.fine("findByPrimaryKey called with invalid arg '"+oid+"'.");
            return null;
        }
    }

    @Override
    public UT findByLogin(final String login) throws FindException {
        try {
            //noinspection unchecked
            UT puser = (UT)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria findByLogin = session.createCriteria(getImpClass());
                    findByLogin.add(Restrictions.eq("login", login));
                    addFindAllCriteria(findByLogin);
                    return findByLogin.uniqueResult();
                }
            });
            if (puser != null)
                puser.setProviderId(getProviderGoid());
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
    @Override
    public EntityHeaderSet<IdentityHeader> search(final String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        try {
            //noinspection unchecked
            return (EntityHeaderSet<IdentityHeader>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria search = session.createCriteria(getImpClass());
                    String s = searchString.replace('*', '%').replace('?', '_');
                    search.add(Restrictions.ilike(getNameFieldname(), s));
                    addFindAllCriteria(search);
                    List entities = search.list();
                    EntityHeaderSet<IdentityHeader> headers = new EntityHeaderSet<IdentityHeader>();
                    for (Object entity : entities) {
                        //noinspection unchecked
                        UT user = (UT) entity;
                        headers.add(newHeader( user ));
                    }
                    return headers;
                }
            });
        } catch (Exception e) {
            final String msg = "Error while searching for " + getInterfaceClass().getName() + " instances.";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
    }

    protected Goid getProviderGoid() {
        return identityProvider.getConfig().getGoid();
    }

    @Override
    public void delete( Goid oid ) throws DeleteException, FindException {
        findAndDelete( oid );
    }

    /**
     * Must be called in a transaction!
     */
    @Override
    public void delete(UT user) throws DeleteException {
        final UT userImp = cast(user);
        try {
            UT originalUser = findByPrimaryKey(userImp.getId());

            if (originalUser == null) {
                throw new ObjectNotFoundException("User " + user.getName());
            }

            preDelete(originalUser);

            final GMT gman = identityProvider.getGroupManager();
            if (gman != null) {
                Set<IdentityHeader> groupHeaders = gman.getGroupHeaders(userImp);
                for (EntityHeader groupHeader : groupHeaders) {
                    GT group = gman.findByPrimaryKey(groupHeader.getStrId());
                    gman.deleteMembership(group, user);
                }
            }

            // Revoke cert before deleting user (Bug #2963)
            revokeCert(userImp);
            deleteLogonInfo(userImp);
            getHibernateTemplate().execute(new HibernateCallback<Void>(){
                @Override
                @SuppressWarnings({"unchecked"})
                public Void doInHibernate( final Session session) throws HibernateException, SQLException {
                    UT entity = (UT)session.get(userImp.getClass(), userImp.getGoid());
                    if (entity == null) {
                        session.delete(userImp);
                    } else {
                        // Avoid NonUniqueObjectException if an older version of this is still in the Session
                        session.delete(entity);
                    }
                    return null;
                }
            });
            postDelete( user );
        } catch (DeleteException e) {
            throw e;
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
     */
    @Override
    public void deleteAll(final Goid ipoid) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE_BY_PROVIDEROID);
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

    @Override
    public void delete(final String identifier) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE);
                    q.setParameter(0, Goid.parseGoid(identifier));
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

    @Override
    public Goid save(UT entity) throws SaveException {
        String id = save(entity, null);
        return Goid.parseGoid(id);
    }

    @Override
    public String save(UT user, Set<IdentityHeader> groupHeaders) throws SaveException {
        UT imp = cast(user);
        imp.setProviderId(getProviderGoid());

        try {
            preSave(imp);

            String oid = getHibernateTemplate().save(imp).toString();

            if (groupHeaders != null) {
                try {
                    GMT gman = identityProvider.getGroupManager();
                    if (gman != null) gman.setGroupHeaders(user, groupHeaders);
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

    @Override
    public void update(UT user) throws UpdateException {
        update(user, null);
    }

    /**
     * checks that passwd was changed. if so, also revokes the existing cert
     * checks if the user is the last standing admin account, throws if so
     *
     * @param user existing user
     */
    @Override
    public void update(UT user, Set<IdentityHeader> groupHeaders) throws UpdateException {
        UT imp = cast(user);

        if (!imp.getProviderId().equals(getProviderGoid())) throw new UpdateException("Can't update users from a different provider");
        try {
            UT originalUser = findByPrimaryKey(user.getId());
            if (originalUser == null) {
                logger.warning("The user " + user.getName() + " is not found.");
                throw new FindException("Couldn't find original version of user " + user.getName());
            }

            // check for version conflict
            if (originalUser.getVersion() != imp.getVersion()) {
                String msg = "version mismatch";
                logger.info(msg);
                throw new StaleUpdateException(msg);
            }

            checkUpdate(originalUser, imp);

            if (groupHeaders != null)
                identityProvider.getGroupManager().setGroupHeaders(user.getId(), groupHeaders);

            // update user
            originalUser.copyFrom(imp);
            // update from existing user
            preUpdate(originalUser);
            getHibernateTemplate().update(originalUser);
            postUpdate(originalUser);
        } catch (DataAccessException se) {
            final String message = "Error updating user: " + ExceptionUtils.getMessage( se );
            logger.log(Level.SEVERE, message, se);
            throw new UpdateException(message, se);
        } catch (UpdateException e) {
            throw e;
        } catch (ObjectModelException e) {
            throw new UpdateException("Error updating user: " + ExceptionUtils.getMessage( e ), e);
        }
    }

    public synchronized void setIdentityProvider(final PersistentIdentityProvider<UT, GT, UMT, GMT> identityProvider) {
        if ( this.identityProvider != null ) throw new IllegalStateException("identityProvider is already set");
        this.identityProvider = identityProvider;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

    @Override
    protected IdentityHeader newHeader(UT entity) {
        return new IdentityHeader(getProviderGoid(), entity.getGoid(), EntityType.USER, entity.getLogin(), null, entity.getName(), entity.getVersion());
    }

    @Override
    @Transactional(propagation= Propagation.SUPPORTS)
    public final IdentityHeader userToHeader( final UT user ) {
        return newHeader( user );
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    @Override
    protected void initDao() throws Exception {
        super.initDao();

        if (logonInfoManager == null) {
            throw new IllegalArgumentException("The Logon Info Manager is required");
        }
    }

    /**
     * Override this method to check something before a user is saved
     *
     * @throws SaveException to veto the save
     */
    protected void preSave(UT user) throws SaveException { }

    /**
     * Override this method to verify changes to a user before it's updated
     *
     * @throws ObjectModelException to veto the update
     */
    protected void checkUpdate(UT originalUser, UT updatedUser) throws ObjectModelException { }

    /**
     * Override this method to check whether a user can be deleted
     *
     * @throws DeleteException to veto the deletion
     */
    protected void preDelete(UT user) throws DeleteException { }

    /**
     * Override this method to check whether a user can be deleted
     *
     * @throws DeleteException to veto the deletion
     */
    protected void postDelete( UT user ) throws DeleteException { }

    /**
     * Override this method to update the user before it is "persisted".
     *
     * @param user The user that will be persisted
     * @throws UpdateException If an error occurs
     */
    protected void preUpdate(UT user) throws UpdateException { }

    /**
     * Override this method to update the user after it is "persisted".
     *
     * NOTE: Any changes you make to the entity will be persisted, but if
     * changing the entity use of preUpdate is preferred.
     *
     * @param user The user that will be persisted
     * @throws UpdateException If an error occurs
     */
    protected void postUpdate(UT user) throws UpdateException { }

    protected void revokeCert(UT originalUser) throws ObjectNotFoundException {
        if ( clientCertManager != null ) {
            try {
                clientCertManager.revokeUserCert(originalUser);
            } catch (UpdateException e) {
                logger.log(Level.FINE, "could not revoke cert for user " + originalUser.getLogin() +
                  " perhaps this user had no existing cert", e);
            }
        }
    }

    /**
     * Deletes the logon info record for the user that will be deleted.
     *
     * @param user  User object
     */
    private void deleteLogonInfo(User user) {
        try {
            logonInfoManager.delete(user.getProviderId(), user.getLogin());
        } catch (DeleteException de) {
            logger.log(Level.FINE, "Could not delete '" + user.getLogin() + "' logon info record.");
        }
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(UT entity) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("providerId", entity.getProviderId());
        attrs.put("name", entity.getName());
        return Arrays.asList(attrs);
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    /**
     * @return the name of the field to be used as the "name" in EntityHeaders
     */
    protected abstract String getNameFieldname();

}
