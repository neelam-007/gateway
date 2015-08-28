package com.l7tech.objectmodel.mqtt;

/**
 * This is an MQTT exception. It is thrown when there is an error with mqtt.
 */
public class MQTTException extends Exception {
    private int returnCode = -1;

    public MQTTException(String message) {
        super(message);
    }

    public MQTTException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new MQTT exception
     *
     * @param message    The message
     * @param returnCode The return code from an mqtt connack message
     */
    public MQTTException(String message, int returnCode) {
        super(message);
        this.returnCode = returnCode;
    }

    /**
     * Creates a new MQTT exception
     *
     * @param message    The message
     * @param returnCode The return code from an mqtt connack message
     * @param cause      The cause of the exception
     */
    public MQTTException(String message, int returnCode, Throwable cause) {
        super(message, cause);
        this.returnCode = returnCode;
    }

    /**
     * Returns the return code of a connack message
     *
     * @return The return code from a connack message or -1 if there was no return code.
     */
    public int getReturnCode() {
        return returnCode;
    }
}
