package com.l7tech.server;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import com.l7tech.server.jdbcconnection.JdbcConnectionPoolManager;
import com.l7tech.server.jdbcconnection.JdbcConnectionManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class JdbcConnectionResolver implements ApplicationListener {
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
                        logger.warning("Error find a JDBC connection, " + modifiedConn.getName());
                    }
                }
            }
        }
    }
}
