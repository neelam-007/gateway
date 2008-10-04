package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.Iterator;

/**
 * Utility routines for working with policy assertion trees.
 */
public class PolicyUtil {

    /**
     * Check if an ordinal location within a policy is after a routing assertion.
     * <p/>
     * This can be used to check whether an assertion would be post-routing if inserted into a given location,
     * even if there is currently no assertion at that location.
     *
     * @param assertionParent the composite assertion that is the parent of the location you are inspecting.
     * @param indexWithinParent the index within this parent assertion that you are inspecting
     * @return true if there is a RoutingAssertion before the specified index within the specified parent,
     *         or before the specified parent in the policy.
     */
    public static boolean isLocationPostRouting(Assertion assertionParent, int indexWithinParent) {
        if (assertionParent instanceof AllAssertion) {
            AllAssertion parent = (AllAssertion)assertionParent;
            Iterator i = parent.children();
            int pos = 0;
            while (i.hasNext()) {
                Assertion child = (Assertion)i.next();
                if (pos < indexWithinParent) {
                    if (child instanceof RoutingAssertion) {
                        return true;
                    }
                }
                pos++;
            }
        }
        Assertion previous = assertionParent;
        assertionParent = assertionParent.getParent();
        while (assertionParent != null) {
            if (assertionParent instanceof AllAssertion) {
                AllAssertion parent = (AllAssertion)assertionParent;
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
            previous = assertionParent;
            assertionParent = assertionParent.getParent();
        }
        return false;
    }

    /**
     * Check if an assertion within a policy is after a routing assertion.
     *
     * @param assertion the assertion to check.  If null, or if it has a null parent, this method always returns false.
     * @return true if there is a routing assertion before this assertion in its owning policy.
     */
    public static boolean isAssertionPostRouting(Assertion assertion) {
        if (assertion == null)
            return false;
        final CompositeAssertion parent = assertion.getParent();
        return parent != null && isLocationPostRouting(parent, assertion.getOrdinal() - parent.getOrdinal());
    }
}
