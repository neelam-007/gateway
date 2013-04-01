package com.l7tech.external.assertions.ssh;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.DefaultAssertionAdvice;
import com.l7tech.policy.assertion.Assertion;

/**
 * This is used to make it so that the Validate Servers host key checkbox is automatically selected.
 *
 * @author Victor Kazakov
 */
public class SshRouteAssertionAdvice extends DefaultAssertionAdvice<SshRouteAssertion> {
    @Override
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();

        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SshRouteAssertion)) {
            throw new IllegalArgumentException("Attempting to execute SshRouteAssertionAdvice without being given a single SshRouteAssertion");
        }

        SshRouteAssertion subject = (SshRouteAssertion) assertions[0];

        // Set the setUsePublicKey switch to true for new assertions
        subject.setUsePublicKey(true);
        subject.setSshPublicKey("");

        super.proceed(pc);
    }
}
