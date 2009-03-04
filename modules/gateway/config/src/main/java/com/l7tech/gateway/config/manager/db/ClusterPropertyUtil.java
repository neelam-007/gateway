package com.l7tech.gateway.config.manager.db;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.ResourceUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with cluster properties during DB create/upgrade.
 */
public class ClusterPropertyUtil {

    private static final Logger logger = Logger.getLogger( ClusterPropertyUtil.class.getName() );

    /**
     * Add a cluster property to the given db.
     *
     * @param dbConfig The DB connection parameters
     * @param name The name of the cluster property
     * @param value The value of the cluster property
     */
    public static void addClusterProperty( final DBActions dbActions,
                                           final DatabaseConfig dbConfig,
                                           final String name,
                                           final String value ) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbActions.getConnection( dbConfig, false );
            long objectid = dbActions.nextIdentifier( connection );
            statement = connection.prepareStatement( "INSERT INTO cluster_properties (objectid, version, propkey, propvalue) VALUES (?,?,?,?)" );
            statement.setLong(1, objectid);
            statement.setInt(2, 0);
            statement.setString(3, name);
            statement.setString(4, value);
            statement.executeUpdate();
        } catch ( SQLException e ) {
            logger.log( Level.WARNING, "Error adding cluster property '"+name+"'.", e );
        } finally {
            ResourceUtils.closeQuietly( statement );
            ResourceUtils.closeQuietly( connection );
        }
    }

}
