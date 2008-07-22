/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

/**
 * Exception thrown when an asked-for MIME multipart part does not exist.
 */
public class NoSuchPartException extends Exception {
    private final String cid;
    private final int ordinal;

    public NoSuchPartException() {
        this(null, null, -1, null);
    }

    public NoSuchPartException(String message) {
        this(message, null, -1, null);
    }

    public NoSuchPartException(String message, String cid) {
        this(message, cid, -1, null);
    }

    public NoSuchPartException(String message, int ordinal) {
        this(message, null, ordinal, null);
    }

    public NoSuchPartException(Throwable cause) {
        this(null, null, -1, cause);
    }

    public NoSuchPartException(String message, Throwable cause) {
        this(message, null, -1, cause);
    }

    public NoSuchPartException(String message, String cid, int ordinal, Throwable cause) {
        super(message, cause);
        this.cid = cid;
        this.ordinal = ordinal;
    }

    /** @return the MIME content ID that wasn't found, or null if that wasn't the problem. */
    public String getCid() {
        return cid;
    }

    /** @return the ordinal that wasn't found, or -1 if that wasn't the problem. */
    public int getOrdinal() {
        return ordinal;
    }

    /** @return a String in one of the following formats:
     *              ""
     *              "in position #3 (counting from zero) "
     *              "with Content-ID: mumble "
     */
    public String getWhatWasMissing() {
        if (cid != null)
            return "with Content-ID <" + cid + "> ";
        else if (ordinal >= 0)
            return "in position #" + ordinal + " (counting from zero) ";
        else
            return "";
    }
}
