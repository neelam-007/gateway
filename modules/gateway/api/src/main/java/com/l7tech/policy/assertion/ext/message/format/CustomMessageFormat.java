package com.l7tech.policy.assertion.ext.message.format;

import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomMessageAccessException;

/**
 * Provide means for accessing and modifying CustomMessage data.
 * <p/>
 * The following is a list of commonly used message formats:
 * <ol>
 *     <li>
 *         <code>CustomMessageFormat&lt;Document&gt;</code> with representation class {@link org.w3c.dom.Document Document}
 *         and name {@link CustomMessageFormatFactory#XML_FORMAT XML}
 *     </li>
 *     <li>
 *         <code>CustomMessageFormat&lt;CustomJsonData&gt;</code> with representation class
 *         {@link com.l7tech.policy.assertion.ext.message.CustomJsonData CustomJsonData}
 *         and name {@link CustomMessageFormatFactory#JSON_FORMAT JSON}
 *     </li>
 *     <li>
 *         <code>CustomMessageFormat&lt;InputStream&gt;</code> with representation class {@link java.io.InputStream InputStream}
 *         and name {@link CustomMessageFormatFactory#INPUT_STREAM_FORMAT InputStream}
 *     </li>
 * </ol>
 *
 * @param <T>    specifies the message data representation class.
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
     * <ol>
     *     <li>
     *         For {@link CustomMessageFormatFactory#XML_FORMAT XML} format the method will return the XML portion of the
     *         message body i.e. returns the message content whose first part's content type is <code>text/xml</code>, or null otherwise.
     *     </li>
     *     <li>
     *         For {@link CustomMessageFormatFactory#JSON_FORMAT JSON} format the method will return the JSON portion of the
     *         message body i.e. returns the message content whose first part's content type is <code>application/json</code>, or null otherwise.
     *         <p/>
     *         Note that for this format the method will <b>not</b> throw {@link CustomMessageAccessException} even if
     *         the message first part's content type is application/json, and the body is not a well formatted json.
     *         However the returned {@link com.l7tech.policy.assertion.ext.message.CustomJsonData CustomJsonData} object,
     *         will throw when {@link com.l7tech.policy.assertion.ext.message.CustomJsonData#getJsonObject() CustomJsonData.getJsonObject()}
     *         method is called.
     *     </li>
     *     <li>
     *         For {@link CustomMessageFormatFactory#INPUT_STREAM_FORMAT InputStream} format the method will return an
     *         InputStream containing the entire message body bytes, including any attachments, or <b>null</b>
     *         if the message object is empty i.e. not initialized.
     *     </li>
     * </ol>
     *
     * @param message    the custom message to extract data from.
     * @return an instance of the message data representation class.
     * @throws CustomMessageAccessException if an error happens during the extraction process, which includes:
     * <ol>
     *     <li>if the <tt>message</tt> parameter is null</li>
     *     <li>if the <tt>message</tt> implementation is of unsupported type</li>
     *     <li>if the <tt>message</tt> first part's content type is <code>text/xml</code> i.e. <b>XML</b> but the body is not a well formatted xml</li>
     *     <li>if the message content <code>InputStream</code> was read destructively.</li>
     * </ol>
     */
    T extract(CustomMessage message) throws CustomMessageAccessException;

    /**
     * Overwrite the specified <tt>message</tt> content body, with the specified <tt>contents</tt> data,
     * using the representation class specified in the template argument.
     *
     * <ol>
     *     <li>
     *         For {@link CustomMessageFormatFactory#XML_FORMAT XML} format the method will try to overwrite the entire message
     *         content with the specified <tt>contents</tt> DOM {@link org.w3c.dom.Document}, however the method will throw
     *         if the previous message content was not <code>text/xml</code> i.e. <i>XML</i>.
     *         <p/>
     *         As a workaround, first set the content-type to <code>text/xml</code>, then override the message using InputStream format:
     *         <pre>
     *             <code>message.setContentType(policy.createContentType("text/xml; encoding\"utf-8\""));
     *             policy.getFormat().getStreamFormat().overwrite(message, some_stream_containing_xml_data);</code>
     *         </pre>
     *     </li>
     *     <li>
     *         For {@link CustomMessageFormatFactory#JSON_FORMAT JSON} format the method will try to overwrite the entire
     *         message content with the specified <tt>contents</tt> JSON {@link com.l7tech.policy.assertion.ext.message.CustomJsonData},
     *         and set the content-type accordingly to <code>application/json; charset=UTF-8</code>
     *     </li>
     *     <li>
     *         For {@link CustomMessageFormatFactory#INPUT_STREAM_FORMAT InputStream} format the method will try to overwrite
     *         the entire message content with the specified <tt>contents</tt> {@link java.io.InputStream}.
     *         <p/>
     *         Note that the previous message content-type will be preserved, so in order to change the content-type either use
     *         {@link CustomMessage#setContentType(com.l7tech.policy.assertion.ext.message.CustomContentType) CustomMessage.setContentType}
     *         or <tt>ContentTypeAssertion</tt> assertion.
     *         <p/>
     *         Additional note, any subsequent calls to overwrite on an previously initialized message as InputStream
     *         will close the stream and make it unusable afterwards, unless overwrite is called for a new input stream.
     *     </li>
     * </ol>
     *
     * @param message     the custom message to overwrite data.
     * @param contents    the new content.
     * @throws CustomMessageAccessException if an error happens while overwriting the message data.
     */
    void overwrite(CustomMessage message, T contents) throws CustomMessageAccessException;

    /**
     * Creates a new instance of the message data representation class from the specified <tt>content</tt>.
     * <p/>
     * For now supported content classes are <tt>String</tt> and <tt>InputStream</tt>.
     * <p/>
     * The function expects <tt>content</tt> encoding to be default.
     * <p/>
     * Some notes on the different formats:
     * <ol>
     *     <li>
     *         For {@link CustomMessageFormatFactory#XML_FORMAT XML} format:
     *         <ul>
     *             <li>Both <tt>String</tt> and <tt>InputStream</tt> content classes are supported.</li>
     *             <li>This is more of a convenient method for creating DOM documents from <tt>String</tt> and <tt>InputStream</tt>.</li>
     *         </ul>
     *     </li>
     *     <li>
     *         For {@link CustomMessageFormatFactory#JSON_FORMAT JSON} format:
     *         <ul>
     *             <li>Both <tt>String</tt> and <tt>InputStream</tt> content classes are supported.</li>
     *             <li>For <tt>String</tt> <tt>content</tt> this function never throws, even if the <tt>content</tt> is not a well-formed JSON data.
     *             In that case calling {@link com.l7tech.policy.assertion.ext.message.CustomJsonData#getJsonObject() CustomJsonData.getJsonObject()}
     *             will throw {@link com.l7tech.policy.assertion.ext.message.InvalidDataException InvalidDataException}</li>
     *         </ul>
     *     </li>
     *     <li>
     *         For {@link CustomMessageFormatFactory#INPUT_STREAM_FORMAT InputStream} format:
     *         <ul>
     *             <li>Both <tt>String</tt> and <tt>InputStream</tt> content classes are supported.</li>
     *             <li>This is more of a convenient method for creating InputStream from <tt>String</tt>.</li>
     *             <li>When <tt>content</tt> is <tt>InputStream</tt>, the function simply returns the content reference.</li>
     *             <li>When <tt>content</tt> is <tt>String</tt>, the function returns a <tt>InputStream</tt> wrapped
     *             around {@link java.io.StringReader StringReader}.</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @param <K>        the type of the <tt>content</tt>.  For the commonly used formats it can be <tt>String</tt> or <tt>InputStream</tt>.
     * @param content    the content of the message, it can be either <tt>String</tt> or <tt>InputStream</tt>.
     * @return a new instance of the message data representation class holding the <tt>content</tt>.
     * @throws CustomMessageAccessException if <tt>content</tt> is null
     * or if content class is other then <tt>String</tt> or <tt>InputStream</tt>
     * or, when error happens while parsing <tt>content</tt> data.
     */
    <K> T createBody(K content) throws CustomMessageAccessException;
}
