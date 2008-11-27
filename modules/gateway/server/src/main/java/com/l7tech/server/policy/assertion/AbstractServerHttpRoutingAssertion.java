package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        Integer timeout = validateTimeout(assertion.getConnectionTimeout());

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
        Integer timeout = validateTimeout(assertion.getTimeout());

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
        return assertion.getMaxConnections();
    }

    /**
     * Get the maximum number of connections to all hosts
     *
     * @return The max
     */
    protected int getMaxConnectionsAllHosts() {
        return assertion.getMaxConnections() * 10;
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
                auditor.logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, addr);
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

    /**
     *
     */
    protected void doTaiCredentialChaining(PolicyEnforcementContext context, GenericHttpRequestParams routedRequestParams, URL url) {
        String chainId = null;
        if (!context.isAuthenticated()) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_NOT_AUTHENTICATED);
        } else {
            User clientUser = context.getLastAuthenticatedUser();
            if (clientUser != null) {
                String id = clientUser.getLogin();
                if (id == null || id.length() < 1) id = clientUser.getName();
                if (id == null || id.length() < 1) id = clientUser.getId();

                if (id != null && id.length() > 0) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_CHAIN_USERNAME, id);
                    chainId = id;
                } else
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_NO_USER_ID, id);
            } else {
                final String login = context.getLastCredentials().getLogin();
                if (login != null && login.length() > 0) {
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_CHAIN_LOGIN, login);
                    chainId = login;
                } else
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_TAI_NO_USER);
            }

            if (chainId != null && chainId.length() > 0) {
                routedRequestParams.addExtraHeader(new GenericHttpHeader(IV_USER, chainId));
                HttpCookie ivUserCookie = new HttpCookie(IV_USER, chainId, 0, url.getPath(), url.getHost());
                Collection cookies = Collections.singletonList(ivUserCookie);
                routedRequestParams.addExtraHeader(
                        new GenericHttpHeader(HttpConstants.HEADER_COOKIE,
                                              HttpCookie.getCookieHeader(cookies)));

                // there is no defined quoting or escape mechanism for HTTP cookies so we'll use URLEncoding
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_ADD_OUTGOING_COOKIE, IV_USER);
            }
        }
    }

    //- PRIVATE

    private static final String IV_USER = "IV_USER";

    private final Logger logger;
}
