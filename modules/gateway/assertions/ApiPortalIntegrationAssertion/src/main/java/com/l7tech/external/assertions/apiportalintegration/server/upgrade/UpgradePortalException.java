package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

import java.io.Serializable;

/**
 * Throw if an unexpected error occurs during upgrade.
 */
public class UpgradePortalException extends RuntimeException implements Serializable {
    public UpgradePortalException(final String s) {
        super(s);
    }

    public UpgradePortalException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
