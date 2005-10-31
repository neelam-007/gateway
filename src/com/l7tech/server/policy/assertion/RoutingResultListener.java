package com.l7tech.server.policy.assertion;

import java.net.URL;

import com.l7tech.common.http.HttpHeaders;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Interface for those interested in routing results.
 *
 * @author Alex, $Author$
 * @version $Revision$
 */
public interface RoutingResultListener {

    /**
     * The request was routed to the protected service but failed, return true to request a retry.
     *
     * <p>The context.{@link com.l7tech.server.message.PolicyEnforcementContext#getResponse()}
     * is <em>likely</em> to have been initialized with the protected service's response.</p>
     *
     * <p>It is not certain that the request will fail when this method is called. To take action
     * on failure implement routed or failed.</p>
     *
     * @param routedUrl the URL that the request was routed to
     * @param status the HTTP response status from the routed request
     * @param headers the HTTP response headers from the routed request
     * @param context the context from the original request.
     * @return true if the request should be retried, false otherwise.
     */
    boolean reroute(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context);

    /**
     * The request was routed to the protected service (possibly successfully).
     *
     * <p>The context.{@link com.l7tech.server.message.PolicyEnforcementContext#getResponse()}
     * is <em>likely</em> to have been initialized with the protected service's response.</p>
     *
     * <p>When this method is called there will be no further routing attempt.</p>
     *
     * @param routedUrl the URL that the request was routed to
     * @param status the HTTP response status from the routed request
     * @param headers the HTTP response headers from the routed request
     * @param context the context from the original request.
     */
    void routed(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context);

    /**
     * The request could not be routed, or the routing has failed, possibly because an exception occurred.
     *
     * <p>If this request was routed the listener will already received a routed message.</p>
     *
     * <p>The context.{@link com.l7tech.server.message.PolicyEnforcementContext#getResponse()}
     * is <em>unlikely</em> to have been initialized with the protected service's response.</p>
     *
     * @param attemptedUrl the URL to which the routing was attempted
     * @param thrown the exception that was thrown.  May be null.
     * @param context the context from the original request.
     */
    void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context);
}
