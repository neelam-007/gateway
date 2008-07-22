package com.l7tech.server.transport.http;

import com.l7tech.server.ServerConfig;
import com.l7tech.util.ShutdownExceptionHandler;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter that wraps the input stream for http requests that (may) have a body.
 *
 * @author $Author$
 * @version $Revision$
 */
public class InputTimeoutFilter implements Filter {

    //- PUBLIC

    /**
     *
     */
    public InputTimeoutFilter() {
        // init with defaults
        timeout = new AtomicLong(DEFAULT_BLOCKED_READ_TIMEOUT);
        readTime = new AtomicLong(DEFAULT_SLOW_READ_TIMEOUT);
        readRate = new AtomicInteger(DEFAULT_SLOW_READ_LIMIT);
    }

    /**
     * Read the initialization parameters.
     *
     * <p>Optional settings are:</p>
     * <ul>
     * <li><code>blocked-read-timeout</code> - time in milliseconds to allow for a blocked read</li>
     * <li><code>slow-read-timeout</code> - time in milliseconds before checking for slow read</li>
     * <li><code>slow-read-throughput</code> - minimum allowed read rate in bytes per second</li>
     * </ul>
     * <p>If values are not set then defaults are used.</p>
     *
     * @param filterConfig the configuration
     * @throws ServletException if the filterConfig is null
     */
    public void init(final FilterConfig filterConfig) throws ServletException {
        if(filterConfig==null) throw new ServletException("null parameter");

        String configTimeout = filterConfig.getInitParameter(INIT_PROP_TIMEOUT);
        String configReadTime = filterConfig.getInitParameter(INIT_PROP_SLOW_READ_THRESHOLD);
        String configReadRate = filterConfig.getInitParameter(INIT_PROP_SLOW_READ_THROUGHPUT);

        timeout.set(parseLong(configTimeout, 500, 3600000, DEFAULT_BLOCKED_READ_TIMEOUT, INIT_PROP_TIMEOUT));
        readTime.set(parseLong(configReadTime, 500, 3600000, DEFAULT_SLOW_READ_TIMEOUT, INIT_PROP_SLOW_READ_THRESHOLD));
        readRate.set(parseInt(configReadRate, 0, 1000000, DEFAULT_SLOW_READ_LIMIT, INIT_PROP_SLOW_READ_THROUGHPUT));

        initPropUpdater();
    }

    /**
     * If the request is a method that may have a body (OPTIONS, POST, PUT may
     * have a body; TRACE, DELETE, GET, HEAD do not) then the request is wrapped
     * to use a timeout input stream.
     *
     * @param servletRequest the servlet request
     * @param servletResponse the servlet response
     * @param filterChain the remaining filters
     * @throws IOException if the next filter throws an IOException
     * @throws ServletException if any argument is null or if the next filter throws a ServletException
     */
    public void doFilter( final ServletRequest servletRequest
                        , final ServletResponse servletResponse
                        , final FilterChain filterChain) throws IOException, ServletException {

        if(servletRequest==null || servletResponse==null || filterChain==null)
            throw new ServletException("null parameter");

        ServletRequest requestForNextFilter = servletRequest;
        ServletResponse responseForNextFilter = servletResponse;
        TimeoutServletRequest tsr = null;

        if(servletRequest instanceof HttpServletRequest) {
            long reqTimeout = timeout.get();
            long reqReadTime = readTime.get();
            int reqReadRate = readRate.get();

            tsr = new TimeoutServletRequest((HttpServletRequest) servletRequest, reqTimeout, reqReadTime, reqReadRate);
            requestForNextFilter = tsr;
        }

        try {
            filterChain.doFilter(requestForNextFilter, responseForNextFilter);
        }
        finally {
            if(tsr!=null) {
                TimeoutInputStream tis = tsr.getTimeoutInputStream();
                if(tis!=null) tis.done(); // don't close underlying stream but mark as finished
            }
        }
    }

    /**
     * Stop the timeout property update thread.
     */
    public void destroy() {
        shutdownPropUpdater();
    }

