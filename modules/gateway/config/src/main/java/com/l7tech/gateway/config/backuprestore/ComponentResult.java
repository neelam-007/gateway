/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jul 22, 2009
 * Time: 9:31:06 AM
 */
package com.l7tech.gateway.config.backuprestore;

public class ComponentResult {

    /**
     * Any restore method can result in success, failure or not applicable, if the component does not apply for the
     * given back up image
     * There is no FAILURE case, as when a failure happens, a RestoreException is thrown
     */
    public static enum Result{SUCCESS, NOT_APPLICABLE}

    private final String notApplicableMessage;
    private final Result result;

    public ComponentResult(final Result result, final String notApplicableMessage) {
        this.notApplicableMessage = notApplicableMessage;
        this.result = result;
    }

    public ComponentResult(Result result) {
        this.result = result;
        notApplicableMessage = null;
    }

    public String getNotApplicableMessage() {
        return notApplicableMessage;
    }

    public Result getResult() {
        return result;
    }
}
