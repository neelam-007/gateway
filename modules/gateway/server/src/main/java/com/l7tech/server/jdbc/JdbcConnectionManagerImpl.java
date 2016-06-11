package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The implementation of managing JDBC Connection Entity
 *
 * @author ghuang
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class JdbcConnectionManagerImpl
    extends HibernateEntityManager<JdbcConnection, EntityHeader>
    implements JdbcConnectionManager {

    private static final int JDBC_CONNECTION_CACHE_MAX_AGE = 500;

    @Override
    public Class<? extends PersistentEntity> getImpClass() {
        return JdbcConnection.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    /**
     * <p>Retrieve a JDBC Connection entity from the database by using a connection name.</p>
     *
     * <p>This method hits the database every single time it is called.
     * You might want to consider using {@link #getJdbcConnectionCached(String)} instead.</p>
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
     * <p>Retrieve a JDBC Connection entity from the database by using a connection name.</p>
     *
     * <p>This method caches connections. Consider using this instead of {@link #getJdbcConnection(String)} unless
     * you specifically want to hit the database with each call.</p>
     *
     * @param connectionName the name of a JDBC connection
     * @return a JDBC Connection entity with the name <code>connectionName</code>
     * @throws FindException when there is an error finding the connection
     */
    @Override
    public JdbcConnection getJdbcConnectionCached(String connectionName) throws FindException {
        return getCachedEntityByName(connectionName, JDBC_CONNECTION_CACHE_MAX_AGE);
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
        if (driverClass != null ) {
            if (!driverClass.isEmpty()) {
                return getSupportedDriverClass().contains(driverClass);
            }
        }
        return false;
    }

}