    //- PRIVATE

    // logger for the class
    private static final Logger logger = Logger.getLogger(InputTimeoutFilter.class.getName());

    // filter initialization parameters
    private static final String INIT_PROP_TIMEOUT = "blocked-read-timeout";
    private static final String INIT_PROP_SLOW_READ_THRESHOLD = "slow-read-timeout";
    private static final String INIT_PROP_SLOW_READ_THROUGHPUT = "slow-read-throughput";

    // initialization defaults
    private static final long DEFAULT_BLOCKED_READ_TIMEOUT = 30000;
    private static final long DEFAULT_SLOW_READ_TIMEOUT = 30000;
    private static final int DEFAULT_SLOW_READ_LIMIT = 1024; //Bytes per second

    // property update shutdown flag;
    private Thread updateThread;
    private TimeoutPropertyUpdate updater;

    //
    private final AtomicLong timeout;
    private final AtomicLong readTime;
    private final AtomicInteger readRate;

    /**
     * Parse the value, log an error and use default if invalid.
     */
    private long parseLong(String value, long min, long max, long defaultValue, String name) {
        long parsed = defaultValue;
        if(value!=null) {
            try {
                parsed = Long.parseLong(value);
                if(parsed<min) {
                    parsed = min;
                    logger.warning("Init parameter '"+name+"', config is '"+value+"', BELOW minimum allowed, using minimum '"+min+"'.");
                }
                else if(parsed>max) {
                    parsed = max;
                    logger.warning("Init parameter '"+name+"', config is '"+value+"', ABOVE maximum allowed, using maximum '"+max+"'.");
                }
            }
            catch(NumberFormatException nfe) {
                logger.warning("Error parsing init parameter '"+name+"', config is '"+value+"', using default '"+defaultValue+"'.");
            }
        }
        return parsed;
    }

    /**
     * Parse the value, log an error and use default if invalid.
     */
    private int parseInt(String value, int min, int max, int defaultValue, String name) {
        int parsed = defaultValue;
        if(value!=null) {
            try {
                parsed = Integer.parseInt(value);
                if(parsed<min) {
                    parsed = min;
                    logger.warning("Init parameter '"+name+"', config is '"+value+"', BELOW minimum allowed, using minimum '"+min+"'.");
                }
                else if(parsed>max) {
                    parsed = max;
                    logger.warning("Init parameter '"+name+"', config is '"+value+"', ABOVE maximum allowed, using maximum '"+max+"'.");
                }
            }
            catch(NumberFormatException nfe) {
                logger.warning("Error parsing init parameter '"+name+"', config is '"+value+"', using default '"+defaultValue+"'.");
            }
        }
        return parsed;
    }

