package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.jdbc.InvalidPropertyException;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Pair;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.util.BeanUtils.getProperties;
import static com.l7tech.util.BeanUtils.omitProperties;
import static com.l7tech.util.Functions.equality;
import static com.l7tech.util.Functions.grepFirst;

/**
 * An implementation manages JDBC connection pooling and data sources (i.e., pools).
 * 
 * @author ghuang
 */
public class JdbcConnectionPoolManager implements InitializingBean {
    private static final Logger logger = Logger.getLogger(JdbcConnectionPoolManager.class.getName());

    private static final long MIN_CHECK_AGE = ConfigFactory.getLongProperty( "com.l7tech.server.jdbc.poolConnectionCheckMinAge", 30000L );
    private static final String[] CPDS_IGNORE_PROPS = new String[]{ "connectionPoolDataSource", "driverClass", "initialPoolSize", "jdbcUrl", "logWriter", "maxPoolSize", "minPoolSize", "password", "properties", "propertyCycle", "user", "userOverridesAsString" };
    private static final HashMap<String, Goid> goidNameMap = new HashMap<String, Goid>();

    private final Audit auditor = new LoggingAudit(logger);
    private final JdbcConnectionManager jdbcConnectionManager;
    private Context context;
    @Inject
    private Config config;

    public JdbcConnectionPoolManager( @NotNull final JdbcConnectionManager jdbcConnectionManager ) {
        this.jdbcConnectionManager = jdbcConnectionManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Initializing JDBC connection pooling.");

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
            //remove the disabled connection from the connection pool.
            deleteConnectionPool(connection.getName(), false);
            auditor.logAndAudit(AssertionMessages.JDBC_CONNECTION_DISABLED, connection.getName());
            return new Pair<ComboPooledDataSource, String>(null, null);
        }

