package com.l7tech.policy.assertion;

/**
 * Implemented by {@link Assertion}s that can be configured to require a specific identity or identity provider.
 */
public interface IdentityTargetable {

    /**
     * Get the target identity.
     *
     * @return The target identity (null if not set)
     */
    public IdentityTarget getIdentityTarget();

    /**
     * Set the target identity.
     *
     * @param identityTarget The target identity (may be null)
     */
    public void setIdentityTarget( IdentityTarget identityTarget );

}
