/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.upgrade;

/**
 * Exception thrown when an upgrade task has failed (or could not be performed), and that particular task's
 * transaction should be rolled back, but the Gateway can attempt to continue starting.
 */
public class NonfatalUpgradeException extends Exception {
    public NonfatalUpgradeException() {
    }

    public NonfatalUpgradeException(String message) {
        super(message);
    }

    public NonfatalUpgradeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonfatalUpgradeException(Throwable cause) {
        super(cause);
    }
}
