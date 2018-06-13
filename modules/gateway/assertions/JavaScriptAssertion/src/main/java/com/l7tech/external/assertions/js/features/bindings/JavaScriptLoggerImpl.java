package com.l7tech.external.assertions.js.features.bindings;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaScriptLoggerImpl implements JavaScriptLogger {

    private static final Logger SCRIPT_LOGGER = Logger.getLogger(JavaScriptLogger.class.getName());

    private final String prefix;

    public JavaScriptLoggerImpl(final String name) {
        this.prefix = name + ": ";
    }

    @Override
    public void log(final String level, final String message) {
        final Level logLevel = Level.parse(level);
        if (SCRIPT_LOGGER.isLoggable(logLevel)) {
            SCRIPT_LOGGER.log(logLevel, prefix + message);
        }
    }
}
