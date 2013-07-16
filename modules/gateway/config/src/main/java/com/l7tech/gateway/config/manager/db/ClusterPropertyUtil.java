package com.l7tech.gateway.config.manager.db;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;

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
     * Add or update a cluster property to the given db.
     *
     * @param dbConfig The DB connection parameters
     * @param goid the goid of the cluster property
     * @param name The name of the cluster property
     * @param value The value of the cluster property
     */
    public static void addClusterProperty( final DBActions dbActions,
                                           final DatabaseConfig dbConfig,
                                           @NotNull final Goid goid,
                                           final String name,
                                           final String value ) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dbActions.getConnection( dbConfig, false );
            statement = connection.prepareStatement( "INSERT INTO cluster_properties (goid, version, propkey, propvalue) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE propkey=values(propkey), propvalue=values(propvalue), version=version+1;" );
            statement.setBytes(1, goid.getBytes());
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
