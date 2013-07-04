package com.l7tech.policy.assertion.ext.message.format;

import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Collection;

/**
 * Registry of custom message formats.
 * <p/>
 * Among others, the registry includes commonly used ones:
 * <ol>
 *  <li>{@link #XML_FORMAT} with representation class {@link org.w3c.dom.Document}</li>
 *  <li>{@link #JSON_FORMAT} with representation class {@link com.l7tech.policy.assertion.ext.message.CustomJsonData}</li>
 *  <li>{@link #INPUT_STREAM_FORMAT} with representation class {@link java.io.InputStream}</li>
 * </ol>
 */
public interface CustomMessageFormatFactory {

    public static final String XML_FORMAT = "XML";
    public static final String JSON_FORMAT = "JSON";
    public static final String INPUT_STREAM_FORMAT = "InputStream";

    /**
     * Obtain a full list of all known <tt>CustomMessageFormat</tt>'s.
     *
     * @return read-only collection of <tt>CustomMessageFormat</tt>'s.
     */
    Collection<CustomMessageFormat> getKnownFormats();

    /**
     * Retrieve <tt>CustomMessageFormat</tt> associated with the requested message data representation class.
     *
     * @param representationClass    message data runtime class e.g. <tt>org.w3c.dom.Document</tt> for <tt>XML</tt> format.
     * @return the <tt>CustomMessageFormat</tt> object associated with <tt>representationClass</tt>.
     * @throws NoSuchMessageFormatException if there is no <tt>CustomMessageFormat</tt> associated with the <tt>representationClass</tt>.
     */
    <T> CustomMessageFormat<T> getFormatForRepresentationClass(Class<T> representationClass) throws NoSuchMessageFormatException;

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
     * A quick access methods for common {@link #XML_FORMAT} format.
     *
     * @throws NoSuchMessageFormatException if the XML format is not present in the registry.
     */
    CustomMessageFormat<Document> getXmlFormat() throws NoSuchMessageFormatException;

    /**
     * A quick access methods for common {@link #JSON_FORMAT} format.
     *
     * @throws NoSuchMessageFormatException if the JSON format is not present in the registry.
     */
    CustomMessageFormat<CustomJsonData> getJsonFormat() throws NoSuchMessageFormatException;

    /**
     * A quick access methods for common {@link #INPUT_STREAM_FORMAT} format.
     *
     * @throws NoSuchMessageFormatException if the In format is not present in the registry.
     */
    CustomMessageFormat<InputStream> getStreamFormat() throws NoSuchMessageFormatException;
}
