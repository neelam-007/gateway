package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.ExceptionListener;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A builder to build an instance of {@link SafeXMLDecoder}, this is a convenient method of creating and adding
 * new filters to the decoder.</p>
 */
public final class SafeXMLDecoderBuilder {
    private static final Logger logger = Logger.getLogger(SafeXMLDecoderBuilder.class.getName());
    public static final String PROP_ERRORS_FATAL_BY_DEFAULT = "com.l7tech.util.SafeXMLDecoder.errorsFatalByDefault";

    private InputStream inputStream;
    private ClassFilterBuilder classFilterBuilder;
    private Object owner;
    private ExceptionListener exceptionListener;
    private ClassLoader classLoader;

    /**
     * Create a SafeXMLDecoderBuilder that will build a class filter with the default whitelist.
     *
     * @param inputStream the input stream to decode.  Required.
     */
    public SafeXMLDecoderBuilder(@NotNull final InputStream inputStream) {
        this.inputStream = inputStream;
        classFilterBuilder = new ClassFilterBuilder().allowDefaults();
    }

    /**
     * Create a SafeXMLDecoder that will build a class filter using the specified builder.
     *
     * @param classFilterBuilder a ClassFilterBuilder to use to build the class filter.  Requried.
     * @param inputStream the input stream to decode.  Required.
     */
    public SafeXMLDecoderBuilder(@NotNull ClassFilterBuilder classFilterBuilder, @NotNull final InputStream inputStream) {
        this.inputStream = inputStream;
        this.classFilterBuilder = classFilterBuilder;
    }

    /**
     * Set the owner object for the XML decoder.
     *
     * @param owner the owner, or null.
     * @return this builder
     */
    public SafeXMLDecoderBuilder setOwner(final Object owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Set the exception listener for the XML decoder.
     * If specified, the default exception listener will not be used.
     *
     * @param exceptionListener the exception listener, or null to use the default.
     * @return this builder
     */
    public SafeXMLDecoderBuilder setExceptionListener(@Nullable final ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
        return this;
    }

    /**
     * Set the class loader to use for creating objects from the XML.
     *
     * @param classLoader the class loader, or null to use the default.
     * @return this builder
     */
    public SafeXMLDecoderBuilder setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Add a custom class filter to permit additional classes, constructors, or methods while decoding XML.
     *
     * @param classFilter a class filter to add.  Required.
     * @return this builder
     */
    public SafeXMLDecoderBuilder addClassFilter(@NotNull final ClassFilter classFilter) {
        classFilterBuilder.addClassFilter(classFilter);
        return this;
    }

    /**
     * Build a SafeXMLDecoder using the current configuration.
     *
     * @return a new SafeXMLDecoder instance using the current builder configuration.
     */
    public SafeXMLDecoder build(){
        ExceptionListener exl = exceptionListener != null ? exceptionListener : getDefaultExceptionListener();
        return new SafeXMLDecoder(classFilterBuilder.build(), inputStream, owner, exl, classLoader);
    }

    /**
     * Get the default error listener that will be used if a specific error listener is not specified
     * using {@link #setExceptionListener(java.beans.ExceptionListener)} before {@link #build()} is called.
     *
     * @return the default error listener, if one not specified.
     */
    public static ExceptionListener getDefaultExceptionListener() {
        return SyspropUtil.getBoolean(PROP_ERRORS_FATAL_BY_DEFAULT, false)
            ? getFatalExceptionListener()
            : getLoggingExceptionListener();
    }

    /**
     * Get an exception listener that logs the exception and continues.
     *
     * @return an ExceptionListener that logs the error but takes no further action, allowing decoding to (attempt to) proceed.
     */
    public static ExceptionListener getLoggingExceptionListener() {
        return new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception e) {
                logger.log(Level.WARNING, "Error while decoding XML: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        };
    }

    /**
     * Get an exception listener that rethrows every exception wrapped in a RuntimeException.
     *
     * @return an ExceptionListener that always rethrows every exception.
     */
    public static ExceptionListener getFatalExceptionListener() {
        return new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }
}