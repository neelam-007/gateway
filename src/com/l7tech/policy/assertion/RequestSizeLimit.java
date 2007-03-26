/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

/**
 * Assertion for limiting request size.
 */
public class RequestSizeLimit extends Assertion {
    private long limit = 128 * 1024;
    private boolean entireMessage = true;

    public RequestSizeLimit() {
    }

    /** @return the current size limit, in bytes.  Always positive. */
    public long getLimit() {
        return limit;
    }

    /** @param limit the new limit in bytes.  Must be positive. */
    public void setLimit(long limit) {
        if (limit < 1)
            throw new IllegalArgumentException("Invalid size limit");
        this.limit = limit;
    }

    /**
     * @return true if this limit applies to the entire message including attachments.
     * Otherwise it applies only to the XML part.
     */
    public boolean isEntireMessage() {
        return entireMessage;
    }

    /**
     * @param entireMessage true if this limit should apply to the entire message (including attachments.
     * Otherwise it applies only to the XML part.
     */
    public void setEntireMessage(boolean entireMessage) {
        this.entireMessage = entireMessage;
    }
}
