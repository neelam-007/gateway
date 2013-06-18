package com.l7tech.policy.assertion.ext.message;

/**
 * A simple pair for holding message data and the content type.
 */
public interface CustomMessageData<T> {
    /**
     * @return The content type associated with the Message
     */
    CustomContentHeader getContentType();

    /**
     * @return Depending on the requested format, i.e. T would be:
     *         <p>if {@link CustomMessageFormat#XML} then, {@link org.w3c.dom.Document}.</p>
     *         <p>if {@link CustomMessageFormat#JSON} then, {@link CustomJsonData} </p>
     *         <p>if {@link CustomMessageFormat#BYTES} then, byte array i.e. byte[] </p>
     *         <p>if {@link CustomMessageFormat#INPUT_STREAM} then, {@link java.io.InputStream}.
     *         <p/>
     *         Note that this method can return null, if the data is malformed or empty.
     */
    T getData();
}
