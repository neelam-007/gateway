package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.message.HttpInboundResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.HttpInboundResponseFacet;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ValidationUtils;
import org.springframework.context.ApplicationContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

/**
 * Base class for Server HTTP routing assertions
 *
 * @author Steve Jones
 */
public abstract class AbstractServerHttpRoutingAssertion<HRAT extends HttpRoutingAssertion> extends ServerRoutingAssertion<HRAT> {

    //- PROTECTED

    /**
     * Create a new AbstractServerHttpRoutingAssertion.
     *
     * @param assertion The assertion data.
     * @param applicationContext The spring application context.
     */
    protected AbstractServerHttpRoutingAssertion(final HRAT assertion,
                                                 final ApplicationContext applicationContext) {
        super(assertion, applicationContext);
    }

    /**
     * Get the connection timeout.
     *
     * <p>This is either from the assertion data or the system default.</p>
     *
     * @return The connection timeout in millis
     */
    @Override
    protected int getConnectionTimeout(Map vars) {

        Integer timeout = expandVariableAsInt(assertion.getConnectionTimeout(), "Connection timeout", 1, 86400000, vars);
        if (timeout == null)
            timeout = super.getConnectionTimeout(null);

        return timeout;
    }

    /**
     * Get the timeout.
     *
     * <p>This is either from the assertion data or the system default.</p>
     *
     * @return The timeout in millis
     */
    @Override
    protected int getTimeout(Map vars) {
        Integer timeout = expandVariableAsInt(assertion.getTimeout(), "Read Timeout", 1, 86400000, vars);
        if (timeout == null)
            timeout = super.getTimeout(null);

        return timeout;
    }

    private Integer expandVariableAsInt( final String expressionValue,
                                             final String description,
                                             final int min,
                                             final int max,
                                             final Map<String, Object> variables ) {
        final Integer value;

        if ( expressionValue != null ) {
            final String textValue = ExpandVariables.process(expressionValue, variables, getAudit()).trim();

            if ( ValidationUtils.isValidInteger(textValue, false, min, max) ) {
                value = Integer.parseInt( textValue );
            } else {
                logAndAudit(AssertionMessages.HTTPROUTE_CONFIGURATION_ERROR, description + " invalid: " + textValue);
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        } else {
            value = null;
        }

        return value;
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
                logAndAudit(AssertionMessages.IP_ADDRESS_INVALID, addr);
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
    protected void doTaiCredentialChaining(AuthenticationContext context, GenericHttpRequestParams routedRequestParams, URL url) {
        String chainId = null;
        if (!context.isAuthenticated()) {
            logAndAudit(AssertionMessages.HTTPROUTE_TAI_NOT_AUTHENTICATED);
        } else {
            User clientUser = context.getLastAuthenticatedUser();
            if (clientUser != null) {
                String id = clientUser.getLogin();
                if (id == null || id.length() < 1) id = clientUser.getName();
                if (id == null || id.length() < 1) id = clientUser.getId();

                if (id != null && id.length() > 0) {
                    logAndAudit(AssertionMessages.HTTPROUTE_TAI_CHAIN_USERNAME, id);
                    chainId = id;
                } else
                    logAndAudit(AssertionMessages.HTTPROUTE_TAI_NO_USER_ID, id);
            } else {
                final String login = context.getLastCredentials().getLogin();
                if (login != null && login.length() > 0) {
                    logAndAudit(AssertionMessages.HTTPROUTE_TAI_CHAIN_LOGIN, login);
                    chainId = login;
                } else
                    logAndAudit(AssertionMessages.HTTPROUTE_TAI_NO_USER);
            }

            if (chainId != null && chainId.length() > 0) {
                routedRequestParams.addExtraHeader(new GenericHttpHeader(IV_USER, chainId));
                HttpCookie ivUserCookie = new HttpCookie(IV_USER, chainId, 0, url.getPath(), url.getHost());
                Collection cookies = Collections.singletonList(ivUserCookie);
                routedRequestParams.addExtraHeader(
                        new GenericHttpHeader(HttpConstants.HEADER_COOKIE,
                                              CookieUtils.getCookieHeader(cookies)));

                // there is no defined quoting or escape mechanism for HTTP cookies so we'll use URLEncoding
                logAndAudit(AssertionMessages.HTTPROUTE_ADD_OUTGOING_COOKIE, IV_USER);
            }
        }
    }

    /**
     * Ensure that the specified message has an HttpInboundResponseKnob.
     *
     * @param message the Message that should have an HttpInboundResponseKnob.  Required.
     * @return an existing or new HttpInboundResponseKnob.  Never null.
     */
    protected static HttpInboundResponseKnob getOrCreateHttpInboundResponseKnob(Message message) {
        HttpInboundResponseKnob httpInboundResponseKnob = message.getKnob(HttpInboundResponseKnob.class);
        if (httpInboundResponseKnob == null) {
            httpInboundResponseKnob = new HttpInboundResponseFacet();
            message.attachKnob(HttpInboundResponseKnob.class, httpInboundResponseKnob);
        }
        return httpInboundResponseKnob;
    }

    //- PRIVATE

    private static final String IV_USER = "IV_USER";
}
