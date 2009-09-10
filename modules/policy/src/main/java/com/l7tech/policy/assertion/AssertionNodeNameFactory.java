/**
 * @author darmstrong
 */
package com.l7tech.policy.assertion;

/**
 * Assertions which dynamically generate the name to display in the policy window can provide an implementation of this
 * interface in their meta data. If the Assertion make suse of the DefaultAssertionPolicyNode, then this interface
 * will be used for generating the display name of the assertion for use in the policy window and in validator
 * warning messages.
 *
 */
public interface AssertionNodeNameFactory <AT extends Assertion>{

    /**
     * Get the name to display in the policy window
     *
     * Implementations should not access Assertion state directly from within getAssertionName if this interface is
     * implemented anonymously within Assertion.meta(), as there is no guarnatee that the enclosing instance will be the
     * instance for who this piece of meta data is being retrieved.
     *
     * @param assertion Assertion to generate a name for. Cannot be null
     * @param decorate if true, the implementation can decorate the name with values which are specific to the state
     * of the Assertion and it's location in a policy
     * @return String name to display
     */
    public String getAssertionName(final AT assertion, final boolean decorate);
}
