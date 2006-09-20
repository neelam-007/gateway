/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.upgrade;

/**
 * Exception thrown when an upgrade task has failed, and the upgrade task's transaction should be rolled back,
 * and this failure is not ignorable and the Gateway should not attempt to continue startup.
 */
public class FatalUpgradeException extends Exception {
    public FatalUpgradeException() {
    }

    public FatalUpgradeException(String message) {
        super(message);
    }

    public FatalUpgradeException(String message, Throwable cause) {
        super(message, cause);
    }

    public FatalUpgradeException(Throwable cause) {
        super(cause);
    }
}
