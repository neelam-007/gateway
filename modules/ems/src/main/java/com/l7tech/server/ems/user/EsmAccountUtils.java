package com.l7tech.server.ems.user;

import com.l7tech.objectmodel.InvalidPasswordException;

/**
 *
 */
public class EsmAccountUtils {

    /**
     * Validate an ESM user password.
     *
     * @param password The password to validate
     * @throws InvalidPasswordException If the password is invalid
     */
    public static void validateEsmPassword( final String password ) throws InvalidPasswordException {
        if (password == null) throw new InvalidPasswordException("Empty password is not valid", null);
        if (password.length() < 6) throw new InvalidPasswordException("Password must be no shorter than 6 characters", null);
        if (password.length() > 32) throw new InvalidPasswordException("Password must be no longer than 32 characters", null);
    }
}
