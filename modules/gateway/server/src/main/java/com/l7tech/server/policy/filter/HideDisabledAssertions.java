package com.l7tech.server.policy.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;

/**
 * A Filter that processes a policy to remove any assertions that are not enabled.
 * This filter should run before the other filters are run so the other filters will
 * not have to worry about disabled assertions.
 */
public class HideDisabledAssertions implements Filter {
    @Override
    public Assertion filter(User policyRequestor, Assertion assertionTree) {
        return Assertion.filterOutDisabledAssertions(assertionTree);
    }
}
