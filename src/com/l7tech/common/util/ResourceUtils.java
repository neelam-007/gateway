package com.l7tech.common.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

/**
 * Utility class for working with resources.
 *
 * <p>This is intended to contain helper methods for tasks such as closing
 * io streams, jdbc objects (statements, connections) or working with resource
 * bundles, etc.</p>
 *
 * <p>You would not normally create instances of this class.</p>
 *
 * <p>NOTE: Since we're aiming for 1.4.x compliance we can't use the Closeable
 * interface.</p>
 *
 * @author steve
 */
public final class ResourceUtils {

    //- PUBLIC

    /**
     * Close a ResultSet without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param resultSet the result set to close (may be null)
     */
    public static void closeQuietly(final ResultSet resultSet) {
        if(resultSet!=null) {
            try {
                resultSet.close();
            }
            catch(SQLException se) {
                logger.log(Level.INFO, "SQL error when closing result set.", se);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing result set.", e);
            }
        }
    }

    /**
     * Close a Statement without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param statement the statement to close (may be null)
     */
    public static void closeQuietly(final Statement statement) {
        if(statement!=null) {
            try {
                statement.close();
            }
            catch(SQLException se) {
                logger.log(Level.INFO, "SQL error when closing statement.", se);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing statement.", e);
            }
        }
    }

    /**
     * Close a SQL connection withouth throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param connection the Connection to close (may be null)
     */
     public static void closeQuietly(final Connection connection) {
        if(connection!=null) {
            try {
                connection.close();
            }
            catch(SQLException se) {
                logger.log(Level.INFO, "SQL error when closing connection.", se);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing connection.", e);
            }
        }
    }

    /**
     * Close a {@link com.l7tech.common.util.Closeable} without throwing any exceptions.
     *
     * @param closeable the object to close.
     */
    public static void closeQuietly(com.l7tech.common.util.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing object", e);
            }
        }
    }

    /**
     * Close a {@link java.io.Closeable} without throwing any exceptions.
     *
     * @param closeable the object to close.
     */
    public static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch(IOException ioe) {
                logger.log(Level.INFO, "IO error when closing closeable.", ioe);
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Unexpected error when closing object", e);
            }
        }
    }

    //- PRIVATE

    /**
     * The logger for the class
     */
    private static final Logger logger = Logger.getLogger(ResourceUtils.class.getName());

}
