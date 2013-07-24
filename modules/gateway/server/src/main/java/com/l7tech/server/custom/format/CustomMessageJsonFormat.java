package com.l7tech.server.custom.format;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.custom.JsonDataToCustomConverter;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONFactory;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomMessageAccessException;

import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.custom.CustomMessageImpl;
import com.l7tech.util.BufferPool;

import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * Implementation of <tt>CustomMessageFormat&lt;CustomJsonData&gt;</tt>
 */
public class CustomMessageJsonFormat extends CustomMessageFormatBase<CustomJsonData> {

    /**
     * @see CustomMessageFormatBase#CustomMessageFormatBase(com.l7tech.server.StashManagerFactory, String, String)
     */
    public CustomMessageJsonFormat(@NotNull StashManagerFactory stashManagerFactory, @NotNull final String name, @NotNull final String description) {
        super(stashManagerFactory, name, description);
    }

    @Override
    public Class<CustomJsonData> getRepresentationClass() {
        return CustomJsonData.class;
    }

    @Override
    public CustomJsonData extract(final CustomMessage message) throws CustomMessageAccessException {
        if (message == null) {
            throw new CustomMessageAccessException("message cannot be null");
        }
        if (!(message instanceof CustomMessageImpl)) {
            throw new CustomMessageAccessException("message is of unsupported type");
        }

        final Message targetMessage = ((CustomMessageImpl)message).getMessage();
        try {
            if (targetMessage.isInitialized() && targetMessage.isJson()) {
                return new JsonDataToCustomConverter(targetMessage.getJsonKnob().getJsonData());
            }
        } catch (IOException | InvalidJsonException e) {
            throw new CustomMessageAccessException("Error while extracting JSON message value", e);
        }

        return null;
    }

    @Override
    public void overwrite(final CustomMessage message, final CustomJsonData contents) throws CustomMessageAccessException {
        if (message == null) {
            throw new CustomMessageAccessException("message cannot be null");
        }
        if (!(message instanceof CustomMessageImpl)) {
            throw new CustomMessageAccessException("message is of unsupported type");
        }

        final Message targetMessage = ((CustomMessageImpl)message).getMessage();
        try {
            final byte[] bytes =
                    contents != null ?
                    contents.getJsonData() != null ? contents.getJsonData().getBytes() : new byte[0] :
                    new byte[0];
            targetMessage.initialize(ContentTypeHeader.APPLICATION_JSON, bytes);
        } catch (IOException e) {
            throw new CustomMessageAccessException("Error while overwriting JSON message value", e);
        }
    }

    /**
     * Create an CustomJsonData object from a template content.
     *
     * <p>Supported content classes are <tt>String</tt> and <tt>InputStream</tt>.</p>
     *
     * <p>Note that for <tt>String</tt> <tt>content</tt> this function never throws,
     * even if the <tt>content</tt> is not a well-formed JSON data.
     * In that case calling {@link com.l7tech.policy.assertion.ext.message.CustomJsonData#getJsonObject() CustomJsonData.getJsonObject()}
     * will throw {@link com.l7tech.policy.assertion.ext.message.InvalidDataException InvalidDataException}</p>
     *
     * @param content    the content of the message, it can be either <tt>String</tt> or <tt>InputStream</tt>.
     * @return a new {@link CustomJsonData} holding the <tt>content</tt>.
     * @throws CustomMessageAccessException if <tt>content</tt> is null
     * or if content class is other then <tt>String</tt> or <tt>InputStream</tt>
     * or, when <tt>content</tt> is <tt>InputStream</tt>, if an error happens during stream copying.
     *
     * @see com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat#createBody(Object)
     */
    @Override
    public <K> CustomJsonData createBody(K content) throws CustomMessageAccessException {
        if (content == null) {
            throw new CustomMessageAccessException("content cannot be null");
        }

        if (content instanceof String) {
            return new JsonDataToCustomConverter(JSONFactory.getInstance().newJsonData((String)content));
        } else if (content instanceof InputStream) {
            byte[] buf = BufferPool.getBuffer(16384);
            try {
                final InputStream inputStream = (InputStream)content;
                final StringBuilder sb = new StringBuilder();
                int bytesRead;
                while ((bytesRead = inputStream.read(buf)) > 0) {
                    sb.append(new String(buf, 0, bytesRead));
                }
                return new JsonDataToCustomConverter(JSONFactory.getInstance().newJsonData(sb.toString()));
            } catch (IOException e) {
                throw new CustomMessageAccessException("Error while creating CustomJsonData from InputStream", e);
            } finally {
                BufferPool.returnBuffer(buf);
            }
        }

        throw new CustomMessageAccessException("Unsupported content type: " + content.getClass().getSimpleName());
    }
}
