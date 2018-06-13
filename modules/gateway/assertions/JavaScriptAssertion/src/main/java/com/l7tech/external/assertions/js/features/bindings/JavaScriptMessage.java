package com.l7tech.external.assertions.js.features.bindings;

/**
 * Interface for all message specific properties
 */
@SuppressWarnings("unused")
public interface JavaScriptMessage {

    /**
     * Gets the content type of the message.
     * @return content type
     */
    String getContentType();

    /**
     * Gets the content of the message. Returns JavaScript JSON object if content is of type JSON, returns String if
     * any other textual content or Java byte[] array.
     * @return content
     */
    Object getContent();

    /**
     * Sets the content of the message
     * @param content
     * @param contentType
     */
    void setContent(Object content, String contentType);

    /**
     * Gets the headers in the JavaScript JSON format
     * @return headers
     */
    Object getHeaders();

    /**
     * Sets the headers of the message. Replaces all the existing headers.
     * @param headers
     */
    void setHeaders(Object headers);

    /**
     * Gets the header value(s) in
     * @param name
     * @return
     */
    Object getHeader(String name);

    void setHeader(String name, Object value);

    void addHeader(String name, String value);

    void removeHeader(String name);

    boolean hasHeader(String name);

    void end();
}
