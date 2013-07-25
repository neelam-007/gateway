package com.l7tech.policy.assertion.ext.message.format;

import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Collection;

/**
 * Factory class for holding registered <code>CustomMessageFormat</code>'s.
 * <p/>
 * Among others, the registry includes the following commonly used formats:
 * <ol>
 *  <li>
 *      {@link CustomMessageFormat CustomMessageFormat&lt;Document&gt;} with representation class {@link org.w3c.dom.Document}
 *      and name {@link #XML_FORMAT XML}
 *  </li>
 *  <li>
 *      {@link CustomMessageFormat CustomMessageFormat&lt;CustomJsonData&gt;} with representation class
 *      {@link com.l7tech.policy.assertion.ext.message.CustomJsonData} and name {@link #JSON_FORMAT JSON}
 *  </li>
 *  <li>
 *      {@link CustomMessageFormat CustomMessageFormat&lt;InputStream&gt;} with representation class {@link java.io.InputStream}
 *      and name {@link #INPUT_STREAM_FORMAT InputStream}
 *  </li>
 * </ol>
 */
public interface CustomMessageFormatFactory {

    /**
     * This is the message format name for the <b>XML</b> standard format,
     * with representation class {@link org.w3c.dom.Document}
     * <p/>
     * Use {@link CustomMessageFormat CustomMessageFormat&lt;Document&gt;} message format to extract or overwrite
     * certain message content body in <i>XML</i> format, i.e. as {@link org.w3c.dom.Document DOM Document}.
     * <p/>
     * When extracting message content, extraction methods
     * ({@link CustomMessageFormat#extract(com.l7tech.policy.assertion.ext.message.CustomMessage) CustomMessageFormat.extract(CustomMessage)} or
     * {@link com.l7tech.policy.assertion.ext.message.CustomMessage#extract(CustomMessageFormat) CustomMessage.extract(CustomMessageFormat)})
     * will return the XML portion of the message body i.e. returns the message content whose first part's content type is <code>text/xml</code>, or null otherwise.
     * They will throw {@link com.l7tech.policy.assertion.ext.message.CustomMessageAccessException CustomMessageAccessException} exception,
     * if the message first part's content type is <code>text/xml</code> but the body is not a well formatted xml.
     * <p/>
     * When overwriting message content, overwrite methods
     * ({@link CustomMessageFormat#overwrite(com.l7tech.policy.assertion.ext.message.CustomMessage, Object) CustomMessageFormat.overwrite(CustomMessage, Object)} or
     * {@link com.l7tech.policy.assertion.ext.message.CustomMessage#overwrite(CustomMessageFormat, Object) CustomMessage.overwrite(CustomMessageFormat, Object)})
     * will try to overwrite the entire message content with the specified {@link org.w3c.dom.Document DOM Document},
     * however the method will throw if the previous message content was not <code>text/xml</code> i.e. <i>XML</i>.
     * <p/>
     * As a workaround, first set the content-type to <code>text/xml</code>, then override the message using InputStream format:
     * <pre>
     *     <code>message.setContentType(policy.createContentType("text/xml; encoding\"utf-8\""));
     *     policy.getFormat().getStreamFormat().overwrite(message, some_stream_containing_xml_data);</code>
     * </pre>
     * <p/>
     * When using {@link CustomMessageFormat#createBody(Object)} to create a {@link org.w3c.dom.Document DOM Document} instance,
     * both <tt>String</tt> and <tt>InputStream</tt> contents are supported.
     * This is more of a convenient method for creating DOM documents from <tt>String</tt> and <tt>InputStream</tt>.
     */
    public static final String XML_FORMAT = "XML";

    /**
     * This is the message format name for the <b>JSON</b> standard format,
     * with representation class {@link CustomJsonData}
     * <p/>
     * Use {@link CustomMessageFormat CustomMessageFormat&lt;CustomJsonData&gt;} message format to extract or overwrite
     * certain message content body in <i>JSON</i> format, i.e. as {@link CustomJsonData}.
     * <p/>
     * When extracting message content, extraction methods
     * ({@link CustomMessageFormat#extract(com.l7tech.policy.assertion.ext.message.CustomMessage) CustomMessageFormat.extract(CustomMessage)} or
     * {@link com.l7tech.policy.assertion.ext.message.CustomMessage#extract(CustomMessageFormat) CustomMessage.extract(CustomMessageFormat)})
     * will return the JSON portion of the message body i.e. returns the message content whose first part's content type is
     * <code>application/json</code>, or null otherwise.
     * Note that for this format the method will <b>not</b> throw {@link com.l7tech.policy.assertion.ext.message.CustomMessageAccessException CustomMessageAccessException}
     * even if the message first part's content type is application/json, and the body is not a well formatted json.
     * However the returned {@link CustomJsonData} object will throw when {@link CustomJsonData#getJsonObject()} method is called.
     * <p/>
     * When overwriting message content, overwrite methods
     * ({@link CustomMessageFormat#overwrite(com.l7tech.policy.assertion.ext.message.CustomMessage, Object) CustomMessageFormat.overwrite(CustomMessage, Object)} or
     * {@link com.l7tech.policy.assertion.ext.message.CustomMessage#overwrite(CustomMessageFormat, Object) CustomMessage.overwrite(CustomMessageFormat, Object)})
     * will try to overwrite the entire message content with the specified {@link CustomJsonData}, and set the content-type accordingly to [<code>application/json; charset=UTF-8</code>].
     * <p/>
     * When using {@link CustomMessageFormat#createBody(Object)} to create a {@link CustomJsonData} instance
     * both <tt>String</tt> and <tt>InputStream</tt> content classes are supported.
     * For <tt>String</tt> content the function never throws, even if the content is not a well-formed JSON data, however
     * calling {@link com.l7tech.policy.assertion.ext.message.CustomJsonData#getJsonObject() CustomJsonData.getJsonObject()},
     * on the newly created object, will throw {@link com.l7tech.policy.assertion.ext.message.InvalidDataException InvalidDataException}
     */
    public static final String JSON_FORMAT = "JSON";

