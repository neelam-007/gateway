package com.l7tech.common.util;

import java.nio.channels.Channel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

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
     * Close an InputStream without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param in the input stream to close (may be null)
     */
    public static void closeQuietly(final InputStream in) {
        if(in!=null) {
            try {
                in.close();
            }
            catch(IOException ioe) {
                logger.log(Level.INFO, "IO error when closing stream.", ioe);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected IO error when closing stream.", e);
            }
        }
    }

    /**
     * Close an InputStream without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param out the output stream to close (may be null)
     */
    public static void closeQuietly(final OutputStream out) {
        if(out!=null) {
            try {
                out.close();
            }
            catch(IOException ioe) {
                logger.log(Level.INFO, "IO error when closing stream.", ioe);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected IO error when closing stream.", e);
            }
        }
    }

    /**
     * Close a Channel without throwing any exceptions.
     *
     * <p>Note that the exception may still be logged.</p>
     *
     * @param out the channel to close (may be null)
     */
    public static void closeQuietly(final Channel out) {
        if(out!=null) {
            try {
                out.close();
            }
            catch(IOException ioe) {
                logger.log(Level.INFO, "IO error when closing channel.", ioe);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected IO error when closing channel.", e);
            }
        }
    }

    /**
     * Close a {@link com.l7tech.common.util.Closeable} without throwing any exceptions.
     *
     * Takes no action if the object is not {@link com.l7tech.common.util.Closeable}.
     * @param o the object to close.
     */
    public static void closeQuietly(Object o) {
        if (o instanceof com.l7tech.common.util.Closeable) {
            com.l7tech.common.util.Closeable closeable = (com.l7tech.common.util.Closeable) o;
            try {
                closeable.close();
            } catch (Exception e) {
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
