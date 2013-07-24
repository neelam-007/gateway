package com.l7tech.policy.assertion.ext.message;

import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;
import com.l7tech.policy.assertion.ext.message.knob.CustomMessageKnob;
import com.l7tech.policy.assertion.ext.message.knob.NoSuchKnobException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Represents an abstract Message in the system.
 * This can be a request, reply or a context variable.
 * All Messages should be assumed <em>not</em> to be thread-safe.
 */
public interface CustomMessage {

    /**
     * Extract the outer content type header associated with the message.  Never null.
     * <p/>
     * If the message is multipart, use the {@link com.l7tech.policy.assertion.ext.message.knob.CustomPartsKnob CustomPartsKnob}
     * to extract the multipart part's content type.
     *
     * @return message outer content type header, or <code>application/octet-stream</code> if the message is not initialized.
     */
    CustomContentType getContentType();

    /**
     * Change the outer content type of the message.  This will change the value returned by future calls to {@link #getContentType()}.
     * <p/>
     * Note that this function will only set the outer content-type, in order to set the content-type for
     * certain part in multipart message, use the <b><tt>ContentTypeAssertion</tt></b> assertion.
     * <p/>
     * To create a new instance of <tt>CustomContentType</tt> use {@link CustomPolicyContext#createContentType(String)}
     *
     * @param contentType a new content type.  Required.
     * @throws IllegalArgumentException  if content type is null or syntactically invalid.
     */
    void setContentType(CustomContentType contentType) throws IllegalArgumentException;

    /**
     * Retrieve entire message body as InputStream.
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #extract(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat)}
     * with a parameter having instance of </tt>CustomMessageFormat&lt;InputStream&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormat(Class) CustomMessageFormatFactory.getFormat(Class)},
     * where <tt>Class</tt> is <tt>InputStream.class</tt>.
     *
     * @throws CustomMessageAccessException if there is an error while parsing the message data.
     * @see #extract(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat)
     * @see CustomMessageFormat#extract(CustomMessage)
     */
    InputStream getInputStream() throws CustomMessageAccessException;

    /**
     * Convenient function for setting the entire message content using <tt>InputStream</tt>.
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #overwrite(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat, Object)}
     * with a parameter having instance of <tt>CustomMessageFormat&lt;InputStream&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormat(Class) CustomMessageFormatFactory.getFormat(Class)},
     * where <tt>Class</tt> is <tt>InputStream.class</tt>.
     * <p/>
     * Note that, you can use {@link #setContentType(CustomContentType)} or <tt>ContentTypeAssertion</tt> to set the content type if needed or not already set accordingly.
     *
     * @param inputStream input stream containing the new message data.
     * @throws CustomMessageAccessException if there is an error while writing the message data.
     * @see #overwrite(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat, Object)
     * @see CustomMessageFormat#overwrite(CustomMessage, Object)
     */
    void setInputStream(InputStream inputStream) throws CustomMessageAccessException;

    /**
     * Retrieve the message content using the specified message format.
     *
     * @param format    requested message format to output the message content data.
     * @return  instance of the format representation class.
     * @throws CustomMessageAccessException if there is an error while parsing the message data.
     * @see CustomMessageFormat#extract(CustomMessage)
     */
    <T> T extract(CustomMessageFormat<T> format) throws CustomMessageAccessException;

    /**
     * Set the message content using the specified message format.
     * <p/>
     * Note that, you can use {@link #setContentType(CustomContentType)} or <tt>ContentTypeAssertion</tt> to set the content type if needed or not already set accordingly.
     *
     * @param format    requested message format.
     * @param value     new value of the message content, represented by the format class.
     * @throws CustomMessageAccessException if there is an error while writing the message data.
     * @see CustomMessageFormat#overwrite(CustomMessage, Object)
     */
    <T> void overwrite(CustomMessageFormat<T> format, T value) throws CustomMessageAccessException;

    /**
     * Retrieve the knob specified by the <tt>knobClass</tt>.
     * <p/>
     * A knob represents a feature or aspect of a particular message.
     * For example, its HTTP headers, details about the transport, protocol etc.
     *
     * @param knobClass    requested knob class.
     * @param <K>          the knob representation class e.g. {@link com.l7tech.policy.assertion.ext.message.knob.CustomHttpHeadersKnob CustomHttpHeadersKnob}
     * @return an instance of <tt>CustomMessageKnob</tt> represented with the <tt>knobClass</tt>.
     * @throws NoSuchKnobException if there is no knob with class <tt>knobClass</tt> attached to the message.
     * @see CustomMessageKnob
     */
    <K extends CustomMessageKnob> K getKnob(Class<K> knobClass) throws NoSuchKnobException;

    /**
     * Obtain a list of knobs attached to this message.
     */
    Collection<CustomMessageKnob> getAttachedKnobs();

}
