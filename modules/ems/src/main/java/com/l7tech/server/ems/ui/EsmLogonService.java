package com.l7tech.server.ems.ui;

import com.l7tech.server.logon.LogonService;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;

/**
 * ESM implementation for LogonService
 */
public final class EsmLogonService implements LogonService {

    /**
     * This implementation does nothing.
     *
     * @param user The user to check
     * @param time The login attempt timestamp
     */
    @Override
    public void hookPreLoginCheck( final User user, final long time ) {
    }

    /**
     * This implementation does nothing.
     *
     * @param user The user to reset
     */
    @Override
    public void resetLogonAttempt( final User user ) {
    }

    /**
     * This implementation does nothing.
     *
     * @param user The user to reset
     */
    @Override
    public void resetLogonFailCount( final User user) throws FindException, UpdateException {
    }

    /**
     * This implementation does nothing.
     *
     * @param user The user to update
     * @param ar The update context
     */
    @Override
    public void updateLogonAttempt( final User user, final AuthenticationResult ar ) {
    }

    /**
     * This implementation does nothing.
     *
     * @param user The user to reset
     * @param ar The update context
     * @param updateWithTimestamp True to update timestamp
     */
    @Override
    public void updateLogonAttempt( final User user, final AuthenticationResult ar, final boolean updateWithTimestamp ) {
    }
}
