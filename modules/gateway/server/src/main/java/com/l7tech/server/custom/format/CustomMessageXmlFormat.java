package com.l7tech.server.custom.format;

import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomMessageAccessException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.custom.CustomMessageImpl;
import com.l7tech.common.io.XmlUtil;

import org.jetbrains.annotations.NotNull;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of <tt>CustomMessageFormat&lt;Document&gt;</tt>
 */
public class CustomMessageXmlFormat extends CustomMessageFormatBase<Document> {

    /**
     * @see CustomMessageFormatBase#CustomMessageFormatBase(com.l7tech.server.StashManagerFactory, String, String)
     */
    public CustomMessageXmlFormat(@NotNull StashManagerFactory stashManagerFactory, @NotNull final String name, @NotNull final String description) {
        super(stashManagerFactory, name, description);
    }

    @Override
    public Class<Document> getRepresentationClass() {
        return Document.class;
    }

    @Override
    public Document extract(final CustomMessage message) throws CustomMessageAccessException {
        if (message == null) {
            throw new CustomMessageAccessException("message cannot be null");
        }
        if (!(message instanceof CustomMessageImpl)) {
            throw new CustomMessageAccessException("message is of unsupported type");
        }

        final Message targetMessage = ((CustomMessageImpl)message).getMessage();

        try {
            if (targetMessage.isInitialized() && targetMessage.isXml()) {
                return targetMessage.getXmlKnob().getDocumentReadOnly();
            }
        } catch (SAXException | IOException e) {
            throw new CustomMessageAccessException( "Error while extracting message XML value" , e);
        }

        return null;
    }

    @Override
    public void overwrite(final CustomMessage message, final Document contents) throws CustomMessageAccessException {
        if (message == null) {
            throw new CustomMessageAccessException("message cannot be null");
        }
        if (!(message instanceof CustomMessageImpl)) {
            throw new CustomMessageAccessException("message is of unsupported type");
        }

        final Message targetMessage = ((CustomMessageImpl)message).getMessage();
        final XmlKnob knob = targetMessage.getKnob(XmlKnob.class);
        if (knob == null) {
            targetMessage.initialize(contents);
        } else {
            knob.setDocument(contents);
        }
    }

    /**
     * Create new Document object from a template content.
     * <p>Supported content classes are <tt>String</tt> and <tt>InputStream</tt>.</p>
     * <p>This is more of a convenient method for creating DOM documents from <tt>String</tt> and <tt>InputStream</tt>.</p>
     *
     * @param content    the content of the message, it can be either <tt>String</tt> or <tt>InputStream</tt>.
     * @return a new {@link Document} holding the <tt>content</tt>.
     * @throws CustomMessageAccessException if <tt>content</tt> is null
     * or if content class is other then <tt>String</tt> or <tt>InputStream</tt>
     * or if IOException or SAXException are thrown during parsing of the <tt>content</tt>.
     *
     * @see com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat#createBody(Object)
     */
    @Override
    public <K> Document createBody(K content) throws CustomMessageAccessException {
        if (content == null) {
            throw new CustomMessageAccessException("content cannot be null");
        }

        if (content instanceof String) {
            try {
                return XmlUtil.stringToDocument((String)content);
            } catch (SAXException e) {
                throw new CustomMessageAccessException("Error while creating DOM Document from string", e);
            }
        } else if (content instanceof InputStream) {
            try {
                return XmlUtil.parse((InputStream)content);
            } catch (IOException | SAXException e) {
                throw new CustomMessageAccessException("Error while creating DOM Document from InputStream", e);
            }
        }

        throw new CustomMessageAccessException("Unsupported content type: " + content.getClass().getSimpleName());
    }
}
