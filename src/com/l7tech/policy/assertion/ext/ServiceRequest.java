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
     * Returns a <code>java.util.Map</code> of objects representing the current policy execution context.
     *
     * <p>The <code>Map</code> is populated with the following key/value pairs:
     *
     * <p><b>request.http.headerValues</b> -- a <code>java.lang.String</code> array of request header values
     * <p><b>httpRequest</b> -- <code>javax.servlet.http.HttpServletRequest</code> object
     * <p><b>httpResponse</b> -- <code>javax.servlet.http.HttpServletResponse</code> object
     * <p><b>updatedCookies</b> -- a <code>java.util.Vector</code> of request cookies
     * <p><b>originalCookies</b> -- an unmodifiable <code>java.util.Collection</code> of the request cookies
     * <p><b>messageparts</b> -- a two-dimensional <code>java.lang.Object</code> array containing the request mime parts and associated content-types.
     * The size of the 2D array is [number of mime parts][2], where the content-types appear in indeces [i][0] and the associated mime parts appear in indeces [i][1].
     *
     * <p>Note that the <code>HttpServletResponse</code> object is actually a wrapper around <code>javax.servlet.http.HttpServletResponse</code> 
     * with an additional method called <code>addCookie</code> which provides the ability to insert cookies into the response.
     * @return a <code>java.util.Map</code> of data representing the current policy execution context
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