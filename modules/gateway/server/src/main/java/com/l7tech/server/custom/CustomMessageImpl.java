package com.l7tech.server.custom;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.custom.ContentTypeHeaderToCustomConverter;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.ext.message.*;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory;
import com.l7tech.policy.assertion.ext.message.format.NoSuchMessageFormatException;
import com.l7tech.policy.assertion.ext.message.knob.CustomMessageKnob;
import com.l7tech.policy.assertion.ext.message.knob.NoSuchKnobException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CustomMessage implementation class, wrapping {@link Message} class.
 * <p>Implements {@link CustomMessageFormat} and {@link CustomMessageKnob} functionality.</p>
 * <p>To attach new <tt>CustomMessageKnob</tt>'s use {@link #attachKnob(Class, com.l7tech.policy.assertion.ext.message.knob.CustomMessageKnob) attachKnob} method.</p>
 */
public class CustomMessageImpl implements CustomMessage {

    final Logger logger = Logger.getLogger(CustomMessageImpl.class.getName());

    private final Message message;
    final public Message getMessage() {
        return message;
    }

    private final CustomMessageFormatFactory messageFormatFactory;
    private final Map<Class<? extends CustomMessageKnob>, CustomMessageKnob> attachedKnobs;

    public CustomMessageImpl(@NotNull final CustomMessageFormatFactory messageFormatFactory, @NotNull final Message message) {
        this.message = message;
        this.messageFormatFactory = messageFormatFactory;
        this.attachedKnobs = new HashMap<>();
    }

    /**
     * Utility function for extracting the outer content type of certain message.
     *
     * @param message the message which content type should be extracted
     * @return the outer content type of certain message, or <code>application/octet-stream</code> is message is not initialized.
     */
    static public ContentTypeHeader extractContentTypeHeader(@NotNull Message message) {
        ContentTypeHeader contentTypeHeader = null;
        MimeKnob mimeKnob;
        if (message.isInitialized() && (mimeKnob = message.getKnob(MimeKnob.class)) != null) {
            contentTypeHeader = mimeKnob.getOuterContentType();
        }
        return (contentTypeHeader != null) ? contentTypeHeader : ContentTypeHeader.OCTET_STREAM_DEFAULT; // default to app octet;
    }

    @Override
    public CustomContentType getContentType() {
        return new ContentTypeHeaderToCustomConverter(extractContentTypeHeader(message));
    }

    @Override
    public void setContentType(CustomContentType contentType) throws IllegalArgumentException {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType is required");
        }

        // if the contentType is of our instance (which it should)
        if (contentType instanceof ContentTypeHeaderToCustomConverter) {
            message.getMimeKnob().setOuterContentType(((ContentTypeHeaderToCustomConverter)contentType).getContentTypeHeader());
        } else {

            // TODO: maybe we should throw here instead of re-parsing the content-type header.
            // This basically means that the API developer implemented the CustomContentType interface to provide its own implementation.
            // Its discussable whether we should allow this or not.

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("contentType is not instanceof ContentTypeHeaderToCustomConverter, its actually: " + contentType.getClass().getName());
            }

            try {
                // try to recreate from fullValue
                final String fullValue = contentType.getFullValue();
                message.getMimeKnob().setOuterContentType(ContentTypeHeader.parseValue(fullValue));
            } catch (IOException e) {
                // throw if content-type is not valid.
                throw new IllegalArgumentException("contentType is either empty or syntactically invalid", e);
            }
        }
    }

    @Override
    public InputStream getInputStream() throws CustomMessageAccessException {
        try {
            return extract(messageFormatFactory.getStreamFormat());
        } catch (NoSuchMessageFormatException e) {
            // it should not happen, this is one of the default CustomMessageFormat
            logger.log(Level.WARNING, "getInputStream returning null since it failed unexpectedly with exception:\n", e);
            return null;
        }
    }

    @Override
    public void setInputStream(InputStream inputStream) throws CustomMessageAccessException {
        try {
            overwrite(messageFormatFactory.getStreamFormat(), inputStream);
        } catch (NoSuchMessageFormatException e) {
            // it should not happen, this is one of the default CustomMessageFormat
            logger.log(Level.WARNING, "setInputStream failed unexpectedly with exception:\n", e);
        }
    }

    @Override
    public <T> T extract(CustomMessageFormat<T> format) throws CustomMessageAccessException {
        if (format == null) {
            throw new CustomMessageAccessException("format cannot be null");
        }
        return format.extract(this);
    }

    @Override
    public <T> void overwrite(CustomMessageFormat<T> format, T value) throws CustomMessageAccessException {
        if (format == null) {
            throw new CustomMessageAccessException("format cannot be null");
        }
        format.overwrite(this, value);
    }

    @Override
    public <T extends CustomMessageKnob> T getKnob(Class<T> knobClass) throws NoSuchKnobException {
        if (knobClass == null) {
            throw new NoSuchKnobException(null, "knobClass is null");
        }
        //noinspection unchecked
        T knob = (T) attachedKnobs.get(knobClass);
        if (knob == null) {
            throw new NoSuchKnobException(knobClass);
        }
        return knob;
    }

    @Override
    public Collection<CustomMessageKnob> getAttachedKnobs() {
        return Collections.unmodifiableCollection(attachedKnobs.values());
    }

    /**
     * Use this method to attach specified knob to the message,
     * existing knob associated with the <tt>knobClass</tt> will be overwritten.
     *
     * @param knobClass    the class of the interface provided by this knob implementation
     * @param knob         the knob to attach.
     * @throws IllegalArgumentException when the <tt>knob</tt> is not implementation of <tt>knobClass</tt>
     */
    public <T extends CustomMessageKnob> void attachKnob(@NotNull Class<T> knobClass, @NotNull T knob) throws IllegalArgumentException {
        if (!knobClass.isAssignableFrom(knob.getClass()))
            throw new IllegalArgumentException("knob is not an implementation of knobClass " + knobClass);
        attachedKnobs.put(knobClass, knob);
    }
}
