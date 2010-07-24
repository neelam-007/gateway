/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Assertion for limiting request size.
 */
@ProcessesRequest
public class RequestSizeLimit extends Assertion {
    public static final int MIM_SIZE_LIMIT = 1;

    private String limit = "128"; // kbytes
    private boolean entireMessage = true;

    public RequestSizeLimit() {
    }

    /** @return the current size limit, in kilobytes, or a context variable reference. */
    public String getLimit() {
        return limit;
    }

    /** @param limit the new limit in kilobytes, or a context variable reference to a kilobyte value. */
    public void setLimit(String limit) {
        final String errorMsg = validateSizeLimit(limit);
        if (errorMsg != null) throw new IllegalArgumentException(errorMsg);

        this.limit = limit;
    }

    /**
     * Provided for backwards compatability for policies before bug5044 was fixed
     * Previous internal value was bytes, post bug 5044 is kilobyte or context variable reference.
     *
     * @param limit the (request) size limit in bytes
     */
    public void setLimit(long limit) {
        setLimit(String.valueOf(limit/1024));
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"threatProtection"});

        meta.put(SHORT_NAME, "Limit Request Size");
        meta.put(DESCRIPTION, "Enable a size limit for the entire message or just the XML portion of the message.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/MessageLength-16x16.gif");

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.RequestSizeLimitDialogAction");
        meta.put(PROPERTIES_ACTION_NAME, "Request Size Limit Properties");
        return meta;
    }

    public static String validateSizeLimit(String limit) {
        String error = Syntax.validateAtMostOneVariableReference(limit, "size limit");
        if (error == null && ! ValidationUtils.isValidLong(limit, false, MIM_SIZE_LIMIT, Long.MAX_VALUE)) {
            error = "Size limit must be a long value no less than " + MIM_SIZE_LIMIT;
        }
        return error;
    }
}
