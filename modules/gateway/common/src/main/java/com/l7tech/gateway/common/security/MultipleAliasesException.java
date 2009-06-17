package com.l7tech.gateway.common.security;

/**
 * Exception thrown to indicate that more than one alias was found in a keystore.
 * <p/>
 * This exception also carries information about what aliases were actually found.
 */
public class MultipleAliasesException extends Exception {
    private String[] aliases;

    public MultipleAliasesException(String[] aliases) {
        this.aliases = aliases;
    }

    public MultipleAliasesException(String message, String[] aliases) {
        super(message);
        this.aliases = aliases;
    }

    public MultipleAliasesException(String message, Throwable cause, String[] aliases) {
        super(message, cause);
        this.aliases = aliases;
    }

    public MultipleAliasesException(Throwable cause, String[] aliases) {
        super(cause);
        this.aliases = aliases;
    }

    public MultipleAliasesException() {
    }

    public MultipleAliasesException(String message) {
        super(message);
    }

    public MultipleAliasesException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipleAliasesException(Throwable cause) {
        super(cause);
    }

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }
}
