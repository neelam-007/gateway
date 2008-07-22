package com.l7tech.console.tree.policy.advice;

import java.util.Iterator;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;

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
        if (ass instanceof AllAssertion) {
            AllAssertion parent = (AllAssertion)ass;
            Iterator i = parent.children();
            int pos = 0;
            while (i.hasNext()) {
                Assertion child = (Assertion)i.next();
                if (pos < pc.getChildLocation()) {
                    if (child instanceof RoutingAssertion) {
                        return true;
                    }
                }
                pos++;
            }
        }
        Assertion previous = ass;
        ass = ass.getParent();
        while (ass != null) {
            if (ass instanceof AllAssertion) {
                AllAssertion parent = (AllAssertion)ass;
                Iterator i = parent.children();
                while (i.hasNext()) {
                    Assertion child = (Assertion)i.next();
                    System.out.println(child.getClass().getName());
                    if (child instanceof RoutingAssertion) {
                        return true;
                    }
                    if (child == previous) break;
                }
            }
            previous = ass;
            ass = ass.getParent();
        }
        return false;
    }
}
