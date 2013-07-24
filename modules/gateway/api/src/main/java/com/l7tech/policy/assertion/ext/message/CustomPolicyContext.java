package com.l7tech.policy.assertion.ext.message;

import com.l7tech.policy.assertion.ext.SecurityContext;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;

import java.io.IOException;
import java.util.Map;

/**
 * Policy enforcement context for the CustomAssertion.
 */
public interface CustomPolicyContext {

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
     *                                                      This is deprecated, use {@link CustomMessage#getKnob(Class)} with <tt>Class</tt> <tt>CustomHttpHeadersKnob</tt>.
     * <p><b>httpRequest</b> -- {@link javax.servlet.http.HttpServletRequest HttpServletRequest} object that can be used to modify the request
     * <p><b>httpResponse</b> -- {@link javax.servlet.http.HttpServletResponse HttpServletResponse} object that can be used to modify the response
     *
     * <p><b>updatedCookies</b> -- an immutable <code>java.util.Vector</code> of request {@link javax.servlet.http.Cookie Cookie} objects
     * <p><b>originalCookies</b> -- an immutable <code>java.util.Collection</code> of the request {@link javax.servlet.http.Cookie Cookie} objects
     *
     * <p><b>messageParts</b> -- an immutable two-dimensional <code>java.lang.Object</code> array containing the request mime parts and associated content-types.
     *                           The size of the 2D array is [number of mime parts][2], where the content-types appear in indexes [i][0] and the associated mime parts appear in indexes [i][1].
     *                           Content-types are of type <code>java.lang.String</code> and mime parts are <code>byte</code> arrays and can be cast as such.
     *
     * <p><b>request</b> -- {@link com.l7tech.policy.assertion.ext.ServiceRequest ServiceRequest} returns deprecated default Request message object aka. ServiceRequest.
     *                      This context variable is provided for backwards compatibility, in order to access the default request, use either
     *                      {@link #getTargetMessage(com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable)} or
     *                      {@link #getMessage(String)} with request as a target e.g. <code>getMessage("request")</code>.
     * <p><b>response</b> -- {@link com.l7tech.policy.assertion.ext.ServiceResponse ServiceResponse} returns deprecated default Response message object aka. ServiceResponse.
     *                       This context variable is provided for backwards compatibility, in order to access the default response, use either
     *                       {@link #getTargetMessage(com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable)} or
     *                       {@link #getMessage(String)} with response as a target e.g. <code>getMessage("response")</code>.
     *
     * <p><b>Notes</b> The {@link javax.servlet.http.HttpServletResponse HttpServletResponse} object is actually a wrapper around {@link javax.servlet.http.HttpServletResponse HttpServletResponse}
     * with an additional method called <code>addCookie</code> which provides the ability to insert {@link javax.servlet.http.Cookie Cookie} objects into the response.
     * <p> <i>Header values</i>, <i>httpRequests</i> and <i>httpResponses</i> are not guaranteed to exist in the <code>Map</code>.
     * <p> The <i>messageParts</i> object is guaranteed to exist but may not contain any useful data if no mime parts exist in the message.
     *
     * @return a {@link java.util.Map Map} of data representing the current policy execution context
     */
    @SuppressWarnings("JavadocReference")
    Map getContext();

    /**
     * Access a context variable from the policy enforcement context.
     *
     * @param name    The name of the context variable to access.
     * @return  The context variable with the name specified in the method parameter.
     */
    Object getVariable(String name);

    /**
     * Process the input string and expand the variables using the default variables map.
     *
     * @param s the input string
     * @return the string with expanded/resolved variables
     */
    String expandVariable(String s);

    /**
     * Process the input string and expand the variables using the supplied variables map.
     *
     * @param s the input string
     * @param vars the variable map
     * @return the string with expanded/resolved variables
     */
    String expandVariable(String s, Map<String, Object> vars);

    /**
     * Get variables map for named variables in a new mutable case-insensitive map.
     *
     * @param names variables to retrieve. The variable names must not be boxed in ${name} pattern. They must be
     *              variable name only.
     * @return a new mutable map of case-insensitive variable name to value. May be empty but never null.
     */
    Map<String, Object> getVariableMap(String[] names);

    /**
     * Set a context variable from the policy enforcement context.
     *
     * @param name     The name of the context variable to set.
     * @param value    The new value of the context variable.
     */
    void setVariable(String name, Object value);

    /**
     * Obtain the custom message format factory.
     */
    CustomMessageFormatFactory getFormats();

    /**
     * Access the message object associated with the message targetable object.
     *
     * @param targetable    A instance of {@link CustomMessageTargetable} targeting default request, default response or other context variable.
     * @return {@link CustomMessage} object for the targeted variable.
     * @throws NoSuchVariableException If the variable name is null or variable exists but is not a message variable.
     * @throws VariableNotSettableException If the variable does not exist and cannot be created.
     */
    CustomMessage getTargetMessage(CustomMessageTargetable targetable) throws NoSuchVariableException, VariableNotSettableException;

    /**
     * Access the message object associated with the message variable name.
     *
     * <p>To access the default request use <code>"request"</code></p>
     * <p>To access the default response use <code>"response"</code></p>
     *
     * @param targetMessageVariable    The name of the variable exists but is not a message variable
     * @return {@link CustomMessage} object for the variable name.
     * @throws NoSuchVariableException If the variable is not found or is not a Message and a Message is required
     * @throws VariableNotSettableException If the variable does not exist and cannot be created
     */
    CustomMessage getMessage(String targetMessageVariable) throws NoSuchVariableException, VariableNotSettableException;

    /**
     * Determines whether the assertion is invoked before or after routing.
     *
     * @return true if the assertion is invoked after routing, false otherwise.
     */
    boolean isPostRouting();

    /**
     * Parse a MIME Content-Type: header, not including the header name and colon.
     * <p>Example: <code>createContentType("text/html; charset=\"UTF-8\"")</code></p>
     *
     * @param contentTypeValue    the header value to parse.
     * @return a ContentTypeHeader instance.  Never null.
     * @throws java.io.IOException if the specified header value was missing, empty, or syntactically invalid.
     */
    CustomContentType createContentType(String contentTypeValue) throws IOException;
}
