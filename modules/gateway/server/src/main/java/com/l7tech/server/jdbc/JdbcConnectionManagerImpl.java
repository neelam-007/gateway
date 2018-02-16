package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.RoleAwareEntityDeletionEvent;
import com.l7tech.server.util.PostStartupTransactionalApplicationListener;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The implementation of managing JDBC Connection Entity
 *
 * @author ghuang
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class JdbcConnectionManagerImpl
    extends HibernateEntityManager<JdbcConnection, EntityHeader>
    implements JdbcConnectionManager, PostStartupTransactionalApplicationListener {
    private static final Logger logger = Logger.getLogger(JdbcConnectionManagerImpl.class.getName());
    @Override
    public Class<? extends PersistentEntity> getImpClass() {
        return JdbcConnection.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    /**
     * Retrieve a JDBC Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a JDBC connection
     * @return a JDBC Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the JDBC Connection entity.
     */
    @Override
    public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
        return findByUniqueName(connectionName);
    }

    /**
     * This method is overridden so that the query can be added to the query cache (prevent numerous queries for same JdbcConnection object)
     *
     * @param name the name of the entity to locate.  Required.
     * @return
     * @throws FindException
     */
    @Override
    @Transactional(readOnly=true)
    public JdbcConnection findByUniqueName(final String name) throws FindException {
        if (name == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");

        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<JdbcConnection>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public JdbcConnection doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria criteria = session.createCriteria(getImpClass());
                    addFindByNameCriteria(criteria);
                    criteria.add(Restrictions.eq(F_NAME, name)).setCacheable(true).setCacheMode(CacheMode.NORMAL);
                    final JdbcConnection et = (JdbcConnection) criteria.uniqueResult();
                    initializeLazilyLoaded(et);
                    return et;
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    @Override
    public List<String> getSupportedDriverClass() {
        final List<String> driverClassWhiteList = new ArrayList<String>();

        final String whiteListString = ServerConfig.getInstance().getProperty(ServerConfigParams.PARAM_JDBC_CONNECTION_DRIVERCLASS_WHITE_LIST);
        if (whiteListString != null && (! whiteListString.isEmpty())) {
            final StringTokenizer tokens = new StringTokenizer(whiteListString, "\n");
            while (tokens.hasMoreTokens()) {
                final String driverClass = tokens.nextToken();
                if (driverClass != null && (! driverClass.isEmpty())) {
                    driverClassWhiteList.add(driverClass);
                }
            }
        }
        return driverClassWhiteList;
    }

    @Override
    public boolean isDriverClassSupported(String driverClass) {
        if (driverClass != null && !driverClass.isEmpty()) {
            return getSupportedDriverClass().contains(driverClass);
        }
        return false;
    }

    /**
     * Clears all query cache and cached items of type:  JdbcConnection (from the Hibernate L2 Cache)
     */
    @Override
    public void clearAllFromCache() {
        if (getSessionFactory().getCache() != null) {
            logger.warning("Clearing all cached JdbcConnection entities");
            getSessionFactory().getCache().evictQueryRegions();
            getSessionFactory().getCache().evictEntityRegion(JdbcConnection.class);
        }
    }

    @Override
    public ApplicationListener getListener() {
        return new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                Cache cache = getSessionFactory().getCache();
                if (cache == null) {
                    return;
                }

                if (event instanceof RoleAwareEntityDeletionEvent) {
                    RoleAwareEntityDeletionEvent roleEvent = (RoleAwareEntityDeletionEvent) event;
                    if (roleEvent.getEntity() instanceof SecurityZone) {
                        SecurityZone zone = (SecurityZone) roleEvent.getEntity();
                        try {
                            // To avoid clear all the cache we find only the affected connections by security zone deletion
                            for (JdbcConnection connection : findByPropertyMaybeNull("securityZone.goid", zone.getGoid())) {
                                cache.evictEntity(JdbcConnection.class, connection.getGoid());
                            }
                        } catch (FindException e) {
                            logger.log(Level.WARNING, "Could not load JdbcConnections with SecurityZone " + zone.getGoid());
                        }
                    }
                }
            }
        };
    }


}
