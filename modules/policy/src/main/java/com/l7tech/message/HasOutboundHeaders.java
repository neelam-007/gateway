package com.l7tech.message;

import com.l7tech.common.http.GenericHttpRequestParams;

/**
 * Extension of HasHeaders with support for header configuration.
 */
public interface HasOutboundHeaders extends HasHeaders {

    /**
     * Sets the specified header to a date value
     *
     * @param name the name of the header to be set. Must not be null or empty.
     * @param date the date value to set.
     */
    void setDateHeader(String name, long date);

    /**
     * Adds a date value to the specified header
     *
     * @param name the name of the header to add a value to. Must not be null or empty.
     * @param date the date value to add.
     */
    void addDateHeader(String name, long date);

    /**
     * Sets a header to a specified value.
     *
     * <p>If the header already has multiple values it will be replaced with the specified single value.</p>
     *
     * @param name the name of the header to be set. Must not be null or empty.
     * @param value the value to set
     */
    void setHeader(String name, String value);

    /**
     * Adds a value to the specified header.
     *
     * @param name the name of the header to add a value to. Must not be null or empty.
     * @param value the value to add. Must not be null.
     */
    void addHeader(String name, String value);

    /**
     * Returns true if the response already contains at least one value for the header with the specified name.
     *
     * @param name The name of the header to test
     * @return true if the response contains at least one value for the specified header, otherwise false.
     */
    boolean containsHeader(String name);

    /**
     * Remove all headers with the given name.
     *
     * @param name The name of the header to remove.
     */
    void removeHeader(String name);

    /**
     * Remove a specific header matching the given name and value
     *
     * @param name The name of the header to remove
     * @param value The value of the header to remove
     */
    void removeHeader(String name, Object value);

    /**
     * Remove all headers.
     */
    void clearHeaders();

    /**
     * Write the accumulated headers to the given GenericHttpRequestParams.
     *
     * <p>Any conflicting headers in the target will be overwritten.</p>
     *
     * @param target The target of the header flush.
     */
    void writeHeaders(GenericHttpRequestParams target);
}
