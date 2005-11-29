package com.l7tech.server.transport.http;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

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
        timeout = DEFAULT_BLOCKED_READ_TIMEOUT;
        readTime = DEFAULT_SLOW_READ_TIMEOUT;
        readRate = DEFAULT_SLOW_READ_LIMIT;
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

        timeout = parseLong(configTimeout, 500, 3600000, DEFAULT_BLOCKED_READ_TIMEOUT, INIT_PROP_TIMEOUT);
        readTime = parseLong(configReadTime, 500, 3600000, DEFAULT_SLOW_READ_TIMEOUT, INIT_PROP_SLOW_READ_THRESHOLD);
        readRate = parseInt(configReadRate, 0, 1000000, DEFAULT_SLOW_READ_LIMIT, INIT_PROP_SLOW_READ_THROUGHPUT);
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
            tsr = new TimeoutServletRequest((HttpServletRequest) servletRequest, timeout, readTime, readRate);
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
     * Nothing to destroy.
     */
    public void destroy() {
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

    //
    private long timeout;
    private long readTime;
    private int readRate;

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
