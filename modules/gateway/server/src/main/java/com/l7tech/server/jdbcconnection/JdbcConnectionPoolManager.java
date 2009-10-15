package com.l7tech.server.jdbcconnection;

import com.l7tech.server.*;
import com.l7tech.server.audit.Auditor;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.gateway.common.jdbcconnection.InvalidPropertyException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Pair;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.sql.DataSource;
import java.util.logging.Logger;
import java.util.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.beans.PropertyVetoException;

/**
 * @author ghuang
 */
public class JdbcConnectionPoolManager extends LifecycleBean {
    private static final Logger logger = Logger.getLogger(JdbcConnectionPoolManager.class.getName());

    private JdbcConnectionManager jdbcConnectionManager;
    private Timer timer;
    private Context context;
    private Auditor auditor;

    public JdbcConnectionPoolManager(
        final LicenseManager licenseManager,
        final JdbcConnectionManager jdbcConnectionManager,
        final Timer timer) {

        super("JDBC Connection Pooling Manager", logger, GatewayFeatureSets.SERVICE_FTP_MESSAGE_INPUT, licenseManager);

        this.timer = timer;
        this.jdbcConnectionManager = jdbcConnectionManager;
    }

    @Override
    protected void init() {
        auditor = new Auditor(this, getApplicationContext(), logger);

        timer.schedule(new TimerTask(){
            public void run() {
                // TBD: probably there are somethings to update here.
            }
        }, 30000, 30000 );
    }

