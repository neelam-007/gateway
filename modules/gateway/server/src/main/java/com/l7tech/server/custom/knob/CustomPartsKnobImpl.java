package com.l7tech.server.custom.knob;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.gateway.common.custom.ContentTypeHeaderToCustomConverter;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.ext.message.CustomContentType;
import com.l7tech.policy.assertion.ext.message.knob.CustomPartsKnob;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements CustomPartsKnob
 */
public class CustomPartsKnobImpl extends CustomMessageKnobBase implements CustomPartsKnob {

    private final Message message;

    public CustomPartsKnobImpl(@NotNull Message message) {
        this("MimeMultiParts", "MimeMultiParts custom message knob", message);
    }

    public CustomPartsKnobImpl(@NotNull final String name,
                               @NotNull final String description,
                               @NotNull Message message) {
        super(name, description);
        this.message = message;
    }

    @Override
    public Iterator<Part> iterator() throws NoSuchElementException {
        // get the parts iterator
        PartIterator parts = null;
        boolean buffering = false;
        try {
            MimeKnob mimeKnob;
            if (message.isInitialized() && (mimeKnob = message.getKnob(MimeKnob.class)) != null) {
                parts = mimeKnob.getParts();
                buffering = mimeKnob.isBufferingDisallowed();
            }
        } catch (IOException e) {
            // throw NoSuchElementException, with cause IOException as per getParts javaDoc, IOException can only be
            // thrown if there is a problem reading enough of the message to start the iterator.
            NoSuchElementException exception = new NoSuchElementException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }

        final PartIterator partIterator = parts;
        final boolean disallowBuffering = buffering;

        return new Iterator<Part>() {
            @Override
            public boolean hasNext() {
                return (partIterator != null && partIterator.hasNext());
            }

            @Override
            public Part next() {
                if (partIterator == null) {
                    // should not have happen, throw exception
                    throw new NoSuchElementException("Failed to extract the next element, iterator is invalid!");
                }

                final PartInfo partInfo = partIterator.next();
                return new Part() {
                    @Override
                    public CustomContentType getContentType() {
                        return new ContentTypeHeaderToCustomConverter(partInfo.getContentType());
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        try {
                            return partInfo.getInputStream(disallowBuffering);
                        } catch (NoSuchPartException e) {
                            String message = e.getWhatWasMissing();
                            throw new IOException((message != null && !message.isEmpty()) ? message : e.getMessage(), e);
                        }
                    }
                };
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
