package com.l7tech.policy.assertion;

import com.l7tech.policy.Policy;

/**
 * Contains a reference to a policy fragment.
 */
public interface PolicyReference {
    public Policy retrieveFragmentPolicy();

    public void replaceFragmentPolicy(Policy policy);

    public String retrievePolicyGuid();
}
