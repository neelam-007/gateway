package com.l7tech.policy.assertion.ext.message;

import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;
import com.l7tech.policy.assertion.ext.message.knob.CustomMessageKnob;
import com.l7tech.policy.assertion.ext.message.knob.NoSuchKnobException;
import java.io.InputStream;
import java.util.Collection;
import org.w3c.dom.Document;

/**
 * Represents an abstract Message in the system.
 * This can be a request, reply or a context variable.
 * All Messages should be assumed <em>not</em> to be thread-safe.
 */
public interface CustomMessage {

    /**
     * Extract the outer content type header associated with the message.
     */
    CustomContentType getContentType();

    /**
     * Change the outer content type of the message.  This will change the value returned by future calls to {@link #getContentType()}.
     * <p/>
     * Use this function in conjunction with {@link #overwrite(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat, Object)},
     * or one of the convenient functions for well-known content types like, {@link #getDocument()}, {@link #getJsonData()} etc.
     * <p/>
     * Note that this function will only set the outer content-type, in order to set the content-type for
     * certain part in multipart message, use the <b><tt>ContentTypeAssertion</tt></b>.
     * <p/>
     * To create a new instance of <tt>CustomContentType</tt> use {@link CustomPolicyContext#createContentType(String)}
     *
     * @param contentType a new content type.  Required.
     * @throws IllegalArgumentException  if content type is null or syntactically invalid.
     */
    void setContentType(CustomContentType contentType) throws IllegalArgumentException;

    /**
     * Get a read-only reference to the message Document.
     * <p/>
     * If the message body is single-part then the function will parse the entire message content as XML.
     * If the message body is multi-part then the function will parse only the first part content as XML.
     * <p/>
     * Callers are expected to avoid changing the returned document in any way.
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #extract(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat)}
     * with a parameter having instance of </tt>CustomMessageFormat&lt;Document&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormatForRepresentationClass(Class) CustomMessageFormatFactory.getFormatForRepresentationClass(Class)},
     * where <tt>Class</tt> is <tt>Document.class</tt>.
     *
     * @return  a read-only reference to the message Document, if the message body or first-part contain XML data.
     *          Null otherwise.
     * @throws CustomMessageAccessException if there is an error while parsing the message data.
     */
    Document getDocument() throws CustomMessageAccessException;

    /**
     * Convenient function for setting the message content to XML Document.
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #overwrite(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat, Object)}
     * with a parameter having instance of <tt>CustomMessageFormat&lt;Document&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormatForRepresentationClass(Class) CustomMessageFormatFactory.getFormatForRepresentationClass(Class)},
     * where <tt>Class</tt> is <tt>Document.class</tt>.
     * <p/>
     * Note that, you can use {@link #setContentType(CustomContentType)} or <tt>ContentTypeAssertion</tt> to set the content type if needed or not already set accordingly.
     *
     * @param document the DOM Document containing the new message data.
     * @throws CustomMessageAccessException if there is an error while writing the message data.
     */
    void setDocument(Document document) throws CustomMessageAccessException;

    /**
     * Retrieve JSON data from the message body.
     * <p/>
     * If the message body is single-part then the function will parse the entire message content as JSON.
     * If the message body is multi-part then the function will parse only the first part content as JSON.
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #extract(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat)}
     * with a parameter having instance of </tt>CustomMessageFormat&lt;CustomJsonData&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormatForRepresentationClass(Class) CustomMessageFormatFactory.getFormatForRepresentationClass(Class)},
     * where <tt>Class</tt> is <tt>CustomJsonData.class</tt>.
     *
     * @throws CustomMessageAccessException if there is an error while parsing the message data.
     */
    CustomJsonData getJsonData() throws CustomMessageAccessException;

    /**
     * Convenient function for setting the message content to JSON data.
     * <p/>
     * You can obtain new instance of <tt>CustomJsonData</tt> from {@link CustomMessageFormat#createBody(Object)}
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #overwrite(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat, Object)}
     * with a parameter having instance of <tt>CustomMessageFormat&lt;CustomJsonData&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormatForRepresentationClass(Class) CustomMessageFormatFactory.getFormatForRepresentationClass(Class)},
     * where <tt>Class</tt> is <tt>CustomJsonData.class</tt>.
     * <p/>
     * Note that, you can use {@link #setContentType(CustomContentType)} or <tt>ContentTypeAssertion</tt> to set the content type if needed or not already set accordingly.
     *
     * @param jsonData object containing the new message data.
     * @throws CustomMessageAccessException if there is an error while writing the message data.
     */
    void setJsonData(CustomJsonData jsonData) throws CustomMessageAccessException;

    /**
     * Retrieve entire message body as InputStream.
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #extract(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat)}
     * with a parameter having instance of </tt>CustomMessageFormat&lt;InputStream&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormatForRepresentationClass(Class) CustomMessageFormatFactory.getFormatForRepresentationClass(Class)},
     * where <tt>Class</tt> is <tt>InputStream.class</tt>.
     *
     * @throws CustomMessageAccessException if there is an error while parsing the message data.
     */
    InputStream getInputStream() throws CustomMessageAccessException;

    /**
     * Convenient function for setting the entire message content using <tt>InputStream</tt>.
     * <p/>
     * This is a convenient function and is equivalent to calling {@link #overwrite(com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat, Object)}
     * with a parameter having instance of <tt>CustomMessageFormat&lt;InputStream&gt;</tt> obtained from
     * {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory#getFormatForRepresentationClass(Class) CustomMessageFormatFactory.getFormatForRepresentationClass(Class)},
     * where <tt>Class</tt> is <tt>InputStream.class</tt>.
     * <p/>
     * Note that, you can use {@link #setContentType(CustomContentType)} or <tt>ContentTypeAssertion</tt> to set the content type if needed or not already set accordingly.
     *
     * @param inputStream input stream containing the new message data.
     * @throws CustomMessageAccessException if there is an error while writing the message data.
     */
    void setInputStream(InputStream inputStream) throws CustomMessageAccessException;

    /**
     * Retrieve the message content using the specified message format.
     *
     * @param format    requested message format to output the message content data.
     * @return  instance of the format representation class.
     * @throws CustomMessageAccessException if there is an error while parsing the message data.
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
     */
    <T> void overwrite(CustomMessageFormat<T> format, T value) throws CustomMessageAccessException;

    /**
     * Retrieve the knob specified by the <tt>knobClass</tt>.
     *
     * @param knobClass    requested knob class.
     * @return an instance of <tt>CustomMessageKnob</tt> represented with the <tt>knobClass</tt>.
     * @throws NoSuchKnobException if there is no knob with class <tt>knobClass</tt> attached to the message.
     */
    <K extends CustomMessageKnob> K getKnob(Class<K> knobClass) throws NoSuchKnobException;

    /**
     * Obtain a list of knobs attached to this message.
     */
    Collection<CustomMessageKnob> getAttachedKnobs();

}
