package com.l7tech.server.policy.assertion;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.MalformedURLException;

import org.springframework.context.ApplicationContext;

import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;

/**
 * Base class for Server HTTP routing assertions
 *
 * @author Steve Jones
 */
public abstract class AbstractServerHttpRoutingAssertion<HRAT extends HttpRoutingAssertion> extends ServerRoutingAssertion<HRAT> {

    //- PROTECTED

    protected final Auditor auditor;

    /**
     * Create a new AbstractServerHttpRoutingAssertion.
     *
     * @param assertion The assertion data.
     * @param applicationContext The spring application context.
     * @param logger The logger to use.
     */
    protected AbstractServerHttpRoutingAssertion(final HRAT assertion,
                                                 final ApplicationContext applicationContext,
                                                 final Logger logger) {
        super(assertion, applicationContext, logger);
        this.logger = logger;
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    /**
     * Get the connection timeout.
     *
     * <p>This is either from the assertion data or the system default.</p>
     *
     * @return The connection timeout in millis
     */
    protected int getConnectionTimeout() {
        Integer timeout = validateTimeout(data.getConnectionTimeout());

        if (timeout == null)
            timeout = super.getConnectionTimeout();

        return timeout;
    }

    /**
     * Get the timeout.
     *
     * <p>This is either from the assertion data or the system default.</p>
     *
     * @return The timeout in millis
     */
    protected int getTimeout() {
        Integer timeout = validateTimeout(data.getTimeout());

        if (timeout == null)
            timeout = super.getTimeout();

        return timeout;
    }

    /**
     * Get the maximum number of connections to each host
     *
     * @return The max
     */
    protected int getMaxConnectionsPerHost() {
        return data.getMaxConnections();
    }

    /**
     * Get the maximum number of connections to all hosts
     *
     * @return The max
     */
    protected int getMaxConnectionsAllHosts() {
        return data.getMaxConnections() * 10;
    }

    /**
     * Validate the given addresses as URL hosts.
     *
     * @param addrs The addresses to validate
     * @return true if all addresses are valid
     */
    protected boolean areValidUrlHostnames(String[] addrs) {
        for (String addr : addrs) {
            try {
                new URL("http", addr, 777, "/foo/bar");
            } catch (MalformedURLException e) {
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, new String[]{addr});
                return false;
            }
        }
        return true;
    }

    /**
     * Validate the given timeout value
     *
     * @param timeout The timeout to check (may be null)
     * @return The timeout if valid, else null
     */
    protected Integer validateTimeout(Integer timeout) {
        Integer value = timeout;

        if (value != null) {
            if (value <= 0 || value > 86400000) { // 1 day in millis
                value = null;
                logger.log(Level.WARNING, "Ignoring out of range timeout {0} (will use system default)", value);
            }
        }

        return value;
    }

    //- PRIVATE

    private final Logger logger;
}
