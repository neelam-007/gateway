package com.l7tech.common;

/**
 * Exception caused by version mismatch.
 * String is passed as expected version, and received version.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class VersionException extends Exception {
    /**
     * Standard exception constructor.
     */
    public VersionException(String message) {
        this(message, null, null);
    }

    public VersionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor using a message and a the expected version.
     */
    public VersionException(String message, String expected) {
        this(message, expected, null);
    }

    /**
     * Constructor using a message, the expected version and
     * the received version.
     */
    public VersionException(String message, String expected, String received) {
        super(message);
        this.expected = expected;
        this.received = received;
    }

    /**
     * @return String instance representing the expected
     *         version
     */
    public String getExpectedVersion() {
        return expected;
    }

    /**
     * @return String instance representing the received
     *         version
     */
    public String getReceivedVersion() {
        return received;
    }


    private String expected;
    private String received;
}
