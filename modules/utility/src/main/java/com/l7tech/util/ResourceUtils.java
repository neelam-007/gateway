package com.l7tech.util;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.io.*;
import java.nio.channels.FileLock;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import java.net.Socket;

/**
 * Utility class for working with resources.
 *
 * <p>This is intended to contain helper methods for tasks such as closing
 * io streams, jdbc objects (statements, connections) or working with resource
 * bundles, etc.</p>
 *
 * <p>You would not normally create instances of this class.</p>
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

    /**
     * Close one or more {@link java.io.Closeable}s without throwing any exceptions.
     *
     * @param closeables the object(s) to close.
     */
    public static void closeQuietly(java.io.Closeable... closeables) {
        for (java.io.Closeable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    public static void closeQuietly(Context context) {
        if (context == null) return;

        try {
            context.close();
        } catch (NamingException e) {
            logger.log(Level.INFO, "JNDI error when closing JNDI Context.", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing JNDI Context", e);
        }
    }

    public static void closeQuietly(NamingEnumeration answer) {
        if (answer == null) return;

        try {
            answer.close();
        } catch (NamingException e) {
            logger.log(Level.INFO, "JNDI error when closing JNDI NamingEnumeration.", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing JNDI NamingEnumeration", e);
        }
    }

    public static void closeQuietly(FileLock lock) {
        if (lock == null) return;

        try {
            lock.release();
        } catch (IOException e) {
            logger.log(Level.INFO, "IO error when releasing FileLock.", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when releasing FileLock.", e);
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException e) {
            logger.log(Level.INFO, "IOException when closing Socket", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing Socket", e);
        }
    }

    public static void closeQuietly(ZipFile zipFile) {
        if (zipFile == null) return;
        try {
            zipFile.close();
        } catch (IOException e) {
            logger.log(Level.INFO, "IOException when closing ZipFile", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error when closing ZipFile", e);
        }
    }

    //- PRIVATE

    /**
     * The logger for the class
     */
    private static final Logger logger = Logger.getLogger(ResourceUtils.class.getName());
}
