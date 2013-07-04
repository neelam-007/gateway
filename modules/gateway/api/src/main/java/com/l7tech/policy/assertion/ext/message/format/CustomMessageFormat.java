package com.l7tech.policy.assertion.ext.message.format;

import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomMessageAccessException;

/**
 * Provide means for accessing and modifying CustomMessage data.
 *
 * <p>Template parameter <tt>T</tt> specifies the message data runtime class </p>
 */
public interface CustomMessageFormat<T> {

    /**
     * Get the message data runtime class e.g. org.w3c.dom.Document for XML format.
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
     * Retrieve the <tt>message</tt> data
     *
     * @param message    the custom message to extract data from.
     * @return an instance of the message data representation class.
     * @throws CustomMessageAccessException if an error happens during the extraction process.
     */
    T extract(CustomMessage message) throws CustomMessageAccessException;

    /**
     * Overwrite the <tt>message</tt> data.
     *
     * <p>Some notes on the different formats:</p>
     * <ol>
     *     <li>
     *         XML --- will throw if the previous content was not XML.
     *         <p>As a workaround, first set the content-type to XML, then override the message content using InputStream e.g.</p>
     *         <p>
     *             <code>
     *                 message.setContentType(policy.createContentType("text/xml; encoding\"utf-8\""));
     *                 message.overwrite(policy.getFormats().getStreamFormat(), some_stream_containing_xml_data);
     *             </code>
     *         </p>
     *     </li>
     *     <li>
     *         JSON --- will automatically set the content-type to application/json; charset=UTF-8
     *     </li>
     *     <li>
     *         InputStream --- will preserve the previous content-type, so in order to change the content-type either use
     *         {@link CustomMessage#setContentType(com.l7tech.policy.assertion.ext.message.CustomContentType) CustomMessage.setContentType}
     *         or <tt>ContentTypeAssertion</tt>.
     *         <p>Note that any subsequent calls to overwrite on an previously initialized message as InputStream
     *         will close the stream and make it unusable afterwards, unless overwrite is called for a new insput stream.</p>
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
     *
     * <p>For now supported content classes are <tt>String</tt> and <tt>InputStream</tt>.</p>
     *
     * <p>The function expects <tt>content</tt> encoding to be default.</p>
     *
     * Some notes on the different formats:
     * <ol>
     *     <li>
     *         XML
     *         <ul>
     *             <li>Supports both <tt>String</tt> and <tt>InputStream</tt> content classes.</li>
     *             <li>This is more of a convenient method for creating DOM documents from <tt>String</tt> and <tt>InputStream</tt>.</li>
     *         </ul>
     *     </li>
     *     <li>
     *         JSON
     *         <ul>
     *             <li>Supports both <tt>String</tt> and <tt>InputStream</tt> content classes.</li>
     *             <li>For <tt>String</tt> <tt>content</tt> this function never throws, even if the <tt>content</tt> is not a well-formed JSON data.
     *             In that case calling {@link com.l7tech.policy.assertion.ext.message.CustomJsonData#getJsonObject() CustomJsonData.getJsonObject()}
     *             will throw {@link com.l7tech.policy.assertion.ext.message.InvalidDataException InvalidDataException}</li>
     *         </ul>
     *     </li>
     *     <li>
     *         InputStream
     *         <ul>
     *             <li>Supports both <tt>String</tt> and <tt>InputStream</tt> content classes.</li>
     *             <li>This is more of a convenient method for creating InputStream from <tt>String</tt>.</li>
     *             <li>When <tt>content</tt> is <tt>InputStream</tt>, the function simply returns the content reference.</li>
     *             <li>When <tt>content</tt> is <tt>String</tt>, the function returns a <tt>InputStream</tt> wrapped around {@link java.io.StringReader StringReader}.</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @param content    the content of the message, it can be either <tt>String</tt> or <tt>InputStream</tt>.
     * @return a new instance of the message data representation class holding the <tt>content</tt>.
     * @throws CustomMessageAccessException if <tt>content</tt> is null
     * or if content class is other then <tt>String</tt> or <tt>InputStream</tt>
     * or, when error happens while parsing <tt>content</tt> data.
     */
    <K> T createBody(K content) throws CustomMessageAccessException;
}
