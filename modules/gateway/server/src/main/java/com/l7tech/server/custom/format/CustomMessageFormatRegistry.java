package com.l7tech.server.custom.format;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

/**
 * This is a registry used to store {@link com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat CustomMessageFormat}'s.
 */
public class CustomMessageFormatRegistry {

    private final Map<Class, CustomMessageFormat> knownFormats;
    public final Map<Class, CustomMessageFormat> getFormatMap() {
        return knownFormats;
    }
    
    private final CustomMessageFormatFactory messageFormatFactory;
    public final CustomMessageFormatFactory getMessageFormatFactory() {
        return messageFormatFactory;
    }

    /**
     * singleton instance
     */
    static private CustomMessageFormatRegistry messageFormatRegistry = null;

    /**
     * Create the singleton instance.
     *
     * Initially register three default message formats:
     * <li>{@link CustomMessageFormatFactory#XML_FORMAT} - representation class: {@link org.w3c.dom.Document}</li>
     * <li>{@link CustomMessageFormatFactory#JSON_FORMAT} - representation class: {@link CustomJsonData}</li>
     * <li>{@link CustomMessageFormatFactory#INPUT_STREAM_FORMAT} - representation class: {@link InputStream}</li>
     *
     * @return the singleton instance.
     */
    static public CustomMessageFormatRegistry getInstance() {
        if (messageFormatRegistry == null) {
            //noinspection serial
            messageFormatRegistry = new CustomMessageFormatRegistry(new HashMap<Class, CustomMessageFormat>(){{
                put(Document.class,
                        new CustomMessageXmlFormat(CustomMessageFormatFactory.XML_FORMAT,
                                CustomMessageFormatFactoryImpl.XML_FORMAT_DESC
                        )
                );
                put(CustomJsonData.class,
                        new CustomMessageJsonFormat(CustomMessageFormatFactory.JSON_FORMAT,
                                CustomMessageFormatFactoryImpl.JSON_FORMAT_DESC
                        )
                );
                put(InputStream.class,
                        new CustomMessageInputStreamFormat(CustomMessageFormatFactory.INPUT_STREAM_FORMAT,
                                CustomMessageFormatFactoryImpl.INPUT_STREAM_FORMAT_DESC
                        )
                );
            }});
        }
        return messageFormatRegistry;
    }

    /**
     * Construct using predefined formats.
     * 
     * @param formats    Collection of well known formats.
     */
    protected CustomMessageFormatRegistry(@NotNull final Map<Class, CustomMessageFormat> formats) {
        this.knownFormats = new ConcurrentHashMap<>(formats);
        this.messageFormatFactory = new CustomMessageFormatFactoryImpl(this);
    }

    /**
     * Registers new {@link CustomMessageFormat}.
     * If the CustomMessageFormat instance for <tt>representationClass</tt> already exist,
     * then the existing one will be overridden by the new <tt>format</tt>
     *
     * @param representationClass    {@link CustomMessageFormat} representation class, required.
     * @param format                 {@link CustomMessageFormat} object to be associated with <tt>representationClass</tt>, required.
     */
    public <T> void register(@NotNull final Class<T> representationClass,
                             @NotNull final CustomMessageFormat format) {
        getFormatMap().put(representationClass, format);
    }

    /**
     * Removes the mapping for the specified representation class from the known formats map, if present.
     *
     * @param representationClass the representation class object
     * @return the previous CustomMessageFormat associated with <tt>representationClass</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>representationClass</tt>.
     */
    public <T> CustomMessageFormat remove(@NotNull final Class<T> representationClass) {
        return getFormatMap().remove(representationClass);
    }

    /**
     * Returns the custom message format to which the <tt>representationClass</tt> is associated with.
     *
     * @param representationClass    the representation class whose associated {@link CustomMessageFormat} object
     *                               is to be returned.
     * @return the {@link CustomMessageFormat} object to which the specified <tt>representationClass</tt> is mapped,
     *         or null if there is no mapping for the specified <tt>representationClass</tt>
     */
    public <T> CustomMessageFormat<T> get(@NotNull final Class<T> representationClass) {
        //noinspection unchecked
        return (CustomMessageFormat<T>)getFormatMap().get(representationClass);
    }

    /**
     * Returns the custom message format to which the <tt>formatName</tt> is associated with.
     *
     * Note that <tt>formatName</tt> is case insensitive.
     *
     * Note that this implementation might have performance impact, since it will iterate through
     * all elements, comparing the message format with the given name.
     *
     * @param formatName    a string whose associated {@link CustomMessageFormat} object is to be returned.
     * @return the {@link CustomMessageFormat} object to which the specified <tt>formatName</tt> is mapped,
     *         or null if there is no mapping for the specified <tt>formatName</tt>
     */
    public CustomMessageFormat getForName(@NotNull final String formatName) {
        for (CustomMessageFormat format: getFormatMap().values()) {
            final String fName = format.getFormatName();
            if (fName != null && formatName.compareToIgnoreCase(fName) == 0) {
                return format;
            }
        }

        return null;
    }

    /**
     * @return return a thread-safe unmodifiable collection of values contained by the <tt>knownFormats</tt>
     */
    public Collection<CustomMessageFormat> getKnownFormats() {
        return Collections.unmodifiableCollection(getFormatMap().values());
    }
}
