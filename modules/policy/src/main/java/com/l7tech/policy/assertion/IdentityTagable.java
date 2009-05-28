package com.l7tech.policy.assertion;

/**
 * Implemented by (identity) assertions that can associate a tag with the authenticated identity,
 * so that it can be programatically referenced from other assertions in the policy.
 */
public interface IdentityTagable {

    public void setIdentityTag(String tag);

    public String getIdentityTag();

}
