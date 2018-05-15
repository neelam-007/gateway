package com.l7tech.external.assertions.js.features;

/**
 * Logger for JavaScript in execution.
 */
public interface JavaScriptLogger {

    /**
     * Logs the message at the specified level.
     * @param level Represents common Level at which message can be logged.
     * @param message Text to be logged.
     */
    void log(String level, String message);

}
