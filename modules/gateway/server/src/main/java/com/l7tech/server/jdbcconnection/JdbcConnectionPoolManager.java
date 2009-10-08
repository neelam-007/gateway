package com.l7tech.server.jdbcconnection;

import com.l7tech.server.*;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;

import sun.jdbc.odbc.ee.DataSource;

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

    public Connection getJdbcConnection(String connectionName) throws NamingException, SQLException {
        DataSource ds = (DataSource)context.lookup(connectionName);
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
            Properties props = connection.getAllProperties();
//            props.remove("minPoolSize");
//            props.remove("maxPoolSize");
            cpds.setProperties(props);
            // maybe number format problem for minpoolsize
            
            cpds.setDriverClass(connection.getDriverClass());
            cpds.setJdbcUrl(connection.getJdbcUrl());
            cpds.setUser(connection.getUserName());
            cpds.setPassword(connection.getPassword());
            cpds.setMinPoolSize(connection.getMinPoolSize());
            cpds.setMaxPoolSize(connection.getMaxPoolSize());

            return true;
        } catch (Exception e) {
            // todo: auditing and logging
            logger.warning("Cannot create a data source due to invalid properties.");
            return false;
        }
    }

    @Override
    protected void init() {
        timer.schedule(new TimerTask(){
            public void run() {
                update();
            }
        }, 30000, 30000 );
    }

    @Override
    protected void doStart() throws LifecycleException {
        try {
            initJndiContext();
        } catch (NamingException e) {
            // todo: need auditing?
            logger.warning("Cannot start pooling JDBC connections due to unable to initialize a JNDI context.");
        }

        //bindDataSources();
    }

    private void initJndiContext() throws NamingException {
        Hashtable table = new Hashtable();
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
            cpds.setProperties(connection.getAllProperties());

            try {
                context.bind(connection.getName(), cpds);
            } catch (NamingException e) {
                // todo: need auditing and logging?
                logger.warning("Cannot bind a datasource in the context, due to naming error.");
            }
        }
    }

    private void update() {

    }
}
