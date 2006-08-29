package com.l7tech.identity;

/**
 * Represents a set of {@link User}s.
 *
 * Memberships are queried and managed using the {@link IdentityAdmin} API rather than through properties of 
 * the Group objects themselves.
 */
public interface Group extends Identity {

    /**
     * Gets a human-readable description for this Group
     * @return a human-readable description for this Group
     */
    String getDescription();

    /**
     * Gets the mutable {@link GroupBean} for this Group.
     * @return the mutable {@link GroupBean} for this Group.
     */
    GroupBean getGroupBean();
}
