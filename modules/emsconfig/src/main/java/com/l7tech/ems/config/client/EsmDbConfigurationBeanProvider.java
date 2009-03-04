package com.l7tech.ems.config.client;

import com.l7tech.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.File;

import org.apache.derby.jdbc.EmbeddedDataSource40;

/**
 * Support class for ConfigurationBeanProviders that modify the ESM Database.
 */
public class EsmDbConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    /**
     * Valid if the DB is available.
     *
     * @return True if the DB is accessible.
     */
    @Override
    public boolean isValid() {
        boolean valid = false;
        File dbDirectory = new File( getDatabaseName() );
        if ( dbDirectory.isDirectory() ) {
            Connection connection = null;
            try {
                connection = getConnection();
                valid = true;
            } catch (SQLException e) {
                if ( "XJ040".equals(e.getSQLState()) && "XSDB6".equals( e.getNextException().getSQLState() ) ) {
                    logger.warning("ESM database is in use.");
                } else {
                    logger.log( Level.WARNING, "Error accessing database '"+ExceptionUtils.getMessage(e)+"', code '"+e.getSQLState()+"'.", ExceptionUtils.getDebugException(e) );
                }
            } finally {
                ResourceUtils.closeQuietly( connection );
            }
        }
        return valid;
    }

    /**
     * This implementation returns an empty list.
     *
     * @return An empty collection.
     * @throws ConfigurationException never.
     */
    @Override
    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        return Collections.emptyList();
    }

    /**
     * This implementation does nothing.
     *
     * @param configuration The configuration to ignore
     * @throws ConfigurationException never
     */
    @Override
    public void storeConfiguration( final Collection<ConfigurationBean> configuration ) throws ConfigurationException {
    }

    //- PROTECTED

    protected EsmDbConfigurationBeanProvider( final Logger logger ) {
        this.logger = logger;
    }

    protected Object doWithConnection( final Functions.Unary<Object, Connection> callback ) {
        Object result = null;

        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            result = callback.call( connection );
            connection.commit();
        } catch (SQLException e) {
            logger.log( Level.WARNING, "Error getting database connection", e );
        } finally {
            ResourceUtils.closeQuietly( connection );
        }

        return result;
    }

    protected String getDatabaseName() {
        return SyspropUtil.getString(SYSPROP_DATABASE_NAME,"var/db/emsdb");
    }

    //- PRIVATE

    private static final String SYSPROP_DATABASE_NAME = "com.l7tech.esm.dbName";

    private final Logger logger;

    private Connection getConnection() throws SQLException {
        EmbeddedDataSource40 ds = new EmbeddedDataSource40();
        ds.setDatabaseName( getDatabaseName() );
        return ds.getConnection();
    }
}
