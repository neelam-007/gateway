package com.l7tech.server.custom.format;

import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory;
import com.l7tech.policy.assertion.ext.message.format.NoSuchMessageFormatException;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Collection;

import org.w3c.dom.Document;

/**
 * {@link CustomMessageFormatFactory} implementation.
 * <p/>
 * Responsible for providing means to access <tt>CustomMessageFormat</tt>'s from CustomAssertions API.
 * Additionally acts as a register for new <tt>CustomMessageFormat</tt>'s.
 */
public class CustomMessageFormatFactoryImpl implements CustomMessageFormatFactory {

    // IMPORTANT: the bean reads the values below in order to set the appropriate message format descriptions.
    // These descriptions should be modified accordingly.
    public static final String XML_FORMAT_DESC = "DOM Custom Message Format";
    public static final String JSON_FORMAT_DESC = "JSON Custom Message Format";
    public static final String INPUT_STREAM_FORMAT_DESC = "Input Stream Custom Message Format";

    private final CustomMessageFormatRegistry customMessageFormatRegistry;
    public CustomMessageFormatRegistry getCustomMessageFormatRegistry() {
        return customMessageFormatRegistry;
    }

    /**
     * Default Constructor.
     *
     * @param customMessageFormatRegistry    The custom message formats registry.
     */
    protected CustomMessageFormatFactoryImpl(@NotNull CustomMessageFormatRegistry customMessageFormatRegistry) {
        this.customMessageFormatRegistry = customMessageFormatRegistry;
    }

    @Override
    public Collection<CustomMessageFormat> getKnownFormats() {
        return customMessageFormatRegistry.getKnownFormats();
    }

    @Override
    public <T> CustomMessageFormat<T> getFormatForRepresentationClass(final Class<T> representationClass) throws NoSuchMessageFormatException {
        if (representationClass == null) {
            throw new NoSuchMessageFormatException(representationClass, "Representation Class is null.");
        }

        final CustomMessageFormat<T> format = getCustomMessageFormatRegistry().get(representationClass);
        if (format != null) {
            return format;
        }

        throw new NoSuchMessageFormatException(representationClass);
    }

    @Override
    public CustomMessageFormat getFormatByName(final String formatName) throws NoSuchMessageFormatException {
        if (formatName == null) {
            throw new NoSuchMessageFormatException(formatName, "Format name is null");
        }

        final CustomMessageFormat format = getCustomMessageFormatRegistry().getForName(formatName);
        if (format != null) {
            return format;
        }

        throw new NoSuchMessageFormatException(formatName);
    }

    @Override
    public CustomMessageFormat<Document> getXmlFormat() throws NoSuchMessageFormatException {
        return getFormatForRepresentationClass(Document.class);
    }

    @Override
    public CustomMessageFormat<CustomJsonData> getJsonFormat() throws NoSuchMessageFormatException {
        return getFormatForRepresentationClass(CustomJsonData.class);
    }

    @Override
    public CustomMessageFormat<InputStream> getStreamFormat() throws NoSuchMessageFormatException {
        return getFormatForRepresentationClass(InputStream.class);
    }
}
