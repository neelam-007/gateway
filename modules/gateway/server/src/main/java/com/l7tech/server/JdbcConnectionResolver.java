package com.l7tech.server;

import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class JdbcConnectionResolver implements PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger(JdbcConnectionResolver.class.getName());

    private JdbcConnectionManager jdbcConnectionManager;
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    public JdbcConnectionResolver(JdbcConnectionManager jdbcConnectionManager, JdbcConnectionPoolManager jdbcConnectionPoolManager) {
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (JdbcConnection.class.equals(event.getEntityClass())) {
                Object source = event.getSource();
                if (source instanceof JdbcConnection) {
                    JdbcConnection modifiedConn = (JdbcConnection)event.getSource();
                    try {
                        if (jdbcConnectionManager.getJdbcConnection(modifiedConn.getName()) == null) {
                            jdbcConnectionPoolManager.deleteConnectionPool(modifiedConn);
                        } else {
                            jdbcConnectionPoolManager.updateConnectionPool(modifiedConn, false);
                        }
                    } catch (FindException e) {
                        logger.log( Level.WARNING, "Error find a JDBC connection, " + modifiedConn.getName(), ExceptionUtils.getDebugException( e ) );
                    }
                }
            }
        }
    }
}
