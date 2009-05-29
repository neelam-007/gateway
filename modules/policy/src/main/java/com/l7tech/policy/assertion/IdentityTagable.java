package com.l7tech.policy.assertion;

/**
 * Implemented by (identity) assertions that can associate a tag with the authenticated identity,
 * so that it can be programatically referenced from other assertions in the policy.
 */
public interface IdentityTagable {

    /**
     * Get the identity tag.
     *
     * @return The tag or null if not set.
     */
    String getIdentityTag();

    /**
     * Set the identity tag.
     *
     * @param tag The tag to use, or null for no tag.
     */
    void setIdentityTag(String tag);

}
