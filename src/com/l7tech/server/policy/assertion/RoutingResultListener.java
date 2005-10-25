package com.l7tech.server.policy.assertion;

import com.l7tech.server.message.PolicyEnforcementContext;

import java.net.URL;

public interface RoutingResultListener {
    /**
     * The request was routed to the protected service.
     *
     * context.{@link com.l7tech.server.message.PolicyEnforcementContext#getResponse()}
     * is <em>likely</em> to have been initialized with the protected service's response.
     *
     * @param routedUrl the URL that the request was routed to
     * @param status the HTTP response status from the routed request
     * @param context the context from the original request.
     * @return true if the request should be retried, false otherwise.
     */
    boolean routed(URL routedUrl, int status, PolicyEnforcementContext context);

    /**
     * The request could not be routed, possibly because an exception occurred.
     * <p>
     * context.{@link com.l7tech.server.message.PolicyEnforcementContext#getResponse()} 
     * is <em>unlikely</em> to have been initialized with the protected service's response.
     *
     * @param attemptedUrl the URL to which the routing was attempted
     * @param thrown the exception that was thrown.  May be null.
     * @param context the context from the original request.
     * @return true if the request should be retried, false otherwise.
     */
    boolean failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context);
}
