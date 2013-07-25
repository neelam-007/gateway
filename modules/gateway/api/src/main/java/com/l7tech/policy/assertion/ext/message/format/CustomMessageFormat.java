package com.l7tech.policy.assertion.ext.message.format;

import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomMessageAccessException;

/**
 * Provide means for accessing and modifying CustomMessage data.
 *
 * @param <T>    specifies the message data representation class.
 * @see CustomMessageFormatFactory
 */
public interface CustomMessageFormat<T> {

    /**
     * Get the message data runtime class associated with the format e.g. org.w3c.dom.Document for XML format.
     */
    Class<T> getRepresentationClass();

    /**
     * Retrieve the format name.
     */
    String getFormatName();

    /**
     * Retrieve the format description.
     */
    String getFormatDescription();

    /**
     * Retrieve the specified <tt>message</tt> content body, using the representation class specified in the template argument.
     *
     * @param message    the custom message to extract data from.
     * @return an instance of the message data representation class.
     * @throws CustomMessageAccessException if an error happens during the extraction process, which includes:
     * <ol>
     *     <li>if the <tt>message</tt> parameter is null</li>
     *     <li>if the <tt>message</tt> implementation is of unsupported type</li>
     *     <li></li>
     *     <li></li>
     * </ol>
     * @see CustomMessageFormatFactory
     */
    T extract(CustomMessage message) throws CustomMessageAccessException;

    /**
     * Overwrite the specified <tt>message</tt> content body, with the specified <tt>contents</tt> data,
     * using the representation class specified in the template argument.
     *
     * @param message     the custom message to overwrite data.
     * @param contents    the new content.
     * @throws CustomMessageAccessException if an error happens while overwriting the message data.
     * @see CustomMessageFormatFactory
     */
    void overwrite(CustomMessage message, T contents) throws CustomMessageAccessException;

    /**
     * Creates a new instance of the message data representation class from the specified <tt>content</tt>.
     * <p/>
     * Supported content classes are <tt>String</tt> and <tt>InputStream</tt>.
     * <p/>
     * The function expects <tt>content</tt> encoding to be default.
     *
     * @param <K>        the type of the <tt>content</tt>.  For the commonly used formats it can be <tt>String</tt> or <tt>InputStream</tt>.
     * @param content    the content of the message, it can be either <tt>String</tt> or <tt>InputStream</tt>.
     * @return a new instance of the message data representation class holding the <tt>content</tt>.
     * @throws CustomMessageAccessException if <tt>content</tt> is null,
     * or if content class is other then <tt>String</tt> or <tt>InputStream</tt>,
     * or when error happens while parsing <tt>content</tt> data.
     * @see CustomMessageFormatFactory
     */
    <K> T createBody(K content) throws CustomMessageAccessException;
}