    /**
     * This is the message format name for the <b>InputStream</b> standard format,
     * with representation class {@link java.io.InputStream InputStream}
     * <p/>
     * Use {@link CustomMessageFormat CustomMessageFormat&lt;InputStream&gt;} message format to extract or overwrite
     * certain message entire bytes, using {@link java.io.InputStream InputStream}.
     * <p/>
     * When extracting message content, extraction methods
     * ({@link CustomMessageFormat#extract(com.l7tech.policy.assertion.ext.message.CustomMessage) CustomMessageFormat.extract(CustomMessage)} or
     * {@link com.l7tech.policy.assertion.ext.message.CustomMessage#extract(CustomMessageFormat) CustomMessage.extract(CustomMessageFormat)})
     * will return an InputStream containing the entire message body bytes, including any attachments, or <b>null</b>
     * if the message object is empty i.e. not initialized.
     * They will throw {@link com.l7tech.policy.assertion.ext.message.CustomMessageAccessException CustomMessageAccessException} exception,
     * if the message content <code>InputStream</code> was already read destructively (e.g. already read once while message streaming is enabled).
     * <p/>
     * When overwriting message content, overwrite methods
     * ({@link CustomMessageFormat#overwrite(com.l7tech.policy.assertion.ext.message.CustomMessage, Object) CustomMessageFormat.overwrite(CustomMessage, Object)} or
     * {@link com.l7tech.policy.assertion.ext.message.CustomMessage#overwrite(CustomMessageFormat, Object) CustomMessage.overwrite(CustomMessageFormat, Object)})
     * will try to overwrite the entire message content with the specified {@link java.io.InputStream InputStream}.
     * The previous message content-type will be preserved, so in order to change the content-type afterward either use
     * {@link com.l7tech.policy.assertion.ext.message.CustomMessage#setContentType(com.l7tech.policy.assertion.ext.message.CustomContentType)} CustomMessage.setContentType(CustomContentType)}
     * or <tt>ContentTypeAssertion</tt> assertion.
     * Any subsequent calls to overwrite on an previously initialized message as InputStream will close the stream and
     * make it unusable afterwards, unless overwrite is called for a new input stream.
     * <p/>
     * When using {@link CustomMessageFormat#createBody(Object)} to create a {@link CustomJsonData} instance
     * both <tt>String</tt> and <tt>InputStream</tt> content classes are supported.
     * This is more of a convenient method for creating InputStream from <tt>String</tt>.
     * When input parameter is <tt>InputStream</tt>, the function simply returns the content reference, when it is
     * <tt>String</tt>, the function returns a <tt>InputStream</tt> wrapped around {@link java.io.StringReader StringReader}.
     */
    public static final String INPUT_STREAM_FORMAT = "InputStream";


    /**
     * Obtain a full list of all standard <code>CustomMessageFormat</code>'s.
     *
     * @return read-only collection of <code>CustomMessageFormat</code>'s.
     */
    Collection<CustomMessageFormat> getKnownFormats();

    /**
     * Retrieve <code>CustomMessageFormat</code> associated with the requested message data representation class.
     *
     * @param representationClass    message data runtime class e.g. <code>org.w3c.dom.Document</code> for <tt>XML</tt> format.
     * @return the <code>CustomMessageFormat</code> object associated with <tt>representationClass</tt>.
     * @throws NoSuchMessageFormatException if there is no <code>CustomMessageFormat</code> associated with the <tt>representationClass</tt>.
     */
    <T> CustomMessageFormat<T> getFormat(Class<T> representationClass) throws NoSuchMessageFormatException;

    /**
     * Retrieve <tt>CustomMessageFormat</tt>  by its name e.g. <tt>XML</tt>.
     * Note that <tt>name</tt> is case insensitive.
     *
     * @param name    the name of the message format to retrieve.
     * @return the <tt>CustomMessageFormat</tt> having name <tt>name</tt>.
     * @throws NoSuchMessageFormatException if there is no <tt>CustomMessageFormat</tt> with name <tt>name</tt>.
     */
    CustomMessageFormat getFormatByName(String name) throws NoSuchMessageFormatException;

    /**
     * A quick access methods for common {@link #XML_FORMAT XML} format.
     *
     * @throws NoSuchMessageFormatException if the XML format is not present in the registry.
     */
    CustomMessageFormat<Document> getXmlFormat() throws NoSuchMessageFormatException;

    /**
     * A quick access methods for common {@link #JSON_FORMAT JSON} format.
     *
     * @throws NoSuchMessageFormatException if the JSON format is not present in the registry.
     */
    CustomMessageFormat<CustomJsonData> getJsonFormat() throws NoSuchMessageFormatException;

    /**
     * A quick access methods for common {@link #INPUT_STREAM_FORMAT InputStream} format.
     *
     * @throws NoSuchMessageFormatException if the In format is not present in the registry.
     */
    CustomMessageFormat<InputStream> getStreamFormat() throws NoSuchMessageFormatException;
}
