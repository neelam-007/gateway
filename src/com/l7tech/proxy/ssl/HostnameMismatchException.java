/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

/**
 * Exception thrown during our HandshakeCompletedListener if the SSG hostname we connected to doesn't match up with
 * the one in their certificate.
 *
 * User: mike
 * Date: Sep 15, 2003
 * Time: 3:55:55 PM
 */
public class HostnameMismatchException extends RuntimeException {
    private String whatWasWanted;
    private String whatWeGotInstead;

    public HostnameMismatchException(String whatWasWanted, String whatWeGotInstead) {
        this.whatWasWanted = whatWasWanted;
        this.whatWeGotInstead = whatWeGotInstead;
    }

    public HostnameMismatchException(String whatWasWanted, String whatWeGotInstead, String message) {
        super(message);
        this.whatWasWanted = whatWasWanted;
        this.whatWeGotInstead = whatWeGotInstead;
    }

    public HostnameMismatchException(String whatWasWanted, String whatWeGotInstead, String message, Throwable cause) {
        super(message, cause);
        this.whatWasWanted = whatWasWanted;
        this.whatWeGotInstead = whatWeGotInstead;
    }

    public HostnameMismatchException(String whatWasWanted, String whatWeGotInstead, Throwable cause) {
        super(cause);
        this.whatWasWanted = whatWasWanted;
        this.whatWeGotInstead = whatWeGotInstead;
    }

    public String getWhatWasWanted() {
        return whatWasWanted;
    }

    public String getWhatWeGotInstead() {
        return whatWeGotInstead;
    }
}
