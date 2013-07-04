package com.l7tech.policy.assertion.ext.message;

import java.nio.charset.Charset;

/**
 * A generic Message content-type interface.
 */
public interface CustomContentType {
    /**
     * Convert MIME charset into Java encoding.
     *
     * @return the Java Charset corresponding to the charset of this content-type header,
     *         or "ISO8859-1" if there isn't any. Always returns some Charset, never null.
     *         The returned Charset may be a default value if the content header did not specify one,
     *         if the one it specified is not textual or if it's not recognized by this system.
     */
    Charset getEncoding();

    /**
     * @return the type, ie "text".  never null
     */
    String getType();

    /**
     * @return the subtype, ie "xml". never null
     */
    String getSubtype();

    /**
     * @return The complete value of this Message content, including all parameters.  Never null.
     *         This will return the complete value even for Messages with a predefined value format.
     *         (that is, it will return "text/xml; charset=utf-8" rather than just "text/xml").
     */
    String getFullValue();

    /**
     * Get the multipart boundary, if this content type isMultipart.
     *
     * @return the Multipart boundary.  Never null or empty.
     * @throws IllegalStateException if not isMultipart().
     */
    String getMultipartBoundary() throws IllegalStateException;

    /**
     * @return true if the type is "text"
     */
    boolean isText();

    /**
     * @return true if the type is "application"
     */
    boolean isApplication();

    /**
     * @return true if the type is "x-www-form-urlencoded"
     */
    boolean isApplicationFormUrlEncoded();

    /**
     * @return true if the type is "application" and subtype is "json"
     */
    boolean isJson();

    /**
     * @return true if the type is "multipart"
     */
    boolean isMultipart();

    /**
     * Check if this type is for some kind of textual XML.
     *
     * Note that this can be true for xhtml content.
     *
     * @return true if the type is "text/xml", "application/xml" or "application/<em>anything</em>+xml
     */
    boolean isXml();

    /**
     * Check if this type is for some kind of textual (X)HTML.
     *
     * <ul>
     * <li>text/html</li>
     * <li>application/xhtml+xml</li>
     * </ul>
     *
     * Note that it is possible for isHtml and isXml to both be true.
     *
     * @return true if textual html content.
     */
    boolean isHtml();

    /**
     * Check if this type is for SOAP.
     *
     * <ul>
     * <li>application/soap+xml</li>
     * </ul>
     *
     * Note that it is possible for isHtml and isXml to both be true.
     *
     * @return true if textual html content.
     */
    boolean isSoap12();

    /**
     * Check if this content type header matches the type and subtype of the given
     * content type header.
     *
     * @param contentHeader    header with the type and subtype to match
     * @return true if this content type matched the specified pattern
     * @see #matches(String, String)
     */
    boolean matches(CustomContentType contentHeader);

    /**
     * Check if this content type header matches the specified pattern.
     *
     * @param type       type to match, or null or "*" to match any type
     * @param subtype    subtype to match, or null or "*" to match any type
     * @return true if this content type matched the specified pattern
     */
    boolean matches(String type, String subtype);

}
