package com.l7tech.server.admin;

import com.l7tech.identity.ValidationException;

import java.security.Principal;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 17, 2008
 * Time: 12:39:55 PM
 * SessionValidator's validate method can be used to determine if a User's
 * session is still valid.
 * A User's session would no longer be valid if their Identity Provider
 * was deleted, their User account was deleted or if they were disabled in
 * their LDAP
 *
 */
public interface SessionValidator {

    /**
     * validate that the User represented by Principal p is still a valid User
     * The principal should be castable to a {@link com.l7tech.identity.User}.
     * The implementation should contact the User's IdentityProvider to validate
     * that the User still exists and that the User has not been disabled. This also
     * validates that the IdentityProvider has not been removed.
     * All Principals that we want to be able to associate with p's {@link javax.security.auth.Subject}
     * should be obtained and made availabled in the returned Set.
     *
     * The implementation should Cache the Set<Principal> for this user so that there is as little
     * runtime overhead as possible as validate will be called for most SSM interactions with the SSG.
     *
     * The intent of the returned Set<Principal> is that the Subject into which the supplied
     * Principal will eventually be put will also have this set added to it, so that it is available
     * anywhere the Subject is accessible in the SSG.
     *
     * Most prinipals should only exist only once for a subject. The Set<Principal> returned should only contain
     * one Principal of the same type. E.g. There should be only one GroupPrincipal in the returned Set ever.
     *
     * @param p The Principal which represents the User who we need to validate.
     *
     * @return Set<Principal> Each Principal in the Set represents something we want
     * to associate with the User represented by p. Initially this Set will contain
     * a Principal representing the User's group membership which will be used by the. This Set
     * will NOT contain the User Principal
     * {@link com.l7tech.server.admin.AdminSessionManager}
     *
     * */
    public Set<Principal> validate(Principal p) throws ValidationException;
}
