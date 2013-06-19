package com.l7tech.policy.assertion.ext;

import org.w3c.dom.Document;
import java.util.Map;

/**
 * Defines an interface to provide request information to custom assertions.
 *
 * @see ServiceInvocation
 */
public interface ServiceRequest {
    /**
     * Get the copy of the document that is associated with the default request.
     *
     * @return the DOM <code>Document</code> attached to this service request.
     */
    Document getDocument();

    /**
     * Set or replace the default request document
     *
     * @param document the DOM <code>Document</code> to attach to this service
     *                 request. The existing document will be replaced.
     */
    void setDocument(Document document);

    // TODO: For now only DOM is supported and extracting message targetable data is to come in the next milestone.

    /**
     * Get the security context associated with the request
     *
     * @return the security context associated with the request.
     */
    SecurityContext getSecurityContext();

    /**
     * Returns a {@link java.util.Map Map} of objects representing the current policy execution context.
     *
     * <p>The {@link java.util.Map Map} is populated with the following key/value pairs:
     *
     * <p><b>request.http.headerValues.[header name]</b> -- a <code>java.lang.String</code> array of request header value(s) for the given [header name].
     * <p><b>httpRequest</b> -- {@link javax.servlet.http.HttpServletRequest HttpServletRequest} object that can be used to modify the request
     * <p><b>httpResponse</b> -- {@link javax.servlet.http.HttpServletResponse HttpServletResponse} object that can be used to modify the response
     * <p><b>updatedCookies</b> -- an immutable <code>java.util.Vector</code> of request {@link javax.servlet.http.Cookie Cookie} objects
     * <p><b>originalCookies</b> -- an immutable <code>java.util.Collection</code> of the request {@link javax.servlet.http.Cookie Cookie} objects
     * <p><b>serviceFinder</b> -- an implementation of {@link ServiceFinder}, which provides access to additional Layer 7 API services.
     * <p><b>messageparts</b> -- an immutable two-dimensional <code>java.lang.Object</code> array containing the request mime parts and associated content-types.
     *
     * The size of the 2D array is [number of mime parts][2], where the content-types appear in indexes [i][0] and the associated mime parts appear in indexes [i][1].
     * Content-types are of type <code>java.lang.String</code> and mime parts are <code>byte</code> arrays and can be cast as such.
     *
     *
     * <p><b>Notes</b> The {@link javax.servlet.http.HttpServletResponse HttpServletResponse} object is actually a wrapper around {@link javax.servlet.http.HttpServletResponse HttpServletResponse}
     * with an additional method called <code>addCookie</code> which provides the ability to insert {@link javax.servlet.http.Cookie Cookie} objects into the response.
     * <p> <i>Header values</i>, <i>httpRequests</i> and <i>httpResponses</i> are not guaranteed to exist in the <code>Map</Map>.
     * <p> The <i>messageparts</i> object is guaranteed to exist but may not contain any useful data if no mime parts exist in the message.
     *
     * @return a {@link java.util.Map Map} of data representing the current policy execution context
     */
    @SuppressWarnings("JavadocReference")
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