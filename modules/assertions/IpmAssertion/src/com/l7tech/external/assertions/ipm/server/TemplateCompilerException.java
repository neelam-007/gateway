package com.l7tech.external.assertions.ipm.server;

/**
 * Exception thrown if there is a problem compiling an IPM tempalte.
 */
public class TemplateCompilerException extends Exception {
    public TemplateCompilerException() {
    }

    public TemplateCompilerException(String message) {
        super(message);
    }

    public TemplateCompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    public TemplateCompilerException(Throwable cause) {
        super(cause);
    }
}
