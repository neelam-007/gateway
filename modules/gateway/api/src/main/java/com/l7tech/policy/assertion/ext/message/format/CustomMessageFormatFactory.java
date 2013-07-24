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
     * certain message content body in <i>XML</i> format, i.e. as DOM {@link Document}.
     */
    public static final String XML_FORMAT = "XML";

    /**
     * This is the message format name for the <b>JSON</b> standard format,
     * with representation class {@link CustomJsonData}
     * <p/>
     * Use {@link CustomMessageFormat CustomMessageFormat&lt;CustomJsonData&gt;} message format to extract or overwrite
     * certain message content body in <i>JSON</i> format, i.e. as {@link CustomJsonData}.
     */
    public static final String JSON_FORMAT = "JSON";

    /**
     * This is the message format name for the <b>InputStream</b> standard format,
     * with representation class {@link InputStream}
     * <p/>
     * Use {@link CustomMessageFormat CustomMessageFormat&lt;InputStream&gt;} message format to extract or overwrite
     * certain message entire bytes, using {@link InputStream}.
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
