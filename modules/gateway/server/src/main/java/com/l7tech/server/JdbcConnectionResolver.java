package com.l7tech.server;

import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;

import java.text.MessageFormat;
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
                for (int i = 0; i < event.getEntityOperations().length; i++) {
                    final char op = event.getEntityOperations()[i];
                    final long oid = event.getEntityIds()[i];
                    switch (op) {
                        case EntityInvalidationEvent.CREATE: // Intentional fallthrough
                        case EntityInvalidationEvent.UPDATE:
                            try {
                                JdbcConnection conn = jdbcConnectionManager.findByPrimaryKey(oid);
                                jdbcConnectionPoolManager.updateConnectionPool(conn, false);
                                break;
                            } catch (FindException e) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.log(Level.WARNING, MessageFormat.format("Unable to find created/updated jdbc connection #{0}", oid), e);
                                }
                                continue;
                            }
                        case EntityInvalidationEvent.DELETE:
                            String name = jdbcConnectionPoolManager.getConnectionName(oid);
                            jdbcConnectionPoolManager.deleteConnectionPool(name);
                            break;
                    }
                }
            }
        }
    }
}