        // Validate the Driver Class to use for the JDBC Connection
        String driverClass;
        boolean resolved = false;
        try {
            // If the DriverManager recgonizes the URL it means there is a Driver Class already loaded in the system
            // If this is the case we will validate this value as it is the class used to create connections by C3PO.
            driverClass = DriverManager.getDriver(connection.getJdbcUrl()).getClass().getName();
            resolved = true;
        } catch (SQLException e) {
            driverClass = connection.getDriverClass();
        }
        if (!jdbcConnectionManager.isDriverClassSupported(driverClass)) {
            final String errMsg = resolved ? "JDBC URL resolved to unsupported JDBC Driver class " + driverClass : "JDBC Driver class " + driverClass + " is not supported.";
            if (config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_CONNECTION_DRIVERCLASS_WHITE_LIST_VALIDATE, true)) {
                auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                return new Pair<ComboPooledDataSource, String>(null, errMsg);
            }
            auditor.logAndAudit(AssertionMessages.JDBC_CONNECTION_POOL_NON_WHITE_LISTED_DRIVER, driverClass, connection.getName());
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
                    goidNameMap.put(connection.getName(), connection.getGoid());
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
        cpds.setConnectionCustomizerClassName("com.l7tech.server.util.JdbcConnectionCustomizer");

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
                goidNameMap.put(connection.getName(), connection.getGoid());
            } catch (NamingException e) {
                String errMsg = "Error bind a data source with a JDBC connection name, " + connection.getName();
                auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_CONFIG_CONNECTION_POOL, connection.getName(), errMsg);
                return new Pair<ComboPooledDataSource, String>(null, errMsg);
            }
        }

        return new Pair<ComboPooledDataSource, String>(cpds, null);
    }

    /**
     * Creates the connection pool for testing
     *
     * @param connection: the JDBC Connection to be tested.
     * @return connection pool to test with
     * @throws InvalidPropertyException
     */
    public ComboPooledDataSource getTestConnectionPool(JdbcConnection connection) throws InvalidPropertyException{
        Pair<ComboPooledDataSource, String> results;

        // By calling the method updateConnectionPool, test if all properties of a JDBC Connection are valid or not.
        try {
            // All known exceptions have been handled in the method updateConnectionPool.
            results = updateConnectionPool(connection, true);  // "true" means this method called is for testing.
        } catch (Throwable e) {
            // Report all other unknown and unexpected exceptions
            throw new RuntimeException(e.getClass().getSimpleName() + " occurs");
        }

        if (results.right != null) throw new InvalidPropertyException( results.right);
        else if (results.left == null) throw new IllegalStateException(  "Illegal State: data source must not be null");
        return results.left;
    }

    /**
     * Delete a connection pool associated with a JDBC Connection entity name.
     *
     * @param connectionName: a JDBC Connection name.
     * @return This will return true if the connection pool was removed. It returns false otherwise.
     */
    public boolean deleteConnectionPool(String connectionName) {
        return deleteConnectionPool(connectionName, true);
    }

    /**
     * Delete a connection pool associated with a JDBC Connection entity name.
     *
     * @param connectionName: a JDBC Connection name.
     * @param logErrorIfNotFound if true an error will be logged if the connection cannot be found, otherwise no error will be logged.
     * @return This will return true if the connection pool was removed. It returns false otherwise.
     */
    public boolean deleteConnectionPool(String connectionName, boolean logErrorIfNotFound) {
        ComboPooledDataSource cpds;
        try {
            cpds = (ComboPooledDataSource)context.lookup(connectionName);
        } catch (NamingException e) {
            if (logErrorIfNotFound) {
                String errMsg = "Error lookup a data source by a JDBC connection name, " + connectionName;
                auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_DELETE_CONNECTION_POOL, connectionName, errMsg);
            }
            return false;
        }

        if (cpds == null) return false;
        else cpds.close();

        try {
            context.unbind(connectionName);
            goidNameMap.remove(connectionName);
        } catch (NamingException e) {
            String errMsg = "Error unbind a data source with a JDBC connection name, " + connectionName;
            auditor.logAndAudit(AssertionMessages.JDBC_CANNOT_DELETE_CONNECTION_POOL, connectionName, errMsg);
            return false;
        }
        auditor.logAndAudit(AssertionMessages.JDBC_DELETED_CONNECTION_POOL, connectionName);
        return true;
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

        // Set C3P0 basic properties
        cpds.setInitialPoolSize(connection.getMinPoolSize());
        cpds.setMinPoolSize(connection.getMinPoolSize());
        cpds.setMaxPoolSize(connection.getMaxPoolSize());
        cpds.setMaxIdleTime(120);
        int acquireIncrement = (connection.getMaxPoolSize() - connection.getMinPoolSize()) / 10;
        if (acquireIncrement <= 0) acquireIncrement = 1;
        cpds.setAcquireIncrement(acquireIncrement);

        // Set additional properties
        final Properties props = new Properties();
        final Map<String, Object> additionalProps = connection.getAdditionalProperties();
        final Set<PropertyDescriptor> dataSourceProperties = omitProperties( getProperties( ComboPooledDataSource.class ), CPDS_IGNORE_PROPS );
        for ( final String propName: additionalProps.keySet() ) {
            final String propValue = (String) additionalProps.get(propName);
            if ( propName.toLowerCase().startsWith( "c3p0." ) ) {
                final String c3p0PropertyName = propName.substring( 5 ).toLowerCase();
                final PropertyDescriptor propertyDescriptor = grepFirst( dataSourceProperties, equality( propName(), c3p0PropertyName ) );
                if ( propertyDescriptor != null ) {
                    try {
                        if ( propertyDescriptor.getPropertyType().isAssignableFrom( String.class ) ) {
                            propertyDescriptor.getWriteMethod().invoke( cpds, propValue );
                        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom( Integer.class ) ||
                                propertyDescriptor.getPropertyType().isAssignableFrom( Integer.TYPE ) ) {
                            propertyDescriptor.getWriteMethod().invoke( cpds, Integer.valueOf( propValue ) );
                        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom( Boolean.class ) ||
                                propertyDescriptor.getPropertyType().isAssignableFrom( Boolean.TYPE )) {
                            propertyDescriptor.getWriteMethod().invoke( cpds, Boolean.valueOf( propValue ) );
                        } else {
                            logger.warning( "Ignoring C3P0 property '"+propName+"' for JDBC connection '"+connection.getName()+"' (unsupported type)" );
                        }
                    } catch (NumberFormatException e) {
                        throw new InvalidPropertyException("Invalid integer property, " + propName);
                    } catch ( InvocationTargetException e ) {
                        throw new InvalidPropertyException("Invalid property, " + propName);
                    } catch ( IllegalAccessException e ) {
                        throw new InvalidPropertyException("Invalid property, " + propName);
                    }
                } else {
                    logger.warning( "Ignoring unknown C3P0 property '"+propName+"' for JDBC connection '"+connection.getName()+"'." );
                }
            } else {
                props.put( propName, propValue );
            }
        }
        cpds.setProperties(props);

        // set username/password after setProperties, else they will be reset
        if ( connection.getUserName()!=null && !connection.getUserName().isEmpty() ) {
            cpds.setUser(connection.getUserName());
            cpds.setPassword(ServerVariables.expandSinglePasswordOnlyVariable(new LoggingAudit(logger), connection.getPassword()));
        }
    }

    private Unary<String, PropertyDescriptor> propName() {
        return new Unary<String, PropertyDescriptor>() {
            @Override
            public String call( final PropertyDescriptor propertyDescriptor ) {
                return propertyDescriptor.getName().toLowerCase();
            }
        };
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

    public String getConnectionName(Goid goid) {
        for(String key: goidNameMap.keySet()){
            if(goidNameMap.get(key).equals(goid))
                return key;
        }
        return null;
    }
}
