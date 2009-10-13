package com.l7tech.server.jdbcconnection;

import com.l7tech.server.*;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
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
        timer.schedule(new TimerTask(){
            public void run() {
                //todo: 
                // TBD: probably there are somethings to update here.
            }
        }, 30000, 30000 );
    }

    @Override
    protected void doStart() throws LifecycleException {
        logger.info("Starting JDBC Connections Pooling.");
        try {
            initJndiContext();
            bindDataSources();
        } catch (NamingException e) {
            // todo: need auditing?
            logger.warning("Cannot start pooling JDBC connections due to unable to initialize a JNDI context.");
        } catch (FindException e) {
            // todo: need auditing?
            logger.warning("Cannot start pooling JDBC connections due to unable to find JDBC connections.");
        }
    }

    public Connection getConnection(String jdbcConnName) throws NamingException, SQLException {
        DataSource ds = (DataSource)context.lookup(jdbcConnName);
        return ds.getConnection();
    }

    public boolean createDataSource(JdbcConnection connection) {
        // Check if a data source with such connection has been existing.
        try {
            DataSource ds = (DataSource)context.lookup(connection.getName());
            if (ds != null) return true;
        } catch (NamingException e) {
            logger.warning("Cannot lookup JDBC Connection from the context.");
        }

        try {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            setDataSourceByJdbcConnection(cpds, connection);
            return true;
        } catch (Exception e) {
            // todo: auditing and logging
            logger.warning("Cannot create a data source due to invalid properties.");
            return false;
        }
    }

    private void setDataSourceByJdbcConnection(ComboPooledDataSource cpds, JdbcConnection connection) throws PropertyVetoException {
        // Set basic configuration
        cpds.setDriverClass(connection.getDriverClass());
        cpds.setJdbcUrl(connection.getJdbcUrl());
        cpds.setUser(connection.getUserName());
        cpds.setPassword(connection.getPassword());

        // Set C3P0 basic properties
        cpds.setMinPoolSize(connection.getMinPoolSize());
        cpds.setMaxPoolSize(connection.getMaxPoolSize());
        cpds.setMaxIdleTime(120);
        int acquireIncrement = (connection.getMaxPoolSize() - connection.getMinPoolSize()) / 10;
        if (acquireIncrement <= 0) acquireIncrement = 1;
        cpds.setAcquireIncrement(acquireIncrement);

        // Set additional properties
        Properties props = new Properties();
        Map<String, Object> additionalProps = connection.getAddtionalProperties();
        for (String key: additionalProps.keySet()) {
            String value = (String) additionalProps.get(key);
            // Reset maxIdleTime and acquireIncrement if they are overridden in the additional properties.
            if ("c3p0.maxIdleTime".compareToIgnoreCase(key) == 0) {
                cpds.setMaxIdleTime(Integer.parseInt(value));
            } else if ("c3p0.acquireIncrement".compareToIgnoreCase(key) == 0) {
                cpds.setAcquireIncrement(Integer.parseInt(value));
            } else {
                props.put(key, value);
            }
        }
        cpds.setProperties(props);
    }

    private void initJndiContext() throws NamingException {
        Hashtable<String, String> table = new Hashtable<String, String>();
        table.put(Context.INITIAL_CONTEXT_FACTORY,"org.apache.naming.java.javaURLContextFactory");
        context = new InitialContext(table);
    }

    private void bindDataSources() throws FindException {
        for (JdbcConnection connection: jdbcConnectionManager.findAll()) {
            if (! connection.isEnabled()) {
                //todo: auditing and logging
                logger.info("Cannot create a datasource for a disabled JDBC Connection '" + connection.getName() + "'.");
                continue;
            }

            ComboPooledDataSource cpds = new ComboPooledDataSource();
            try {
                setDataSourceByJdbcConnection(cpds, connection);
            } catch (Exception e) {
                // todo: need auditing and logging?
                logger.warning("Cannot bind a datasource with the context due to invalid datasource properties.");
                continue;
            }

            try {
                context.bind(connection.getName(), cpds);
            } catch (NamingException e) {
                // todo: need auditing and logging?
                logger.warning("Cannot bind a datasource with the context due to error naming.");
            }
        }
    }
}
