package com.l7tech.server.message;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.util.ClassUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.util.Collection;

/**
 * Utility methods for working with a Message instance backed by a context variable within some PolicyEnforcementContext.
 */
public class ContextVariableBackedMessageUtils {

    /**
     * Attempt to get a Message from the value of the specified variable within the specified PolicyEnforcementContext.
     * <p/>
     * If the value is of type Message, it will be returned as-is.
     * <p/>
     * Otherwise, if allowNonMessageVar is true, and the value type is either a CharSequence or a singleton array or collection of CharSequence,
     * then a new Message will be created backed by the specified context variable.
     * Changes to the Message will be written back to the context variable when a stash is triggered (such as when
     * the MIME body bytes are changed).
     * <p/>
     * The created Message will be closed when the associated context is closed.
     *
     * @param context the PEC in which variableName is meaningful.  Required.
     * @param variableName the name of the context variable.  Required.
     * @param allowNonMessageVar true to create a context variable backed Message if the target value isn't already of type Message.
     * @return a possibly-new Message instance.  Never null.
     * @throws NoSuchVariableException
     */
    public static Message getTargetMessage(@NotNull PolicyEnforcementContext context, @NotNull String variableName, boolean allowNonMessageVar)
            throws NoSuchVariableException
    {
        final Object value = context.getVariable(variableName);

        if (value == null)
            throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is null");

        if (value instanceof Message)
            return (Message) value;

        if (!allowNonMessageVar)
            throw new NoSuchVariableException(variableName,
                    MessageFormat.format("Request message source (\"{0}\") is a context variable of the wrong type (expected=Message, actual={1}).",
                            variableName, ClassUtils.getClassName(value.getClass())));

        return createContextVariableBackedMessage(context, variableName, ContentTypeHeader.TEXT_DEFAULT, asMessageValueString(variableName, value));
    }

    /**
     * Create a Message backed by the specified context variable within the specified policy enforcement context using the specified
     * initial value interpreted as the specified content type.
     *
     * @param context policy enforcement context in which the variable lives.  Required.
     * @param variableName name of the context variable within this context to which changes to the Message should be written.  Required.
     * @param contentType the content type to use for the message body.  Required.
     * @param initialValue a String representing the initial value to use for the message body.  Required.
     * @return a new Message instance, configured with a ContextVariableKnob, which will be closed when the context is closed.
     */
    public static Message createContextVariableBackedMessage(final @NotNull PolicyEnforcementContext context,
                                                             final @NotNull String variableName,
                                                             final @NotNull ContentTypeHeader contentType,
                                                             @NotNull String initialValue)
    {
        final Message mess = new Message();
        context.runOnClose(new Runnable() {
            @Override
            public void run() {
                ResourceUtils.closeQuietly(mess);
            }
        });

        try {
            final ContextVariableKnob cvk = new ContextVariableKnob(variableName);

            StashManager sm = new ByteArrayStashManager() {
                @Override
                public void stash(int ordinal, byte[] in, int offset, int length) {
                    super.stash(ordinal, in, offset, length);
                    if (ordinal != 0) // Probably won't happen but you never knob
                        return;

                    // Write back the modified context variable
                    try {
                        Charset encoding = contentType.getEncoding();
                        if (cvk.getOverrideEncoding() != null)
                            encoding = cvk.getOverrideEncoding();
                        final String val = new String(in, offset, length, encoding);
                        if (!val.equals(context.getVariable(variableName)))
                            context.setVariable(variableName, val);
                    } catch (UnsupportedCharsetException e) {
                        throw new RuntimeException(e); // can't happen
                    } catch (NoSuchVariableException e) {
                        throw new RuntimeException(e); // Normally not possible
                    }
                }
            };

            mess.initialize(sm, contentType, new ByteArrayInputStream(initialValue.getBytes(contentType.getEncoding())));
            mess.attachKnob(ContextVariableKnob.class, cvk);
            return mess;
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Examine the specified variable value and attempt to extract a useful value to use as the initial value for
     * a new context-variable-backed Message.
     *
     * @param variableName the name of the variable, for reporting errors.  Required.
     * @param value the value of the variable.
     * @return a String to use an initial value for a context-variable-backed Message.  Never null.
     * @throws NoSuchVariableException if the value is null, or any type other than CharSequence or a non-singleton array or Collection.
     */
    public static String asMessageValueString(String variableName, Object value) throws NoSuchVariableException {
        if (value == null)
            throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is null");

        if (value instanceof CharSequence) {
            return value.toString();
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            switch (collection.size()) {
                case 0:
                    throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is empty collection");
                case 1:
                    return asMessageValueString(variableName, collection.iterator().next());
                default:
                    throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is collection with more than one value");
            }
        } else if (value instanceof Object[]) {
            Object[] objects = (Object[]) value;
            switch (objects.length) {
                case 0:
                    throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is empty array");
                case 1:
                    return asMessageValueString(variableName, objects[0]);
                default:
                    throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is array with more than one value");
            }
        } else {
            throw new NoSuchVariableException(variableName, "Target is OTHER but variable value is unsupported type " + ClassUtils.getClassName(value.getClass()));
        }
    }
}