    @Override
    protected void doStart() throws LifecycleException {
        logger.info("Starting JDBC Connections Pooling.");

        try {
            initJndiContext();
        } catch (NamingException e) {
            auditor.logAndAudit(AssertionMessages.MCM_CANNOT_START_POOLING, "unable to initialize a JNDI context");
            return;
        }

        Collection<JdbcConnection> allJdbcConns;
        try {
            allJdbcConns = jdbcConnectionManager.findAll();
        } catch (FindException e) {
            auditor.logAndAudit(AssertionMessages.MCM_CANNOT_START_POOLING, " unable to find JDBC connections");
            return;
        }

        for (JdbcConnection connection: allJdbcConns) {
            try {
                updateConnectionPool(connection, false); // All known exceptions have been handled in the method updateConnectionPool.
            } catch (Throwable e) {
                auditor.logAndAudit(AssertionMessages.MCM_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), e.getClass().getSimpleName() + " occurs");
            }
        }
    }

    public Connection getRawConnection(String jdbcConnName) throws NamingException, SQLException {
        DataSource ds = (DataSource)context.lookup(jdbcConnName);
        return ds.getConnection();
    }

    // create and update
    public Pair<ComboPooledDataSource, String> updateConnectionPool(JdbcConnection connection, boolean isForTesting) {
        // Check if the JDBC connection is disabled or not
        if (!connection.isEnabled() && !isForTesting) {
            auditor.logAndAudit(AssertionMessages.MCM_CONNECTION_DISABLED, connection.getName());
            return new Pair<ComboPooledDataSource, String>(null, null);
        }

        // Check if a data source with such connection has been existing.
        DataSource ds;
        try {
            ds = (DataSource)context.lookup(connection.getName());
        } catch (NameNotFoundException e) {
            ds = null;
        } catch (NamingException e) {
            String errMsg = "Error lookup a data source binded with a JDBC connection name, " + connection.getName();
            auditor.logAndAudit(AssertionMessages.MCM_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
            return new Pair<ComboPooledDataSource, String>(null, errMsg);
        }

        if (ds != null && !isForTesting) {
            if (ds instanceof ComboPooledDataSource) {
                // update the data source
                try {
                    setDataSourceByJdbcConnection((ComboPooledDataSource)ds, connection);
                } catch (InvalidPropertyException e) {
                    String errMsg = e.getMessage();
                    auditor.logAndAudit(AssertionMessages.MCM_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                    return new Pair<ComboPooledDataSource, String>(null, errMsg);
                }
                // Rebind the data source
                try {
                    context.rebind(connection.getName(), ds);
                } catch (NamingException e) {
                    String errMsg = "Error rebind a data source with a JDBC connection name, " + connection.getName();
                    auditor.logAndAudit(AssertionMessages.MCM_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                    return new Pair<ComboPooledDataSource, String>(null, errMsg);
                }
                return new Pair<ComboPooledDataSource, String>((ComboPooledDataSource)ds, null);
            } else {
                String errMsg = "The connection pool is not a C3P0 pool.";
                auditor.logAndAudit(AssertionMessages.MCM_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                return new Pair<ComboPooledDataSource, String>(null, errMsg);
            }
        }

         // Create a new data source
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            setDataSourceByJdbcConnection(cpds, connection);
        } catch (InvalidPropertyException e) {
            String errMsg = e.getMessage();
            auditor.logAndAudit(AssertionMessages.MCM_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
            return new Pair<ComboPooledDataSource, String>(null, errMsg);
        }

        // Bind this data source if this is not for testing.
        if (! isForTesting) {
            try {
                context.bind(connection.getName(), cpds);
            } catch (NamingException e) {
                String errMsg = "Error bind a data source with a JDBC connection name, " + connection.getName();
                auditor.logAndAudit(AssertionMessages.MCM_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                return new Pair<ComboPooledDataSource, String>(null, errMsg);
            }
        }

        return new Pair<ComboPooledDataSource, String>(cpds, null);
    }

    public String testJdbcConnection(JdbcConnection connection) {
        Pair<ComboPooledDataSource, String> results;
        try {
            // All known exceptions have been handled in the method updateConnectionPool.
            results = updateConnectionPool(connection, true);
        } catch (Throwable e) {
            return e.getClass().getSimpleName() + " occurs";
        }

        if (results.right != null) return results.right;
        else if (results.left == null) return "Illegal State: data source must not be null";

        ComboPooledDataSource cpds  = results.left;

        Connection conn = null;
        try {
            conn = cpds.getConnection();
        } catch (SQLException e) {
            return "invalid connection properties setting.";
        } catch (Throwable e) {
            return "unexpected error, " + e.getClass().getSimpleName() + " thrown";
        } finally {
            if (conn != null) try {
                conn.close();
            } catch (SQLException e) {}

            cpds.close();
        }

        return null;
    }

    public void deleteConnectionPool(JdbcConnection connection) {
        ComboPooledDataSource cpds;
        try {
            cpds = (ComboPooledDataSource)context.lookup(connection.getName());
        } catch (NamingException e) {
            String errMsg = "Error lookup a data source by a JDBC connection name, " + connection.getName();
            auditor.logAndAudit(AssertionMessages.MCM_CANNOT_DELETE_CONNECTION_POOL, connection.getName(), errMsg);
            return;
        }

        if (cpds == null) return;
        else cpds.close();

        try {
            context.unbind(connection.getName());
        } catch (NamingException e) {
            String errMsg = "Error unbind a data source with a JDBC connection name, " + connection.getName();
            auditor.logAndAudit(AssertionMessages.MCM_CANNOT_DELETE_CONNECTION_POOL, connection.getName(), errMsg);
        }
    }

    private void setDataSourceByJdbcConnection(ComboPooledDataSource cpds, JdbcConnection connection) throws InvalidPropertyException {
        // Set basic configuration
        try {
            cpds.setDriverClass(connection.getDriverClass());
        } catch (PropertyVetoException e) {
            throw new InvalidPropertyException("Invalid property, driverClass");
        }
        cpds.setJdbcUrl(connection.getJdbcUrl());
        cpds.setUser(connection.getUserName());
        cpds.setPassword(connection.getPassword());

        // Set C3P0 basic properties
        cpds.setInitialPoolSize(connection.getMinPoolSize());
        cpds.setMinPoolSize(connection.getMinPoolSize());
        cpds.setMaxPoolSize(connection.getMaxPoolSize());
        cpds.setMaxIdleTime(120);
        int acquireIncrement = (connection.getMaxPoolSize() - connection.getMinPoolSize()) / 10;
        if (acquireIncrement <= 0) acquireIncrement = 1;
        cpds.setAcquireIncrement(acquireIncrement);

        // Set additional properties
        Properties props = new Properties();
        Map<String, Object> additionalProps = connection.getAddtionalProperties();
        for (String propName: additionalProps.keySet()) {
            String propValue = (String) additionalProps.get(propName);
            // Reset maxIdleTime and acquireIncrement if they are overridden in the additional properties.
            try {
                if ("c3p0.maxIdleTime".compareToIgnoreCase(propName) == 0) {
                    cpds.setMaxIdleTime(Integer.parseInt(propValue));
                } else if ("c3p0.acquireIncrement".compareToIgnoreCase(propName) == 0) {
                    cpds.setAcquireIncrement(Integer.parseInt(propValue));
                } else {
                    props.put(propName, propValue);
                }
            } catch (NumberFormatException e) {
                throw new InvalidPropertyException("Invalid property, " + propName);
            }
        }
        if (! props.isEmpty()) cpds.setProperties(props);
    }

    private void initJndiContext() throws NamingException {
        Hashtable<String, String> table = new Hashtable<String, String>();
        table.put(Context.INITIAL_CONTEXT_FACTORY,"org.apache.naming.java.javaURLContextFactory");
        context = new InitialContext(table);
    }
}
