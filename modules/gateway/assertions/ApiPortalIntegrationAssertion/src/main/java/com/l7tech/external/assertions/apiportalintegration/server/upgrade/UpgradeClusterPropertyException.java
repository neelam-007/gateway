package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

/**
 * Throw if an error occurs during cluster property upgrade.
 */
public class UpgradeClusterPropertyException extends UpgradePortalException {
    public UpgradeClusterPropertyException(final String s) {
        super(s);
    }

    public UpgradeClusterPropertyException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
