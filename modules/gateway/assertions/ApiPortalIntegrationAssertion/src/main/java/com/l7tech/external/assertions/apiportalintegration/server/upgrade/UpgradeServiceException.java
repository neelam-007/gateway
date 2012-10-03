package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

/**
 * Throw if an unexpected error occurs during service upgrade.
 */
public class UpgradeServiceException extends UpgradePortalException {
    public UpgradeServiceException(final String s) {
        super(s);
    }

    public UpgradeServiceException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
