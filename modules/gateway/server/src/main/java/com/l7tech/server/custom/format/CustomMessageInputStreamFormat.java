package com.l7tech.server.custom.format;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomMessageAccessException;
import com.l7tech.server.custom.CustomMessageImpl;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Implementation of <tt>CustomMessageFormat&lt;InputStream&gt;</tt>
 */
public class CustomMessageInputStreamFormat extends CustomMessageFormatBase<InputStream> {

    public CustomMessageInputStreamFormat(@NotNull final String name, @NotNull final String description) {
        super(name, description);
    }

    @Override
    public Class<InputStream> getRepresentationClass() {
        return InputStream.class;
    }

    @Override
    public InputStream extract(final CustomMessage message) throws CustomMessageAccessException {
        if (message == null) {
            throw new CustomMessageAccessException("message cannot be null");
        }
        if (!(message instanceof CustomMessageImpl)) {
            throw new CustomMessageAccessException("message is of unsupported type");
        }

        final Message targetMessage = ((CustomMessageImpl)message).getMessage();
        try {
            final MimeKnob knobBytes;
            if (targetMessage.isInitialized() && (knobBytes = targetMessage.getKnob(MimeKnob.class)) != null) {
                return knobBytes.getEntireMessageBodyAsInputStream();
            }
        } catch (IOException | NoSuchPartException e) {
            throw new CustomMessageAccessException("Error while extracting InputStream message value", e);
        }

        return null;
    }

    @Override
    public void overwrite(final CustomMessage message, final InputStream contents) throws CustomMessageAccessException {
        if (message == null) {
            throw new CustomMessageAccessException("message cannot be null");
        }
        if (!(message instanceof CustomMessageImpl)) {
            throw new CustomMessageAccessException("message is of unsupported type");
        }

        final Message targetMessage = ((CustomMessageImpl)message).getMessage();
        try {
            targetMessage.initialize(
                    new ByteArrayStashManager(),
                    CustomMessageImpl.extractContentTypeHeader(targetMessage),
                    contents
            );
        } catch (IOException e) {
            throw new CustomMessageAccessException("Error while overwriting InputStream message value", e);
        }
    }

    /**
     * Create new InputStream from a template content.
     * <p>Supported content classes are <tt>String</tt> and <tt>InputStream</tt>.</p>
     * <p>This is more of a convenient method for creating InputStream from <tt>String</tt>.</p>
     * <p>Note that if <tt>content</tt> is <tt>InputStream</tt>, then the function simply returns the content reference.</p>
     * <p>When <tt>content</tt> is <tt>String</tt>, then the function returns a <tt>InputStream</tt> wrapped around {@link StringReader}.</p>
     *
     * @param content    the content of the message, it can be either <tt>String</tt> or <tt>InputStream</tt>.
     * @return a new {@link InputStream} holding the <tt>content</tt>.
     * @throws CustomMessageAccessException if <tt>content</tt> is null
     * or if content class is other then <tt>String</tt> or <tt>InputStream</tt>
     * or if Exception is thrown during reading of the <tt>content</tt>.
     *
     * @see com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat#createBody(Object)
     */
    @Override
    public <K> InputStream createBody(K content) throws CustomMessageAccessException {
        if (content == null) {
            throw new CustomMessageAccessException("content cannot be null");
        }

        if (content instanceof String) {
            final StringReader reader = new StringReader((String)content);
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return reader.read();
                }
            };
        } else if (content instanceof InputStream) {
            // simply return the content
            return (InputStream)content;
        }

        throw new CustomMessageAccessException("Unsupported content type: " + content.getClass().getSimpleName());
    }
}
