package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.PolicyUtil;

/**
 * Abstract add assertion advice that provides pre/post routing notification.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public abstract class AddContextSensitiveAssertionAdvice implements Advice {

    //- PUBLIC

    public void proceed(PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1) {
            throw new IllegalArgumentException();
        }
        Assertion assertion = assertions[0];

        if (isInsertPostRouting(pc)) {
            notifyPostRouting(pc, assertion);
        }
        else {
            notifyPreRouting(pc, assertion);
        }
    }

    //- PROTECTED

    /**
     * Notification that the assertion is being added pre-routing.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param pc        The policy change event
     * @param assertion The assertion being added.
     */
    protected void notifyPreRouting(PolicyChange pc, Assertion assertion)  {
    }

    /**
     * Notification that the assertion is being added post-routing.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param pc        The policy change event
     * @param assertion The assertion being added.
     */
    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) {
    }

    //- PRIVATE

    private boolean isInsertPostRouting(PolicyChange pc) {
        Assertion ass = pc.getParent().asAssertion();
        return PolicyUtil.isLocationPostRouting(ass, pc.getChildLocation());
    }
}
