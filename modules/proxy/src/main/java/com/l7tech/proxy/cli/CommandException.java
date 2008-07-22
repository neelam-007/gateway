/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

/**
 * Exception thrown when an illegal, meaningless, or invalid command is entered.
 */
class CommandException extends Exception {
    public CommandException() {
    }

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandException(Throwable cause) {
        super(cause);
    }
}
