package com.l7tech.internal.signer.command;

/**
 * Indicates an error while executing a command.
 */
@SuppressWarnings("UnusedDeclaration")
public class CommandException extends Exception {

    private final int exitCode;

    public CommandException(final int exitCode, final String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public CommandException(final int exitCode, final Throwable t) {
        super(t);
        this.exitCode = exitCode;
    }

    public CommandException(final int exitCode, final String message, final Throwable t) {
        super(message, t);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
