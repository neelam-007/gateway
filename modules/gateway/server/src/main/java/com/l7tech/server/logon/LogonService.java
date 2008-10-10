package com.l7tech.server.logon;

import com.l7tech.identity.User;
import com.l7tech.identity.FailAttemptsExceededException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;

/**
 * Interface to the services that are being used for handling functionality that takes place during logon into the system.
 * 
 * User: dlee
 * Date: Jun 27, 2008
 */
public interface LogonService {

    /**
     * Provides the service of updating the logon attempt information made by a particular user.
     *
     * @param user  The user that has attempted to log onto the system.
     * @param ar    The authentication result.  This will be used to determine the update based on the authentication result.
     */
    public void updateLogonAttempt(final User user, final AuthenticationResult ar);

    /**
     * Service of updating the logon attempt information made by a particular user.  It will update the last login
     * timestamp when the updateWithTimestamp field is set to TRUE.
     *
     * @param user  The user that has attempted to login
     * @param ar    Teh authentication result.
     * @param updateWithTimestamp   TRUE if would like to update the login timestamp
     */
    public void updateLogonAttempt(final User user, final AuthenticationResult ar, final boolean updateWithTimestamp);

    /**
     * A pre-login check to see if the user attempting to login has exceeded the number of failure attempts already.
     * It basically tries to stop the processes from going further.
     *
     * @param user  The user that is attempting to log onto the system.
     * @param now   The login time.
     * @throws FailAttemptsExceededException
     */
    public void hookPreLoginCheck(final User user, long now) throws FailAttemptsExceededException;

    /**
     * Update the fail count for the specified user with the current time.  This should be used for when we don't have
     * an administrative account to perform the transactions.
     *
     * @param user  The user who's fail count will be reset back to zero.
     */
    public void resetLogonAttempt(final User user);

    /**
     * Resets the fail count for this particular user to be zero.  It does not change the last login time where as the
     * resetLogonAttempt() does.  In addition, this should only be used when there is an administrative account to perform
     * the transactions.
     *
     * @param user  the user who's  fail count will be rest back to zero
     */
    public void resetLogonFailCount(User user)  throws FindException, UpdateException;
}
