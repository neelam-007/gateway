package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.jdbc.InvalidPropertyException;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.LifecycleBean;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * An implementation manages JDBC connection pooling and data sources (i.e., pools).
 * 
 * @author ghuang
 */
public class JdbcConnectionPoolManager extends LifecycleBean {
    private static final Logger logger = Logger.getLogger(JdbcConnectionPoolManager.class.getName());

    private static final long MIN_CHECK_AGE = SyspropUtil.getLong( "com.l7tech.server.jdbc.poolConnectionCheckMinAge", 30000L );

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

        // This timer is created for future use.  Currently its schedule is empty.
        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                // TBD: probably there are somethings to update here.
            }
        }, 30000, 30000 );
    }

    @Override
    protected void doStart() throws LifecycleException {
        logger.info("Starting JDBC Connections Pooling.");

        // Step 1: initialize JNDI Context
        try {
            initJndiContext();
        } catch (NamingException e) {
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_START_POOLING, "unable to initialize a JNDI context");
            return;
        }

        // Step 2: Get all JDBC Connection entities (each contains JDBC connection configuration.)
        Collection<JdbcConnection> allJdbcConns;
        try {
            allJdbcConns = jdbcConnectionManager.findAll();
        } catch (FindException e) {
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_START_POOLING, " unable to find JDBC connections");
            return;
        }

        // Step 3: Create/update a pool per a JDBC Connection entity.
        for (JdbcConnection connection: allJdbcConns) {
            try {
                // All known exceptions have been handled in the method updateConnectionPool.
                updateConnectionPool(connection, false); // "false" means this method called is not for testing.
            } catch (Throwable e) {
                // Log and audit all other unknown and unexpected exceptions
                auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), e.getClass().getSimpleName() + " occurs");
            }
        }
    }

    /**
     * Get a data source by using a JDBC connection name.
     *
     * @param jdbcConnName: a JDBC Connection name associated with the data source.
     * @return a DataSource object.
     * @throws NamingException: thrown when errors retrieving the data source.
     */
    public DataSource getDataSource(String jdbcConnName) throws NamingException, SQLException {
        final DataSource ds = (DataSource) context.lookup(jdbcConnName);

        // Verify that the pool is functional to avoid long delays when connections
        // cannot be created
        if ( ds instanceof ComboPooledDataSource ) {
            final ComboPooledDataSource cpds = (ComboPooledDataSource) ds;
            if ( cpds.getNumConnectionsAllUsers() == 0 &&
                 (System.currentTimeMillis() - cpds.getStartTimeMillisDefaultUser() > MIN_CHECK_AGE) ) {
                throw new SQLException("No connections available for '"+jdbcConnName+"'");
            }
        }

        return ds;
    }

    /**
     * Create or update a data source to hold a pool of connections.  This method is shared by two processes, starting
     * Connection Pooling and testing JDBC connections.  If it is for the latter, then set a boolean flag isForTesting
     * to be true.
     *
     * @param connection: a JDBC Connection entity containing connection properties.
     * @param isForTesting: a flag indicating if this method is for testing JDBC connection or not.
     * @return a pair of information, a data source object and an error message.  The returned data source object could
     *         be null, which means the JDBC Connection is disabled or some errors occur during the method call.  If the
     *         returned error message is null, this implies no errors occur during the method call.
     */
    public Pair<ComboPooledDataSource, String> updateConnectionPool(JdbcConnection connection, boolean isForTesting) {
        // Check if the JDBC connection is disabled or not.  This checking will be ignored if this method is called for testing JDBC connection.
        if (!connection.isEnabled() && !isForTesting) {
            auditor.logAndAudit(AssertionMessages.JDBC_CONNECTION_DISABLED, connection.getName());
            return new Pair<ComboPooledDataSource, String>(null, null);
        }

        // Check if a data source associated with such connection name already exists or not.
        DataSource ds;
        try {
            ds = (DataSource)context.lookup(connection.getName());
        } catch (NameNotFoundException e) {
            ds = null;
        } catch (NamingException e) {
            String errMsg = "Error lookup a data source binded with a JDBC connection name, " + connection.getName();
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
            return new Pair<ComboPooledDataSource, String>(null, errMsg);
        }

        // Update the data source if this is not for testing.
        if (ds != null && !isForTesting) {
            if (ds instanceof ComboPooledDataSource) {
                // update the data source
                try {
                    setDataSourceByJdbcConnection((ComboPooledDataSource)ds, connection);
                } catch (InvalidPropertyException e) {
                    String errMsg = e.getMessage();
                    auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                    return new Pair<ComboPooledDataSource, String>(null, errMsg);
                } catch (FindException e) {
                    String errMsg = e.getMessage();
                    auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                    return new Pair<ComboPooledDataSource, String>(null, errMsg);
                }
                // Rebind the data source
                try {
                    context.rebind(connection.getName(), ds);
                } catch (NamingException e) {
                    String errMsg = "Error rebind a data source with a JDBC connection name, " + connection.getName();
                    auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                    return new Pair<ComboPooledDataSource, String>(null, errMsg);
                }
                return new Pair<ComboPooledDataSource, String>((ComboPooledDataSource)ds, null);
            } else {
                String errMsg = "The connection pool is not a C3P0 pool.";
                auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                return new Pair<ComboPooledDataSource, String>(null, errMsg);
            }
        }

         // Create a new data source
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            setDataSourceByJdbcConnection(cpds, connection);
        } catch (InvalidPropertyException e) {
            String errMsg = e.getMessage();
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
            return new Pair<ComboPooledDataSource, String>(null, errMsg);
        } catch (FindException e) {
            String errMsg = e.getMessage();
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
            return new Pair<ComboPooledDataSource, String>(null, errMsg);
        }

        // Bind this data source if this is not for testing.
        if (! isForTesting) {
            try {
                context.bind(connection.getName(), cpds);
            } catch (NamingException e) {
                String errMsg = "Error bind a data source with a JDBC connection name, " + connection.getName();
                auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                return new Pair<ComboPooledDataSource, String>(null, errMsg);
            }
        }

        return new Pair<ComboPooledDataSource, String>(cpds, null);
    }

    /**
     * Test a JDBC Connection entity.
     *
     * @param connection: the JDBC Connection to be tested.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    public String testJdbcConnection(JdbcConnection connection) {
        Pair<ComboPooledDataSource, String> results;

        // By calling the method updateConnectionPool, test if all properties of a JDBC Connection are valid or not.
        try {
            // All known exceptions have been handled in the method updateConnectionPool.
            results = updateConnectionPool(connection, true);  // "true" means this method called is for testing.
        } catch (Throwable e) {
            // Report all other unknown and unexpected exceptions
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
            } catch (SQLException e) {
                logger.warning("Cannot close a JDBC connection.");
            }

            cpds.close();
        }

        // Returning null means no errors.
        return null;
    }

    /**
     * Delete a connection pool associated with a JDBC Connection entity.
     *
     * @param connection: a JDBC Connection entity.
     */
    public void deleteConnectionPool(JdbcConnection connection) {
        ComboPooledDataSource cpds;
        try {
            cpds = (ComboPooledDataSource)context.lookup(connection.getName());
        } catch (NamingException e) {
            String errMsg = "Error lookup a data source by a JDBC connection name, " + connection.getName();
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_DELETE_CONNECTION_POOL, connection.getName(), errMsg);
            return;
        }

        if (cpds == null) return;
        else cpds.close();

        try {
            context.unbind(connection.getName());
        } catch (NamingException e) {
            String errMsg = "Error unbind a data source with a JDBC connection name, " + connection.getName();
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_DELETE_CONNECTION_POOL, connection.getName(), errMsg);
        }
    }

    /**
     * Configure a data source by using the properties defined in a JDBC Connection entity.
     *
     * @param cpds: a ComboPooledDataSource object to be configured.
     * @param connection: a JDBC Connection entity containing all connection properties.
     * @throws InvalidPropertyException: thrown when invalid properties are used.
     */
    private void setDataSourceByJdbcConnection(ComboPooledDataSource cpds, JdbcConnection connection) throws InvalidPropertyException, FindException {
        // Set basic configuration
        try {
            cpds.setDriverClass(connection.getDriverClass());
        } catch (PropertyVetoException e) {
            throw new InvalidPropertyException("Invalid property, driverClass");
        }
        cpds.setJdbcUrl(connection.getJdbcUrl());
        cpds.setUser(connection.getUserName());
        cpds.setPassword(ServerVariables.expandSinglePasswordOnlyVariable(new LoggingAudit(logger), connection.getPassword()));

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

    /**
     * Initialize the JNDI Context for starting C3P0 connection pooling.
     *
     * @throws NamingException: thrown when errors naming.
     */
    private void initJndiContext() throws NamingException {
        Hashtable<String, String> table = new Hashtable<String, String>();
        table.put(Context.INITIAL_CONTEXT_FACTORY,"org.apache.naming.java.javaURLContextFactory");
        context = new InitialContext(table);
    }
}