    /**
     * Get the timeout from server config, use servlet config as default.
     */
    private long getTimeout(long currentValue) {
        String rawVal = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_IO_FRONT_BLOCKED_READ_TIMEOUT);
        long scTimeout = parseLong(rawVal, 500, 3600000, currentValue, ServerConfig.PARAM_IO_FRONT_BLOCKED_READ_TIMEOUT);
        return scTimeout;
    }

    /**
     * Get the slow read threshold from server config, use servlet config as default.
     */
    private long getReadTime(long currentValue) {
        String rawVal = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_IO_FRONT_SLOW_READ_THRESHOLD);
        long scReadTimeout = parseLong(rawVal, 500, 3600000, currentValue, ServerConfig.PARAM_IO_FRONT_SLOW_READ_THRESHOLD);
        return scReadTimeout;
    }

    /**
     * Get the slow read rate from server config, use servlet config as default.
     */
    private int getReadRate(int currentValue) {
        String rawVal = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_IO_FRONT_SLOW_READ_RATE);
        int scReadRate = parseInt(rawVal, 0, 1000000, currentValue, ServerConfig.PARAM_IO_FRONT_SLOW_READ_RATE);
        return scReadRate;
    }

    /**
     * Stop property updates
     */
    private void shutdownPropUpdater() {
        if (updater != null) {
            synchronized(updater.lock) {
                updater.run = false;
            }
            updateThread.interrupt();

            boolean stopped = false;
            try {
                updateThread.join(3000);
                if (!updateThread.isAlive()) {
                    stopped = true;
                }
            }
            catch(InterruptedException ie) {
            }

            if (!stopped)
                logger.warning("Property update thread did not stop on request.");

            updater = null;
            updateThread = null;
        }
    }

    /**
     * Update the properties periodically.
     */
    private void initPropUpdater() {
        updater = new TimeoutPropertyUpdate();
        updateThread = new Thread(updater, "TimeoutFilterConfigRefresh");
        updateThread.setDaemon(true);
        updateThread.setPriority(Thread.NORM_PRIORITY-1);
        updateThread.setUncaughtExceptionHandler(ShutdownExceptionHandler.getInstance());
        updateThread.start();
    }

    private final class TimeoutPropertyUpdate implements Runnable {
        private boolean run;
        private Object lock = new Object();

        private TimeoutPropertyUpdate() {
            synchronized(lock) {
                run = true;
            }
        }

        public void run() {
            while(true) {
                try {
                    boolean stop = false;
                    while(true) {
                        synchronized(lock) {
                            stop = !run;
                        }
                        if (stop) {
                            break;
                        }

                        long currentTimeout = timeout.get();
                        long currentReadTime = readTime.get();
                        int currentReadRate = readRate.get();

                        long newTimeout = getTimeout(currentTimeout);
                        long newReadTime = getReadTime(currentReadTime);
                        int newReadRate = getReadRate(currentReadRate);

                        if (newTimeout != currentTimeout ||
                            newReadTime != currentReadTime ||
                            newReadRate != currentReadRate) {

                            if (newTimeout != currentTimeout)
                                logger.log(Level.CONFIG, "Updating timeout, new value is {0}", newTimeout);

                            if (newReadTime != currentReadTime)
                                logger.log(Level.CONFIG, "Updating readTime, new value is {0}", newReadTime);

                            if (newReadRate != currentReadRate)
                                logger.log(Level.CONFIG, "Updating readRate, new value is {0}", newReadRate);

                            timeout.set(newTimeout);
                            readTime.set(newReadTime);
                            readRate.set(newReadRate);
                        }

                        Thread.sleep(30000);
                    }

                    if (stop) {
                        break;
                    }
                }
                catch(InterruptedException ie) {
                    synchronized(lock) {
                        run = false;
                    }
                    logger.log(Level.INFO, "Setting shutdown flag for timeout property update thread (interrupted).");
                }
                catch(Exception e) {
                    logger.log(Level.WARNING, "Unexpected exception in timeout property update thread.", e);
                }

                // sleep for a while after any exception
                try {
                    if(run) Thread.sleep(60000);
                }
                catch(InterruptedException ie) {
                    synchronized(lock) {
                        run = false;
                    }
                    logger.log(Level.INFO, "Setting shutdown flag for timeout property update thread (interrupted).");
                }
            }
            logger.log(Level.INFO, "Shutting down timeout property update thread.");
        }
    }

    /**
     * Wrapper for request that creates a timeout servlet input stream
     */
    private static final class TimeoutServletRequest extends HttpServletRequestWrapper {
        private TimeoutInputStream tis;
        private long timeout;
        private long readTime;
        private int readRate;

        private TimeoutServletRequest(HttpServletRequest toWrap, long timeout, long readTime, int readRate) {
            super(toWrap);
            this.timeout = timeout;
            this.readTime = readTime;
            this.readRate = readRate;
        }

        private TimeoutInputStream getTimeoutInputStream() {
            return tis;
        }

        public ServletInputStream getInputStream() throws IOException {
            return tis = new TimeoutInputStream(super.getInputStream(), timeout, readTime, readRate);
        }

        /**
         * @deprecated
         */
        public boolean isRequestedSessionIdFromUrl() {
            return super.isRequestedSessionIdFromUrl();
        }

        /**
         * @deprecated
         */
        public String getRealPath(String string) {
            return super.getRealPath(string);
        }
    }
}
