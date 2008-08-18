package com.l7tech.server.admin;

import com.l7tech.identity.ValidationException;

import java.security.Principal;
import java.util.Set;

/**
 * Validation interface for administrative user sessions.
 *
 * <p>SessionValidator's validate method can be used to determine if a User's
 * session is still valid.</p>
 *
 * <p>A User's session would no longer be valid if their Identity Provider
 * was deleted, their User account was deleted or if they were disabled in
 * their LDAP.</p>
 */
public interface SessionValidator {

    /**
     * Validate that the User represented by Principal p is still a valid User
     *
     * <p>The principal should be castable to a {@link com.l7tech.identity.User}.
     * The implementation should contact the User's IdentityProvider to validate
     * that the User still exists and that the User has not been disabled. This also
     * validates that the IdentityProvider has not been removed.
     * All Principals that we want to be able to associate with p's {@link javax.security.auth.Subject}
     * should be obtained and made availabled in the returned Set.</p>
     *
     * <p>The implementation should Cache the Set&lt;Principal> for this user so that there is as little
     * runtime overhead as possible as validate will be called for most SSM interactions with the SSG.</p>
     *
     * <p>The intent of the returned Set&lt;Principal> is that the Subject into which the supplied
     * Principal will eventually be put will also have this set added to it, so that it is available
     * anywhere the Subject is accessible in the SSG.</p>
     *
     * <p>Most prinipals should only exist only once for a subject. The Set&lt;Principal> returned should only contain
     * one Principal of the same type. E.g. There should be only one GroupPrincipal in the returned Set ever.</p>
     *
     * @param p The Principal which represents the User who we need to validate.
     *
     * @return Set<Principal> Each Principal in the Set represents something we want
     * to associate with the User represented by p. Initially this Set will contain
     * a Principal representing the User's group membership which will be used by the. This Set
     * will NOT contain the User Principal
     * {@link com.l7tech.server.admin.AdminSessionManager}
     *
     * @throws ValidationException If principal validation fails.
     */
    public Set<Principal> validate(Principal p) throws ValidationException;
}
