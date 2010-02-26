package com.l7tech.gateway.api;

/**
 * Accessor for PolicyMO that allows update of policy details.
 */
public interface PolicyMOAccessor extends PolicyAccessor<PolicyMO> {

    /**
     * Get the details for the identified policy resource.
     *
     * @param identifier The identifier for the policy.
     * @return The policy resource details
     * @throws AccessorException If an error occurs
     */
    PolicyDetail getPolicyDetail( String identifier ) throws AccessorException;

    /**
     * Set the details for the identified policy resource.
     *
     * @param identifier The identifier for the policy.
     * @param policyDetail The policy resource details
     * @throws AccessorException If an error occurs
     */
    void putPolicyDetail( String identifier, PolicyDetail policyDetail ) throws AccessorException;
}
