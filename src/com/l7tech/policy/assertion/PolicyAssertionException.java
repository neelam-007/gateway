/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * Something is wrong in the policy.
 *
 * @author alex
 */
public class PolicyAssertionException extends Exception {
    private final Assertion assertion;
    private final int messageId;
    private final String[] messageArgs;

    public PolicyAssertionException(Assertion ass) {
        super();
        this.assertion = ass;
        this.messageId = 0;
        this.messageArgs = null;
    }

    public PolicyAssertionException(Assertion ass, String message) {
        super( message );
        this.assertion = ass;
        this.messageId = 0;
        this.messageArgs = null;
    }

    public PolicyAssertionException(Assertion ass, Throwable cause) {
        super( cause );
        this.assertion = ass;
        this.messageId = 0;
        this.messageArgs = null;
    }

    public PolicyAssertionException(Assertion ass, String message, Throwable cause) {
        super( message, cause );
        this.assertion = ass;
        this.messageId = 0;
        this.messageArgs = null;
    }

    public PolicyAssertionException(Assertion ass, String message, int messageId, String[] args) {
        super( message );
        this.assertion = ass;
        this.messageId = messageId;
        this.messageArgs = new String[args!=null ? args.length : 0];
        if (args != null) {
            System.arraycopy(args, 0, this.messageArgs, 0, args.length);
        }
    }

    /**
     * Get the message id for the exception if any.
     *
     * @return The message id or 0 if not set.
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Get the message args.
     *
     * @return The message arguments or null.
     */
    public String[] getMessageArgs() {
        return messageArgs;
    }

    /**
     * Get the assertion that threw.
     *
     * @return The assertion or null if not known. 
     */
    public Assertion getAssertion() {
        return assertion;
    }
}
