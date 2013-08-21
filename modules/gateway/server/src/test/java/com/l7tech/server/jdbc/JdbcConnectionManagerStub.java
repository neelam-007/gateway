package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 */
public class JdbcConnectionManagerStub extends EntityManagerStub<JdbcConnection, EntityHeader> implements JdbcConnectionManager {

    public JdbcConnectionManagerStub( final JdbcConnection... entitiesIn ) {
        super( entitiesIn );
    }

    @Override
    public JdbcConnection getJdbcConnection( final String connectionName ) throws FindException {
        JdbcConnection e = findByUniqueName(connectionName);
        if (e == null) {
            throw new FindException("Couldn't find unique entity");
        }
        return e;
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

    @Override
    public Class<? extends PersistentEntityImp> getImpClass() {
        return JdbcConnection.class;
    }

}
