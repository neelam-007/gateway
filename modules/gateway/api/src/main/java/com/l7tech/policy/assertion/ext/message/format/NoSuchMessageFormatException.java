package com.l7tech.policy.assertion.ext.message.format;

/**
 * Thrown when the requested message format is not found in the registry.
 * The exception can be thrown if either the representation class is not registered or
 * there if no format with the requested name.
 */
public class NoSuchMessageFormatException extends Exception {
    private static final long serialVersionUID = -4827522716470467075L;

    private final Class formatClass;
    private final String formatName;

    public NoSuchMessageFormatException(final Class formatClass) {
        this(formatClass, "Message format for representation class [" + formatClass.getSimpleName() + "] doesn't exists.");
    }
    public NoSuchMessageFormatException(final Class formatClass, final String message) {
        super(message);
        this.formatClass = formatClass;
        this.formatName = null;
    }

    public NoSuchMessageFormatException(final String formatName) {
        this(formatName, "Message format for name [" + formatName + "] doesn't exists.");
    }
    public NoSuchMessageFormatException(final String formatName, final String message) {
        super(message);
        this.formatClass = null;
        this.formatName = formatName;
    }

    public Class getFormatClass() {
        return formatClass;
    }

    public String getFormatName() {
        return formatName;
    }
}
