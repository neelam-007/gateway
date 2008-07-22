package com.l7tech.common.http.prov.apache;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;

import com.l7tech.util.ShutdownExceptionHandler;
import com.l7tech.util.ExceptionUtils;

/**
 * Extension of the MultiThreadedHttpConnectionManager that will close stale connections.
 *
 * <p>This connection manager is not intended for use connecting to a large number of different
 * hosts.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class StaleCheckingHttpConnectionManager extends MultiThreadedHttpConnectionManager {

    //- PUBLIC

    /**
     *
     */
    public StaleCheckingHttpConnectionManager() {
        staleCleanupCountPerHost = DEFAULT_STALE_CLEANUP_COUNT;
        staleCleanupMaxHosts = DEFAULT_MAX_HOSTS;
        seenHostConfigurations = new CopyOnWriteArraySet();
        cleanupTimer.schedule(new CleanupTimerTask(this), 5000, 5000);
    }

    /**
     * Get the per-host cleanup count.
     *
     * <p>This is the number of connections for each host pool that are stale
     * checked each period (every 5 seconds).</p>
     *
     * <p>This can be used to ensure that sockets do not get left in a
     * CLOSE_WAIT state when a pool is idle.</p>
     *
     * @return The count.
     */
    public int getPerHostStaleCleanupCount() {
        return staleCleanupCountPerHost;
    }

    /**
     * Set the per-host cleanup count.
     *
     * <p>The minimum permitted value is 0 the maximum is 1000.</p>
     *
     * <p>If the value given is out of range it is set to the nearest limit (and
     * a warning is logged).</p>
     *
     * @param perHostStaleCleanupCount The count to use.
     */
    public void setPerHostStaleCleanupCount(int perHostStaleCleanupCount) {
        if(perHostStaleCleanupCount < MIN_STALE_CLEANUP_COUNT) {
            logger.warning("Invalid stale cleanup count '"+perHostStaleCleanupCount+"' using '"+MIN_STALE_CLEANUP_COUNT+"'.");
            perHostStaleCleanupCount = MIN_STALE_CLEANUP_COUNT;
        }
        else if(perHostStaleCleanupCount > MAX_STALE_CLEANUP_COUNT) {
            logger.warning("Invalid stale cleanup count '"+perHostStaleCleanupCount+"' using '"+MAX_STALE_CLEANUP_COUNT+"'.");
            perHostStaleCleanupCount = MAX_STALE_CLEANUP_COUNT;
        }

        this.staleCleanupCountPerHost = perHostStaleCleanupCount;
    }

    /**
     * Get the host count for stale checks.
     *
     * <p>This is the maximum number of host pools that are stale
     * checked each period (every 5 seconds).</p>
     *
     * <p>If the number of hosts exceeds this number the oldest hosts are
     * removed (FIFO).</p>
     *
     * <p>This can be used to ensure that sockets do not get left in a
     * CLOSE_WAIT state when a pool is idle.</p>
     *
     * @return The count.
     */
    public int getMaxStaleCheckHosts() {
        return staleCleanupMaxHosts;
    }

    /**
     * Set the host count for stale checks.
     *
     * <p>The minimum permitted value is 0 the maximum is 1000.</p>
     *
     * <p>If the value given is out of range it is set to the nearest limit (and
     * a warning is logged).</p>
     *
     * @param maxStaleCheckHosts The count to use.
     */
    public void setMaxStaleCheckHosts(int maxStaleCheckHosts) {
        if(maxStaleCheckHosts < MIN_MAX_HOSTS) {
            logger.warning("Invalid stale cleanup count '"+maxStaleCheckHosts+"' using '"+MIN_MAX_HOSTS+"'.");
            maxStaleCheckHosts = MIN_MAX_HOSTS;
        }
        else if(maxStaleCheckHosts > MAX_MAX_HOSTS) {
            logger.warning("Invalid stale cleanup count '"+maxStaleCheckHosts+"' using '"+MAX_MAX_HOSTS+"'.");
            maxStaleCheckHosts = MAX_MAX_HOSTS;
        }

        this.staleCleanupMaxHosts = maxStaleCheckHosts;
    }


    /**
     *
     */
    public HttpConnection getConnection(HostConfiguration hostConfiguration) {
        seenHostConfiguration(hostConfiguration);
        return super.getConnection(hostConfiguration);
    }

    public HttpConnection getConnectionWithTimeout(HostConfiguration hostConfiguration, long timeout) throws ConnectionPoolTimeoutException {
        seenHostConfiguration(hostConfiguration);
        return super.getConnectionWithTimeout(hostConfiguration, timeout);
    }

    //- PROTECTED

    /**
     * Periodic cleanup task (closes stale pooled connections).
     */
    protected void doCleanup() {
        // This code runs when there is not much activity and causes any sockets hanging
        // around in CLOSE WAIT to be closed (stale connections).
        //
        // You only get these if the back end service is sending data that we're not
        // reading. Perhaps the content-length was not correct and they flushed at
        // the wrong time, etc.
        //
        for (Iterator hcIter=seenHostConfigurations.iterator(); hcIter.hasNext(); ) {
            HostConfiguration hostConfiguration = (HostConfiguration) hcIter.next();
            try {
                for (int c=0; c<staleCleanupCountPerHost; c++) {
                    HttpConnection connection = null;
                    connection = super.getConnectionWithTimeout(hostConfiguration, 10);
                    try {
                        connection.closeIfStale();
                    } catch(IOException ioe) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, "Error when stale checking connection '"+ExceptionUtils.getMessage(ioe)+"'.",
                                    ExceptionUtils.getDebugException(ioe));
                        }
                    }
                    finally {
                        if (connection != null) connection.releaseConnection();
                    }
                }
            }
            catch(ConnectionPoolTimeoutException cpte) {
                // timeout
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(StaleCheckingHttpConnectionManager.class.getName());
    private static final int DEFAULT_STALE_CLEANUP_COUNT = 1;
    private static final int MIN_STALE_CLEANUP_COUNT = 0;
    private static final int MAX_STALE_CLEANUP_COUNT = 1000;
    private static final int DEFAULT_MAX_HOSTS = 10;
    private static final int MIN_MAX_HOSTS = 0;
    private static final int MAX_MAX_HOSTS = 1000;
    private static final Timer cleanupTimer = new Timer("HttpConnectionManagerCleanupTimer", true);

    static {
        ShutdownExceptionHandler.addShutdownHandler(cleanupTimer);
    }

    private Set seenHostConfigurations; // this set must have an insertion order iterator
    private int staleCleanupCountPerHost;
    private int staleCleanupMaxHosts;

    private void seenHostConfiguration(final HostConfiguration hostConfiguration) {
        seenHostConfigurations.add(new HostConfiguration(hostConfiguration));

        int seenHostsSize = seenHostConfigurations.size();
        int maxHosts = staleCleanupMaxHosts;

        if (seenHostsSize > maxHosts) {
            // loggit
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Pruning host set, maximum is {0}.", Integer.valueOf(maxHosts));
            }

            // seenHostConfigurations must have insertion order iterator
            List hostList = new ArrayList(seenHostConfigurations);

            // recheck size with non-shared copy
            if (hostList.size() > maxHosts) {
                int removeCount = hostList.size() - maxHosts;
                seenHostConfigurations.removeAll(hostList.subList(0, removeCount));
            }
        }
    }

    /**
     *
     */
    private static class CleanupTimerTask extends TimerTask {
        private final WeakReference managerRef;

        CleanupTimerTask(final StaleCheckingHttpConnectionManager manager) {
            managerRef = new WeakReference(manager);
        }

        public void run() {
            StaleCheckingHttpConnectionManager manager =
                    (StaleCheckingHttpConnectionManager) managerRef.get();

            if (manager == null) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.fine("Cancelled timer task for GC'd connection manager.");
                }
                cancel();
            }
            else {
                // Do any cleanup
                manager.doCleanup();
            }
        }
    }
}
