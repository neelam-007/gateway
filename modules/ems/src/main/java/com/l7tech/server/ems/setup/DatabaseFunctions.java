package com.l7tech.server.ems.setup;

import com.l7tech.util.ResourceUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Database helper functions for Derby.
 *
 * <p>These helper functions should be created and dropped for upgrades as
 * required.</p>
 */
public class DatabaseFunctions {

    private static final Logger logger = Logger.getLogger(DatabaseFunctions.class.getName());

    /**
     * Drop the item of the given type if it exists.
     *
     * @param type The item type (table, function, procedure, etc)
     * @param name The item name
     * @throws SQLException If an unexpected error occurs
     */
    public static void dropIfExists( final String type,
                                     final String name ) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection( "jdbc:default:connection" );
            statement = connection.prepareStatement(  "drop " + type + " " + name );
            statement.execute();
        } catch ( SQLException se ) {
            // ignore expected error when function/table does not exist
            if ("42Y55".equals(se.getSQLState()) ) {
                logger.finest("Not found during drop '"+type+"' named '"+name+"'.");
            } else {
                throw se;
            }
        } finally {
            ResourceUtils.closeQuietly( statement );
            ResourceUtils.closeQuietly( connection );
        }
    }
}
