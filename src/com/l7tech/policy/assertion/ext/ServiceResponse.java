package com.l7tech.policy.assertion.ext;

import org.w3c.dom.Document;

import java.util.Map;

/**
 * Defines an interface to provide request information to custom assertions.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @see ServiceInvocation
 */
public interface ServiceResponse {
    /**
     * Get the copy of the document that is associated with the current response.
     *
     * @return the DOM <code>Document</code> attached to this service response.
     */
    Document getDocument();

    /**
     * Set the response document.
     *
     * @param document the DOM <code>Document</code> to attach to this service
     *                 response. The exisitng document will be replaced.
     */
    void setDocument(Document document);

    /**
     * Context is a <code>Map</code> of key/value properties that is associated
     * with the service invocation.
     * When running within the servlet engine the contaxt will contain elements
     * "httpRequest" and "httpResponse" that correspond to the HttpServletRequest
     * and HttpServletResponse.
     *
     * @return the map of key/value pairs represnting the invocation context.
     */
    Map getContext();
}