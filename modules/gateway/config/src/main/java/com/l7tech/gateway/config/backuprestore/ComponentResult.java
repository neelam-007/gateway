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
     * There is no FAILURE case, as when a failure happens, an Exception is thrown
     */
    public static enum Result{SUCCESS, NOT_APPLICABLE}

    private final String notApplicableMessage;
    private final Result result;
    private final boolean restartMaybeRequired;

    public ComponentResult(final Result result, final String notApplicableMessage) {
        this.notApplicableMessage = notApplicableMessage;
        this.result = result;
        this.restartMaybeRequired = false;
    }

    public ComponentResult(final Result result, final String notApplicableMessage, boolean restartMaybeRequired) {
        this.notApplicableMessage = notApplicableMessage;
        this.result = result;
        this.restartMaybeRequired = restartMaybeRequired;
    }

    public ComponentResult(Result result) {
        this.result = result;
        this.restartMaybeRequired = false;
        notApplicableMessage = null;
    }

    public ComponentResult(Result result, boolean restartMaybeRequired) {
        this.result = result;
        this.restartMaybeRequired = restartMaybeRequired;
        notApplicableMessage = null;
    }

    public String getNotApplicableMessage() {
        return notApplicableMessage;
    }

    public Result getResult() {
        return result;
    }

    public boolean isRestartMaybeRequired() {
        return restartMaybeRequired;
    }
}
