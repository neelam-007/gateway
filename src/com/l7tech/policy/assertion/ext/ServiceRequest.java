package com.l7tech.policy.assertion.ext;

import org.w3c.dom.Document;

import java.util.Map;

/**
 * Defines an interface to provide request information to custom assertions.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @see ServiceInvocation
 */
public interface ServiceRequest {
    /**
     * Get the copy of the document that is associated with the current request.
     *
     * @return the DOM <code>Document</code> attached to this service request.
     */
    Document getDocument();

    /**
     * Set or replace the request document
     *
     * @param document the DOM <code>Document</code> to attach to this service
     *                 request. The exisitng document will be replaced.
     */
    void setDocument(Document document);

    /**
     * Get the security context associated with the request
     *
     * @return the security context associated with the request.
     */
    SecurityContext getSecurityContext();

    /**
     * @return A <code>Map</code> to store and retrieve data pertaining to the scope of the
     * request at hand. Custom Assertions may use this map to share objects between themselves within
     * a policy execution context.
     */
    Map getContext();

    /**
     * Access a context variable from the policy enforcement context
     */
    Object getVariable(String name);

    /**
     * Set a context variable from the policy enforcement context
     */
    void setVariable(String name, Object value);
}