package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

/**
 * Throw if an unexpected error occurs during key upgrade.
 */
public class UpgradeKeyException extends UpgradePortalException {
    public UpgradeKeyException(final String s) {
        super(s);
    }

    public UpgradeKeyException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
