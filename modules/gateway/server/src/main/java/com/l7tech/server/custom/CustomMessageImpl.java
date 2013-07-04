package com.l7tech.server.custom;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.custom.ContentTypeHeaderToCustomConverter;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.ext.message.*;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory;
import com.l7tech.policy.assertion.ext.message.format.NoSuchMessageFormatException;
import com.l7tech.policy.assertion.ext.message.knob.CustomMessageKnob;
import com.l7tech.policy.assertion.ext.message.knob.NoSuchKnobException;
import com.l7tech.server.custom.format.CustomMessageFormatRegistry;
import com.l7tech.server.message.PolicyEnforcementContext;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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

    public CustomMessageImpl(@NotNull final Message message) {
        this.message = message;
        this.messageFormatFactory = CustomMessageFormatRegistry.getInstance().getMessageFormatFactory();
        this.attachedKnobs = new HashMap<>();
    }

    @Override
    public CustomContentType getContentType() {
        return new ContentTypeHeaderToCustomConverter(message.getMimeKnob().getOuterContentType());
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
    public Document getDocument() throws CustomMessageAccessException {
        try {
            return extract(messageFormatFactory.getXmlFormat());
        } catch (NoSuchMessageFormatException e) {
            // it should not happen, this is one of the default CustomMessageFormat
            logger.log(Level.WARNING, "getDocument returning null since it failed unexpectedly with exception:\n", e);
            return null;
        } catch (CustomMessageAccessException e) {
            if (e.getCause() instanceof SAXException) {
                // message doesn't contain any XML data, probably cause the content-type is xml but the data is not.
                logger.log(Level.FINE, "getDocument returning null since it failed unexpectedly with exception:\n", e);
                return null;
            }
            // it's either IOException caused while reading the stream or something else, in any case throw the exception.
            logger.log(Level.WARNING, "getDocument throwing since it failed unexpectedly with exception:\n", e);
            throw e;
        }
    }

    @Override
    public void setDocument(Document document) throws CustomMessageAccessException {
        try {
            overwrite(messageFormatFactory.getXmlFormat(), document);
        } catch (NoSuchMessageFormatException e) {
            // it should not happen, this is one of the default CustomMessageFormat
            logger.log(Level.WARNING, "setDocument failed unexpectedly with exception:\n", e);
        } catch (CustomMessageAccessException e) {
            logger.log(Level.WARNING, "setDocument throwing since it failed unexpectedly with exception:\n", e);
            throw e;
        }
    }

    @Override
    public CustomJsonData getJsonData() throws CustomMessageAccessException {
        try {
            return extract(messageFormatFactory.getJsonFormat());
        } catch (NoSuchMessageFormatException e) {
            // it should never happen, this is one of the default CustomMessageFormat
            logger.log(Level.WARNING, "getJsonData returning null since it failed unexpectedly with exception:\n", e);
            return null;
        } catch (CustomMessageAccessException e) {
            if (e.getCause() instanceof InvalidJsonException) {
                // message doesn't contain any JSON data, probably cause the content-type is json but the data is not.
                logger.log(Level.FINE, "getJsonData returning null since it failed unexpectedly with exception:\n", e);
                return null;
            }
            // it's either IOException caused while reading the stream or something else, in any case throw the exception.
            logger.log(Level.WARNING, "getJsonData throwing since it failed unexpectedly with exception:\n", e);
            throw e;
        }
    }

    @Override
    public void setJsonData(CustomJsonData jsonData) throws CustomMessageAccessException {
        try {
            overwrite(messageFormatFactory.getJsonFormat(), jsonData);
        } catch (NoSuchMessageFormatException e) {
            // it should not happen, this is one of the default CustomMessageFormat
            logger.log(Level.WARNING, "setJsonData failed unexpectedly with exception:\n", e);
        } catch (CustomMessageAccessException e) {
            logger.log(Level.WARNING, "setJsonData throwing since it failed unexpectedly with exception:\n", e);
            throw e;
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
        } catch (CustomMessageAccessException e) {
            logger.log(Level.WARNING, "getInputStream throwing since it failed unexpectedly with exception:\n", e);
            throw e;
        }
    }

    @Override
    public void setInputStream(InputStream inputStream) throws CustomMessageAccessException {
        try {
            overwrite(messageFormatFactory.getStreamFormat(), inputStream);
        } catch (NoSuchMessageFormatException e) {
            // it should not happen, this is one of the default CustomMessageFormat
            logger.log(Level.WARNING, "setInputStream failed unexpectedly with exception:\n", e);
        } catch (CustomMessageAccessException e) {
            logger.log(Level.WARNING, "setInputStream throwing since it failed unexpectedly with exception:\n", e);
            throw e;
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
