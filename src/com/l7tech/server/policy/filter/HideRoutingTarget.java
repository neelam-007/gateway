package com.l7tech.server.policy.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import java.util.Iterator;

/**
 * Removes all routing assertions.
 *
 * If, as a result, a composite is left with no children, this composite assertion is also removed from it's parent
 * except if it's the root assertion in the tree.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 14, 2003<br/>
 * $Id$
 */
public class HideRoutingTarget extends Filter {
    public Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException {
        if (assertionTree == null) return null;
        applyRules(assertionTree, null);
        return assertionTree;
    }

    public HideRoutingTarget() {
        super();
    }

    /**
     * returns true if one or more assertion was deleted amoungs the siblings of this assertion
     */
    private boolean applyRules(Assertion arg, Iterator parentIterator) throws FilteringException {
        // apply rules on this one
        if (arg instanceof RoutingAssertion) {
            if (parentIterator == null) {
                throw new RuntimeException("Invalid policy, all policies must have a composite assertion at the root");
            }
            parentIterator.remove();
            return true;
        } else if (arg instanceof CompositeAssertion) {
            // apply rules to children
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                applyRules(kid, i);
            }
            // if all children of this composite were removed, we have to remove it from it's parent
            if (root.getChildren().isEmpty() && parentIterator != null) {
                parentIterator.remove();
                return true;
            }
        }
        return false;
    }
}
