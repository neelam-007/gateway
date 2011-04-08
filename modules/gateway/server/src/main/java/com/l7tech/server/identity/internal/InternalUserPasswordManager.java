/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.identity.internal;

import com.l7tech.identity.internal.InternalUser;

public interface InternalUserPasswordManager {

    /**
     * Check if the user needs a password storage related update. If so make the appropriate updates to the
     * user.                
     *
     * Note: Maintains the invariant that if the digest property is set or configured to be set on the Internal User that it
     * represents the same clear text password that the hashedPassword property represents.
     *
     * @param dbUser user to check and update if necessary.
     * @param clearTextPassword users clear text password as a result of an authentication or password reset request
     * @return true if the user was updated and needs a subsequent change. If this method returns true you MUST update
     * the user, otherwise the logging will be misleading. No need to call if the intent is not to immediately update
     * the user.
     */
    boolean configureUserPasswordHashes(InternalUser dbUser, String clearTextPassword);
}
